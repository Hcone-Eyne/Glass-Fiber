---
title: File Map — App Shell, Build Config, Spring Engine & Theme
date: 2026-11-10
tags:
  - fox-mobile
  - file-map
  - core
  - theme
---

# File Map — App Shell, Build Config, Spring Engine & Theme

Back to Index: [[File_Map_Index]] | Main Brain: [[../../../PROGRESS|PROGRESS.md]]

---

## 1. [`apps/fox-mobile/build.gradle.kts`](file:///Users/enoch/Desktop/Glass_Main%28me%29/Android%20Launcher%21/apps/fox-mobile/build.gradle.kts) (9 lines — **ACTIVE**)
- **Purpose:** Root Gradle configuration for `apps/fox-mobile`.
- **Critical Line (Lines 6–8):**
  ```kotlin
  allprojects {
      layout.buildDirectory.set(file("/tmp/fox-gradle-build/${project.name}"))
  }
  ```
- **Why:** The workspace path contains an exclamation mark (`Android Launcher!`), which breaks Java's `JarURLConnection` when Android Gradle Plugin processes `R.jar`. Redirecting `buildDirectory` to `/tmp/fox-gradle-build/` solves this permanently (see [[../02_Mistakes_And_How_We_Overcame_Them#Mistake 2 Exclamation Mark in Workspace Folder Path Breaking R jar|Mistake 2]]).

---

## 2. [`apps/fox-mobile/app/build.gradle.kts`](file:///Users/enoch/Desktop/Glass_Main%28me%29/Android%20Launcher%21/apps/fox-mobile/app/build.gradle.kts) (73 lines — **ACTIVE**)
- **Purpose:** App module Gradle build file (Pure Kotlin + Jetpack Compose, zero NDK/CMake).
- **Key Sections:**
  - **Lines 6–15:** `namespace = "com.fox.mobile"`, `compileSdk = 35`, `minSdk = 26`, `targetSdk = 35`.
  - **Lines 17–38 (`buildTypes`):**
    - `release`: `isMinifyEnabled = false`.
    - `benchmark` (Lines 28–37): Cloned from `release` with `isMinifyEnabled = true`, `isShrinkResources = true`, `isDebuggable = false`, signed with `debug` signing config so it installs directly via `adb install -r /tmp/fox-gradle-build/app/outputs/apk/benchmark/app-benchmark.apk`.
  - **Lines 53–72 (`dependencies`):** Compose BOM `2024.09.00`, `activity-compose:1.9.2`, `okhttp:4.12.0`, `androidx.profileinstaller:profileinstaller:1.4.1`, `junit:4.13.2`, `org.json:json:20240303`.

---

## 3. [`apps/fox-mobile/app/src/main/AndroidManifest.xml`](file:///Users/enoch/Desktop/Glass_Main%28me%29/Android%20Launcher%21/apps/fox-mobile/app/src/main/AndroidManifest.xml) (35 lines — **ACTIVE**)
- **Permissions:**
  - `INTERNET`, `ACCESS_NETWORK_STATE` (for Phase 13 `FoxLinkClient`).
  - `com.android.alarm.permission.SET_ALARM` (for Phase 4 `Set 7:00` alarm chip).
  - *(Note for Phase 6: Add `<uses-permission android:name="android.permission.QUERY_ALL_PACKAGES" />` when loading real installed apps into the Search Drawer).*
- **Intent Filters:** `MAIN` + `LAUNCHER` + `HOME` + `DEFAULT` on `.MainActivity`.

---

