---
title: File Map — Pending Phase 6 Drawer, Phase 13 Protocol & Fox-Desk
date: 2026-11-10
tags:
  - fox-mobile
  - file-map
  - phase-6
  - phase-13
  - protocol
---

# File Map — Pending Phase 6 Drawer, Phase 13 Protocol & Fox-Desk

Back to Index: [[File_Map_Index]] | Main Brain: [[../../../PROGRESS|PROGRESS.md]]

---

## 1. [`ui/drawer/SpotlightSheet.kt`](file:///Users/enoch/Desktop/Glass_Main%28me%29/Android%20Launcher%21/apps/fox-mobile/app/src/main/java/com/fox/mobile/ui/drawer/SpotlightSheet.kt) (194 lines — **LEGACY: MUST REPLACE IN PHASE 6**)

> [!CAUTION] Do NOT Reuse the Styling in Current `SpotlightSheet.kt`!
> The current `SpotlightSheet.kt` is a leftover pre-Phase-1 file that is **not** mounted in `MainActivity.kt`.
> - Lines 79–85 draw a `CyanLens` / `EmberWarm` horizontal gradient border, which violates `FOX_MOBILE_PLAN.md` Rule #3 (*"No chromatic edge refraction. The search capsule is plain Ink (`inkSurface` + `hair`)."*).
> - Lines 118–122 draw a Mac `⌘K` badge instead of the `Fox_Line_v30.html` `⋯` view-mode button (`List` vs `Grid`).

### What Phase 6 (`The search drawer`) Must Build Instead (Replacing `SpotlightSheet.kt`):
1. **Home Search Capsule (`.search` on Home screen):**
   - `44.dp` tall, `22.dp` radius, `FoxColors.inkSurface` (`#9914161E`) + `1.dp FoxColors.hair` border (NO rainbow/chromatic border!).
   - Search icon + `"Search apps and files"` in `FoxColors.t2`.
   - Positioned at `bottom = 54.dp`, `left = 22.dp`, `right = 22.dp` on the Home screen.
   - Tapping it (or swiping up from the bottom half of the home screen when the dashboard is closed) opens the Spotlight Drawer.
2. **Spotlight Drawer Layout (`.drawer` in `Fox_Line_v30.html` lines 124–160, 1002–1136):**
   - Anchored near the bottom: `left = 10.dp`, `right = 10.dp`, `bottom = 30.dp`, `height = 56% of H`, `display = Column`.
   - **Top Search Capsule (`.dr-search`):** `44.dp` tall, `999.dp` pill radius, `inkPanel` (`#BD101219`), `1.dp hair` border:
     - Magnifying glass icon (`15dp`) + text input (`"Search apps, files, contacts, or 24*7"` in `Figtree 15sp t1`) + `28dp` circular `⋯` button on the right.
     - Tapping `⋯` opens the `.dr-menu` popover (`top = 50.dp, right = 4.dp, width = 150.dp, radius = 16.dp, inkMenu + hair`) with two options: **`List`** (default) and **`Grid`** (`4 columns`).
   - **9.dp gap** (`Spacer(Modifier.height(9.dp))`).
   - **Results Panel (`.dr-panel`):** Fills remaining height (`weight(1f)`), `30.dp` corner radius, `inkPanel` (`#BD101219`), `1.dp hair` border, `padding(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 10.dp)`.
   - **Category Chips (`.dr-cats`):** `All`, `Apps`, `Work`, `Setup`, `Social`, `Media` (`28.dp` height, `999.dp` radius, active chip has `t1` background and `#121318` dark text).
   - **5 Recent Items Row (`.dr-rec`):** 5 columns (`Files`, `Calendar`, `Notes`, `Browser`, `Clock`) stored in preferences/state, updating whenever an app is launched.
   - **Real Installed Apps + Prototype Fallback:** Load real launchable apps via `PackageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)` on `Dispatchers.IO`, cached in memory, plus tapping launches `startActivity(launchIntent)`.
   - **Search Modes:**
     - Empty query → Recents row + categorized app list (or 4-column grid).
     - Math expression (e.g. `24*7`, `(12+8)*3`) → Inline calculator answer card (`= 168`, tap to copy to clipboard).
     - Contact match (`Kavin`, `Mom`, `Arun`, `Priya`, `Prof. Ramesh`) → Contact card with `Call` (`ACTION_DIAL`) and `WhatsApp`/`Message` (`ACTION_VIEW` `sms:`) buttons.
     - Quick action match (`Timer 20s`, `Alarm 7:00`, `Continue on Mac`) → Action row triggering `foxLineState`.
