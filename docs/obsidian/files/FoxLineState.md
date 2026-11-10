---
title: FoxLineState.kt
tags:
  - active-v30
  - fox-line
---

# `FoxLineState.kt`

- **Source File:** `apps/fox-mobile/app/src/main/java/com/fox/mobile/ui/line/FoxLineState.kt`
- **Role:** Central state engine for all dynamic launcher activities, urgency queue, and physics.

## Urgency Queue (`activeList()`)
Strict priority ordering matching `Fox_Line_v30.html`:
```
XFER -> urgent/done TIMER (<= 10s) -> MSG -> CONT -> ORDER -> normal TIMER -> MUSIC
```
- Supports secondary bead when 2 activities are active.
- Horizontal swipe > 30dp swaps primary and secondary activities.
- Finished ringing red timer is protected from being swapped out of primary position.

## Dynamic Features
- `orderStage()`: 4 delivery stages (`Preparing`, `On the way`, `Arriving`, `Delivered`).
- `wavyBarOffset()`: Decaying sinusoidal wave math for splashes.
- `performFoxBuzz()`: Waveform and single-shot haptic feedback.
- `summonFox()` & `closeFox()`: 1.1s hold-to-wake logic.

## Connected Notes
- [[PROGRESS]] — Main AI brain
- [[FoxLineCanvas]] — Renders the line and bead
- [[IslandCard]] — Displays the expanded activity view
- [[FoxLineDebugMenu]] — Tests all states
- [[ContractTest]] — Verified by unit tests
- [[Prototype_Fox_Line_v30]] — Prototype state engine
