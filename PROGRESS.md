---
title: PROGRESS.md — Main AI & Developer Brain (Start Here)
updated: 2026-11-10
current_phase: Phase 6 In Progress (v40 direction; POCO drawer behavior verified, navigation styling remains)
last_commit: 2af3bcb ("phase 5: the fox line")
device: POCO C71 (25028PC03I, Android 15 HyperOS, Unisoc T603, 720x1640)
tags:
  - main-index
  - ai-memory
  - fox-mobile
---

# `PROGRESS.md` — Main AI & Developer Brain (Start Here)

> [!NOTE] Latest Steering (2026-11-10)
> The user supplied **Fox Line v40** as the updated product vision. Read [[docs/FOX_MOBILE_CURRENT_STATUS]] for the active navigation-bar issue, partial Phase 6 implementation, verification state, and v40 priorities. The old v30-only design lock is superseded where it conflicts with v40.

> [!IMPORTANT] AI Agent Cold-Start Rule (Read This First!)
> **Do NOT waste time scanning the entire repository from scratch.**
> 1. Read this `PROGRESS.md` first — it maps the current phase, every file in the workspace, what is **Active (`v30`)** vs **Legacy (pre-v30 placeholder)**, hardware rules for the **POCO C71 (`Unisoc T603`)**, and all **11 past mistakes** so you never repeat them.
> 2. Follow the Obsidian `[[WikiLinks]]` below into [`docs/obsidian/`](file:///Users/enoch/Desktop/Glass_Main%28me%29/Android%20Launcher%21/docs/obsidian) for deep file-level maps.
> 3. **Update `PROGRESS.md` and `docs/obsidian/02_Mistakes_And_How_We_Overcame_Them.md` at the end of every phase** before committing.

---

## 1. Where Is the Obsidian Vault?

- **Workspace Root:** `/Users/enoch/Desktop/Glass_Main(me)/Android Launcher!`
- **Obsidian Vault Directory:** `/Users/enoch/Desktop/Glass_Main(me)/Android Launcher!/docs/obsidian`
- **How to open in Obsidian on Mac:**
  Open **Obsidian.app** → *Open folder as vault* → select `/Users/enoch/Desktop/Glass_Main(me)/Android Launcher!/docs/obsidian` (or the root `/Users/enoch/Desktop/Glass_Main(me)/Android Launcher!`).

### Core Obsidian Vault Nodes
- **Main Hub:** [[00_Fox_Mobile_Progress_Hub]]
- **Interactive Visual Canvas Board:** [[Fox_Mobile_Map.canvas]]
- **Complete File-by-File Index:** [[File_Map_Index]]
- **Active v30 Components:**
  - [[MainActivity]] (`MainActivity.kt` — App host & refresh rate)
  - [[FoxSpring]] (`FoxSpring.kt` — Spring physics engine)
  - [[FoxTheme]] (`FoxTheme.kt` — 10 exact color tokens & Ink material)
  - [[DepthWallpaperAndClock]] (`DepthWallpaperAndClock.kt` — 3-layer depth & calm tilt)
  - [[WordsRepository]] (`WordsRepository.kt` — Quotes & mountain ridge fit)
  - [[WorkHomeScreen]] (`WorkHomeScreen.kt` — Home work block & Continue pill)
  - [[FoxLineState]] (`FoxLineState.kt` — Urgency queue & activity engine)
  - [[FoxLineCanvas]] (`FoxLineCanvas.kt` — Line touch strip & water ripple)
  - [[IslandCard]] (`IslandCard.kt` — Live Activity card)
  - [[FoxLineDebugMenu]] (`FoxLineDebugMenu.kt` — Clock long-press debug menu)
- **Pending Phases & Protocol:**
  - [[SpotlightSheet]] (`SpotlightSheet.kt` — **ACTIVE, Phase 6 WIP**: people/chat search opens on tap; app browsing requires upward drag)
  - [[FoxProtocol]] & [[FoxLinkClient]] (WebSocket protocol for Phase 13)
  - [[ContractTest]] (`ContractTest.kt` — Verification test suite)
- **Knowledge & Reference:**
  - [[Prototype_Fox_Line_v30]] (Exact prototype line lookup)
  - [[POCO_C71_Hardware_Rules]] (5 golden hardware rules for Unisoc T603)
  - [[02_Mistakes_And_How_We_Overcame_Them]] (All 11 mistakes post-mortem)
  - [[01_Architecture_And_Phase_Progress]] (Architecture breakdown)

---

## 2. Phase Status & Git Commit Ledger

| Phase | Title | Status | Commit | Key Metrics / Deliverables |
| :--- | :--- | :--- | :--- | :--- |
| **Phase 0** | Set up the Mac and the phone | **DONE** | *(Toolchain)* | JDK 17 (`/opt/homebrew/opt/openjdk@17`), SDK 35, Gradle 8.11.1, POCO C71 ADB (`9b010059305331323800204926576c`) |
| **Phase 1** | Clean up the last attempt | **DONE** | `c76a464` | Deleted all C/JNI/CMake/NDK; pure Kotlin `FoxSpring.kt` (`Spring1D`, `SpringChannelSet`); `/tmp/fox-gradle-build` |
| **Phase 2** | Theme, fonts and the material | **DONE** | `be8a09a` | Bundled `bricolage_grotesque.ttf`, `figtree.ttf`, `jetbrains_mono.ttf`; 10 exact tokens + `FoxInkBox` in `FoxTheme.kt` |
| **Phase 3** | The depth wallpaper and the clock | **DONE** | `1b60c30` | `DepthWallpaperAndClock.kt`: 191-pt ridge cut, 3-layer stack, 118sp clock behind ridge, accelerometer tilt parallax |
| **Phase 4** | The home content | **DONE** | `d4890d8` | `WordsRepository.kt` + `Phase4HomeContent`: `.work` block at 54% H, 52dp `Continue` pill, `Set 7:00` alarm chip, `#words` |
| **Phase 4.5** | Speed (120Hz vote + R8 benchmark) | **DONE** | `f2cf075` | Pre-baked screen-sized layers (-78% VRAM), offscreen GPU layers, 0fps idle tilt. **`p50=11ms, p90=15ms, p99=16ms, gpu=6ms, janky=0.00%`** |
| **Phase 5** | The Fox Line (the bar) | **DONE** | `2af3bcb` | `FoxLineState.kt`, `FoxLineCanvas.kt`, `IslandCard.kt`, `FoxLineDebugMenu.kt`. Urgency queue, 8 bar shapes, hidden system nav bar (`gpu_p50=6ms, gpu_p90=8ms`) |
| **Phase 6** | Search drawer + updated v40 direction | **IN PROGRESS (partially verified)** | — | `installDebug` passes on POCO C71; tap opens conversations only and upward drag reveals app categories/list. HyperOS system gesture handle and Fox line both show while `show_navigationbar=1`. More drawer behaviors and navigation QA remain. See [[docs/FOX_MOBILE_CURRENT_STATUS]]. |
| **Phase 7** | Send and receive | Queued | — | 1 liquid morphing composable with 6 spring channels (`x, y, w, h, r` + opacities `o, p`) and velocity carry-over |
| **Phase 8** | The dashboard | **IN PROGRESS (initial v40 screen integrated and checked)** | — | Pull-down from the clock parts the painting; dashboard is layered between wallpaper planes and was checked on POCO. Mac status, Continue, Needs You, alarm, fix review, and Spaces are wired in `FoxDashboard.kt`. Detailed action and visual polish QA remains. |
| **Phases 9–13** | Polish, Perf, AGSL, Video, Real Data | Queued | — | Full audit, `<180 MB` PSS, optional AGSL ripple, `screenrecord`, `FoxLinkClient` to Mac port `8787` |

---

## 3. Fast File Map (What Every File Does & Whether to Touch It)

> [!WARNING] Critical Distinction: Active `v30` vs Legacy Pre-Phase-1 Code
> Some files still contain legacy functions from before the Phase 1 reset for backwards-compilation compatibility until their phase arrives. **Always check the status column below before editing!**

### A. Android App (`apps/fox-mobile/`)
| File Path | Lines | Status | What Is Inside & Instant Context |
| :--- | :--- | :--- | :--- |
| `build.gradle.kts` | 9 | **ACTIVE** | Redirects `buildDirectory` to `/tmp/fox-gradle-build/${project.name}` because `!` in `Android Launcher!` breaks Java `JarURLConnection` on `R.jar`. |
| `app/build.gradle.kts` | 73 | **ACTIVE** | `compileSdk = 35`, `minSdk = 26`, Compose enabled, `benchmark` buildType (`isMinifyEnabled = true`, R8), `profileinstaller:1.4.1`, zero NDK/CMake. |
| `app/proguard-rules.pro` | 15 | **ACTIVE** | R8 keep rules for `com.fox.mobile.**` and `org.json.**` used by `benchmark` APK. |
| `app/src/main/AndroidManifest.xml` | 35 | **ACTIVE** | Declares `MainActivity` as `HOME` + `DEFAULT` launcher, `SET_ALARM` & `INTERNET` permissions, and launcher activity query visibility for the app drawer. |
| `MainActivity.kt` | ~240 | **ACTIVE (v40 WIP)** | Edge-to-edge setup, system inset tracking, capsule search/app drawer, pull-down dashboard state, ridge parting via `dashProgressProvider`, and existing Fox Line/island interactions. |
| `core/FoxSpring.kt` | 183 | **ACTIVE (v30)** | Pure Kotlin `Spring1D` & `SpringChannelSet` (`k = (2π/response)²`, `c = 4π·damping/response`, 4 substeps/frame, velocity carry-over on `retarget()`). |
| `ui/theme/FoxTheme.kt` | 212 | **ACTIVE (v30)** | 10 exact `FoxColors` tokens (`screenBg`, `inkSurface`, `inkPanel`, `inkMenu`, `hair`, `t1`, `t2`, `ember`, `ok`, `alert`, `lineWarm`), `FoxFonts` (`Bricolage`, `Figtree`, `JetBrainsMono`), `FoxType`, `FoxMotion`, `FoxInkBox`. *(Lines 113–129 keep legacy aliases for `SpotlightSheet.kt` until Phase 6)*. |
| `ui/theme/Phase2DebugScreen.kt` | 432 | **DEBUG ONLY** | Phase 2 typography & Ink material verification screen. Not mounted in `MainActivity`. |
| `ui/depth/DepthWallpaperAndClock.kt` | 821 | **ACTIVE (v30)** | `WallpaperBox.calculate`, 191-pt `RIDGE_NORMALIZED_POINTS`, `TiltParallaxState` (`0.08` low-pass, `0.002f` deadband, sleeps at `0 fps` after 1s stillness), pre-baked screen-sized `backBmp` & ridge-clipped `frontBmp` (-78% VRAM), `118sp` offscreen-cached clock, `onClockLongPress` hook, `dashProgressProvider` (ready for Phase 8 ridge parting!). |
| `ui/home/WordsRepository.kt` | 175 | **ACTIVE (v30)** | `WordItem`, `SampleWordsRepository` (3 quotes, time-of-day initial index), `maxRidgeYInSpan`, `selectWordsFit` (`0.92f/16sp`, `0.80f/15sp`, `0.68f/14sp`). |
| `ui/home/WorkHomeScreen.kt` | 739 | **MIXED** | **Lines 329–739 (`Phase4HomeContent`, `ContinueActionPill`, `NextAlarmRow`) are ACTIVE (v30)** with pre-composed offscreen `#words` layers (`0.00%` jank). **Lines 78–311 (`WorkHomeScreen` overload) are LEGACY pre-Phase-1** (not called by `MainActivity`). |
| `ui/line/FoxLineState.kt` | 491 | **ACTIVE (v30)** | Phase 5 state engine: `activeList()` urgency queue (`XFER -> urgent/done TIMER -> MSG -> CONT -> ORDER -> TIMER -> MUSIC`), `orderStage()`, `wavyBarOffset()`, `performFoxBuzz()`, `tick()`. |
| `ui/line/FoxLineCanvas.kt` | 1182 | **ACTIVE (v30)** | `FoxLineTouchStrip` (`56dp` tall, 8 primary bar shapes, secondary `6dp` bead at `12dp` gap, 1.1s hold-to-wake Fox glass capsule, `0 fps` when idle), `TransferRippleCanvas`, `FoxWakeOverlay`. |
| `ui/island/IslandCard.kt` | 930 | **ACTIVE (v30)** | **Lines 49–754 (`LiveActivityIslandCard`) are ACTIVE (v30)** (`30dp` radius, 6 activity views, GPU `graphicsLayer` open/close, outside-tap dismiss). **Lines 763–930 are LEGACY** compatibility overload. |
| `ui/line/FoxLineDebugMenu.kt` | 316 | **ACTIVE (v30)** | Long-press clock debug popup with 10 instant triggers for all Phase 5 bar states. |
| `ui/drawer/SpotlightSheet.kt` | ~330 | **ACTIVE (Phase 6 WIP)** | Native app launcher and conversation-search panel; tap-to-search and upward-drag-to-browse-apps are verified on POCO. Remaining: real contact/chat source, file/action search coverage, grid QA, and v40 visual polish. |
| `ui/dashboard/FoxDashboard.kt` | ~270 | **ACTIVE (Phase 8 WIP)** | Scrollable v40 Mac status, Continue, Needs You, alarm, suggested fix, and Spaces cards; pointer/hold controls are local demo callbacks. Dashboard is inserted between wallpaper layers. |
| `protocol/FoxProtocol.kt` | 419 | **PHASE 13 READY** | Pure Kotlin `FoxSchemaValidator`, `FoxProtocolCatalog`, `ReplayGuard`, `FoxTask`, `FoxStateSpec`. Tested by `ContractTest.kt`. Do not touch until Phase 13. |
| `net/FoxLinkClient.kt` | 286 | **PHASE 13 READY** | OkHttp WebSocket client to Mac `fox-desk` with exponential backoff, `FoxSchemaValidator`, and `ReplayGuard`. Do not wire into UI until Phase 13. |
| `test/.../ContractTest.kt` | 628 | **ACTIVE (v30)** | JUnit 4 suite verifying: (1) Protocol schemas & 53 state catalog, (2) Phase 1 `FoxSpring` physics, (3) Phase 2 `FoxColors` & `wallpaper.webp`, (4) Phase 3 `WallpaperBox`, 191 ridge points & tilt, (5) Phase 4 `WordsRepository` & ridge fit, (6) Phase 4.5 calm tilt deadband & spring settle, (7) Phase 5 `FoxLineState` urgency queue, order stages & splash wave. |

### B. Mac Daemon (`apps/fox-desk/`) & Shared Protocol (`packages/fox-protocol/`)
- **Rule from `FOX_MOBILE_PLAN.md`:** Do **not** touch `apps/fox-desk/` or `packages/fox-protocol/` until **Phase 13**.
- Details mapped in [[docs/obsidian/files/Fox_Mobile_Pending_Phase6_To_13|Fox_Mobile_Pending_Phase6_To_13]].

---

## 4. Golden Hardware & Performance Rules for POCO C71 (`Unisoc T603`)

Every future phase (**Phases 6–13**) MUST obey these rules discovered during Phases 3–5 (full details in [[docs/obsidian/02_Mistakes_And_How_We_Overcame_Them|02_Mistakes_And_How_We_Overcame_Them]]):

1. **Never allocate oversized bitmaps or clip complex paths per-frame:** Pre-bake static layers at screen resolution once on load (`DepthWallpaperAndClock.kt` saved 78% VRAM and 5ms/frame GPU time this way).
2. **Never animate `Modifier.blur(dp)` frame-by-frame or use `AnimatedVisibility` on heavy cards:** Keep heavy cards/text composed in the tree and animate `alpha`, `scaleX`, `scaleY`, and `translationY` inside `Modifier.graphicsLayer { ... }` lambdas so Compose skips composition and layout phases completely.
3. **Every `withFrameNanos` loop MUST sleep (`0 frames/sec`) when settled:** Use `snapshotFlow` or a conflated channel so idle screens schedule zero frames.
4. **Inside `Canvas { ... }`, always read a `mutableFloatStateOf` frame timestamp** updated inside `withFrameNanos` — calling a non-State function like `FoxLineState.nowSec()` alone will not invalidate the `Canvas` draw scope once layout dimensions settle.
5. **Always benchmark the R8-minified `benchmark` APK (`./gradlew assembleBenchmark`)** with the statusbar collapsed (`cmd statusbar collapse`) and never run `screencap -p` inside the `dumpsys gfxinfo` window.
