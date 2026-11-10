---
title: WordsRepository.kt
tags:
  - active-v30
  - home-content
---

# `WordsRepository.kt`

- **Source File:** `apps/fox-mobile/app/src/main/java/com/fox/mobile/ui/home/WordsRepository.kt`
- **Role:** Quotes repository & dynamic mountain-ridge text fitting.

## Quotes
1. **Morning (`hour < 12`):** `"I can do all things through Christ which strengtheneth me."` (Philippians 4:13)
2. **Afternoon (`hour < 18`):** `"Stay hungry. Stay foolish."` (Steve Jobs)
3. **Evening (`hour >= 18`):** `"Revise unit 3 tonight. Your phone goes quiet at 1 AM."` (Fox reminder)

## Mountain Ridge Fit Algorithm
- Tests candidates in order:
  1. `(maxWidth = 0.92f, fontSize = 16sp)`
  2. `(maxWidth = 0.80f, fontSize = 15sp)`
  3. `(maxWidth = 0.68f, fontSize = 14sp)`
- Validates that quote top edge clears `maxRidgeYInSpan(...)` by at least `10.dp`.

## Connected Notes
- [[PROGRESS]] — Main AI brain
- [[DepthWallpaperAndClock]] — Supplies ridge points via `ridgeYAtScreenX`
- [[WorkHomeScreen]] — Renders the fitted words line
- [[ContractTest]] — Verified by unit tests
- [[Prototype_Fox_Line_v30]] — Prototype quote strings and fit algorithm
