---
title: Complete File-by-File Map Index (All 42 Repository Files)
date: 2026-11-10
tags:
  - fox-mobile
  - file-map
  - architecture
---

# Complete File-by-File Map Index

Back to Main Brain: [[../../PROGRESS|PROGRESS.md]] | Hub: [[00_Fox_Mobile_Progress_Hub]]

> [!TIP] Why This Map Exists
> Instead of an AI agent reading 6,000+ lines of Kotlin, Swift, HTML, and JSON schemas on every cold start, these 5 linked notes document **every file in the repository**, its exact line ranges, its classes/functions, and whether it is **ACTIVE (`v30`)**, **LEGACY (to be replaced in Phase 6)**, or **PHASE 13 READY**.

---

## Deep File Maps by Subsystem

1. **[[Fox_Mobile_App_And_Core]]** — Build files, `MainActivity.kt`, `FoxSpring.kt`, `FoxTheme.kt`, `Phase2DebugScreen.kt`
2. **[[Fox_Mobile_Depth_And_Home]]** — `DepthWallpaperAndClock.kt`, `WordsRepository.kt`, `WorkHomeScreen.kt`
3. **[[Fox_Mobile_Line_And_Island]]** — `FoxLineState.kt`, `FoxLineCanvas.kt`, `IslandCard.kt`, `FoxLineDebugMenu.kt`
4. **[[Fox_Mobile_Pending_Phase6_To_13]]** — `SpotlightSheet.kt` *(Legacy — replace in Phase 6)*, `FoxProtocol.kt`, `FoxLinkClient.kt`, `ContractTest.kt`, `apps/fox-desk/*`, `packages/fox-protocol/*`
5. **[[Prototype_Source_Of_Truth]]** — Exact line-by-line index into `docs/prototypes/Fox_Line_v30.html` (`1,400+` lines) for Phases 1–8.

---

## Visual Dependency Graph of `apps/fox-mobile`

```mermaid
flowchart TD
    Main["MainActivity.kt (ACTIVE v30)"] --> Theme["FoxTheme.kt (10 tokens + FoxFonts + FoxInkBox)"]
    Main --> Depth["DepthWallpaperAndClock.kt (3 layers + 191-pt ridge + calm tilt)"]
    Depth --> Home["WorkHomeScreen.kt :: Phase4HomeContent (lines 329-739 ACTIVE)"]
    Home --> Words["WordsRepository.kt (3 quotes + selectWordsFit)"]
    Main --> LineState["FoxLineState.kt (Urgency queue + order/timer/msg/xfer/fox)"]
    Main --> LineCanvas["FoxLineCanvas.kt (FoxLineTouchStrip + TransferRippleCanvas + FoxWakeOverlay)"]
    Main --> Island["IslandCard.kt :: LiveActivityIslandCard (lines 49-754 ACTIVE)"]
    Main --> DebugMenu["FoxLineDebugMenu.kt (Long-press clock 10-action menu)"]
    LineCanvas --> Spring["FoxSpring.kt (Pure Kotlin Spring1D & SpringChannelSet)"]
    Drawer["SpotlightSheet.kt (LEGACY - Replace in Phase 6)"] -. "Not mounted in MainActivity yet" .-> Main
    Net["FoxLinkClient.kt + FoxProtocol.kt (PHASE 13 READY)"] -. "Wire in Phase 13" .-> Main
```
