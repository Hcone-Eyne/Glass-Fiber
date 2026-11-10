---
title: FoxLineDebugMenu.kt
tags:
  - active-v30
  - debug-tools
---

# `FoxLineDebugMenu.kt`

- **Source File:** `apps/fox-mobile/app/src/main/java/com/fox/mobile/ui/line/FoxLineDebugMenu.kt`
- **Role:** Temporary on-device verification menu for Phase 5.

## How to Trigger
Long-press the 118sp clock widget on the home screen.

## 10 Instant Verification Actions
1. `Wake Fox` (`summonFox()`)
2. `Splash Wave` (`splashBar(1f)`)
3. `Timer (20s)` (`startTimer(20f)`)
4. `Timer (Red/Done)` (`startTimer(20f, markDoneImmediately = true)`)
5. `Music (Wave)` (`startMusic()`)
6. `Food Order` (`startOrder()`)
7. `Message (Drop)` (`triggerMessage()`)
8. `Send File (In)` (`triggerTransfer("out", 0.6f)`)
9. `Receive File (Out)` (`triggerTransfer("in", 0.6f)`)
10. `Stop All (Idle)` (`clearAll()`)

## Connected Notes
- [[PROGRESS]] — Main AI brain
- [[FoxLineState]] — Actions target state engine
- [[MainActivity]] — Mounted in root container
- [[DepthWallpaperAndClock]] — Long-press hook on clock Layer 2
