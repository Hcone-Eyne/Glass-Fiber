#!/usr/bin/env python3
"""FoxLink v1 reference validator.

Reads the schemas in schema/ (it hard-codes no field rules), so it cannot drift from them, and
implements the same JSON Schema subset, error paths and state check as the Swift and Kotlin
validators. Checks states.json, then every golden fixture (must pass) and every invalid case
(must fail at exactly the path in `expect_error`).

Run: python3 packages/fox-protocol/tools/validate_protocol.py
"""
import json
import re
import sys
from pathlib import Path

PROTOCOL_DIR = Path(__file__).resolve().parents[1]
SCHEMA_DIR = PROTOCOL_DIR / "schema"
FIXTURES_DIR = PROTOCOL_DIR / "fixtures"

ALLOWED_KEYWORDS = {
    "$schema", "$id", "title", "description", "type", "required", "properties",
    "additionalProperties", "enum", "const", "minimum", "maxLength", "minLength",
    "pattern", "items", "maxItems", "$ref",
}
LEVELS = {"info", "warn", "fail", "silent", "home", "ok"}


class ProtocolError(Exception):
    def __init__(self, path, reason):
        super().__init__(f"{path or '<root>'}: {reason}")
        self.path = path
        self.reason = reason


def load(name):
    return json.loads((SCHEMA_DIR / name).read_text(encoding="utf-8"))


def payload_schema(name):
    if "/" in name or "\\" in name or name.startswith("."):
        raise ProtocolError("", f"illegal $ref '{name}'")
    return load(f"payloads/{name if name.endswith('.json') else name + '.json'}")


def join(path, key):
    return key if not path else f"{path}.{key}"


def is_int(v):
    return isinstance(v, int) and not isinstance(v, bool) or (isinstance(v, float) and v.is_integer())


def is_number(v):
    return isinstance(v, (int, float)) and not isinstance(v, bool)


def type_ok(v, t):
    return {
        "null": lambda: v is None,
        "boolean": lambda: isinstance(v, bool),
        "integer": lambda: is_int(v),
        "number": lambda: is_number(v),
        "string": lambda: isinstance(v, str),
        "array": lambda: isinstance(v, list),
        "object": lambda: isinstance(v, dict),
    }.get(t, lambda: (_ for _ in ()).throw(ProtocolError("", f"unknown schema type '{t}'")))()


def equal(a, b):
    if isinstance(a, bool) or isinstance(b, bool):
        return isinstance(a, bool) and isinstance(b, bool) and a == b
    if is_number(a) and is_number(b):
        return float(a) == float(b)
    return a == b


def validate(value, schema, path=""):
    unknown = set(schema) - ALLOWED_KEYWORDS
    if unknown:
        raise ProtocolError(path, f"schema uses unsupported keywords {sorted(unknown)}")
    if "$ref" in schema:
        return validate(value, payload_schema(schema["$ref"]), path)
    if "type" in schema:
        types = schema["type"] if isinstance(schema["type"], list) else [schema["type"]]
        if not any(type_ok(value, t) for t in types):
            raise ProtocolError(path, f"expected {'/'.join(types)}")
    if "const" in schema and not equal(value, schema["const"]):
        raise ProtocolError(path, f"must be {schema['const']}")
    if "enum" in schema and not any(equal(value, e) for e in schema["enum"]):
        raise ProtocolError(path, "not an allowed value")
    if value is None:
        return
    if is_number(value) and "minimum" in schema and value < schema["minimum"]:
        raise ProtocolError(path, f"below minimum {schema['minimum']}")
    if isinstance(value, str):
        if "minLength" in schema and len(value) < schema["minLength"]:
            raise ProtocolError(path, "too short")
        if "maxLength" in schema and len(value) > schema["maxLength"]:
            raise ProtocolError(path, "too long")
        if "pattern" in schema:
            p = schema["pattern"].removeprefix("^").removesuffix("$")
            if not re.fullmatch(f"(?:{p})", value):
                raise ProtocolError(path, "does not match pattern")
    if isinstance(value, list):
        if "maxItems" in schema and len(value) > schema["maxItems"]:
            raise ProtocolError(path, "too many items")
        for i, item in enumerate(value):
            if "items" in schema:
                validate(item, schema["items"], f"{path}[{i}]")
    if isinstance(value, dict):
        props = schema.get("properties", {})
        for k in schema.get("required", []):
            if k not in value:
                raise ProtocolError(join(path, k), "required")
        if schema.get("additionalProperties", True) is False:
            for k in sorted(value):
                if k not in props:
                    raise ProtocolError(join(path, k), "not allowed")
        for k in sorted(props):
            if k in value:
                validate(value[k], props[k], join(path, k))


def validate_message(msg, state_ids):
    if not isinstance(msg, dict):
        raise ProtocolError("", "expected envelope object")
    validate(msg, load("envelope.schema.json"))
    validate(msg["payload"], payload_schema(msg["type"]), "payload")
    if msg["type"] == "state.event" and msg["payload"]["state_id"] not in state_ids:
        raise ProtocolError("payload.state_id", f"unknown state '{msg['payload']['state_id']}'")


def check_states():
    data = load("states.json")
    states = data.get("states", [])
    if len(states) != data.get("count", 53):
        raise ValueError(f"states.json says count={data.get('count')} but has {len(states)}")
    ids = set()
    for s in states:
        sid = s.get("id")
        if not isinstance(sid, str) or not re.fullmatch(r"[a-z0-9_]{2,32}", sid):
            raise ValueError(f"invalid state id: {sid}")
        if sid in ids:
            raise ValueError(f"duplicate state id: {sid}")
        if s.get("level") not in LEVELS:
            raise ValueError(f"invalid level {s.get('level')} for {sid}")
        if not s.get("title") or not s.get("group"):
            raise ValueError(f"missing title or group in {sid}")
        ids.add(sid)
    print(f"  [PASS] {len(ids)} states in schema/states.json")
    return ids


def main():
    print("=== FoxLink v1 schema and fixture checks ===")
    ids = check_states()
    failed = 0
    for f in sorted(FIXTURES_DIR.glob("0*.json")):
        try:
            validate_message(json.loads(f.read_text(encoding="utf-8")), ids)
            print(f"  [PASS] golden {f.name}")
        except ProtocolError as e:
            failed += 1
            print(f"  [FAIL] golden {f.name} rejected at {e}", file=sys.stderr)
    for case in json.loads((FIXTURES_DIR / "invalid/cases.json").read_text(encoding="utf-8")):
        try:
            validate_message(case["message"], ids)
            failed += 1
            print(f"  [FAIL] invalid '{case['name']}' was accepted", file=sys.stderr)
        except ProtocolError as e:
            if e.path != case["expect_error"]:
                failed += 1
                print(f"  [FAIL] invalid '{case['name']}' failed at '{e.path}', expected '{case['expect_error']}'", file=sys.stderr)
            else:
                print(f"  [PASS] invalid {case['name']} -> {e.path}")
    if failed:
        print(f"=== {failed} check(s) failed ===", file=sys.stderr)
        return 1
    print("=== All checks passed ===")
    return 0


if __name__ == "__main__":
    sys.exit(main())
