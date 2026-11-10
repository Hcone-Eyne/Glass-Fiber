---
title: FoxSpring.kt
tags:
  - active-v30
  - physics-engine
---

# `FoxSpring.kt`

- **Source File:** `apps/fox-mobile/app/src/main/java/com/fox/mobile/core/FoxSpring.kt`
- **Role:** Pure Kotlin 1D and multi-channel spring simulation.

## Architecture
- **Zero C/JNI/NDK:** Replaced all legacy C code with deterministic pure Kotlin (180 lines).
- **Apple Parameter Conversion:**
  - `k = (2π / response)²`
  - `c = 4π · damping / response`
- **Numerical Stability:** Runs 4 substeps per frame (`subDt = dt / 4f`).
- **Velocity Carry-Over:** When `retarget()` is called mid-flight, current velocity `v` is preserved (no abrupt velocity resets).

## Classes
- `Spring1D`: 1D spring with `value`, `v`, `target`, `k`, `c`.
- `SpringChannelSet`: Multi-channel coordinator for liquid morphing.

## Connected Notes
- [[PROGRESS]] — Main AI brain
- [[MainActivity]] — App entrypoint
- [[FoxLineCanvas]] — Uses spring for glass capsule
- [[SpotlightSheet]] — Will use `k=230, c=26` / `k=320, c=34` in Phase 6
- [[ContractTest]] — Verified by unit tests
