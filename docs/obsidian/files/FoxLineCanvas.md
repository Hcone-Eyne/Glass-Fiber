---
title: FoxLineCanvas.kt
tags:
  - active-v30
  - fox-line
  - canvas
---

# `FoxLineCanvas.kt`

- **Source File:** `apps/fox-mobile/app/src/main/java/com/fox/mobile/ui/line/FoxLineCanvas.kt`
- **Role:** Compose Canvas renderer for the Fox Line, secondary bead, hold-to-wake glass capsule, water ripples, and wake chips.

## Components
1. **`FoxLineTouchStrip`:**
   - 56dp tall touch strip anchored at bottom.
   - 8 primary bar shapes: `IDLE` (118dp), `MSG` (drop + neck), `MUSIC` (album waveform), `TIMER` (progress bar), `ORDER` (ticks + rider), `XFER` (fill), `CONT` (green bar), `FOX` (60% W speech wave).
   - Secondary bead: 6dp radius circle at 12dp gap.
   - Hidden (`alpha = 0f`) via `graphicsLayer` when Island Card is open.
   - Calm 120Hz frame loop: suspends on `snapshotFlow` when settled (**0 frames/sec idle load**).
2. **`TransferRippleCanvas`:**
   - Full-screen non-interactive water ripple rings (`WaterDrop`).
3. **`FoxWakeOverlay`:**
   - Dimmed backdrop with bottom `.fox-chips` row (`"Continue on Mac"`, `"Alarm 7:00"`, etc.).

## Connected Notes
- [[PROGRESS]] — Main AI brain
- [[FoxLineState]] — State driver
- [[MainActivity]] — Mounts touch strip and overlays
- [[FoxSpring]] — Spring physics for glass capsule
- [[POCO_C71_Hardware_Rules]] — Canvas state invalidation rules
- [[02_Mistakes_And_How_We_Overcame_Them]] — `frameNowSec` state invalidation post-mortem