## 4. [`MainActivity.kt`](file:///Users/enoch/Desktop/Glass_Main%28me%29/Android%20Launcher%21/apps/fox-mobile/app/src/main/java/com/fox/mobile/MainActivity.kt) (102 lines — **ACTIVE v30**)
- **Purpose:** Single-activity root host for the Fox Launcher.
- **Line-by-Line Map:**
  - **Lines 25–40 (`onCreate`):** Calls `requestHighRefreshRate()`, configures edge-to-edge (`WindowCompat.setDecorFitsSystemWindows(window, false)`), transparent system bars, and `hideNavigationBar()`.
  - **Lines 41–75 (`setContent`):**
    - Remembers `foxLineState = remember { FoxLineState() }`.
    - Mounts `DepthWallpaperAndClock(onClockLongPress = { foxLineState.debugMenuOpen = true })`.
    - Inside `overlayContent`:
      1. `Phase4HomeContent(box = wpBox, foxLineState = foxLineState)`
      2. `TransferRippleCanvas(state = foxLineState)`
      3. `FoxWakeOverlay(state = foxLineState, onContinueMac = {}, onSetAlarm = { ... })`
      4. `FoxLineTouchStrip(state = foxLineState, modifier = Modifier.align(Alignment.BottomCenter))`
      5. `LiveActivityIslandCard(state = foxLineState)`
      6. `FoxLineDebugMenu(state = foxLineState)`
  - **Lines 68–90 (`requestHighRefreshRate`):** Selects highest refresh-rate `Display.Mode` matching physical resolution (Mode ID 2 `120.00001 Hz` on POCO C71).
  - **Lines 92–99 (`hideNavigationBar`):** Hides `WindowInsetsCompat.Type.navigationBars()` with `BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE` so Android's white system gesture bar does not overlap the Fox Line.

---

## 5. [`core/FoxSpring.kt`](file:///Users/enoch/Desktop/Glass_Main%28me%29/Android%20Launcher%21/apps/fox-mobile/app/src/main/java/com/fox/mobile/core/FoxSpring.kt) (183 lines — **ACTIVE v30**)
- **Purpose:** Pure Kotlin 1D and multi-channel spring physics engine (replaced all legacy C/JNI code in Phase 1).
- **Classes & APIs:**
  - **`Spring1D` (Lines 14–106):**
    - `value`, `v` (velocity), `target`, `k` (stiffness), `c` (damping).
    - `Spring1D.fromApple(initialValue, response, damping)` — converts Apple spring parameters via `k = (2π / response)²` and `c = 4π · damping / response`.
    - `step(dt, substeps = 4)` — integrates `a = -k * (value - target) - c * v` in 4 substeps per frame.
    - `retarget(newTarget)` — updates `target` while preserving current velocity `v` (zero velocity reset on interrupt).
    - `retargetApple(newTarget, response, damping)` & `retargetKC(newTarget, k, c)`.
    - `isAtRest(posThreshold = 0.001f, velThreshold = 0.002f)`.
  - **`SpringChannelSet` (Lines 112–163):**
    - Manages named `Spring1D` channels (ready for Phase 7's 6-channel liquid morph `x, y, w, h, r, o`).
  - **`FoxSpring` object (Lines 169–182):** Single-step stateless helper `FoxSpring.step(...)`.

---

## 6. [`ui/theme/FoxTheme.kt`](file:///Users/enoch/Desktop/Glass_Main%28me%29/Android%20Launcher%21/apps/fox-mobile/app/src/main/java/com/fox/mobile/ui/theme/FoxTheme.kt) (212 lines — **ACTIVE v30**)
- **Purpose:** Single source of truth for colors, fonts, typography, motion constants, and the `FoxInkBox` material.
- **Line-by-Line Map:**
  - **Lines 35–48 (`FoxFonts`):** `Bricolage` (`R.font.bricolage_grotesque`), `Figtree` (`R.font.figtree`), `JetBrainsMono` (`R.font.jetbrains_mono`).
  - **Lines 53–110 (`FoxColors`):** The 10 exact tokens from `Fox_Line_v30.html`:
    - `screenBg` (`#07080C`), `inkSurface` (`#9914161E`), `inkPanel` (`#BD101219`), `inkMenu` (`#EB141620`), `hair` (`#24FFFFFF`), `t1` (`#F6F0E6`), `t2` (`#ADF6F0E6`), `ember` (`#F2B33D`), `ok` (`#7FD6A4`), `alert` (`#FF4D4D`), `lineWarm` (`#D9F6F0E6`).
  - **Lines 113–129:** Temporary legacy aliases (`BgVoid`, `CyanLens`, etc.) kept only so `SpotlightSheet.kt` compiles until Phase 6 replaces it.
  - **Lines 135–143 (`FoxMotion`):** `PressScale = 0.98f`, `EnterEasing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)`.
  - **Lines 172–194 (`FoxInkBox`):** Reusable dark translucent Ink card (`inkSurface` + `1.dp hair` border).
