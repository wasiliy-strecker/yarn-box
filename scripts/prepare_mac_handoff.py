#!/usr/bin/env python3
"""Assemble the German Yarn Box Mac handoff from committed sources, without tests."""
import argparse
from datetime import datetime, timezone
import json
from pathlib import Path
import shutil
import struct
import subprocess

from export_privacy import export_privacy, privacy_url
import macos_release as mac
from runtime_sources import acquire_sources

ROOT = Path(__file__).resolve().parents[1]
IMAGES = ("01-overview.png", "02-edit.png", "03-search.png", "04-json-export.png")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--inputs", type=Path, required=True)
    parser.add_argument("--captures", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    if subprocess.check_output(["git", "diff", "HEAD", "--name-only"], cwd=ROOT).strip():
        raise ValueError("Commit the statically checked sources before creating the handoff")
    mac.check_inputs(args.inputs.resolve())
    capture = mac.read_json(args.captures / "capture-manifest.json")
    if (capture.get("sourceDigest") != mac.source_digest() or capture.get("platform") != "Linux"
            or capture.get("previewOnly") is not True or capture.get("viewport") != [2560, 1600]
            or capture.get("locale") != "de-DE" or capture.get("testsExecuted") is not False):
        raise ValueError("Drafts must match the source and be explicitly marked as Linux previews")
    static = mac.read_json(ROOT / "target/mac-preparation-static.json")
    static_log = ROOT / "target/mac-preparation-static.log"
    if (static.get("sourceDigest") != mac.source_digest() or static.get("testsExecuted") is not False
            or static.get("logSha256") != mac.sha256(static_log)
            or "BUILD SUCCESS" not in static_log.read_text()
            or "Tests are skipped." not in static_log.read_text()):
        raise ValueError("Fresh successful static build with tests skipped is required")
    output = args.output.resolve()
    output.mkdir(parents=True, exist_ok=False)
    revision = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=ROOT, text=True).strip()
    source = output / "Source"
    source.mkdir()
    names = subprocess.check_output(["git", "ls-files", "-z"], cwd=ROOT).decode().split("\0")
    for name in filter(None, names):
        if name.endswith((".local.json", ".p12", ".pfx", ".provisionprofile")):
            raise ValueError("Private Apple signing material cannot be delivered")
        destination = source / name
        destination.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(ROOT / name, destination)
    (source / "source-revision.txt").write_text(revision + "\n")
    shutil.copytree(args.inputs.resolve(), output / "MacInputs")
    shutil.copytree(ROOT / "store-listings/mac-app-store/de-DE", output / "Store/de-DE")
    shutil.copytree(ROOT / "assets/branding", output / "Branding")
    export_privacy(output / "Privacy")
    drafts = output / "Screenshot-Drafts-Linux/de-DE"
    drafts.mkdir(parents=True)
    for name in IMAGES:
        path = args.captures / name
        data = path.read_bytes()
        if (data[:8] != b"\x89PNG\r\n\x1a\n" or struct.unpack(">II", data[16:24]) != (2560, 1600)
                or data[25] != 2 or mac.sha256(path) != capture["images"][name]):
            raise ValueError("RGB draft is missing, changed or has wrong dimensions: " + name)
        shutil.copy2(path, drafts)
    shutil.copy2(args.captures / "capture-manifest.json", drafts.parent)
    (drafts.parent / "ENTWUERFE-NICHT-HOCHLADEN.txt").write_text(
        "Echte Linux-Aufnahmen der aktuellen JavaFX-App mit synthetischen Daten.\n"
        "Bild 4 zeigt den echten Java-Dateinamendialog des Store-Exports.\n"
        "Keine Mac-Abnahme. Auf dem Mac dieselben Ansichten neu aufnehmen.\n")
    (output / "Screenshots-Final-Mac/de-DE").mkdir(parents=True)
    shutil.copy2(ROOT / "docs/mac-app-store-release.md", output / "AUF-DEM-MAC.md")
    shutil.copy2(ROOT / "docs/mac-app-store-fields.md", output / "JETZT-IN-APP-STORE-CONNECT.md")
    acquire_sources(ROOT / "build/releases/runtime-sources/temurin-21.0.13_9-openjfx-21.0.11_4",
                    output / "RuntimeSources")
    for metadata in (ROOT / "packaging/macos").glob("*.tar.gz.json"):
        shutil.copy2(metadata, output / "RuntimeSources")
    verification = output / "Verification"
    verification.mkdir()
    for path in (static_log, ROOT / "target/mac-preparation-static.json",
                 args.captures / "capture-manifest.json", args.captures / "capture.log"):
        shutil.copy2(path, verification)
    report = {"application": mac.settings(), "sourceCommit": revision,
              "sourceDigest": mac.source_digest(), "createdUtc": datetime.now(timezone.utc).isoformat(),
              "localPreparation": "complete", "staticChecks": "passed", "testsExecuted": False,
              "screenshots": "Linux drafts only", "macBuild": False, "macAcceptance": False,
              "signing": False, "upload": False, "publicPrivacyURL": privacy_url(),
              "runtimeSourcePublication": "pending", "storeReady": False}
    (output / "preparation-info.json").write_text(json.dumps(report, indent=2) + "\n")
    (output / "ZUERST-LESEN.txt").write_text(
        "Yarn Box – Mac App Store, Version 1.0, Build 1\n\n"
        "Jetzt: JETZT-IN-APP-STORE-CONNECT.md öffnen und deutsche Feldtexte eintragen.\n"
        "Danach: Den gesamten Ordner auf den Mac kopieren und AUF-DEM-MAC.md folgen.\n"
        "MacInputs enthält Bauinputs für ARM64 und Intel, noch keine fertige Mac-App.\n"
        "Screenshot-Drafts-Linux enthält Entwürfe. Finale Mac-Bilder fehlen.\n"
        "Privacy enthält die englische Erklärung und ihre GitHub-URL. Laufzeitquellen separat prüfen.\n"
        "Statische Prüfungen bestanden. Keine automatisierten Tests ausgeführt.\n"
        "Native Mac-Abnahme durch den Nutzer erforderlich. Kein Upload und kein Push.\n"
        "Es sind ausschließlich erfundene Beispielsammlungen enthalten.\n"
        f"Quellcommit: {revision}\n")
    sums = [f"{mac.sha256(p)}  {p.relative_to(output).as_posix()}\n"
            for p in sorted(output.rglob("*")) if p.is_file()]
    (output / "SHA256SUMS.txt").write_text("".join(sums))
    print(output)


if __name__ == "__main__":
    main()
