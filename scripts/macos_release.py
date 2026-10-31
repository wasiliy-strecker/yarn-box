#!/usr/bin/env python3
"""Prepare pinned inputs anywhere. Build, inspect and sign a universal Store app on macOS."""

import argparse
from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path
import plistlib
import re
import shutil
import struct
import subprocess
import sys
import tarfile
import tempfile
from urllib.request import Request, urlopen
import xml.etree.ElementTree as ET
import zipfile

ROOT = Path(__file__).resolve().parents[1]
PACKAGING = ROOT / "packaging/macos"
ARCHITECTURES = ("arm64", "x86_64")
MACH_MAGICS = (b"\xcf\xfa\xed\xfe", b"\xfe\xed\xfa\xcf", b"\xca\xfe\xba\xbe", b"\xbe\xba\xfe\xca",
               b"\xca\xfe\xba\xbf", b"\xbf\xba\xfe\xca")


def read_json(path):
    return json.loads(path.read_text(encoding="utf-8"))


def sha256(path):
    with path.open("rb") as stream:
        return hashlib.file_digest(stream, "sha256").hexdigest()


def run(*command, capture=False):
    return subprocess.run([str(part) for part in command], cwd=ROOT, check=True,
                          stdout=subprocess.PIPE if capture else None,
                          stderr=subprocess.PIPE if capture else None)


def settings():
    release = read_json(PACKAGING / "release.json")
    version = ET.parse(ROOT / "pom.xml").findtext("{*}version")
    if release["version"] != version or release["architectures"] != list(ARCHITECTURES):
        raise ValueError("Release version/architectures do not match the project")
    if release["bundleId"] != "de.appfabrik.garnbox":
        raise ValueError("Preserve the established bundle ID and check App Store Connect")
    if not re.fullmatch(r"[1-9][0-9]*", release["build"]):
        raise ValueError("The Apple build number must be positive")
    fields = ROOT / "store-listings/mac-app-store/de-DE"
    for name, maximum in (("name", 30), ("subtitle", 30), ("promotional-text", 170),
                          ("description", 4000), ("keywords", 100), ("review-notes", 4000)):
        value = (fields / (name + ".txt")).read_text().strip()
        length = len(value.encode("utf-8")) if name == "keywords" else len(value)
        if not value or length > maximum:
            raise ValueError("Missing or overlength Store field: " + name)
    if (fields / "name.txt").read_text().strip() != release["displayName"]:
        raise ValueError("Store and application display names differ")
    return release


def source_digest():
    digest = hashlib.sha256()
    files = [ROOT / "pom.xml", ROOT / "README.md"]
    for name in ("src/main", "packaging", "assets", "scripts", "docs", "third-party-licenses", "store-listings/mac-app-store"):
        files.extend(p for p in (ROOT / name).rglob("*")
                     if p.is_file() and not p.name.endswith(".local.json") and "__pycache__" not in p.parts)
    files.extend([Path(__file__), ROOT / "THIRD_PARTY_NOTICES.md"])
    for path in sorted(files):
        digest.update(path.relative_to(ROOT).as_posix().encode())
        digest.update(bytes.fromhex(sha256(path)))
    return digest.hexdigest()


def inventory(folder):
    return {str(p.relative_to(folder)): sha256(p) for p in sorted(folder.rglob("*"))
            if p.is_file() and p.name != "inputs.json"}


def checked_download(entry, cache):
    cache.mkdir(parents=True, exist_ok=True)
    path = cache / entry["file"]
    if not path.exists():
        with tempfile.NamedTemporaryFile(dir=cache, suffix=".part", delete=False) as stream:
            temporary = Path(stream.name)
            try:
                with urlopen(Request(entry["url"], headers={"User-Agent": "YarnBox-release"}),
                             timeout=60) as response:
                    shutil.copyfileobj(response, stream)
                stream.flush()
                if sha256(temporary) != entry["sha256"]:
                    raise ValueError("Downloaded dependency checksum mismatch: " + entry["file"])
                temporary.replace(path)
            finally:
                temporary.unlink(missing_ok=True)
    if sha256(path) != entry["sha256"]:
        raise ValueError("Cached dependency checksum mismatch: " + entry["file"])
    return path


