#!/usr/bin/env python3
"""Packaging regressions using synthetic fixtures; no native build or signing."""

from datetime import datetime, timedelta, timezone
import json
from pathlib import Path
import plistlib
import stat
import subprocess
import tempfile
import unittest
from unittest.mock import patch

import macos_release as release


class MacPreparationTest(unittest.TestCase):
    def test_runtime_identifiers_are_valid_and_distinct_for_both_architectures(self):
        identifiers = [release.runtime_bundle_id("de.appfabrik.garnbox", arch)
                       for arch in release.ARCHITECTURES]
        self.assertEqual(2, len(set(identifiers)))
        for identifier in identifiers:
            self.assertRegex(identifier, r"^de\.appfabrik\.garnbox\.runtime\.[A-Za-z0-9.-]+$")
        self.assertEqual("de.appfabrik.garnbox.runtime.x86-64", identifiers[1])

    def test_inspection_rejects_legacy_underscore_in_nested_bundle_identifier(self):
        with tempfile.TemporaryDirectory() as directory:
            app = Path(directory) / "Yarn Box.app"
            contents = app / "Contents/PlugIns/Runtime-x86_64.bundle/Contents"
            contents.mkdir(parents=True)
            info = contents / "Info.plist"
            info.write_bytes(plistlib.dumps({"CFBundleIdentifier":
                                            "de.appfabrik.garnbox.runtime.x86_64"}))
            with self.assertRaisesRegex(ValueError, "Invalid bundle identifier"):
                release.inspect_bundle_resources(app)
            info.write_bytes(plistlib.dumps({"CFBundleIdentifier":
                                            "de.appfabrik.garnbox.runtime.x86-64"}))
            release.inspect_bundle_resources(app)

    def test_inspection_rejects_private_files_and_untraversable_directories(self):
        with tempfile.TemporaryDirectory() as directory:
            app = Path(directory) / "Yarn Box.app"
            contents = app / "Contents"
            contents.mkdir(parents=True)
            profile = contents / "embedded.provisionprofile"
            profile.write_bytes(b"profile")
            profile.chmod(0o600)
            with self.assertRaisesRegex(ValueError, "not readable by all users"):
                release.inspect_bundle_resources(app)
            profile.chmod(0o644)
            contents.chmod(0o744)
            with self.assertRaisesRegex(ValueError, "not readable by all users"):
                release.inspect_bundle_resources(app)
            contents.chmod(0o755)
            release.inspect_bundle_resources(app)

    def test_embedding_private_profile_preserves_source_permissions(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            app = root / "Yarn Box.app"
            (app / "Contents").mkdir(parents=True)
            source = root / "private.provisionprofile"
            source.write_bytes(b"original signed profile")
            source.chmod(0o600)
            config = root / "signing.json"
            config.write_text(json.dumps({"teamId": "ABCDEFGHIJ", "applicationIdentity": "test",
                                         "appStoreConnectBundleId": "de.appfabrik.garnbox",
                                         "provisioningProfile": str(source)}))
            decoded = {"ExpirationDate": datetime.now(timezone.utc) + timedelta(days=1),
                       "TeamIdentifier": ["ABCDEFGHIJ"], "Entitlements": {
                           "com.apple.application-identifier": "ABCDEFGHIJ.de.appfabrik.garnbox"}}
            with patch.object(release, "settings", return_value={"bundleId": "de.appfabrik.garnbox"}), \
                    patch.object(release, "run", return_value=subprocess.CompletedProcess(
                        [], 0, stdout=plistlib.dumps(decoded))):
                release.signing_config(config, app)
            embedded = app / "Contents/embedded.provisionprofile"
            self.assertEqual(source.read_bytes(), embedded.read_bytes())
            self.assertEqual(0o600, stat.S_IMODE(source.stat().st_mode))
            self.assertEqual(0o644, stat.S_IMODE(embedded.stat().st_mode))
            release.inspect_bundle_resources(app)


if __name__ == "__main__":
    unittest.main()
