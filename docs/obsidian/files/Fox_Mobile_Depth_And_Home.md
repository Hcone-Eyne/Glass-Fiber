---
title: File Map — Depth Wallpaper, Clock & Home Content
date: 2026-11-10
tags:
  - fox-mobile
  - file-map
  - depth-wallpaper
  - home
---

# File Map — Depth Wallpaper, Clock & Home Content

Back to Index: [[File_Map_Index]] | Main Brain: [[../../../PROGRESS|PROGRESS.md]]

---

## 1. [`ui/depth/DepthWallpaperAndClock.kt`](file:///Users/enoch/Desktop/Glass_Main%28me%29/Android%20Launcher%21/apps/fox-mobile/app/src/main/java/com/fox/mobile/ui/depth/DepthWallpaperAndClock.kt) (821 lines — **ACTIVE v30**)
- **Purpose:** Phase 3 + Phase 4.5 3-layer depth wallpaper (`wallpaper.webp`), 191-point ridge cut, `118sp` clock tucked behind the ridge, calm accelerometer tilt parallax, and Phase 5 clock long-press hook.
- **Line-by-Line Map:**
  - **Lines 49–83 (`WallpaperBox`):**
    - `WallpaperBox.calculate(screenW, screenH)` computes `ih = 1.1f * screenH`, `iw = ih * 1136f / 615f`, `left = -(420f / 1136f) * iw`, `top = -0.05f * screenH`.
  - **Lines 89–319 (`RIDGE_NORMALIZED_POINTS`):**
    - All **191 `(x, y)` normalized points** from `docs/prototypes/assets/ridge.json`.
  - **Lines 326–377 (`buildRidgePath` & `ridgeYAtScreenX`):**
    - `ridgeYAtScreenX(screenX, box)` interpolates the ridge Y coordinate in screen pixels for any `screenX` (used by Phase 4 `selectWordsFit` so `#words` never touches the mountain ridge).
  - **Lines 389–564 (`TiltParallaxState` & `rememberTiltParallaxState`):**
    - Low-pass `0.08f` smoothing per frame with a **`0.002f` calm-tilt deadband**.
    - Sleeps completely (**`0 frames/sec`**) after 1 second of stillness via `Channel<Unit>(Channel.CONFLATED)` and unregisters the sensor on `Lifecycle.Event.ON_PAUSE`.
  - **Lines 597–821 (`DepthWallpaperAndClock` composable):**
    - Parameters: `tiltState`, `dashProgressProvider: () -> Float = { 0f }` *(ready for Phase 8 dashboard pull-down!)*, `onClockLongPress: (() -> Unit)? = null`, `overlayContent`.
    - **Lines 637–674 (`prebakedLayers`):** Pre-bakes screen-sized (`(screenW + 64) × (screenH + 64)`) `backBmp` and ridge-clipped `frontBmp` once on load (**-78% GPU memory**, zero per-frame stencil clipping).
    - **Lines 680–702 (Layer 1 — Back Sky):** `graphicsLayer { translationX = tiltX * 5f; translationY = tiltY * 4f - 0.20f * screenH * p }`.
    - **Lines 708–766 (Layer 2 — Clock & Date):** At `start = 22.dp, top = 24.5% H`, `CompositingStrategy.Offscreen`, `combinedClickable(onLongClick = onClockLongPress)`, `translationX = tiltX * 10f`, `translationY = tiltY * 8f - 0.20f * screenH * p`.
    - **Lines 772–793 (Layer 3 — Front Mountain Ground):** `graphicsLayer { translationX = tiltX * 18f; translationY = tiltY * 13f + 0.52f * screenH * p }`.
    - **Lines 799–813 (Layer 4 — 130dp Bottom Dark Fade):** Vertical gradient `Transparent -> #8C050609 -> #D9050609`.
    - **Line 819 (Layer 5 — `overlayContent(box)`):** Renders home content, Fox Line, Island Card, etc. above the wallpaper fade.

---