def java_home():
    home = Path(os.environ.get("JAVA_HOME", str(Path.home() / ".local/share/garnbox/jdk-21.0.13+9")))
    release = (home / "release").read_text()
    if 'IMPLEMENTOR="Eclipse Adoptium"' not in release or not re.search(
            r'JAVA_RUNTIME_VERSION="21\.0\.13\+9(?:-LTS)?"', release):
        raise ValueError("JAVA_HOME must point to Eclipse Temurin 21.0.13+9")
    return home


def prepare(output):
    release = settings()
    home = java_home()
    os.environ["JAVA_HOME"] = str(home)
    os.environ["PATH"] = str(home / "bin") + os.pathsep + os.environ["PATH"]
    if output.exists():
        raise ValueError("Existing inputs are never overwritten")
    run(ROOT / "mvnw", "--batch-mode", "--no-transfer-progress", "package", "-Dmaven.test.skip=true")
    output.mkdir(parents=True)
    classpath = output / "classpath"
    classpath.mkdir()
    run(ROOT / "mvnw", "--batch-mode", "--no-transfer-progress", "dependency:copy-dependencies",
        "-DincludeScope=runtime", "-DexcludeGroupIds=org.openjfx", f"-DoutputDirectory={classpath}")
    shutil.copy2(ROOT / f"target/garnbox-{release['version']}.jar", classpath)
    dependencies = output / "dependencies"
    dependencies.mkdir()
    for entry in read_json(PACKAGING / "dependencies.json")["files"]:
        print("Preparing " + entry["file"], flush=True)
        path = checked_download(entry, ROOT / "build/mac-dependency-cache")
        shutil.copy2(path, dependencies / path.name)
    report = {"sourceDigest": source_digest(), "release": release,
              "createdUtc": datetime.now(timezone.utc).isoformat(),
              "nativeMacBuild": False, "files": inventory(output)}
    (output / "inputs.json").write_text(json.dumps(report, indent=2) + "\n")
    check_inputs(output)


def check_inputs(folder):
    report = read_json(folder / "inputs.json")
    if report["release"] != settings() or report["sourceDigest"] != source_digest():
        raise ValueError("Source/configuration changed. Prepare fresh inputs from this source")
    if inventory(folder) != report["files"]:
        raise ValueError("Input files are missing, changed or unexpected")
    for entry in read_json(PACKAGING / "dependencies.json")["files"]:
        path = folder / "dependencies" / entry["file"]
        if sha256(path) != entry["sha256"]:
            raise ValueError("Dependency does not match its pinned source")
        if entry["kind"] == "jdk":
            with tarfile.open(path) as archive:
                matches = [member for member in archive.getmembers() if member.name.endswith("/Contents/Home/bin/java")]
                if len(matches) != 1:
                    raise ValueError("Unexpected Mac JDK archive")
                with archive.extractfile(matches[0]) as stream:
                    if macho_architectures(stream.read(4096)) != {entry["architecture"]}:
                        raise ValueError("JDK architecture differs from its lock entry")
        else:
            with zipfile.ZipFile(path) as archive:
                for name in archive.namelist():
                    if name.endswith(".dylib") and macho_architectures(archive.read(name)) != {entry["architecture"]}:
                        raise ValueError("JavaFX architecture differs from its lock entry")
    print("Pinned inputs and source digest verified. Native Mac acceptance is pending.")


def require_mac():
    if sys.platform != "darwin":
        raise ValueError("This step requires macOS. Use prepare/check on Linux")


def macho_architectures(data):
    if len(data) < 8 or data[:4] not in MACH_MAGICS:
        raise ValueError("Not a supported Mach-O file")
    cpus = {0x1000007: "x86_64", 0x100000c: "arm64"}
    magic = data[:4]
    little = magic in (b"\xcf\xfa\xed\xfe", b"\xbe\xba\xfe\xca", b"\xbf\xba\xfe\xca")
    order = "<" if little else ">"
    value = struct.unpack_from(order + "I", data, 4)[0]
    if magic in MACH_MAGICS[:2]:
        if len(data) < 32 or value not in cpus:
            raise ValueError("Unsupported or truncated Mach-O header")
        return {cpus[value]}
    width = 32 if magic in MACH_MAGICS[4:] else 20
    if not 1 <= value <= 8 or len(data) < 8 + value * width:
        raise ValueError("Truncated or invalid universal Mach-O header")
    result = set()
    for index in range(value):
        cpu = struct.unpack_from(order + "I", data, 8 + index * width)[0]
        if cpu not in cpus or cpus[cpu] in result:
            raise ValueError("Unsupported or duplicate architecture in universal executable")
        result.add(cpus[cpu])
    return result


