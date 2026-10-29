#!/usr/bin/env python3
"""Exercise the actual Linux jpackage launcher with isolated synthetic data and native input."""

import argparse
import hashlib
import json
import os
import re
import select
import shutil
import sqlite3
import subprocess
import tempfile
import time
from pathlib import Path

from run_gui import ROOT, source_hashes


def file_hashes(folder):
    return {
        str(p.relative_to(folder)): hashlib.sha256(p.read_bytes()).hexdigest()
        for p in sorted(folder.rglob("*"))
        if p.is_file()
    }


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    output = args.output.resolve()
    output.mkdir(parents=True, exist_ok=False)
    package = ROOT / "target/app-image/GarnBox"
    launcher = package / "bin/GarnBox"
    hashes = file_hashes(package)
    sources = source_hashes()
    xvfb = os.environ.get("YARNBOX_XVFB") or shutil.which("Xvfb")
    if not xvfb:
        raise RuntimeError("Install Xvfb or set YARNBOX_XVFB")
    read_fd, write_fd = os.pipe()
    process = None
    with (output / "xvfb.log").open("w") as log:
        server = subprocess.Popen(
            [
                xvfb,
                "-displayfd",
                str(write_fd),
                "-screen",
                "0",
                "1920x1080x24",
                "-noreset",
                "-nolisten",
                "tcp",
                "-ac",
            ],
            pass_fds=(write_fd,),
            stdout=log,
            stderr=log,
        )
    os.close(write_fd)
    try:
        ready, _, _ = select.select([read_fd], [], [], 15)
        if not ready:
            raise RuntimeError("Xvfb did not start")
        display = os.read(read_fd, 64).decode().strip()
        if not display.isdecimal():
            raise RuntimeError("Invalid Xvfb display")
        env = os.environ.copy()
        env["DISPLAY"] = ":" + display
        env.pop("WAYLAND_DISPLAY", None)
        app_env = {
            key: value
            for key, value in env.items()
            if key
            not in (
                "JAVA_HOME",
                "JDK_HOME",
                "CLASSPATH",
                "JAVA_TOOL_OPTIONS",
                "_JAVA_OPTIONS",
                "JDK_JAVA_OPTIONS",
                "LD_LIBRARY_PATH",
            )
        }
        app_env["PATH"] = "/nonexistent"
        checks = []

        def command(*items, **kwargs):
            return subprocess.run(
                items,
                env=env,
                check=True,
                timeout=10,
                capture_output=True,
                text=True,
                **kwargs,
            ).stdout.strip()

        def xdo(*items):
            return command("xdotool", *map(str, items))

        def window(title):
            deadline = time.monotonic() + 15
            while time.monotonic() < deadline:
                try:
                    result = xdo(
                        "search",
                        "--onlyvisible",
                        "--name",
                        "^" + re.escape(title) + "$",
                    )
                    if result:
                        return result.splitlines()[-1]
                except subprocess.CalledProcessError:
                    pass
                time.sleep(0.15)
            raise RuntimeError("Window not found: " + title)

        def key(*keys):
            xdo("key", "--clearmodifiers", *keys)
            time.sleep(0.3)

        def click(win, x, y):
            xdo("mousemove", "--window", win, x, y, "click", "1")
            time.sleep(0.3)

        def paste(value):
            command("xsel", "--clipboard", "--input", input=value)
            key("ctrl+a", "ctrl+v")

        def shot(name):
            time.sleep(0.6)
            command("import", "-window", "root", str(output / name))

        def stop():
            nonlocal process
            if process is not None:
                process.terminate()
                process.wait(timeout=10)
                process = None

        with tempfile.TemporaryDirectory(prefix="garnbox-packaged-") as temporary:
            data = Path(temporary)
            database = data / "garnbox.db"
            (data / "language.txt").write_text("fr-FR\n")
            # Existing schema 1 collection, as produced by the original app.
            with sqlite3.connect(database) as db:
                db.execute(
                    "CREATE TABLE yarns (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, brand TEXT NOT NULL, color TEXT NOT NULL, location TEXT NOT NULL, note TEXT NOT NULL, photo BLOB)"
                )
                db.execute("PRAGMA user_version = 1")
                db.execute(
                    "INSERT INTO yarns VALUES (1, ?, ?, ?, ?, ?, ?)",
                    (
                        "Alpaca demo",
                        "Atelier Mira",
                        "Lilas",
                        "Étagère 2",
                        "Une note de démonstration.",
                        (ROOT / "assets/store_demo/images/sage.png").read_bytes(),
                    ),
                )
                db.executemany(
                    "INSERT INTO yarns(name,brand,color,location,note,photo) VALUES (?, ?, ?, ?, ?, ?)",
                    [
                        (
                            f"Mérinos {i:04}",
                            "Nordfaden",
                            "Sauge",
                            "Boîte A",
                            "Données fictives.",
                            None,
                        )
                        for i in range(1233)
                    ],
                )

            def launch(name):
                nonlocal process
                with (output / name).open("w") as log:
                    process = subprocess.Popen(
                        [str(launcher), "--data-dir=" + str(data)],
                        cwd=data,
                        env=app_env,
                        stdout=log,
                        stderr=log,
                    )
                win = window("GarnBox")
                xdo("windowmove", "--sync", win, 0, 0)
                xdo("windowfocus", "--sync", win)
                time.sleep(1.5)
                return win

            main_window = launch("first-launch.log")
            maps = Path(f"/proc/{process.pid}/maps").read_text()
            expected_jvm = str(package / "lib/runtime/lib/server/libjvm.so")
            if expected_jvm not in maps:
                raise AssertionError("Packaged JVM not loaded")
            checks.append(
                "Actual launcher uses the bundled JVM with no JAVA_HOME and PATH=/nonexistent"
            )
            shot("01-french-count.png")
            key("ctrl+f")
            paste("Alpaca demo")
            click(main_window, 140, 230)
            key("Return")
            editor = window("Modifier le fil")
            click(editor, 190, 153)
            paste("Écheveau café – ação")
            source = data / "replacement.jpg"
            source.write_bytes(
                (ROOT / "assets/store_demo/images/lilac.jpg").read_bytes()
            )
            click(editor, 590, 337)
            chooser = window("Choisir une photo")
            xdo("windowfocus", "--sync", chooser)
            key("ctrl+l")
            xdo("type", "--clearmodifiers", "--delay", 1, str(source))
            key("Return")
            # GTK can first navigate to the file's directory, then accept the file.
            for _ in range(3):
                time.sleep(0.8)
                try:
                    xdo("search", "--onlyvisible", "--name", "^Choisir une photo$")
                except subprocess.CalledProcessError:
                    break
                key("Return")
            else:
                raise AssertionError("Native photo chooser did not accept the file")
            time.sleep(1.5)
            shot("02-native-photo-import.png")
            xdo("windowfocus", "--sync", editor)
            key("ctrl+s")
            expected_photo = source.read_bytes()
            deadline = time.monotonic() + 10
            while True:
                with sqlite3.connect(database) as db:
                    saved = db.execute(
                        "SELECT name, photo FROM yarns WHERE id=1"
                    ).fetchone()
                if saved == ("Écheveau café – ação", expected_photo):
                    break
                if time.monotonic() > deadline:
                    raise AssertionError(
                        "Packaged UI did not save Unicode text and replacement photo"
                    )
                time.sleep(0.2)
            checks.append(
                "Native file picker imports JPEG and the real UI atomically saves Unicode and photo bytes"
            )
            source.unlink()
            click(main_window, 880, 750)
            dialog = window("Langue")
            click(dialog, 190, 160)
            key("Home", "Down", "Down", "Return")
            click(dialog, 470, 220)
            deadline = time.monotonic() + 10
            while (data / "language.txt").read_text().strip() != "en-US":
                if time.monotonic() > deadline:
                    raise AssertionError("Language selection was not persisted")
                time.sleep(0.2)
            stop()
            main_window = launch("restart.log")
            key("ctrl+f")
            paste("Écheveau café")
            click(main_window, 140, 230)
            key("Return")
            window("Edit yarn")
            shot("03-english-restart.png")
            with sqlite3.connect(database) as db:
                assert (
                    db.execute("SELECT name, photo FROM yarns WHERE id=1").fetchone()
                    == saved
                )
                assert db.execute("SELECT count(*) FROM yarns").fetchone()[0] == 1234
            checks.append(
                "Restart applies English, keeps all 1234 entries and opens the saved photo after its source was deleted"
            )
            key("Escape")
            stop()
        if file_hashes(package) != hashes or source_hashes() != sources:
            raise AssertionError("Source or package changed during verification")
        (output / "checks.txt").write_text("\n".join(checks) + "\n")
        (output / "manifest.json").write_text(
            json.dumps(
                {
                    "platform": "Linux",
                    "preview_only": True,
                    "package_sha256": hashes,
                    "source_files_sha256": sources,
                    "manual_review": "Inspect French grouped count, localized dialogs and photo rendering in the three screenshots.",
                },
                indent=2,
                ensure_ascii=False,
            )
            + "\n"
        )
        print("Packaged launcher verification passed", flush=True)
    except BaseException:
        if process is not None:
            subprocess.run(
                ["import", "-window", "root", str(output / "failure.png")],
                env=env,
                timeout=10,
                check=False,
            )
        raise
    finally:
        if process is not None:
            process.terminate()
            process.wait(timeout=10)
        server.terminate()
        server.wait(timeout=10)
        os.close(read_fd)


if __name__ == "__main__":
    main()
