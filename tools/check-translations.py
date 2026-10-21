#!/usr/bin/env python3
"""Checks translated strings.xml files against the English source and store listings against Play limits.

Usage: tools/check-translations.py            # every values-*/strings.xml and docs/play-store/listing/*.md
       tools/check-translations.py values-ja  # only some resource folders
"""
import pathlib
import re
import sys
import xml.etree.ElementTree as ET

ROOT = pathlib.Path(__file__).resolve().parent.parent
RES = ROOT / "app/src/main/res"
LISTING = ROOT / "docs/play-store/listing"
PLACEHOLDER = re.compile(r"%(?:\d+\$)?[-#+ 0,(]*\d*(?:\.\d+)?[sdfx]")
# CLDR plural categories Android needs per language (others are ignored by aapt).
PLURALS = {
    "en": {"one", "other"}, "vi": {"other"}, "es": {"one", "many", "other"}, "pt": {"one", "many", "other"},
    "in": {"other"}, "hi": {"one", "other"}, "ja": {"other"}, "ko": {"other"}, "zh": {"other"},
    "ru": {"one", "few", "many", "other"}, "de": {"one", "other"}, "fr": {"one", "many", "other"},
    "tr": {"one", "other"}, "th": {"other"},
}


def load(folder):
    path = RES / folder / "strings.xml"
    raw = path.read_text(encoding="utf-8")
    root = ET.fromstring(raw)
    strings = {e.get("name"): (e.get("translatable"), "".join(e.itertext())) for e in root.findall("string")}
    plurals = {e.get("name"): {i.get("quantity"): "".join(i.itertext()) for i in e.findall("item")} for e in root.findall("plurals")}
    return raw, strings, plurals


def raw_bodies(raw):
    return re.findall(r"<(?:string|item)\b[^>]*>(.*?)</(?:string|item)>", raw, flags=re.S)


def check_folder(folder, src_strings, src_plurals):
    errors = []
    try:
        raw, strings, plurals = load(folder)
    except ET.ParseError as e:
        return [f"XML parse error: {e}"]
    lang = folder.split("-")[1]
    wanted = {k: v for k, (t, v) in src_strings.items() if t != "false"}
    for k in sorted(set(wanted) - set(strings)):
        errors.append(f"missing string {k}")
    for k in sorted(set(strings) - set(wanted)):
        errors.append(f"extra or non-translatable string {k}")
    for k, (_, text) in strings.items():
        if k in wanted and sorted(PLACEHOLDER.findall(text)) != sorted(PLACEHOLDER.findall(wanted[k])):
            errors.append(f"placeholders differ in {k}: {text!r}")
        if not text.strip():
            errors.append(f"empty string {k}")
    for k, items in src_plurals.items():
        if k not in plurals:
            errors.append(f"missing plurals {k}")
            continue
        need = PLURALS.get(lang, {"other"})
        if not need <= set(plurals[k]):
            errors.append(f"plurals {k} needs {sorted(need)}, has {sorted(plurals[k])}")
        ref = sorted(PLACEHOLDER.findall(items["other"]))
        for q, text in plurals[k].items():
            if sorted(PLACEHOLDER.findall(text)) != ref:
                errors.append(f"placeholders differ in plurals {k}/{q}: {text!r}")
    for body in raw_bodies(raw):
        if re.search(r"(?<!\\)'", body) and not (body.startswith('"') and body.endswith('"')):
            errors.append(f"unescaped apostrophe: {body[:60]!r}")
        if re.search(r'(?<!\\)"', body):
            errors.append(f"unescaped double quote: {body[:60]!r}")
    return errors


def check_listing(path):
    parts = re.split(r"^## .*$", path.read_text(encoding="utf-8"), flags=re.M)
    if len(parts) < 4:
        return ["expected three '## ' sections: name, short, full"]
    name, short, full = (p.strip() for p in parts[1:4])
    errors = []
    for label, text, limit in (("name", name, 30), ("short description", short, 80), ("full description", full, 4000)):
        if not text:
            errors.append(f"{label} is empty")
        elif len(text) > limit:
            errors.append(f"{label} is {len(text)} chars (max {limit})")
    banned = re.findall(r"\b(fake|cheat|prank|pok[eé]mon)\b", f"{name} {short}", flags=re.I)
    if banned:
        errors.append(f"avoid these words in name/short description: {sorted(set(banned))}")
    return errors


def main():
    _, src_strings, src_plurals = load("values")
    folders = sys.argv[1:] or sorted(p.name for p in RES.glob("values-*") if (p / "strings.xml").exists())
    failed = False
    for folder in folders:
        errs = check_folder(folder, src_strings, src_plurals)
        failed |= bool(errs)
        print(f"{'FAIL' if errs else 'ok  '} {folder}")
        for e in errs:
            print(f"     - {e}")
    if not sys.argv[1:]:
        for md in sorted(LISTING.glob("*.md")):
            errs = check_listing(md)
            failed |= bool(errs)
            print(f"{'FAIL' if errs else 'ok  '} listing/{md.name}")
            for e in errs:
                print(f"     - {e}")
    sys.exit(1 if failed else 0)


if __name__ == "__main__":
    main()