3. **1:1 Drag & Spring Physics:**
   - Follows finger `1:1` on vertical drag (`dy`).
   - Velocity threshold `> 500 dp/s` or position `> 30%` travel commits open (`k = 230, c = 26` via `FoxSpring`) or close (`k = 320, c = 34`).
   - Home content (`Phase4HomeContent`) dims to `50%` opacity and scales to `0.975` as drawer progress goes `0 -> 1` (inside `Modifier.graphicsLayer`!).

---

## 2. [`test/java/com/fox/mobile/ContractTest.kt`](file:///Users/enoch/Desktop/Glass_Main%28me%29/Android%20Launcher%21/apps/fox-mobile/app/src/test/java/com/fox/mobile/ContractTest.kt) (628 lines — **ACTIVE v30**)
- **Purpose:** JUnit 4 unit test suite run on every phase (`./gradlew testDebugUnitTest`).
- **Tests Inside:**
  - `testAllValidFixturesPass()` & `testAllInvalidFixturesFailAtExpectedPath()` (Lines 52–113) — validates `FoxProtocol.kt` against `packages/fox-protocol/fixtures`.
  - `testReplayGuardRejectsDuplicateAndSkew()` (Lines 115–145).
  - `testSpringPhysicsDeterminismAndInterrupt()` (Lines 147–194) — Phase 1 `FoxSpring` tests.
  - `testPhase2ThemeTokensAndWallpaperAsset()` (Lines 196–229) — Phase 2 color tokens & `wallpaper.webp`.
  - `testPhase3DepthWallpaperAndTiltMath()` (Lines 241–300) — Phase 3 `WallpaperBox`, 191 ridge points, tilt low-pass.
  - `testPhase4WordsRepositoryAndRidgeFit()` (Lines 302–398) — Phase 4 quotes & mountain ridge clearance on `720×1640`.
  - `testPhase4_5CalmTiltAndSpringSettle()` (Lines 428–523) — Phase 4.5 `0.002f` deadband & `<180` frame settle.
  - `testPhase5FoxLineMathAndUrgencyQueue()` (Lines 525–627) — Phase 5 urgency queue, order stages & splash wave math.

---

## 3. Phase 13 Protocol & Mac Daemon Files (**DO NOT TOUCH UNTIL PHASE 13**)

| File Path | Lines | Description |
| :--- | :--- | :--- |
| `apps/fox-mobile/.../protocol/FoxProtocol.kt` | 419 | Pure Kotlin `FoxSchemaValidator`, `FoxProtocolCatalog`, `ReplayGuard`, `FoxTask`, `FoxStateSpec`. |
| `apps/fox-mobile/.../net/FoxLinkClient.kt` | 286 | OkHttp WebSocket client (`ws://<mac-ip>:8787`) with auto-`hello`, schema validation, replay guard, and exponential backoff (`1s -> 16s`). |
| `apps/fox-desk/Package.swift` | 26 | Swift Package Manager manifest for the macOS daemon (`FoxDeskCore` + `FoxDeskApp` + `FoxDeskTests`). |
| `apps/fox-desk/Sources/FoxDeskApp/main.swift` | 98 | macOS CLI/daemon entrypoint starting `Collector`, `Store`, and WebSocket `Server` on port `8787`. |
| `apps/fox-desk/Sources/FoxDeskCore/Server.swift` | 184 | Swift NIO / Network.framework WebSocket server broadcasting `task.update`, `task.list`, and `state.event`. |
| `apps/fox-desk/Sources/FoxDeskCore/Collector.swift` | 142 | Polls active Xcode/VSCode/Cursor/Terminal files and git branch status on the Mac. |
| `apps/fox-desk/Sources/FoxDeskCore/Scrubber.swift` | 64 | Redacts secrets/tokens/paths before broadcasting over FoxLink. |
| `apps/fox-desk/Sources/FoxDeskCore/Store.swift` | 88 | In-memory & JSON persistence for recent Mac tasks. |
| `apps/fox-desk/Sources/FoxDeskCore/FoxProtocol.swift` | 312 | Swift mirror of `FoxSchemaValidator` and `ReplayGuard`. |
| `packages/fox-protocol/schema/*` | 7 files | Canonical JSON Schemas (`envelope.schema.json`, `hello.json`, `task.json`, `task.update.json`, `task.list.json`, `state.event.json`, `states.json`). |
| `packages/fox-protocol/fixtures/*` | 5 files | Golden valid (`01`–`04`) and invalid (`cases.json`) test vectors shared by Swift, Kotlin, and Python. |
