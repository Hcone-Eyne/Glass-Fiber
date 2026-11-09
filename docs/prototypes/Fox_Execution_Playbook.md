# Fox — Execution Playbook
Inputs reviewed: ChatGPT `Fox_Implementation_Plan_Final.md`, Gemini `fox_pulse_and_vision_helper_specification.md`, Claude method.
Authority for architecture: `Fox_Mobile_Final_Spec_v2.md`. This file decides **how we build it**.

---

## 1. What the two inputs actually are

- **Gemini's file is unchanged.** Byte-identical to the Pulse spec v2.0 reviewed earlier. All findings from that review still stand (section 12 of Final Spec v2).
- **ChatGPT's file is a faithful implementation wrapper around our v2 spec.** It adopts every fix (leases, Tier-1-only relay, worktree patches, manual Vision Helper, claim labels) and adds real process: monorepo, four test layers, FAIL-if acceptance tests, definition of done, git/branch rules, UNKNOWN-not-invented telemetry. New *architecture* is near zero. New *discipline* is high.

---

## 2. Role stress test (the 17 roles)

| # | Role | ChatGPT plan | Gemini Pulse spec |
|---|---|---|---|
| 1 | Business / Product Owner | Strong: one loop (Continue), two demos | Good: every card has an action. "Grok cockpit" pulls focus from Continue |
| 2 | Business Analyst | Gap: no effort sizing, no time or cost view | Good: token ledger, daily cap |
| 3 | Source / Domain Analyst | Good: domain posture kept | Good: college vs desk posture originated here |
| 4 | Product Manager | Gap: no cut line if time runs out | Gap: "zero-config" ignores permission prompts (Accessibility, Automation, FDA) |
| 5 | Data Architect | Good: tables named. No retention rules | Good: ring buffers + compaction. `a2a_dialogues` is sprawl |
| 6 | Solution Architect | Good: two apps + one protocol. Gap: Python daemon identity (see 3.6) | Gap: dual-path escalation router doubles the code paths to test |
| 7 | Software Architect | Good: FoxLink owns no UI | Good: unidirectional flow, ring-buffered UI updates |
| 8 | Technical Lead | Good: monorepo, commits, branches. Gap: no Python↔Kotlin contract tests | Gap: no repo or workflow |
| 9 | Principal Engineer | Good: fail-closed, epoch fencing | Gap: Tier 3 reads a live WAL DB with `nolock`; path likely moved in macOS 15 |
| 10 | Data Engineer | Good: charset-aware, layered scrubber | **Bug:** `H > 4.2` never fires on hex (max 4.0) and misses short keys |
| 11 | QA / Test | Strong: 4 layers, FAIL-if tests | Good fixtures. Test key of all 9s doesn't exercise entropy |
| 12 | Adversarial / Test Architect | Good: untrusted text → static actions; cloud patch isolated | **Gap:** cloud patch trusted after "file exists"; Apply & Test is the primary button |
| 13 | SRE | Good: status/queue/UNKNOWN rule | Good: heartbeat indicator. Latency numbers invented |
| 14 | Platform Engineer | Good: measure first, hardware tests | Gap: unverified C71 claims (90 Hz throttle, frame-rate lock) |
| 15 | API / Integration | Gap: adapter timeouts, 429 handling missing | Good: adapter, 8 s timeout, circuit breaker, normalized schema |
| 16 | Documentation | Good: ADR / spikes / runbooks | Gap: "verified, hardened" with no evidence |
| 17 | Release Engineer | Good: flags + experiment branches | Good: kill-switch flags. Ships Tier 2 in first module |

**Net:** ChatGPT wins process roles (8, 11, 13, 14, 16, 17). Gemini wins integration and cost roles (2, 15) and originated posture. ChatGPT fixes most of Gemini's bugs. Neither covers the gaps in section 3.

---

## 3. What both miss (my findings)

1. **First sprint is too big.** Two apps + SQLite on both + pairing + TLS + VS Code + shell sensing is weeks of plumbing before anything visibly works.
2. **No walking skeleton.** Security and structure come before a single end-to-end result. Feedback arrives late.
3. **No kill criteria or timebox.** Fox shares time with college and other builds. Without gates, scope wins.
4. **No contract tests.** Python and Kotlin will drift on the envelope unless both replay the same golden fixtures.
5. **Mac agent identity.** macOS permissions (Accessibility, Automation, Screen Recording) attach to the code identity of the app, as far as I know. A bare Python script under a LaunchAgent gets awkward, lost grants when the interpreter changes. Verify in Spike.
6. **Android onboarding is under-specified.** Foreground service type (`connectedDevice`), Bluetooth permissions, HyperOS autostart + battery whitelist, Accessibility enable flow. Each is a user-facing step and a failure point.
7. **No versioning/migration plan** for SQLite or FoxLink beyond "v1".
8. **No pairing revoke, uninstall, or backup path.**
9. **No log hygiene rule:** logs must never contain secrets or raw notification bodies.
10. **No battery measurement plan** for the phone (BLE advertising, sockets).

---

## 4. My method: "Prove the loop"

1. **One loop, one metric.** Loop: Mates-Helper on Mac → phone shows it → Continue → Mac restores. Success = it works on **5 consecutive real work sessions**, not a staged demo.
2. **Risk-first spike, subset first.** Test only what gates Demo 1 and 2 first: mDNS/fallback, shell hook, VS Code extension, Chrome/Safari tabs, AX restore, background BLE. Acoustic, voice, hex-scrub run later. Stop rule: if no transport works on your network, redesign transport before writing more code.
3. **Walking skeleton before hardening.** Thinnest end-to-end first: shell hook → JSON → LAN WebSocket with a dev token → phone shows text. Ugly is fine.
4. **Blast-radius ladder.** Each rung has a gate; the next rung stays locked until the gate is green.
   - **Show** (read-only task display) — dev token allowed
   - **Resume** (opens things on Mac) — needs pinned-key pairing
   - **Run** (re-run pytest) — needs outbox, expiry, idempotency, confirmation
   - **Patch** (cloud code) — needs firewall, worktree, diff, policy, tests
   - **Autonomous** — not in scope
