---
title: MainActivity.kt
tags:
  - active-v30
  - app-shell
---

# `MainActivity.kt`

- **Source File:** `apps/fox-mobile/app/src/main/java/com/fox/mobile/MainActivity.kt`
- **Role:** Single-Activity host for the Fox Launcher.

## Key Responsibilities
1. **Edge-to-Edge System UI:**
   - Calls `WindowCompat.setDecorFitsSystemWindows(window, false)`
   - Sets status bar and navigation bar to transparent
2. **Display Refresh Rate:**
   - Calls `requestHighRefreshRate()`, registering Mode ID 2 (`120.00001 Hz`) on the POCO C71.
3. **Hide Bottom Gesture Bar:**
   - Calls `hideNavigationBar()`, using `WindowInsetsControllerCompat.hide(WindowInsetsCompat.Type.navigationBars())` so Android's bottom gesture pill does not overlap the Fox Line.
4. **Composing the Active Surface:**
   - Hosts [[DepthWallpaperAndClock]]
   - In overlay: [[WorkHomeScreen]] (`Phase4HomeContent`), [[FoxLineCanvas]] (`TransferRippleCanvas`, `FoxWakeOverlay`, `FoxLineTouchStrip`), [[IslandCard]] (`LiveActivityIslandCard`), and [[FoxLineDebugMenu]].

## Connected Notes
- [[PROGRESS]] — Main AI brain & ledger
- [[00_Fox_Mobile_Progress_Hub]] — Progress MOC
- [[DepthWallpaperAndClock]] — 3-layer depth wallpaper & clock
- [[WorkHomeScreen]] — Home work block & words line
- [[FoxLineCanvas]] — Fox Line touch strip & wake overlay
- [[IslandCard]] — Live Activity island card
- [[FoxLineDebugMenu]] — Clock long-press debug menu
- [[POCO_C71_Hardware_Rules]] — Hardware performance rules
- [[02_Mistakes_And_How_We_Overcame_Them]] — Navigation bar overlap fix