def assert_architecture(path, expected):
    with path.open("rb") as stream:
        actual = macho_architectures(stream.read(4096))
    if actual != set(expected):
        raise ValueError(f"Wrong Mach-O architecture: {path.name}: {actual}")


def strip_native_jar(source, destination, native, architecture, sqlite=False):
    found = 0
    with zipfile.ZipFile(source) as archive, zipfile.ZipFile(destination, "w", zipfile.ZIP_DEFLATED) as clean:
        for entry in archive.infolist():
            name = entry.filename
            if name.endswith(".dylib"):
                if sqlite:
                    segment = "aarch64" if architecture == "arm64" else "x86_64"
                    if f"/Mac/{segment}/" not in name:
                        continue
                target = native / Path(name).name
                if target.exists():
                    raise ValueError("Duplicate bundled native library: " + target.name)
                target.write_bytes(archive.read(entry))
                assert_architecture(target, [architecture])
                found += 1
                continue
            if sqlite and name.startswith("org/sqlite/native/"):
                continue
            if name.upper().startswith("META-INF/") and name.upper().endswith((".SF", ".RSA", ".DSA")):
                raise ValueError("Unexpected signed upstream jar. Do not silently remove signatures")
            clean.writestr(entry, archive.read(entry))
    if sqlite and found != 1:
        raise ValueError("Expected one pinned SQLite native library per architecture")


def build(inputs, output):
    require_mac()
    check_inputs(inputs)
    home = java_home()
    release = settings()
    output.mkdir(parents=True, exist_ok=False)
    app = output / (release["displayName"] + ".app")
    contents = app / "Contents"
    for name in ("MacOS", "Resources", "app/classpath", "Resources/legal"):
        (contents / name).mkdir(parents=True, exist_ok=True)
    plist = {"CFBundleName": release["displayName"], "CFBundleDisplayName": release["displayName"],
             "CFBundleIdentifier": release["bundleId"], "CFBundleExecutable": "YarnBox",
             "CFBundlePackageType": "APPL", "CFBundleShortVersionString": release["version"],
             "CFBundleVersion": release["build"], "CFBundleIconFile": "icon.icns",
             "LSMinimumSystemVersion": release["minimumMacOS"],
             "LSApplicationCategoryType": release["category"], "NSHighResolutionCapable": True,
             "CFBundleDevelopmentRegion": "de", "NSHumanReadableCopyright": "2026 Wasiliy Strecker"}
    (contents / "Info.plist").write_bytes(plistlib.dumps(plist))
    shutil.copy2(ROOT / "assets/branding/icon.icns", contents / "Resources/icon.icns")
    shutil.copytree(ROOT / "third-party-licenses", contents / "Resources/legal/licenses")
    shutil.copy2(ROOT / "THIRD_PARTY_NOTICES.md", contents / "Resources/legal")
    shutil.copy2(ROOT / "README.md", contents / "Resources/legal/USER_GUIDE.md")
    lock = read_json(PACKAGING / "dependencies.json")
    sqlite = list((inputs / "classpath").glob("sqlite-jdbc-*.jar"))
    if len(sqlite) != 1:
        raise ValueError("Exactly one SQLite dependency required")
    for jar in (inputs / "classpath").glob("*.jar"):
        if jar != sqlite[0]:
            shutil.copy2(jar, contents / "app/classpath")
    with tempfile.TemporaryDirectory(prefix="yarnbox-mac-") as temporary:
        work = Path(temporary)
        for arch in ARCHITECTURES:
            native = contents / "Frameworks/Native" / arch
            native.mkdir(parents=True)
            modules = contents / "app/javafx" / arch
            modules.mkdir(parents=True)
            for entry in lock["files"]:
                if entry["architecture"] != arch:
                    continue
                source = inputs / "dependencies" / entry["file"]
                if entry["kind"] == "jdk":
                    unpack = work / arch
                    unpack.mkdir()
                    with tarfile.open(source) as archive:
                        archive.extractall(unpack, filter="data")
                    homes = list(unpack.glob("*/Contents/Home"))
                    if len(homes) != 1:
                        raise ValueError("Unexpected pinned Mac JDK archive layout")
                    jdk = homes[0]
                    assert_architecture(jdk / "bin/java", [arch])
                    runtime_bundle = contents / f"PlugIns/Runtime-{arch}.bundle"
                    runtime = runtime_bundle / "Contents/Home"
                    runtime.parent.mkdir(parents=True)
                    # jlink can target the platform of the input JMODs, without executing it.
                    run(home / "bin/jlink", "--module-path", jdk / "jmods", "--add-modules",
                        "java.se,jdk.unsupported,jdk.localedata", "--strip-debug", "--no-man-pages",
                        "--no-header-files", "--output", runtime)
                    if not (runtime / "lib/libjli.dylib").is_file():
                        raise ValueError("The pinned JDK does not match the native launcher layout")
                    runtime_info = {"CFBundleIdentifier": release["bundleId"] + ".runtime." + arch,
                                    "CFBundleName": "Temurin Runtime " + arch,
                                    "CFBundleExecutable": "libjli.dylib",
                                    "CFBundlePackageType": "BNDL", "CFBundleVersion": release["build"]}
                    (runtime.parent / "Info.plist").write_bytes(plistlib.dumps(runtime_info))
                    # A code bundle needs its executable in Contents/MacOS for codesign.
                    # Keep the runtime's own copy at the location expected by JLI.
                    (runtime.parent / "MacOS").mkdir()
                    shutil.copy2(runtime / "lib/libjli.dylib", runtime.parent / "MacOS/libjli.dylib")
                    shutil.copy2(jdk / "release", contents / f"Resources/legal/JAVA-{arch}-release.txt")
                else:
                    strip_native_jar(source, modules / source.name, native, arch)
            stripped = work / (arch + "-sqlite.jar")
            strip_native_jar(sqlite[0], stripped, native, arch, sqlite=True)
            common_sqlite = contents / "app/classpath" / sqlite[0].name
            if common_sqlite.exists() and sha256(common_sqlite) != sha256(stripped):
                raise ValueError("Architecture-neutral SQLite classes differ")
            shutil.copy2(stripped, common_sqlite)
            run("xcrun", "clang", "-fobjc-arc", "-Wall", "-Wextra", "-Werror", "-dynamiclib",
                "-arch", arch, "-mmacosx-version-min=" + release["minimumMacOS"],
                "-I" + str(home / "include"), "-I" + str(home / "include/darwin"),
                "-framework", "Cocoa", "-framework", "UniformTypeIdentifiers",
                PACKAGING / "PhotoPicker.m", "-o", native / "libyarnboxdialogs.dylib")
        run("xcrun", "clang", "-fobjc-arc", "-Wall", "-Wextra", "-Werror",
            "-arch", "arm64", "-arch", "x86_64", "-mmacosx-version-min=" + release["minimumMacOS"],
            "-framework", "Cocoa", PACKAGING / "Launcher.m", "-o", contents / "MacOS/YarnBox")
    (output / "build-report.json").write_text(json.dumps({
        "release": release, "sourceDigest": source_digest(), "signed": False,
        "nativeAcceptance": "pending", "files": inventory(app)}, indent=2) + "\n")
    inspect(app)
    print("Universal app assembled. Signed sandbox tests and Store signing remain pending.")


