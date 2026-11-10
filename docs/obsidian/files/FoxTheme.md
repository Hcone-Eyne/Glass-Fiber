---
title: FoxTheme.kt
tags:
  - active-v30
  - design-system
---

# `FoxTheme.kt`

- **Source File:** `apps/fox-mobile/app/src/main/java/com/fox/mobile/ui/theme/FoxTheme.kt`
- **Role:** Design tokens, fonts, and Ink material surface.

## 10 Exact Color Tokens (from [[Prototype_Fox_Line_v30]])
- `screenBg` (`#07080C`)
- `inkSurface` (`#9914161E`)
- `inkPanel` (`#BD101219`)
- `inkMenu` (`#EB141620`)
- `hair` (`#24FFFFFF`)
- `t1` (`#F6F0E6`)
- `t2` (`#ADF6F0E6`)
- `ember` (`#F2B33D`)
- `ok` (`#7FD6A4`)
- `alert` (`#FF4D4D`)
- `lineWarm` (`#D9F6F0E6`)

## Fonts (`FoxFonts`)
- `Bricolage`: Display font for Clock, Words, Home sentence
- `Figtree`: Body font for Dates, Buttons, Drawer, Meta
- `JetBrainsMono`: Code face for Paths, Git branches

## Material
- `FoxInkBox`: Reusable dark translucent material (`inkSurface` + 1dp `hair` border).

## Connected Notes
- [[PROGRESS]] — Main AI brain
- [[MainActivity]] — Theme wrapper
- [[WorkHomeScreen]] — Uses `FoxColors`, `FoxFonts`, `FoxInkBox`
- [[FoxLineCanvas]] — Line and bead color tokens
- [[IslandCard]] — Island card styling
- [[Prototype_Fox_Line_v30]] — Prototype CSS tokens