## 2. [`ui/home/WordsRepository.kt`](file:///Users/enoch/Desktop/Glass_Main%28me%29/Android%20Launcher%21/apps/fox-mobile/app/src/main/java/com/fox/mobile/ui/home/WordsRepository.kt) (175 lines — **ACTIVE v30**)
- **Purpose:** Phase 4 quote repository and dynamic mountain-ridge text fitting algorithm.
- **Line-by-Line Map:**
  - **Lines 12–34 (`WordItem`):** `kind`, `text`, `attribution`, `formattedAttribution` (`"— Philippians 4:13 · Your verse"`), `contentDescription`.
  - **Lines 39–84 (`SampleWordsRepository`):**
    - 3 quotes matching `Fox_Line_v30.html`:
      0. `"I can do all things through Christ which strengtheneth me."` (`Philippians 4:13`, `Your verse`) — morning (`hour < 12`).
      1. `"Stay hungry. Stay foolish."` (`Steve Jobs`, `Saved quote`) — afternoon (`hour < 18`).
      2. `"Revise unit 3 tonight. Your phone goes quiet at 1 AM."` (`Fox reminder`) — evening (`hour >= 18`).
  - **Lines 97–174 (`maxRidgeYInSpan` & `selectWordsFit`):**
    - Tests `(0.92f, 16sp)`, `(0.80f, 15sp)`, `(0.68f, 14sp)` in order and picks the first candidate whose top edge (`wordsBottomPx - heightPx`) clears `maxRidgeYInSpan(...) + 10.dp`.

---

## 3. [`ui/home/WorkHomeScreen.kt`](file:///Users/enoch/Desktop/Glass_Main%28me%29/Android%20Launcher%21/apps/fox-mobile/app/src/main/java/com/fox/mobile/ui/home/WorkHomeScreen.kt) (739 lines — **MIXED: Lines 329–739 ACTIVE v30, Lines 78–311 LEGACY**)
- **CRITICAL NOTE FOR AI AGENTS:**
  - **Lines 329–739 (`Phase4HomeContent`, `ContinueActionPill`, `NextAlarmRow`, and `WorkHomeScreen(box: WallpaperBox)`) are ACTIVE (`v30`)** and mounted by `MainActivity.kt`.
  - **Lines 78–311 (`WorkHomeScreen(currentTask, statesByKey, ...)`) are LEGACY pre-Phase-1** (kept only so nothing breaks before Phase 6 removes `SpotlightSheet`). Do NOT mount lines 78–311!
- **Active `v30` Section (Lines 329–739):**
  - **`Phase4HomeContent(box, foxLineState, modifier)` (Lines 329–581):**
    - Pre-computes `fitsList` (`selectWordsFit`) for all 3 quotes once in `remember(screenW, screenH, box)`.
    - Pre-composes all 3 `#words` quote blocks with `CompositingStrategy.Offscreen` and swaps them on tap via `graphicsLayer { alpha = if (wordIndex == idx) swapAlpha.value else 0f }` (**0.00% jank**).
    - Places `#words` at `workTopY - 14.dp - placeable.height` and `.work` at `workTopY = 0.54f * screenH`.
    - *(Note for Phase 6 & Phase 8: When the Search Drawer opens in Phase 6, home content dims to 50% opacity and scales to `0.975`; when the Dashboard pulls down in Phase 8, `.work` moves down by `0.52 * H * p * 1.05` and fades by `1 - 1.6 * p` — apply both inside `Modifier.graphicsLayer { ... }`!)*
  - **`ContinueActionPill(foxLineState)` (Lines 583–678):**
    - Isolated recomposition scope for the `52.dp` Ink `Continue →` pill button. Calls `foxLineState?.triggerContinue()` and animates `"Sending to your Mac"` → `"Open on your Mac"` → `"Continue"`.
  - **`NextAlarmRow(foxLineState)` (Lines 680–731):**
    - Isolated recomposition scope for `"Exam tomorrow 9:30 · no alarm"` + `"Set 7:00"` chip (`AlarmClock.ACTION_SET_ALARM`). Syncs with `foxLineState.alarmSet`.
