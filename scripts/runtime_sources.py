"""Copy pinned, complete upstream source archives into the release handoff."""
import hashlib
import json
from pathlib import Path
import shutil
import uuid
from urllib.request import Request, urlopen
ROOT = Path(__file__).resolve().parents[1]
def sha256(path):
    with path.open("rb") as stream:
        return hashlib.file_digest(stream, "sha256").hexdigest()

def acquire_sources(cache, destination):
    lock = json.loads((ROOT / "packaging/runtime-sources.json").read_text())
    cache.mkdir(parents=True, exist_ok=True)
    destination.mkdir()
    for entry in lock["files"]:
        cached = cache / entry["file"]
        if not cached.exists():
            temporary = cache / f"{uuid.uuid4().hex}.part"
            try:
                print(f"Download: {entry['file']}", flush=True)
                request = Request(
                    entry["url"], headers={"User-Agent": "YarnBox-release-preparation"}
                )
                with urlopen(request, timeout=60) as response, temporary.open("xb") as stream:
                    shutil.copyfileobj(response, stream)
                if sha256(temporary) != entry["sha256"]:
                    raise RuntimeError(f"Download checksum mismatch: {entry['file']}")
                temporary.rename(cached)
            finally:
                temporary.unlink(missing_ok=True)
        if cached.stat().st_size != entry["bytes"] or sha256(cached) != entry["sha256"]:
            raise RuntimeError(f"Source archive differs from pinned inventory: {cached}")
        shutil.copy2(cached, destination / entry["file"])
    shutil.copy2(ROOT / "packaging/runtime-sources.json", destination)
    shutil.copy2(ROOT / "packaging/runtime-sources-readme.txt", destination / "README.txt")
    shutil.copy2(ROOT / "THIRD_PARTY_NOTICES.md", destination)
    shutil.copytree(ROOT / "third-party-licenses", destination / "licenses")
