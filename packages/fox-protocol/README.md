# fox-protocol

FoxLink v1: the one wire contract between Fox Desk (Python) and Fox Mobile (Kotlin).
Owner: Protocol Owner (FRIDAY). Changes need a matching fixture change and both test suites green.

## Layout

| Path | What |
|---|---|
| `schema/envelope.schema.json` | Envelope every message uses: `v, type, id, epoch, seq, ts, payload` |
| `schema/payloads/<type>.json` | Payload schema per message type (`task.json` is shared via `$ref`) |
| `schema/states.json` | The 53 user-facing states, generated from `docs/prototypes/Fox_Line_v30.html` |
| `fixtures/*.json` | Valid golden messages. Both languages must accept every one |
| `fixtures/invalid/cases.json` | Invalid messages. Both languages must reject each, at the field named in `expect_error` |
| `tools/extract_states.py` | Regenerates `states.json` from the prototype |

## Message types (Slice S)

| type | direction | purpose |
|---|---|---|
| `hello` | both | first message on a connection |
| `task.list` | desk → mobile | recent tasks, sent after `hello` |
| `task.update` | desk → mobile | current task changed |
| `state.event` | both | a state from `states.json` happened |

Slice S is the **Show** rung: there is no message type that makes the Mac do anything.
Unknown types are rejected, not ignored.

## Allowed JSON Schema subset

Kept small on purpose so the Python and Kotlin validators are line-for-line equivalent:
`type` (string or list, `"integer"` excludes booleans), `required`, `properties`,
`additionalProperties: false`, `enum`, `const`, `minimum`, `maxLength`, `minLength`,
`pattern` (full-match), `items`, `maxItems`, `$ref` (sibling file name only).
Anything else in a schema is a validator error, so nobody adds a keyword one side ignores.

## Error paths

Validators report the first failing field as a dotted path: `seq`, `payload.task.summary`,
`payload.tasks[0].uncommitted_count`. Invalid fixtures assert that exact path.
