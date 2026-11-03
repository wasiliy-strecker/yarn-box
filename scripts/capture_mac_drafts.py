#!/usr/bin/env python3
"""Capture four real Linux views for the German Mac handoff. Does not run tests."""
import argparse
from datetime import datetime, timezone
import json
import os
from pathlib import Path
import select
import shutil
import subprocess
import tempfile
from zipfile import ZipFile

import macos_release as mac

ROOT = Path(__file__).resolve().parents[1]


def existing_runtime():
    """Read existing artifacts only. Never invoke Maven or build the application."""
    app = ROOT / "target/app-image/Yarn Box/lib/app"
    modules = ROOT / "target/javafx-modules"
    jar = ROOT / "target/garnbox-1.0.jar"
    dependencies = sorted(p for p in app.glob("*.jar") if not p.name.startswith("garnbox-"))
    files = [jar] + dependencies + sorted(modules.glob("*.jar"))
    if len(dependencies) != 3 or len(list(modules.glob("*.jar"))) != 3:
        raise ValueError(
            "The existing Yarn Box dependencies and JavaFX modules are required"
        )
    # The older app-image is only a dependency source. Use the verified current JAR.
    compiled = ROOT / "target/classes"
    if not jar.is_file() or not compiled.is_dir():
        raise ValueError(
            "The previously verified application JAR and classes are required"
        )
    with ZipFile(jar) as archive:
        for path in compiled.rglob("*"):
            if (
                path.is_file()
                and archive.read(path.relative_to(compiled).as_posix())
                != path.read_bytes()
            ):
                raise ValueError(
                    "The application JAR differs from the existing compiled output"
                )
        for path in (ROOT / "src/main/resources").rglob("*"):
            if (
                path.is_file()
                and archive.read(
                    path.relative_to(ROOT / "src/main/resources").as_posix()
                )
                != path.read_bytes()
            ):
                raise ValueError("The application JAR has outdated resources")
    if any(
        p.stat().st_mtime > jar.stat().st_mtime
        for p in (ROOT / "src/main/java").rglob("*.java")
    ):
        raise ValueError(
            "Application sources are newer than the existing JAR. No automatic build"
        )
    changed = subprocess.check_output(
        ["git", "diff", "HEAD", "--name-only", "src/main", "pom.xml"],
        cwd=ROOT,
        text=True,
    )
    if changed.strip():
        raise ValueError(
            "App sources changed. This asset workflow does not rebuild them"
        )
    return {path.relative_to(ROOT).as_posix(): mac.sha256(path) for path in files}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    output = args.output.resolve()
    output.mkdir(parents=True, exist_ok=False)
    xvfb = os.environ.get("YARNBOX_XVFB") or shutil.which("Xvfb")
    if not xvfb:
        raise SystemExit("Set YARNBOX_XVFB to Xvfb, or install Xvfb")
    home = mac.java_home()
    runtime = existing_runtime()
    javafx = [str(ROOT / path) for path in runtime if "javafx-modules/" in path]
    classes = output / "helper-classes"
    classes.mkdir()
    classpath = os.pathsep.join(str(ROOT / path) for path in runtime
                                if "javafx-modules/" not in path)
    # Compile only the asset helper. It uses package-private production dialogs.
    with (output / "compile-helper.log").open("w") as log:
        subprocess.run([str(home / "bin/javac"), "-encoding", "UTF-8",
                        "-cp", classpath + os.pathsep + os.pathsep.join(javafx),
                        "-d", str(classes),
                        str(ROOT / "scripts/store_assets/MacStoreDrafts.java")],
                       cwd=ROOT, stdout=log, stderr=subprocess.STDOUT,
                       timeout=60, check=True)
    (output / "runtime-info.json").write_text(
        json.dumps({"appBuildExecuted": False, "files": runtime}, indent=2) + "\n")
    digest = mac.source_digest()
    read_fd, write_fd = os.pipe()
    server = None
    try:
        with (output / "xvfb.log").open("w") as log:
            server = subprocess.Popen([xvfb, "-displayfd", str(write_fd), "-screen", "0",
                                       "2560x1600x24", "-noreset", "-nolisten", "tcp", "-ac"],
                                      pass_fds=(write_fd,), stdout=log, stderr=log)
        os.close(write_fd)
        write_fd = None
        if not select.select([read_fd], [], [], 15)[0]:
            raise RuntimeError("Xvfb did not start")
        display = os.read(read_fd, 64).decode().strip()
        if not display.isdecimal():
            raise RuntimeError("Invalid Xvfb display. See xvfb.log")
        env = os.environ.copy()
        env["DISPLAY"] = ":" + display
        env.pop("WAYLAND_DISPLAY", None)
        with tempfile.TemporaryDirectory(prefix="yarnbox-store-assets-") as temporary:
            data = Path(temporary)
            (data / ".synthetic-yarnbox-demo").write_text("Synthetic asset data only\n")
            command = [str(home / "bin/java"), "-Dprism.order=sw", "-Dglass.gtk.uiScale=2.0",
                       "--module-path", os.pathsep.join(javafx), "--add-modules=javafx.controls",
                       "-cp", str(classes) + os.pathsep + classpath,
                       "de.appfabrik.garnbox.ui.MacStoreDrafts", str(data), str(output)]
            with (output / "capture.log").open("w") as log:
                subprocess.run(command, cwd=ROOT, env=env, stdout=log,
                               stderr=subprocess.STDOUT, timeout=150, check=True)
        if runtime != existing_runtime():
            raise ValueError("Application artifacts changed during capture")
        if mac.source_digest() != digest:
            raise ValueError("Sources changed during capture")
        manifest = {"platform": "Linux", "previewOnly": True, "testsExecuted": False,
                    "appBuildExecuted": False,
                    "runtimeInfoSha256": mac.sha256(output / "runtime-info.json"),
                    "viewport": [2560, 1600], "locale": "de-DE", "sourceDigest": digest,
                    "createdUtc": datetime.now(timezone.utc).isoformat(),
                    "method": "Production JavaFX views on isolated Xvfb with synthetic JSON data",
                    "exportView": "Production Store filename dialog shown directly for illustration",
                    "images": {p.name: mac.sha256(p) for p in sorted(output.glob("*.png"))}}
        (output / "capture-manifest.json").write_text(json.dumps(manifest, indent=2) + "\n")
        print(output)
    finally:
        os.close(read_fd)
        if write_fd is not None:
            os.close(write_fd)
        if server is not None:
            server.terminate()
            try:
                server.wait(timeout=5)
            except subprocess.TimeoutExpired:
                server.kill()
                server.wait()


if __name__ == "__main__":
    main()