5. **Contract first.** `packages/fox-protocol` holds JSON Schema + golden fixtures. Python and Kotlin both pass the same fixtures in CI.
6. **Stable Mac identity.** Fox Desk = small signed menu-bar app (Swift shell or py2app) that owns permissions and UI and launches the Python daemon. Python stays the brain; the shell is the identity.
7. **Dogfood from slice 1.** Use it on a real project daily. Every failure becomes a line in `docs/spikes/failures.md`.
8. **Gates + kill criteria** per phase (section 6). A failed gate means fix, not advance.
9. **Timebox.** Set a weekly hour cap for Fox up front. Phases are ordered by value per hour.
10. **AI last.** AI-off must always work. Cloud only after the deterministic core survives real use.

---

## 5. Comparison

| Dimension | Gemini Pulse | ChatGPT plan | Claude method | Best |
|---|---|---|---|---|
| Core strength | Concrete schemas, ledger, adapter, outbox | Process, tests, definition of done | Sequencing and risk control | Combine all three |
| Build order | Modules A/B/C in parallel with phases 1-3 | Strict phases | Skeleton + ladder | ChatGPT spine + skeleton first |
| First deliverable | Mac module + A2A + cockpit UI | Both apps + SQLite + pairing + sensing | Shell hook → phone text, ugly | Skeleton |
| Security | Strong ideas, three bugs | Bugs fixed | Gate before every mutation | Ladder + ChatGPT rules |
| Evidence | "Verified/hardened" claims | Labels + measured | Gates with tests | Labels + gates |
| Time control | None | None | Timebox + kill criteria | Mine |
| Cross-language drift | n/a | Fixtures, no contract tests | Golden fixtures in CI | Mine |
| Mac permissions | Ignored | Python daemon | Signed menu-bar shell | Mine |
| Cloud patches | Applied after existence check | Worktree + policy + tests | Same, as a ladder rung | ChatGPT |

---

## 6. The best plan (final)

Spine: ChatGPT's phases. Fixes and additions: mine. Pulled from Gemini after fixes: ledger, adapter + 429 breaker, outbox (1.5 s → queued), heartbeat indicator, domain posture, replay guard.

| Phase | Build | Gate (must pass to continue) | Kill / stop rule |
|---|---|---|---|
| 0a | **Spike subset:** mDNS + fallback, shell hook, VS Code ext, browser tabs, AX restore, background BLE | Each claim labeled with a measured result | No working transport → redesign transport |
| S | **Skeleton:** shell hook → SQLite → WebSocket (dev token) → phone text | Task text appears on phone from a real command | If > 1 round of fixes can't make it reliable, stop and diagnose |
| 1 | Fox Desk sensing: VS Code, git, shell, calendar, browser | Correct task for 3 real projects | |
| 2 | **Pinned-key pairing, TLS, reconnect, sync queue, golden fixtures in CI** | Reconnect with no duplicates; stale epochs rejected | |
| 3 | Phone task surface + Share-to-Fox + Android onboarding (FGS, BT, autostart) | Mac task on phone quickly; survives 24 h kill/restart | |
| 4 | **Resume both ways + `resume.result`** | **Demo 1 on 5 consecutive real sessions** | If not reliable, no new features until it is |
| 5 | Tier-1 relay, Pulse Card, outbox, posture | **Demo 2:** fail pytest → card → re-run → ack, or honest timeout | |
| 0b | Remaining spike items: voice, acoustic, hardware trigger, scrubber on hex/base62/random | Labeled | |
| 6 | Fox Desk dashboard + Resume All | 2 saved tasks restore layout | |
| 7 | Vision Helper, manual only: firewall, ledger, cache, worktree patches | Secret never leaves; patch never touches working tree | |
| 8 | Glass AI on Mac, then automatic triggers | AI-off still fully usable | |
| 9 | Center ripple + calibrated direction; direction lab behind flag | Measured accuracy before promotion | |
| 10 | Fox Home + Spaces + search + full Pulse + voice | Set as HOME; frame time and barge-in measured | |
| 11 | Capture + hardware trigger (visible, user-triggered) | Fallback trigger exists | |
| 12 | Tier 2/3 notifications, automation, Fox Club | Behind flags | |

**Pulled from Gemini:** token ledger, `VisionHelperAdapter` (8 s timeout, 429 breaker, normalized schema), idempotent outbox, heartbeat indicator, domain posture, `(epoch, seq, ts)` replay guard, JSON replay fixtures.
**Left behind:** `a2a_dialogues` negotiation, "verified across 17 roles" status, confidence < 0.65 trigger, Tier 3 as core, unmeasured 120 Hz and latency claims, stealth recorder.

---

## 7. Slice S checklist (do this first)

1. Create monorepo: `apps/fox-desk`, `apps/fox-mobile`, `packages/fox-protocol`, `docs/spikes`.
2. Write the envelope JSON Schema and 3 golden fixtures (`hello`, `task.update`, `task.list`).
3. Mac: zsh `precmd` hook → local socket → Python collector → SQLite `tasks`.
4. Mac: WebSocket server, dev token, serves `task.list`.
5. Phone: minimal Compose screen, connects by manual IP, shows current task.
6. Run it on a real project for a day. Log what broke.
7. Then, and only then, replace the dev token with QR pairing and pinned keys.

**Rule:** the skeleton may show tasks. It may not run anything on the Mac until step 7 is done.