def is_macho(path):
    if not path.is_file() or path.is_symlink():
        return False
    with path.open("rb") as stream:
        return stream.read(4) in MACH_MAGICS


def inspect(app, signed=False):
    require_mac()
    release = settings()
    contents = app / "Contents"
    info = plistlib.loads((contents / "Info.plist").read_bytes())
    for key, expected in (("CFBundleIdentifier", release["bundleId"]),
                          ("CFBundleShortVersionString", release["version"]),
                          ("CFBundleVersion", release["build"]),
                          ("CFBundleDisplayName", release["displayName"])):
        if info.get(key) != expected:
            raise ValueError("Unexpected application identity: " + key)
    assert_architecture(contents / "MacOS/YarnBox", ARCHITECTURES)
    count = 0
    for arch in ARCHITECTURES:
        native = contents / "Frameworks/Native" / arch
        runtime = contents / f"PlugIns/Runtime-{arch}.bundle/Contents/Home"
        for required in (native / "libsqlitejdbc.dylib", native / "libglass.dylib", native / "libyarnboxdialogs.dylib",
                         runtime / "lib/libjli.dylib", runtime / "lib/server/libjvm.dylib",
                         runtime.parent / "MacOS/libjli.dylib"):
            if not required.is_file():
                raise ValueError("Missing native dependency: " + str(required))
        for path in [*native.rglob("*"), *runtime.parent.rglob("*")]:
            if is_macho(path):
                assert_architecture(path, [arch])
                count += 1
    for jar in (contents / "app").rglob("*.jar"):
        with zipfile.ZipFile(jar) as archive:
            if any(n.endswith((".dylib", ".dll", ".so")) for n in archive.namelist()):
                raise ValueError("Unsigned embedded native code remains in " + jar.name)
    if signed:
        run("codesign", "--verify", "--deep", "--strict", "--verbose=2", app)
        ent = plistlib.loads(run("codesign", "-d", "--entitlements", ":-", app, capture=True).stdout)
        if not ent.get("com.apple.security.app-sandbox") or ent.get("com.apple.security.inherit"):
            raise ValueError("The main application must establish its own sandbox")
        if any(ent.get(key) for key in ("com.apple.security.network.client", "com.apple.security.network.server",
                                       "com.apple.security.cs.disable-library-validation")):
            raise ValueError("Unexpected expanded permissions")
    print(f"Verified bundle identity, universal launcher and {count} native libraries/executables.")


