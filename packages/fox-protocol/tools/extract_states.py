"""Extract the STATES table from Fox_Line_v30.html into packages/fox-protocol/schema/states.json.

Run from the repo root:  python3 packages/fox-protocol/tools/extract_states.py
The prototype is the design source; this file keeps the protocol copy in sync with it.
"""
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
SRC = ROOT / "docs/prototypes/Fox_Line_v30.html"
OUT = ROOT / "packages/fox-protocol/schema/states.json"

LEVELS = {"info", "warn", "fail", "silent", "home", "ok"}


def main() -> int:
    html = SRC.read_text(encoding="utf-8")
    block = re.search(r"const STATES = \[(.*?)\n  \];", html, re.S)
    if not block:
        print("STATES table not found", file=sys.stderr)
        return 1
    body = block.group(1)
    states = []
    # each group: ['Group name', [ ...items... ]]
    for g in re.finditer(r"\['([^']+)', \[(.*?\])\]\]", body, re.S):
        group = g.group(1)
        for item in re.finditer(r"\[([^\[\]]+)\]", g.group(2)):
            fields = re.findall(r"'([^']*)'", item.group(1))
            sid, level, title, *rest = fields
            if level not in LEVELS:
                print(f"unknown level {level!r} for {sid}", file=sys.stderr)
                return 1
            rest += [None] * (3 - len(rest))
            states.append({"id": sid, "group": group, "level": level, "title": title,
                           "detail": rest[0], "action": rest[1], "alt": rest[2]})
    ids = [s["id"] for s in states]
    if len(ids) != len(set(ids)):
        print("duplicate state ids", file=sys.stderr)
        return 1
    doc = {"source": "docs/prototypes/Fox_Line_v30.html", "count": len(states), "states": states}
    OUT.write_text(json.dumps(doc, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"wrote {len(states)} states to {OUT.relative_to(ROOT)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
