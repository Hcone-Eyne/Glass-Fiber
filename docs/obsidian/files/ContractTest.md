---
title: ContractTest.kt
tags:
  - active-v30
  - testing
---

# `ContractTest.kt`

- **Source File:** `apps/fox-mobile/app/src/test/java/com/fox/mobile/ContractTest.kt`
- **Role:** Unit test suite executed on every phase via `./gradlew testDebugUnitTest`.

## Test Cases
1. `testAllValidFixturesPass()` & `testAllInvalidFixturesFailAtExpectedPath()`: Protocol schema compliance.
2. `testReplayGuardRejectsDuplicateAndSkew()`: Replay attack & clock-skew protection.
3. `testSpringPhysicsDeterminismAndInterrupt()`: Phase 1 `FoxSpring` physics.
4. `testPhase2ThemeTokensAndWallpaperAsset()`: Phase 2 color tokens & `wallpaper.webp`.
5. `testPhase3DepthWallpaperAndTiltMath()`: Phase 3 `WallpaperBox`, 191 ridge points, tilt low-pass.
6. `testPhase4WordsRepositoryAndRidgeFit()`: Phase 4 quotes & mountain ridge clearance.
7. `testPhase4_5CalmTiltAndSpringSettle()`: Phase 4.5 `0.002f` deadband & `<180` frame settle.
8. `testPhase5FoxLineMathAndUrgencyQueue()`: Phase 5 urgency queue, order stages, splash wave math.

## Connected Notes
- [[PROGRESS]] — Main AI brain
- [[FoxSpring]] — Physics unit tests
- [[DepthWallpaperAndClock]] — Geometry and ridge unit tests
- [[WordsRepository]] — Clearance unit tests
- [[FoxLineState]] — Queue and stage unit tests
- [[FoxProtocol]] — Schema validation unit tests