def signing_config(config, app):
    value = read_json(config)
    release = settings()
    if not re.fullmatch(r"[A-Z0-9]{10}", value.get("teamId", "")):
        raise ValueError("Provide the real Apple Team ID in the ignored local configuration")
    if value.get("appStoreConnectBundleId") != release["bundleId"]:
        raise ValueError("App Store Connect and the existing bundle ID must match")
    if not value.get("applicationIdentity"):
        raise ValueError("Application signing identity is missing")
    profile_path = Path(value["provisioningProfile"]).expanduser().resolve()
    profile = plistlib.loads(run("security", "cms", "-D", "-i", profile_path, capture=True).stdout)
    expiration = profile["ExpirationDate"].replace(tzinfo=timezone.utc)
    entitlements = profile["Entitlements"]
    app_id = entitlements.get("com.apple.application-identifier", entitlements.get("application-identifier"))
    if (expiration <= datetime.now(timezone.utc) or value["teamId"] not in profile["TeamIdentifier"]
            or app_id != value["teamId"] + "." + release["bundleId"]):
        raise ValueError("Provisioning profile is expired or belongs to another app/team")
    shutil.copy2(profile_path, app / "Contents/embedded.provisionprofile")
    return value, entitlements


def sign(app, config, mode):
    require_mac()
    inspect(app)
    value, profile_entitlements = signing_config(config, app)
    identity = value["applicationIdentity"]
    if mode == "store" and not identity.startswith(("Apple Distribution:", "3rd Party Mac Developer Application:")):
        raise ValueError("Store signing requires an Apple distribution identity, not Developer ID")
    if mode == "development" and not identity.startswith(("Apple Development:", "Mac Developer:")):
        raise ValueError("Sandbox acceptance requires an Apple development identity")
    with tempfile.TemporaryDirectory(prefix="yarnbox-sign-") as temporary:
        ent = plistlib.loads((PACKAGING / "app.entitlements").read_bytes())
        for key in ("com.apple.application-identifier", "application-identifier", "com.apple.developer.team-identifier"):
            if key in profile_entitlements:
                ent[key] = profile_entitlements[key]
        entitlement_path = Path(temporary) / "app.entitlements"
        entitlement_path.write_bytes(plistlib.dumps(ent))
        for path in sorted(app.rglob("*"), key=lambda p: len(p.parts), reverse=True):
            if is_macho(path) and path != app / "Contents/MacOS/YarnBox":
                arguments = ["codesign", "--force", "--options", "runtime", "--timestamp", "--sign", identity]
                if path.suffix != ".dylib":
                    arguments += ["--entitlements", PACKAGING / "helper.entitlements"]
                run(*arguments, path)
        for runtime in (app / "Contents/PlugIns").glob("*.bundle"):
            run("codesign", "--force", "--options", "runtime", "--timestamp", "--sign", identity, runtime)
        run("codesign", "--force", "--options", "runtime", "--timestamp", "--sign", identity,
            "--entitlements", entitlement_path, app)
    inspect(app, signed=True)
    details = run("codesign", "-dvv", app, capture=True).stderr.decode()
    if "TeamIdentifier=" + value["teamId"] not in details:
        raise ValueError("The resulting signature has an unexpected Apple Team ID")
    (app.parent / "signing-report.json").write_text(json.dumps({
        "mode": mode, "sourceDigest": source_digest(), "nativeAcceptance": "pending",
        "files": inventory(app)}, indent=2) + "\n")


