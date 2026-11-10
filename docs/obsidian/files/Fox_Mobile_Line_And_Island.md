---
title: File Map — Fox Line Engine, Canvas, Island Card & Debug Menu
date: 2026-11-10
tags:
  - fox-mobile
  - file-map
  - fox-line
  - island-card
---

# File Map — Fox Line Engine, Canvas, Island Card & Debug Menu (Phase 5)

Back to Index: [[File_Map_Index]] | Main Brain: [[../../../PROGRESS|PROGRESS.md]]

---

## 1. [`ui/line/FoxLineState.kt`](file:///Users/enoch/Desktop/Glass_Main%28me%29/Android%20Launcher%21/apps/fox-mobile/app/src/main/java/com/fox/mobile/ui/line/FoxLineState.kt) (491 lines — **ACTIVE v30**)
- **Purpose:** Phase 5 state engine, urgency queue (`activeList`), song/order/timer/msg/xfer models, splash wave math, and haptics.
- **Line-by-Line Map:**
  - **Lines 22–86 (Data Models):**
    - `FoxActivityType`: `IDLE`, `MSG`, `MUSIC`, `TIMER`, `ORDER`, `XFER`, `CONT`, `FOX`.
    - `FoxSong`, `FoxTimerState`, `FoxMusicState`, `FoxOrderState`, `FoxOrderStage`, `FoxMsgState`, `FoxXferState`, `WaterDrop`.
  - **Lines 90–148 (`Companion`):**
    - `nowSec()`: Monotonic seconds clock (`SystemClock.uptimeMillis() / 1000f` with JVM `System.nanoTime()` fallback for unit tests).
    - `performFoxBuzz(view, ms)` & `performFoxBuzz(view, timings, amplitudes)`.
  - **Lines 150–201 (State Fields & Songs):**
    - `timer`, `music`, `order`, `msg`, `xfer`, `contT0Sec`, `swapped`, `openIslandType`, `foxAwake`, `foxClosing`, `foxCloseT0Sec`, `holdActive`, `holdT0Sec`, `holdProgress`, `splashT0Sec`, `splashAmp`, `debugMenuOpen`, `alarmSet`, `drops`.
    - `songs`: `"Night Drive"` (`#8E8BFF`) and `"Ember Rain"` (`#FF784F`) with exact `ART` gradient colors and 3 blob coordinates from `Fox_Line_v30.html`.
  - **Lines 208–224 (`orderStage`):**
    - `p = ((now - o.t0Sec) / 40f).coerceIn(0f, 1f)`.
    - Stages: `< 0.15f` `"Preparing"`, `< 0.85f` `"On the way"`, `< 1f` `"Arriving"`, `>= 1f` `"Delivered"` (`etaMinutes = max(0, ceil((1 - p) * 14))`).
  - **Lines 226–243 (`activeList` — Urgency Queue):**
    - Exact priority order: `XFER` → urgent/done `TIMER` (`done || timerLeft <= 10f`) → `MSG` → `CONT` → `ORDER` → normal `TIMER` → `MUSIC`.
    - If `swapped && list.size > 1 && !(timer?.done == true && list[0] == TIMER)`, swaps `list[0]` and `list[1]`. Returns `list.take(2)`.
  - **Lines 245–253 (`wavyBarOffset` & `isSplashActive`):**
    - `A = splashAmp * exp(-ageSec * 3.6f)`, `offset = A * sin(π * u) * sin(19u - 22 * ageSec)`.
  - **Lines 255–427 (Action Triggers):**
    - `startTimer`, `toggleTimerPause`, `addTimerMinute`, `stopTimer`, `startMusic`, `toggleMusicPlay`, `nextSong`, `startOrder`, `triggerMessage`, `summariseMessage`, `replyMessage`, `triggerContinue`, `triggerTransfer`, `splashBar`, `drop`, `openIsland`, `closeIsland`, `summonFox`, `closeFox`, `clearAll`.
  - **Lines 429–490 (`needsFrameLoop` & `tick`):**
    - Advances timer completion, `cont` drop at `1.1s` & clear at `3.3s`, `xfer` gulp/splash at `1.6s`, drop expiration, hold progress, and `foxClosing` reset after `0.26s`.

---

