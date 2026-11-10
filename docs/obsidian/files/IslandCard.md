---
title: IslandCard.kt
tags:
  - active-v30
  - island-card
---

# `IslandCard.kt`

- **Source File:** `apps/fox-mobile/app/src/main/java/com/fox/mobile/ui/island/IslandCard.kt`
- **Role:** Floating bottom-anchored Live Activity Island Card.

## Key Architecture
- **Dimensions:** `12dp` side margins, `14dp` bottom margin, `30dp` corner radius.
- **Surface Material:** `inkSurface` (`#D912141C`) + 1dp `hair` border; turns Alert Red (`#E6460A0C` + `#FF4D4D` border) when timer rings.
- **GPU-Only Animation:** Uses `graphicsLayer { alpha = p; scaleX = s; scaleY = s; translationY = ... }` with zero UI-thread composition overhead.
- **Outside-Tap Dismiss:** Tapping anywhere outside the card dismisses it and restores the Fox Line.
- **6 Activity Sub-Views:**
  1. `IslandOrderContent`: Spice Route, ETA in `#9FE3BC`, 28x15dp rider pill.
  2. `IslandTimerContent`: M:SS countdown or "Time's up", Pause/+1min/Cancel buttons.
  3. `IslandMusicContent`: Album art canvas with gradient + 3 blobs, Back/Play/Next.
  4. `IslandMsgContent`: Sender name, summary text, quick reply buttons.
  5. `IslandXferContent`: MacBook Air file transfer progress.
  6. `IslandDefaultContent`: Mac handoff card.

## Connected Notes
- [[PROGRESS]] — Main AI brain
- [[FoxLineState]] — State driver
- [[MainActivity]] — Mounts the island card
- [[POCO_C71_Hardware_Rules]] — Hardware composition rules
- [[02_Mistakes_And_How_We_Overcame_Them]] — GraphicsLayer animation post-mortem
