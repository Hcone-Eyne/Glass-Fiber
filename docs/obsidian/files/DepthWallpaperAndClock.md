---
title: DepthWallpaperAndClock.kt
tags:
  - active-v30
  - depth-wallpaper
---

# `DepthWallpaperAndClock.kt`

- **Source File:** `apps/fox-mobile/app/src/main/java/com/fox/mobile/ui/depth/DepthWallpaperAndClock.kt`
- **Role:** 3-layer depth wallpaper, 191-point mountain ridge cut, 118sp tucked clock, calm accelerometer tilt parallax.

## 3 Layers & Ridge Geometry
1. **Layer 1 (Sky):** Translates `(tiltX * 5f, tiltY * 4f - 0.20f * H * p)`.
2. **Layer 2 (Clock & Date):** At `start = 22.dp, top = 24.5% H`, Bricolage 118sp Bold tucked behind Layer 3. Translates `(tiltX * 10f, tiltY * 8f - 0.20f * H * p)`. Long-press hook triggers [[FoxLineDebugMenu]].
3. **Layer 3 (Ridge Ground):** Pre-clipped to the **191 normalized points** from `ridge.json`. Translates `(tiltX * 18f, tiltY * 13f + 0.52f * H * p)`.
4. **Layer 4 (Bottom Fade):** 130dp vertical gradient (`transparent -> #8C050609 -> #D9050609`).

## GPU & Hardware Optimizations ([[POCO_C71_Hardware_Rules]])
- **Pre-baked layers:** Screen-sized `backBmp` and ridge-clipped `frontBmp` created once on load (-78% VRAM).
- **Calm Tilt:** `0.002f` deadband + frame loop sleep after 1 second of stillness (0 fps at rest).
- **GPU GraphicsLayer:** All layer movement happens on GPU with zero recomposition.

## Connected Notes
- [[PROGRESS]] — Main AI brain
- [[MainActivity]] — Hosts wallpaper composable
- [[WordsRepository]] — Uses `ridgeYAtScreenX()` for clearance
- [[FoxLineDebugMenu]] — Opened by clock long-press
- [[POCO_C71_Hardware_Rules]] — Pre-baking and calm tilt rules
- [[02_Mistakes_And_How_We_Overcame_Them]] — 24MB bitmap & continuous tilt loop post-mortems
- [[Prototype_Fox_Line_v30]] — Prototype ridge & wallpaper math