## 2. [`ui/line/FoxLineCanvas.kt`](file:///Users/enoch/Desktop/Glass_Main%28me%29/Android%20Launcher%21/apps/fox-mobile/app/src/main/java/com/fox/mobile/ui/line/FoxLineCanvas.kt) (1182 lines — **ACTIVE v30**)
- **Purpose:** Renders the bottom `56.dp` Fox Line touch strip, all 8 primary bar shapes, the secondary bead, the hold-to-wake glass capsule, the full-screen water ripple rings, and the Fox Wake action chips.
- **Line-by-Line Map:**
  - **Lines 74–104 (`GlassSpringState`):** Spring simulation (`k = 280/520/760`, `c = 20/44/50`, 4 substeps/frame) for the glass capsule (`w, h, o`) behind the Fox Line.
  - **Lines 127–492 (`FoxLineTouchStrip`):**
    - **Lines 164–328 (Calm 120Hz Frame Loop):** Waits on `snapshotFlow` when settled (`0 frames/sec` idle load); ticks `frameNowSec = now`, `state.tick(now, dt)`, `gs.step(...)`, `lvW`, and `lvBead` inside `withFrameNanos`.
    - **Lines 333–423 (Touch & Gesture Handling):**
      - Hidden via `graphicsLayer { alpha = if (state.openIslandType != null) 0f else 1f }` when Island Card is open.
      - Secondary bead hit zone (`48dp` centered at `beadXPx`) opens secondary activity's Island Card.
      - Horizontal swipe `> 30dp` toggles `state.swapped`.
      - Press & hold (`1.1s`) grows glass capsule with buzzes at `33%` and `66%`, waking Fox at `100%`; releasing early (`< 15%`) toggles the Island Card.
    - **Lines 425–491 (Canvas Draw):** Draws glass capsule (no orb, no halo), primary line (`renderPrimaryLine`), and secondary bead (`renderSecondaryBead`).
  - **Lines 497–773 (`renderPrimaryLine`):** Draws `IDLE`, `MSG`, `MUSIC`, `TIMER`, `ORDER`, `XFER`, `CONT`, and `FOX` shapes.
  - **Lines 778–847 (`renderSecondaryBead`):** Draws `6dp` bead at `12dp` gap to the right of the primary line.
  - **Lines 930–1016 (`TransferRippleCanvas`):** Full-screen non-interactive water ripple rings (`WaterDrop`) for phone ↔ Mac transfers. *(Note for Phase 7: Phase 7's liquid send/receive morph composable will call `foxLineState.drop(...)` and `foxLineState.splashBar(...)` right into this!)*
  - **Lines 1025–1133 (`FoxWakeOverlay`):** Dimmed backdrop (`45%` black) + bottom `.fox-chips` row (`"Continue on Mac"`, `"Alarm 7:00"`, `"Summarise Kavin"` / `"Focus 25 min"`).

---

## 3. [`ui/island/IslandCard.kt`](file:///Users/enoch/Desktop/Glass_Main%28me%29/Android%20Launcher%21/apps/fox-mobile/app/src/main/java/com/fox/mobile/ui/island/IslandCard.kt) (930 lines — **ACTIVE v30: Lines 49–754**)
- **Purpose:** Bottom-anchored floating Live Activity Island Card (`12dp` side margins, `14dp` bottom, `30dp` radius).
- **Line-by-Line Map:**
  - **Lines 49–162 (`LiveActivityIslandCard`):**
    - Only runs `withFrameNanos` (`needsLiveTick`) when `openType` is a live time-varying card (`ORDER`, running `TIMER`, active `XFER`).
    - Permanently composed in the tree and animated via `Modifier.graphicsLayer { alpha = p; scaleX = s; scaleY = s; translationY = ... }` for zero UI-thread composition spikes.
    - Includes full-screen outside-tap catcher (`state.closeIsland()`).
  - **Lines 168–642 (Activity Card Views):**
    - `IslandOrderContent` (`"Spice Route · 2 items"`, ETA in `#9FE3BC`, `OrderProgressBar` with `28×15dp` rider pill).
    - `IslandTimerContent` (`"Timer · Tea"`, `M:SS` or `"Time's up"` in alert red `#FF4D4D`, `Pause`/`+1 min`/`Cancel` or `Stop`/`+1 min`).
    - `IslandMusicContent` (`44dp` album art `Canvas` with gradient + 3 blobs, `Back`/`Pause`/`Next`).
    - `IslandMsgContent` (`"Kavin"`, `"Yes, 9 works"` / `"Summarise"`).
    - `IslandXferContent` (`"Sending to MacBook Air"`, `"MenuBarView.swift"`, `"2.4 MB"`).
    - `IslandDefaultContent` (`"Last on your Mac · 4 min ago"`, `"MenuBarView.swift"`, `Timer`/`Music`/`Continue`).
  - **Lines 763–930:** Legacy overload kept only for `WorkHomeScreen` legacy signature.

---

## 4. [`ui/line/FoxLineDebugMenu.kt`](file:///Users/enoch/Desktop/Glass_Main%28me%29/Android%20Launcher%21/apps/fox-mobile/app/src/main/java/com/fox/mobile/ui/line/FoxLineDebugMenu.kt) (316 lines — **ACTIVE v30**)
- **Purpose:** Temporary Phase 5 debug menu opened by long-pressing the `118sp` home clock (`foxLineState.debugMenuOpen = true`).
- **Buttons:** `Wake Fox`, `Splash Wave`, `Timer (20s)`, `Timer (Red/Done)`, `Music (Wave)`, `Food Order`, `Message (Drop)`, `Send File (In)`, `Receive File (Out)`, `Stop All (Idle)`, `Close Menu`.
