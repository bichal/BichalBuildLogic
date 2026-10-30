"""Third-party sources a mod compiles but does not commit: `bichal vendor`.

A mod declares them in its TOML, pinned to an upstream commit:

    [vendor.astcenc]
    git     = "https://github.com/ARM-software/astc-encoder.git"
    commit  = "d0f91386350222fcbf6da533c0f7a3f9b5c6b783"
    dest    = "native/vendor/astcenc"
    copy    = ["Source/astcenc*.cpp", "Source/astcenc*.h"]   # globs from the repository root, copied flat
    skip    = ["astcenccli_*"]                               # file-name globs left out of `copy`
    rename  = { "LICENSE.txt" = "LICENSE" }                  # single files: source = name in dest
    patches = ["native/vendor/patches/astcenc.patch"]        # unified diffs, relative to the mod, applied last

CI runs it before building [natives.*]; locally it is the same command. A marker in `dest`
records what was written, so an up-to-date directory is left alone and a changed pin
replaces exactly the files the previous one wrote.
"""
from __future__ import annotations

import fnmatch
import hashlib
import json
import shutil
import subprocess
import tempfile
from pathlib import Path

from bichal_mod import BichalError, Mod

MARKER = ".vendored.json"
_KEYS = {"git", "commit", "dest", "copy", "skip", "rename", "patches"}


def specs(mod: Mod) -> dict[str, dict]:
    found = mod.config.get("vendor", {})
    for name, spec in found.items():
        for key in ("git", "commit", "dest"):
            if not spec.get(key):
                raise BichalError(f"[vendor.{name}] needs `{key}`")
        unknown = set(spec) - _KEYS
        if unknown:
            raise BichalError(f"[vendor.{name}] unknown keys: {sorted(unknown)}")
        if not (spec.get("copy") or spec.get("rename")):
            raise BichalError(f"[vendor.{name}] lists nothing to copy (`copy` or `rename`)")
        dest = Path(spec["dest"])
        if dest.is_absolute() or ".." in dest.parts:
            raise BichalError(f"[vendor.{name}] dest must stay inside the mod: {spec['dest']}")
    return found


def fingerprint(mod: Mod, spec: dict) -> str:
    """The pin plus the content of every patch: what the vendored files are made from."""
    digest = hashlib.sha256(json.dumps(spec, sort_keys=True).encode())
    for patch in spec.get("patches", []):
        path = mod.root / patch
        if not path.is_file():
            raise BichalError(f"patch {patch} does not exist")
        digest.update(path.read_bytes())
    return digest.hexdigest()


def up_to_date(mod: Mod, spec: dict) -> bool:
    marker = mod.root / spec["dest"] / MARKER
    if not marker.is_file():
        return False
    try:
        data = json.loads(marker.read_text(encoding="utf-8"))
    except ValueError:
        return False
    return data.get("fingerprint") == fingerprint(mod, spec) and all(
        (mod.root / spec["dest"] / f).is_file() for f in data.get("files", []))


def _run(*args: str, cwd: Path) -> None:
    result = subprocess.run(args, cwd=cwd, capture_output=True, text=True)
    if result.returncode != 0:
        raise BichalError(f"{' '.join(args)}: {result.stderr.strip() or result.stdout.strip()}")


def _checkout(spec: dict, target: Path) -> None:
    """The pinned commit only (GitHub serves any reachable sha); a full clone is the fallback."""
    _run("git", "init", "-q", ".", cwd=target)
    try:
        _run("git", "fetch", "-q", "--depth", "1", spec["git"], spec["commit"], cwd=target)
        _run("git", "checkout", "-q", "FETCH_HEAD", cwd=target)
    except BichalError:
        _run("git", "fetch", "-q", spec["git"], cwd=target)
        _run("git", "checkout", "-q", spec["commit"], cwd=target)


def select(source: Path, spec: dict) -> dict[str, Path]:
    """File name in dest -> file in the checkout."""
    chosen: dict[str, Path] = {}
    skip = spec.get("skip", [])
    for pattern in spec.get("copy", []):
        matches = sorted(p for p in source.glob(pattern) if p.is_file())
        if not matches:
            raise BichalError(f"copy = {pattern!r} matches nothing at {spec['commit'][:12]}; upstream moved?")
        for path in matches:
            if not any(fnmatch.fnmatch(path.name, s) for s in skip):
                chosen[path.name] = path
    for source_name, target_name in spec.get("rename", {}).items():
        path = source / source_name
        if not path.is_file():
            raise BichalError(f"rename: {source_name} is not in the checkout at {spec['commit'][:12]}")
        chosen[target_name] = path
    return chosen


def apply(mod: Mod, name: str, spec: dict, force: bool = False) -> bool:
    """Writes one [vendor.<name>] into the mod; False when it was already up to date."""
    if not force and up_to_date(mod, spec):
        return False
    dest = mod.root / spec["dest"]
    with tempfile.TemporaryDirectory(prefix=f"vendor-{name}-") as tmp:
        source = Path(tmp) / "src"
        source.mkdir()
        _checkout(spec, source)
        staged = Path(tmp) / "out"
        staged.mkdir()
        for target_name, path in select(source, spec).items():
            shutil.copyfile(path, staged / target_name)
        for patch in spec.get("patches", []):
            _run("git", "apply", "--whitespace=nowarn", str((mod.root / patch).resolve()), cwd=staged)
        # `git apply` outside a repository still patches plain files in cwd.
        marker = dest / MARKER
        if marker.is_file():
            try:
                for stale in json.loads(marker.read_text(encoding="utf-8")).get("files", []):
                    (dest / stale).unlink(missing_ok=True)
            except ValueError:
                pass
        dest.mkdir(parents=True, exist_ok=True)
        written = sorted(p.name for p in staged.iterdir())
        for file_name in written:
            shutil.copyfile(staged / file_name, dest / file_name)
    marker.write_text(json.dumps({"commit": spec["commit"], "fingerprint": fingerprint(mod, spec), "files": written},
                                 indent=1) + "\n", encoding="utf-8")
    return True


def vendor(mod: Mod, names: list[str] | None = None, check: bool = False, force: bool = False) -> int:
    declared = specs(mod)
    if not declared:
        print(f"{mod.name}: no [vendor.*] to fetch")
        return 0
    unknown = [n for n in (names or []) if n not in declared]
    if unknown:
        raise BichalError(f"{mod.name} has no [vendor.{unknown[0]}] (it has: {', '.join(declared)})")
    stale = 0
    for name, spec in declared.items():
        if names and name not in names:
            continue
        if check:
            ok = up_to_date(mod, spec)
            stale += 0 if ok else 1
            print(f"{mod.name}: vendor.{name} {'up to date' if ok else 'missing or out of date'}")
            continue
        changed = apply(mod, name, spec, force)
        print(f"{mod.name}: vendor.{name} @ {spec['commit'][:12]} {'written to ' + spec['dest'] if changed else 'up to date'}")
    return 1 if stale else 0
