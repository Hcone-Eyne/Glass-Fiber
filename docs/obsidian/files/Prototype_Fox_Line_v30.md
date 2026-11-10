---
title: Prototype Fox_Line_v30.html
tags:
  - prototype
  - source-of-truth
---

# Prototype `Fox_Line_v30.html`

- **Source File:** `docs/prototypes/Fox_Line_v30.html`
- **Role:** The ultimate, uncompromised visual and behavioural source of truth for the entire launcher.

> [!IMPORTANT] Rule #1 from `FOX_MOBILE_PLAN.md`
> **Copy, don't redesign.** If `FOX_MOBILE_PLAN.md` and `Fox_Line_v30.html` disagree on styling, sizes, margins, or easing, `Fox_Line_v30.html` wins every time.

## Line Lookup Table
- **CSS Tokens & Reset:** Lines 9–24
- **Depth Wallpaper (`.wp`):** Lines 26–43
- **Clock & Date:** Lines 45–58
- **Words Line (`.words`):** Lines 60–70
- **Home Work Block (`.work`, `.cta`, `.next`):** Lines 72–108
- **Search Capsule (`.search`):** Lines 110–122
- **Spotlight Drawer (`.drawer`):** Lines 124–192 *(Phase 6 Target)*
- **Dashboard (`.dash`):** Lines 194–252 *(Phase 8 Target)*
- **Island Card (`.island`):** Lines 254–284
- **Fox Wake (`#foxWake`):** Lines 286–305
- **Liquid Morph (`#morph`):** Lines 837–955 *(Phase 7 Target)*

## Connected Notes
- [[PROGRESS]] — Main AI brain
- [[FoxTheme]] — Implements CSS tokens
- [[DepthWallpaperAndClock]] — Implements wallpaper & clock
- [[WorkHomeScreen]] — Implements work block & words
- [[FoxLineCanvas]] — Implements line shapes
- [[IslandCard]] — Implements island views
- [[SpotlightSheet]] — Will implement drawer in Phase 6
