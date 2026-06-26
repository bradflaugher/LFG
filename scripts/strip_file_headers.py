#!/usr/bin/env python3
"""Remove legacy per-file LFG license headers (one-time cleanup helper)."""

from __future__ import annotations

import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ANDROID = ROOT / "android"
LFG_XML = re.compile(r"<!--\n LFG —[\s\S]*?-->\s*\n?", re.MULTILINE)


def strip_kt(text: str) -> str:
    if not text.startswith("/*\n * LFG —"):
        return text
    end = text.find("*/")
    return text[end + 2 :].lstrip("\n") if end != -1 else text


def strip_xml(text: str) -> str:
    text = LFG_XML.sub("", text)
    if text.startswith("<!--\n LFG —"):
        end = text.find("-->")
        text = text[end + 3 :].lstrip("\n") if end != -1 else text
    return text


def strip_hash(text: str) -> str:
    if not text.startswith("# LFG —"):
        return text
    lines = text.splitlines(keepends=True)
    i = 0
    while i < len(lines) and lines[i].startswith("#"):
        i += 1
    return "".join(lines[i:]).lstrip("\n")


def main() -> None:
    for path in ANDROID.rglob("*"):
        if not path.is_file() or "/build/" in str(path):
            continue
        text = path.read_text(encoding="utf-8")
        if path.suffix in {".kt", ".kts", ".proto"}:
            new = strip_kt(text)
        elif path.suffix == ".xml":
            new = strip_xml(text)
        elif path.suffix == ".pro":
            new = strip_hash(text)
        else:
            continue
        if new != text:
            path.write_text(new, encoding="utf-8")


if __name__ == "__main__":
    main()