def package(app, config, output, acceptance):
    require_mac()
    inspect(app, signed=True)
    report = read_json(app.parent / "signing-report.json")
    if report["mode"] != "store" or report["files"] != inventory(app):
        raise ValueError("A fresh verified Store signature is required")
    accepted = read_json(acceptance)
    required = ("arm64Execution", "x86_64Execution", "sandboxStorage", "jsonTransfer", "restart", "offlineLaunch",
                "photoSelection", "photoDirectoryAfterRestart", "colorSelection", "dialogCancellation")
    if any(accepted.get(key) is not True for key in required):
        raise ValueError("Record completed Mac acceptance before packaging")
    if accepted.get("sourceDigest") != source_digest() or report.get("sourceDigest") != source_digest():
        raise ValueError("Mac acceptance/signature must match the current source digest")
    if accepted.get("x86_64Method") not in ("native", "rosetta"):
        raise ValueError("Record whether Intel execution was native or under Rosetta")
    value = read_json(config)
    if value.get("appStoreConnectBundleId") != settings()["bundleId"]:
        raise ValueError("Installer configuration belongs to a different app")
    if not value.get("installerIdentity", "").startswith(("3rd Party Mac Developer Installer:", "Mac Installer Distribution:")):
        raise ValueError("Provide the Mac App Store installer certificate identity")
    if output.exists():
        raise ValueError("Existing versioned packages are never overwritten")
    run("productbuild", "--component", app, "/Applications", "--sign", value["installerIdentity"], output)
    run("pkgutil", "--check-signature", output)
    print("Signed package created. App Store Connect metadata/review and upload remain separate.")


def static_check():
    home = java_home()
    os.environ["JAVA_HOME"] = str(home)
    os.environ["PATH"] = str(home / "bin") + os.pathsep + os.environ["PATH"]
    digest = source_digest()
    target = ROOT / "target"
    target.mkdir(exist_ok=True)
    log = target / "mac-preparation-static.log"
    command = [str(ROOT / "mvnw"), "--batch-mode", "--no-transfer-progress",
               "verify", "-Dmaven.test.skip=true"]
    with log.open("w") as stream:
        subprocess.run(command, cwd=ROOT, stdout=stream, stderr=subprocess.STDOUT, check=True)
    if digest != source_digest():
        raise ValueError("Source changed during the static build")
    report = {"sourceDigest": digest, "logSha256": sha256(log), "testsExecuted": False,
              "command": command[1:], "createdUtc": datetime.now(timezone.utc).isoformat()}
    (target / "mac-preparation-static.json").write_text(json.dumps(report, indent=2) + "\n")
    print("Compiler, Enforcer, Spotless and SpotBugs passed. Tests were explicitly skipped.")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    sub = parser.add_subparsers(dest="command", required=True)
    sub.add_parser("static-check")
    for name in ("prepare", "check", "build"):
        cmd = sub.add_parser(name)
        if name != "prepare":
            cmd.add_argument("--inputs", type=Path, required=True)
        if name != "check":
            cmd.add_argument("--output", type=Path, required=True)
    cmd = sub.add_parser("inspect")
    cmd.add_argument("--app", type=Path, required=True)
    cmd.add_argument("--signed", action="store_true")
    for name in ("sign", "package"):
        cmd = sub.add_parser(name)
        cmd.add_argument("--app", type=Path, required=True)
        cmd.add_argument("--config", type=Path, required=True)
        if name == "sign":
            cmd.add_argument("--mode", choices=("development", "store"), required=True)
        else:
            cmd.add_argument("--output", type=Path, required=True)
            cmd.add_argument("--acceptance", type=Path, required=True)
    args = parser.parse_args()
    for name in ("inputs", "output", "app", "config", "acceptance"):
        if hasattr(args, name):
            setattr(args, name, getattr(args, name).expanduser().resolve())
    if args.command == "static-check":
        static_check()
    elif args.command == "prepare":
        prepare(args.output)
    elif args.command == "check":
        check_inputs(args.inputs)
    elif args.command == "build":
        build(args.inputs, args.output)
    elif args.command == "inspect":
        inspect(args.app, args.signed)
    elif args.command == "sign":
        sign(args.app, args.config, args.mode)
    else:
        package(args.app, args.config, args.output, args.acceptance)


if __name__ == "__main__":
    try:
        main()
    except (ValueError, OSError, subprocess.CalledProcessError) as failure:
        sys.exit(str(failure))
