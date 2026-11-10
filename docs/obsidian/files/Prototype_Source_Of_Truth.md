---
title: File Map — Prototype Source of Truth (Fox_Line_v30.html Line Index)
date: 2026-11-10
tags:
  - fox-mobile
  - prototype
  - source-of-truth
---

# File Map — Prototype Source of Truth (`Fox_Line_v30.html`)

Back to Index: [[File_Map_Index]] | Main Brain: [[../../../PROGRESS|PROGRESS.md]]

> [!IMPORTANT] Fast Lookup Index into `docs/prototypes/Fox_Line_v30.html`
> Use this exact line-number table to jump straight to the relevant CSS, HTML, or JS section of [`docs/prototypes/Fox_Line_v30.html`](file:///Users/enoch/Desktop/Glass_Main%28me%29/Android%20Launcher%21/docs/prototypes/Fox_Line_v30.html) without reading the entire file!

---

## 1. CSS Styling & Layout Rules (Lines 9–305)

| Section | Line Range | What It Defines |
| :--- | :--- | :--- |
| **Root Tokens & Reset** | `L9–L24` | `--bg:#07080c`, `--glass:rgba(20,22,30,.60)`, `--hair:rgba(255,255,255,.14)`, `--t1:#f6f0e6`, `--t2:rgba(246,240,230,.68)`, `--ember:#f2b33d`, `--ok:#7fd6a4`, `--alert:#ff4d4d`. |
| **Depth Wallpaper (`.wp`, `.wp-front`)** | `L26–L43` | `.wp` cover crop (`ih = 1.1*H`, `iw = ih*1136/615`, `left = -(420/1136)*iw`, `top = -0.05*H`), `130px` bottom dark fade. |
| **Clock (`.clock`, `.date`, `.time`)** | `L45–L58` | `left:22px; top:24.5%; z-index:2`, `118px` Bricolage 700 (`line-height:.86; letter-spacing:-.055em`), `14px` Figtree 600 date. |
| **Words (`.words`)** | `L60–L70` | `left:22px; bottom:calc(46% + 14px); z-index:4`, `16px` Bricolage 500 quote, `12px` Figtree attribution, 200ms fade swap. |
| **Home Work Block (`.work`, `.cta`, `.next`)** | `L72–L108` | `left:22px; right:22px; top:54%; z-index:4`, `23px` Bricolage sentence, `52px` `.cta` pill, `.next` alarm row + `Set 7:00` chip. |
| **Home Search Pill (`.search`)** | `L110–L122` | `left:22px; right:22px; bottom:54px; height:44px; border-radius:22px`, `var(--glass)` + `1px solid var(--hair)`. |
| **Spotlight Drawer (`.drawer`, `.dr-search`, `.dr-panel`)** | `L124–L192` | **Phase 6 target!** `left:10px; right:10px; bottom:30px; height:56%`, `44px` `.dr-search` + `9px` gap + `30px` radius `.dr-panel`, `.dr-cats`, `.dr-rec` (5 cols), `.dr-list` vs `.dr-grid` (4 cols), `.dr-menu` (`List`/`Grid`). |
| **Dashboard (`.dash`, `.d-card`, `.d-diff`)** | `L194–L252` | **Phase 8 target!** Pull-down dashboard parting the painting at the ridge: `.d-top` Mac link plain text, `Continue` card, `Needs you` card with `850ms` hold-to-rerun fill & inline diff (`8 additions · 3 deletions`), `Spaces` row. |
| **Island Card (`.island`, `.ord-track`, `.art`)** | `L254–L284` | **Phase 5 (Done):** `left:12px; right:12px; bottom:14px; border-radius:30px`, order rider pill (`28×15px`), music art (`44px`), action buttons (`44px`). |
| **Fox Wake & Liquid Morph (`#foxWake`, `#morph`)** | `L286–L305` | **Phase 5 & Phase 7:** `.fox-chips` at `bottom:76px`; `#morph` liquid send/receive container (`x, y, w, h, r, o`). |

---

## 2. HTML DOM Structure (Lines 307–485)

| Element ID / Class | Line Range | Phase |
| :--- | :--- | :--- |
| `#wpBack`, `#clock`, `#wpFront` | `L318–L325` | Phase 3 (Done) |
| `#words`, `#work`, `#contBtn`, `#nextRow`, `#searchBtn` | `L327–L345` | Phase 4 (Done) & Phase 6 (`#searchBtn`) |
| `#dash` (Dashboard cards, hold button, diff, spaces) | `L347–L412` | Phase 8 |
| `#drawer` (Spotlight search, `⋯` menu, cats, recents, list/grid) | `L414–L452` | **Phase 6 (Next)** |
| `#foxWake`, `#island`, `#morph`, `#fx` (ripple), `#bar` | `L454–L480` | Phase 5 (Done) & Phase 7 (`#morph`) |

---

## 3. JavaScript Engine & Physics (Lines 487–1420)

| Subsystem | Line Range | Key Functions & Constants |
| :--- | :--- | :--- |
| **Ridge Cut & Fit Words** | `L495–L565` | `RIDGE` 191 points, `setupWallpaper()`, `ridgeYAtScreenX()`, `fitWords()`. |
| **State (`S`), Songs (`ART`, `SONGS`), Words (`WORDS`)** | `L567–L645` | `activeList()`, `timerLeft()`, `orderStage()`. |
| **Fox Line & Bead Renderer (`LINE`, `BEAD`)** | `L647–L765` | `LINE.idle`, `LINE.msg`, `LINE.music`, `LINE.timer`, `LINE.order`, `LINE.xfer`, `LINE.cont`, `LINE.fox`, `BEAD`. |
| **Island Card Renderer (`renderIsland`)** | `L767–L835` | HTML/DOM templates for `order`, `timer`, `music`, `msg`, `xfer`, and `default` Mac handoff. |
| **Liquid Send & Receive (`#morph` — Phase 7)** | `L837–L955` | **Phase 7 target!** `ms = {x,y,w,h,r, vx,vy,vw,vh,vr, tx,ty,tw,th,tr, k,c, o,to, p,tp}`, `stepMorph(dt)`, `blobTo()`, `pillToCard()`, `dropIntoBar()`, `riseFromBar()`, `cardToPill()`, `emergePill()`, `sendFile()`, `receiveFile()`. |
| **Fox Wake & Touch Strip Gestures** | `L957–L1045` | `summonFox()`, `closeFox()`, `1.1s` hold with buzzes at `33%` and `66%`, horizontal swipe swap. |
| **Search Drawer Logic (Phase 6)** | `L1047–L1190` | **Phase 6 target!** `DAPPS` (14 apps across Work/Setup/Social/Media), `DCONTACTS` (5 contacts), `RECENTS` (5 items), `renderDrawer(q)`, math evaluator, drag-to-open/close (`drSpring`: open `k=230, c=26`, close `k=320, c=34`). |
| **Dashboard Pull-Down & Hold-to-Rerun (Phase 8)** | `L1192–L1310` | **Phase 8 target!** `dashSpring`, ridge parting (`sky` `-0.20*H*p`, `ground` `+0.52*H*p`), `850ms` hold-to-rerun (`#rerunBtn`). |
| **Main 120Hz `frame(now)` Loop** | `L1312–L1420` | Integrates tilt low-pass (`0.08`), glass spring (`gs`), morph spring (`ms`), water drops (`drops`), and canvas drawing. |
