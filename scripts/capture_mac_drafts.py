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

import macos_release as mac

ROOT = Path(__file__).resolve().parents[1]


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
    dependencies = (ROOT / "target/gui-classpath.txt").read_text().strip().split(os.pathsep)
    javafx = [p for p in dependencies if "javafx-" in Path(p).name
              and p.endswith(("-linux.jar", "-linux-aarch64.jar"))]
    if len(javafx) != 3:
        raise ValueError("Three native Linux JavaFX modules required")
    classes = ROOT / "target/store-asset-classes"
    classes.mkdir(parents=True, exist_ok=True)
    classpath = os.pathsep.join([str(ROOT / "target/classes"), *dependencies])
    mac.run(home / "bin/javac", "-encoding", "UTF-8", "-cp", classpath, "-d", classes,
            ROOT / "scripts/store_assets/MacStoreDrafts.java")
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
        if mac.source_digest() != digest:
            raise ValueError("Sources changed during capture")
        manifest = {"platform": "Linux", "previewOnly": True, "testsExecuted": False,
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
