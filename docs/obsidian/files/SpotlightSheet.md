---
title: SpotlightSheet.kt
tags:
  - legacy
  - phase-6-target
---

# `SpotlightSheet.kt` (LEGACY: MUST REPLACE IN PHASE 6)

- **Source File:** `apps/fox-mobile/app/src/main/java/com/fox/mobile/ui/drawer/SpotlightSheet.kt`
- **Current Status:** Leftover placeholder from before Phase 1. **Not mounted in MainActivity.**

> [!WARNING] Do Not Reuse Current Styling!
> - Contains rejected `CyanLens` horizontal rainbow gradient border (violates Rule #3: No chromatic edge refraction).
> - Contains macOS `⌘K` badge instead of the prototype `⋯` view mode button (`List` vs `Grid`).

## Phase 6 Target Specifications (from [[Prototype_Fox_Line_v30]])
1. **Home Search Capsule:**
   - `44.dp` tall, `22.dp` radius, `FoxColors.inkSurface` (`#9914161E`) + `1.dp hair` border.
   - At `bottom = 54.dp, left = 22.dp, right = 22.dp`.
2. **Spotlight Drawer (56% H):**
   - Anchored at `bottom = 30.dp, left = 10.dp, right = 10.dp, height = 56% H`.
   - Top search capsule (`44dp`) + `9dp` gap + results panel (`30dp` radius, `inkPanel`).
   - Category chips: `All`, `Apps`, `Work`, `Setup`, `Social`, `Media`.
   - 5 Recent items row.
   - Real installed apps via Android `PackageManager` + 4-column Grid / List toggle.
   - Math expressions (`24*7` -> `= 168`) and Contact hits (`Call` / `Msg`).
3. **Spring Drag:**
   - 1:1 finger tracking, commit open (`k=230, c=26`) or close (`k=320, c=34`).
   - Dims home content to 50% opacity and scales to `0.975` on GPU.

## Connected Notes
- [[PROGRESS]] — Main AI brain & ledger
- [[MainActivity]] — Will mount drawer in Phase 6
- [[FoxSpring]] — Spring physics for open/close
- [[FoxTheme]] — Uses `inkPanel` and `hair` tokens
- [[Prototype_Fox_Line_v30]] — Drawer layout and DAPPS/DCONTACTS data
