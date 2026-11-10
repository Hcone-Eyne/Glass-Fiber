---
title: POCO C71 Hardware & Performance Rules
tags:
  - hardware-rules
  - performance
  - poco-c71
---

# POCO C71 Hardware & Performance Rules

- **Device:** POCO C71 (`25028PC03I`)
- **Firmware:** Android 15 HyperOS
- **SoC:** Unisoc T603 (8× Cortex-A55 @ 1.8 GHz, PowerVR Rogue GE8322 GPU)
- **Display:** 720 × 1640 LCD, 120Hz capable (Mode ID 2)

## The 5 Golden Rules
1. **Never allocate oversized bitmaps or clip complex paths per-frame:**
   - Pre-bake static layers at screen resolution (`(screenW + 64) × (screenH + 64)`) once on load.
   - Saves 78% VRAM and cuts GPU time by 5ms/frame (eliminates `StencilAttachment`).
2. **Never animate `Modifier.blur(dp)` frame-by-frame or use `AnimatedVisibility` on heavy views:**
   - Keep heavy elements composed in the tree.
   - Animate `alpha`, `scaleX`, `scaleY`, and `translationY` inside `Modifier.graphicsLayer { ... }` lambdas.
3. **Every `withFrameNanos` loop MUST sleep (0 fps) when settled:**
   - Use `snapshotFlow` or conflated channels so idle screens draw zero frames.
   - Tilt sensor must have deadband (`> 0.002f`) and sleep after 1 second of stillness.
4. **Inside `Canvas { ... }`, always read a `mutableFloatStateOf` frame timestamp:**
   - Updated inside `withFrameNanos`.
   - Reading non-State functions alone does not invalidate the `Canvas` draw scope once dimensions settle.
5. **Always benchmark the R8-minified `benchmark` APK:**
   - Run `cmd statusbar collapse` before `gfxinfo reset`.
   - Never run `screencap -p` inside the `dumpsys gfxinfo` timing window.

## Connected Notes
- [[PROGRESS]] — Main AI brain
- [[02_Mistakes_And_How_We_Overcame_Them]] — Full post-mortem of all 11 mistakes
- [[DepthWallpaperAndClock]] — Pre-baking implementation
- [[WorkHomeScreen]] — Offscreen text caching
- [[FoxLineCanvas]] — Calm 120Hz frame loop
- [[IslandCard]] — GraphicsLayer animation
