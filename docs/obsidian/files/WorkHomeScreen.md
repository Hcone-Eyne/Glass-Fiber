---
title: WorkHomeScreen.kt
tags:
  - active-v30
  - home-content
---

# `WorkHomeScreen.kt`

- **Source File:** `apps/fox-mobile/app/src/main/java/com/fox/mobile/ui/home/WorkHomeScreen.kt`
- **Role:** Home screen content (Work block, Continue pill, Next alarm row, and Words line).

## Critical Section Warning
- **Lines 329–739 (`Phase4HomeContent`) ARE ACTIVE (v30):** Mounted by `MainActivity.kt`.
- **Lines 78–311 (`WorkHomeScreen` overload) ARE LEGACY:** Kept temporarily for compilation until Phase 6 replaces drawer.

## Key Sub-Composables
1. **`Phase4HomeContent`:**
   - Pre-composes all 3 `#words` blocks in `CompositingStrategy.Offscreen` GPU layers.
   - Tapping `#words` swaps quote purely via `graphicsLayer { alpha = ... }` with **0.00% jank**.
   - Positions `.work` block at `54% H`.
2. **`ContinueActionPill`:**
   - Isolated recomposition scope for 52dp Ink Continue pill button.
   - Calls `foxLineState?.triggerContinue()` and animates `"Continue"` -> `"Sending to your Mac"` -> `"Open on your Mac"`.
3. **`NextAlarmRow`:**
   - `"Exam tomorrow 9:30 · no alarm"` + `"Set 7:00"` chip.
   - Launches `AlarmClock.ACTION_SET_ALARM` and switches to `"Alarm 7:00"` in `ok` mint.

## Connected Notes
- [[PROGRESS]] — Main AI brain
- [[MainActivity]] — Mounts `Phase4HomeContent`
- [[WordsRepository]] — Feeds quotes and ridge fitting
- [[DepthWallpaperAndClock]] — Supplies `WallpaperBox` geometry
- [[FoxLineState]] — Connected to Continue and Alarm actions
- [[02_Mistakes_And_How_We_Overcame_Them]] — Offscreen text caching post-mortem
