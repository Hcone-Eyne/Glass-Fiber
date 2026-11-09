# Fox Mobile: build plan (for Gemini)

You are porting a finished, approved design into the Android app so Enoch can run it on his phone.
The design is done. Your job is to **copy it faithfully**, one phase at a time, and prove each phase works on the phone before starting the next.

- **Phone:** POCO C71 (Android 15, HyperOS; 720 × 1640 LCD; budget GPU; waterdrop notch).
- **App:** `apps/fox-mobile` (Kotlin, Jetpack Compose, minSdk 29, targetSdk 35). It is a **home-screen launcher**: `MainActivity` already has the `HOME` intent filter.
- **Source of truth:** `docs/prototypes/Fox_Line_v30.html`. Open it in Chrome at a phone size (≈ 390 × 844 in DevTools) and keep it open while you work. When this plan and the prototype disagree, **the prototype wins**. Search it by the function and class names given below.
- **Assets ready for you:** `docs/prototypes/assets/wallpaper.webp` (1136 × 615, Enoch's painting) and `docs/prototypes/assets/ridge.json` (the ridge line that splits the painting into sky and ground; 191 points as image fractions). Both are already on the app's asset path.

---

## 0. Rules for every phase (read first)

1. **One phase at a time.** Finish a phase, build it, install it on the phone, check every item in its *Done when* list, then commit with the message `phase N: <title>`. Don't start the next phase until that one passes.
2. **Copy, don't redesign.** Every colour, size, duration and spring value below comes from the approved prototype. Don't "improve" them, add effects, or swap in Material defaults. If something is missing from this plan, look it up in the prototype. If it isn't there either, **stop and ask Enoch**. Don't invent it.
3. **Pure Kotlin only.** No C, no JNI, no CMake, no `.so` or `.dylib` files, no NDK.
4. **No new dependencies** beyond what's listed in Phase 2. No Material 3 components on screen: they bring Google's look, which Enoch rejected. Use `Box`, `Canvas`, `Text`, `BasicTextField` and your own composables. (Material 3 can stay in Gradle for `MaterialTheme` plumbing only.)
5. **Don't touch** `apps/fox-desk/` (Swift, done and tested) or `packages/fox-protocol/` (the shared contract). Only Phase 13 talks to them.
6. **Build and install after every step**, not just at the end of a phase:

   ```bash
   cd apps/fox-mobile && ./gradlew installDebug
   ```

   and watch the log for crashes:

   ```bash
   adb logcat -s AndroidRuntime:E FoxMobile:*
   ```

7. **Motion rules (approved; never break):**
   - Ease-out for things entering, `cubic-bezier(0.23, 1, 0.32, 1)` → `CubicBezierEasing(0.23f, 1f, 0.32f, 1f)`. Never ease-in on enter.
   - Exits are faster than enters. Never animate from scale 0.
   - Press feedback is scale 0.97.
   - Gesture-driven motion follows the finger 1:1 and releases into a spring. Springs are written as **response / damping** (Apple style): `k = (2π / response)²` and `c = 4π · damping / response`. Step them in your own frame loop (`withFrameNanos`) with 4 substeps per frame, exactly like the prototype.
   - Respect *Remove animations* (`Settings.Global.ANIMATOR_DURATION_SCALE == 0`): jump to end states.
8. **Looks Enoch rejected (never add, even "subtly"):** glow or halo shadows, Iron Man / HUD styling, full-screen particle text, an orb, dot-matrix bars, Nothing-style dots, streaks or lines along the screen edge, random particles, bouncy or overshooting easing on buttons and menus, Apple-style "Liquid Glass" refraction (blur plus chromatic edges), colour gradients on buttons, decorative pills around plain text.
9. **What went wrong last time; undo it and don't repeat it** (Phase 1):
   - A C/JNI "native core" (`app/src/main/cpp/`, `libfox_core.dylib`, `core/FoxNativeCore.kt`).
   - A copy of the Kotlin compiler in `bin/` and loose `.jar` files in `libs/`.
   - A custom `run_contract_tests.sh` instead of Gradle tests.
   - Protocol edits made while doing UI work.
   - None of the prototype's latest work was ported. The app still shows an old design.

---

## Phase 0: Set up the Mac and the phone (Enoch does the clicks; you write the instructions)

**Mac**
1. Install Java 17. This Mac has no Java right now, which is why Gradle can't run.

   ```bash
   brew install --cask temurin@17
   ```

2. Point Gradle at it and the SDK. Add to `~/.zshrc`, then open a new terminal:

   ```bash
   export JAVA_HOME=$(/usr/libexec/java_home -v 17)
   export ANDROID_HOME="$HOME/Library/Android/sdk"
   export PATH="$ANDROID_HOME/platform-tools:$PATH"
   ```

3. Check that the build tool works:

   ```bash
   cd apps/fox-mobile && ./gradlew --version
   ```

**POCO C71 (HyperOS)**
1. *Settings → About phone →* tap **OS version** 7 times to unlock Developer options.
2. *Settings → Additional settings → Developer options →* turn on **USB debugging**, plus **Install via USB** (HyperOS asks you to sign in to a Mi account for this; it needs a SIM and data on).
3. Plug in the cable and accept the "Allow USB debugging" prompt on the phone. Then confirm the phone shows up:

   ```bash
   adb devices
   ```

4. Later, to make the bar Fox's own: *Settings → Home screen → System navigation →* **Gestures**, then turn on **Hide full screen indicator**. Fox draws its own line in that spot. Swiping up still goes home.

**Done when:** `./gradlew installDebug` puts "Fox Line" on the phone; pressing Home offers Fox as a launcher choice; choosing it shows the app.

---

## Phase 1: Clean up the last attempt

1. Delete `app/src/main/cpp/` (the C code, CMake file and `libfox_core.dylib`) and the commented `externalNativeBuild` block in `app/build.gradle.kts`.
2. Replace `core/FoxNativeCore.kt` with `core/FoxSpring.kt`: plain Kotlin springs (`Spring1D`, plus a helper that steps many named channels), using the formula in rule 7. Update every caller and the tests.
3. Delete `bin/` (the copied `kotlinc`), `libs/` (loose jars) and `run_contract_tests.sh`. Tests run with `./gradlew testDebugUnitTest`.
4. Keep the Gradle wrapper (`gradlew`, `gradle/wrapper/`). It's correct.
5. Add `apps/fox-mobile/.gitignore` containing `.gradle/`, `.kotlin/`, `build/`, `app/build/` and `local.properties`.
6. Revert any change to `protocol/FoxProtocol.kt` that isn't needed for the build to pass. Explain in the commit what you kept and why.

**Done when:** `./gradlew assembleDebug testDebugUnitTest` passes, nothing native is left (`find . -name "*.c" -o -name "*.so" -o -name "*.dylib"` prints nothing), and the app still installs.

---

## Phase 2: Theme, fonts and the material

**Fonts.** Bundle three SIL OFL fonts as files in `res/font/`, not Google downloadable fonts, so they work offline:
- **Bricolage Grotesque** (variable): the display face (clock, titles).
- **Figtree** 400/500/600/700: the body face.
- **JetBrains Mono** 400/500: only for code text (file paths, branch names, diffs).

**Colours** (put them in `FoxTheme.kt` as a `FoxColors` object):

| Token | Value | Use |
|---|---|---|
| `t1` | `#F6F0E6` | primary text |
| `t2` | `#F6F0E6` at 68% | secondary text |
| `ember` | `#F2B33D` | the one action colour (primary buttons, focus) |
| `alert` | `#FF4D4D` | timer, errors |
| `ok` | `#7FD6A4` | success, the "live" dot |
| `screenBg` | `#07080C` | behind everything |
| `inkSurface` | `rgb(16,16,22)` at **60%** | search bar, file card, drawer panel |
| `inkMenu` | `rgb(24,24,31)` at **80%** | the ⋯ menu |
| `hair` | `#F6F0E6` at 14% | 1 px inside border on every ink surface |
| `lineWarm` | `#F6F0E6` at 85% | the Fox Line |

**The Ink material.** Every surface that is a search bar, card, panel or menu uses: the ink fill, a 1 px hairline border, and a **soft 4 px blur** of what's behind (`RenderEffect.createBlurEffect(4f, 4f, CLAMP)` on API 31+, none below). No refraction, no glow, no coloured edges.

**Type scale** (in sp): 12 meta, 14 body and buttons, 16 row titles, 24 card titles. The clock is 118sp Bricolage at weight 700 with letter spacing −0.055em. Home sentence: 23sp. Words line: 16sp (it may step down to 14; see Phase 4).

**Done when:** a temporary debug screen shows the three fonts and an ink surface over the wallpaper, matching the prototype side by side.

---

## Phase 3: The depth wallpaper and the clock

Prototype reference: `measure()`, `.wp`, `.wp-back`, `.wp-front`, `.clock-wrap`, and the tilt code at the top of `frame()`.

1. **Sizing** (screen W × H in px): image height `ih = 1.1·H`, width `iw = ih·1136/615`, left `= −(420/1136)·iw`, top `= −0.05·H`. Both layers use this same box.
2. **Three layers, back to front:**
   1. *Back:* the full painting.
   2. *Clock:* the date (Figtree 14sp, 600, `t2`) above the time (Bricolage 118sp). It sits at left 22dp and top 24.5% of H.
   3. *Front:* the same painting, **clipped to the area below the ridge**. Build a `Path` from `ridge.json` (x·iw + left, y·ih + top), then close it along the bottom of the image. This is what makes the clock look tucked behind the ridge.
3. **Tilt parallax** (accelerometer, low-pass 0.08 per frame):
   - back moves by (tilt·5, tilt·4) px
   - clock moves by (tilt·10, tilt·8) px
   - front moves by (tilt·18, tilt·13) px
4. A dark fade at the bottom 130dp: transparent, then `#050609` at 55% halfway, then 85% at the bottom.
5. Hide the system status-bar background but keep its icons. The app draws edge to edge.

**Done when:** on the phone, the clock's lower part disappears behind the white ridge exactly as in the prototype, and tilting the phone moves the three layers by different amounts.

---

## Phase 4: The home content

Prototype reference: `.work`, `.next`, `#words`, `fitWords()`, `showWords()`.

1. **Work block** at left/right 22dp, top 54% of H:
   - the sentence "You were writing **MenuBarView.swift** on your Mac, 4 min ago." (Bricolage 23sp, `t2`, the file name in `t1` weight 650)
   - the **Continue** button: 52dp tall, pill shape, ink, Bricolage 16sp 650, with an arrow icon
   - the row "Exam tomorrow 9:30 · no alarm" plus a **Set 7:00** chip (ember text on ember at 16%). Tapping it changes to "Alarm 7:00" in `ok` green, and sets a real alarm with `AlarmClock.ACTION_SET_ALARM`.
2. **Words line**, 14dp above the work block:
   - one item at a time: the text (Bricolage 16sp 500, `t1`), then below it the source in 12sp `t2`, e.g. "— Philippians 4:13 · Your verse"
   - which item shows first depends on the time of day: verse before noon, saved quote until 6 pm, Fox reminder after that
   - tap shows the next item: fade plus 4 px blur out over 200 ms, swap the text, fade back in
   - the three sample items are in `WORDS` in the prototype. Keep them as sample data in a `WordsRepository` that Phase 13 will fill from Keep.
3. **Fit to the ridge** (`fitWords`): try widths 68% → 80% → 92% of the work block at 16sp, then 92% at 15sp and 14sp. Use the first one where the words' top edge sits at least 10dp below the lowest ridge point under the words' left-to-right span. Re-run after every swap and on rotation or resize.
4. Text gets a soft dark shadow so it reads on the painting: offset 0,1, blur 10, black at 75%.

**Done when:** all three words items fit under the ridge on the phone, and nothing overlaps the work text.

---

## Phase 5: The Fox Line (the bar)

Prototype reference: `LINE` (`idle`, `xfer` and the others), `wavyBar()`, `splashBar()`, `drop()`, `startHold()`, `openIsland()`, `renderIsland()`, `BEAD`, `lv`.

Port this in small steps. Build and check on the phone after each one.

1. **Idle line:** 118dp wide, 5dp thick, round caps, `lineWarm`. Its centre is 12dp above the screen's bottom edge, horizontally centred, inside a 56dp-tall touch strip.
2. **Tap** opens the live view card above the bar (`renderIsland`). While it's open, the line itself hides.
3. **Hold** grows the line into a glass capsule; when it fills, Fox wakes (`startHold`, `fox`). Wake Fox exactly as the prototype does, with no orb or glow.
4. **Live activities** change the bar's shape:
   - timer: the bar turns red until touched
   - music: a wave in the album's colour
   - food order: progress
   - message: the line swells into a drop
   - a second activity: a bead with a 48dp touch target; swiping the bar sideways swaps the two
5. **Ripple rule:** a ripple happens **only** for phone ↔ Mac transfers. Sending ripples inward, receiving and Continue ripple outward. A first version can draw `drop()`'s rings on a `Canvas`. The displacement version comes in Phase 11.
6. **Splash:** after something lands, the bar shows small decaying waves (`wavyBar`: amplitude `3.2·e^(−3.6·age)`, for 1.1 s).
7. **Haptics:** use `performHapticFeedback` or `VibrationEffect`, mapping each `buzz()` in the prototype to a short effect.

**Done when:** each of these bar states can be triggered from a temporary debug menu (long-press on the clock) and looks the same as the prototype.

---

## Phase 6: The search drawer (Spotlight layout)

Prototype reference: `.drawer`, `.dr-search`, `.dr-more`, `.dr-menu`, `.dr-chips`, `.dr-recents`, `.dr-apps.list`, `setDrawer()`, `drawerTick()`, `renderDrawer()`.

1. **Geometry:**
   - The drawer is 10dp from each side and 30dp above the bottom, 56% of H tall.
   - The **search capsule** is 44dp tall, fully round, and floats on its own.
   - A **9dp gap**, then the separate **panel** (radius 30dp).
   - Both are ink. Closed, only the capsule shows above the bar.
2. **Physics:** drag follows the finger 1:1, with resistance past the ends (×0.25).
   - On release: if the flick is faster than 0.35 px/ms, go in its direction; otherwise open if past 35%.
   - Spring: opening `k = 230, c = 26`; closing `k = 320, c = 34`; 4 substeps.
   - The home layer dims to 50% and scales to 0.975 while the drawer opens.
3. **Capsule contents:**
   - search icon
   - the field, with placeholder "Search apps and files"
   - a **⋯** button that only shows while the drawer is open
   - the ⋯ button opens a small menu: "View as List" / "View as Grid"
4. **Panel contents**, top to bottom:
   - **category chips** (All, Work, Social, Media, Utilities); the row scrolls sideways and fades out at the right edge
   - **five recent apps** as large 52dp icons with labels
   - a hairline
   - **"All apps"** A–Z as a list (36dp icon, name, one-line subtitle), or as a 4-column grid
5. **Real apps:**
   - The list comes from `PackageManager.queryIntentActivities(MAIN + LAUNCHER)`. Tapping one launches it.
   - Categories come from `ApplicationInfo.category`; anything unknown goes to Utilities.
   - Recents are the last five apps launched from Fox, stored with DataStore. No usage-access permission.
6. **Search:** typing filters contacts (with Call and Message buttons), actions (Continue on Mac, Send a file, Timer, Music, Order food, Set 7:00 alarm) and apps. Enter opens the first result.

**Done when:** you can open the drawer, search, and launch every installed app, and the drawer looks like the prototype's open drawer.

---

## Phase 7: Send and receive (one liquid shape)

Prototype reference: the block starting "the search bar, the file card and the bar are one liquid shape": `blobStart`, `blobTo`, `blobFrame`, `blobPaint`, `pillToCard`, `dropIntoBar`, `riseFromBar`, `cardToPill`, `emergePill`, `xferFrame`, `sendFile`, `receiveFile`.

**One** composable draws the moving shape. It has six spring channels: centre x, centre y, width, height, corner radius, plus two opacities (`o` for the file-card text, `p` for the search text). Each `blobTo` call changes the targets and gives each channel its own response and damping. **Velocity carries over** between calls, which is what makes it liquid. Never swap to a different element in the middle of a motion.

- **Colour while thin:** draw the shape in `lineWarm` while it's thin and blend to ink as it grows: `t = smoothstep(clamp((h − 6)/22))`. A thin shape *is* the line, so it joins and leaves the bar without a seam.
- **Send** (bar shows progress, turns green when done):
  1. Hide the real search capsule and start the shape at its exact rectangle.
  2. Spring to the card size (response/damping: w .46/.74, h .34/.62, y .38/.70; the search text fades with `p` .14).
  3. Wait while the bar fills.
  4. Then 250 ms later, gather into a 58 × 46 drop (w .30/.72, h .28/.62).
  5. Fall into the bar (y .32/.90, h .30/.85, w .42/.66). Stop early when within 10dp of the bar, then fire the splash, the inward ripple and haptics.
  6. Settle flat into the bar and end the shape.
  7. After 320 ms, **emerge**: a bead rises from the bar and opens into the search capsule, with small waves and a small outward ripple.
- **Receive:**
  1. Small waves and an outward ripple on the bar.
  2. A 40dp bead rises from the bar and opens into the card (w .50/.70, h .36/.60, y .40/.72); the card text fades in after 140 ms.
  3. After 2.6 s, relax back into the search capsule (w .44/.80, h .36/.76, `o` → 0, `p` → 1).
  4. Show the real capsule, then end the shape.

**Done when:** recorded on the phone at full speed, the search capsule, card and bar read as one object, with no pops, no text appearing inside the drop, and no colour jump at the end.

---

## Phase 8: The dashboard (the painting parts at the ridge)

Prototype reference: `setDash()`, `dashTick()`, `fitDash()`, the `#dash` markup, and the "dashboard content" block.

1. **Gesture:** pull down anywhere on the home screen except buttons, the bar, the drawer, the words and draggable items.
   - It only starts after 8dp of vertical movement; mostly-sideways moves are ignored.
   - Progress `p = start + dy / (0.52·H)`, with ×0.25 resistance past the ends.
2. **The split:**
   - sky (back layer, clipped to *above* the ridge) and clock move up by `0.20·H·p`
   - ground (front layer) moves down by `0.52·H·p`
   - the work block moves down by `0.52·H·p·1.05` and fades as `1 − 1.6p`
   - the dashboard sits behind, on `#07080C`
3. **Release:** a flick faster than 0.35 px/ms goes in its direction, otherwise open past 50%. Clamp the release velocity to ±6. Settle on a **critically damped** spring (response 0.42, damping 1.0), with no overshoot.
4. **Close:** pull up on the painting edges, tap them, or press Back.
5. **Content** sits in the gap, from just under the raised clock to just above the lowered ridge. It scrolls on its own, and its scrolling never triggers close.
   - **Mac link:** a green dot, "MacBook Air", "Live · 38 ms". Plain text, no pill.
   - **Continue card:**
     - "MenuBarView.swift" (24sp)
     - `glass_horizon · feat/menubar-view` in mono
     - "Edited on your Mac 4 min ago · open in Xcode, Terminal, Safari"
     - an ember **Continue on Mac** button (one line, no icon) and a **Details** button
   - **Needs you:** one panel with hairline rows.
     - Tests failed: hold-to-re-run, an 850 ms fill. A quick tap only says "Keep holding: this runs code on your Mac".
     - Exam tomorrow: **Set 7:00 alarm**, kept in sync with the home chip.
     - Fix ready: **Review diff** shows the inline diff with Dismiss and **Apply in worktree**.
   - **Spaces:** one panel with rows (icon, name, one line, count on the right). Use the prototype's four spaces.
6. Each section arrives a little after the one above, tied to `p`. Ripples still work while it's open: the sky layer is drawn raised and cut at the ridge, the ground lowered.
7. **Accessibility:**
   - while closed, the dashboard is not focusable (`Modifier.semantics { invisibleToUser() }` and no focus)
   - an "Open dashboard" action is reachable by TalkBack
   - Back closes it

**Done when:** pulling down on the phone feels like the prototype, and the flow apply fix → hold to re-run → "240 passed" → set alarm works.

---

## Phase 9: Polish pass on the phone

Fix anything that differs from the prototype at the phone's real size. Check every item:

- touch targets ≥ 48dp
- nothing truncated
- words always under the ridge
- "Continue on Mac" on one line
- TalkBack reads every control with a sensible name
- with animations turned off, everything still works
- no frame drops while dragging the drawer or dashboard (check with *Developer options → Profile HWUI rendering*)

---

## Phase 10: Performance on the POCO C71

- Wallpaper: decode once at screen size, keep the bitmap, and reuse the ridge `Path`.
- Redraw only what moves. Draw the bar in its own `Canvas` layer, and use `graphicsLayer` translations for the split rather than re-measuring.
- If the 4 px blur costs frames, turn it off on this phone and keep the 60% ink. That's allowed; the look stays nearly the same.

---

## Phase 11: The real ripple (optional; only after 1–10 are approved)

On API 33+, port the prototype's WebGL ripple (`glFrame`, `makeGL`, `FS`, `VS`) to an AGSL `RuntimeShader` applied to the wallpaper layers. Keep it off unless a ripple is running. Keep the Canvas-ring version as the fallback.

---

## Phase 12: Show it to Enoch

Record the screen on the phone (`adb shell screenrecord /sdcard/fox.mp4`) going through:

1. home and tilt
2. tap the words
3. open the drawer, search, launch an app
4. send, then receive
5. pull open the dashboard and do its flow
6. hold the bar to wake Fox

Then pull the video:

```bash
adb pull /sdcard/fox.mp4
```

List anything you couldn't match, with the reason.

---

## Phase 13: Real data (later, separate task)

- **Mac link:** connect `net/FoxLinkClient.kt` to Fox Desk (`apps/fox-desk`, port 8787, header `X-Fox-Token`, token from `~/Library/Application Support/FoxDesk/dev-token`). Show its `task.update` in the Continue card and `state.event` in the live view. Validate with the shared schemas in `packages/fox-protocol` (already on the asset path) and reject replays with the numeric-epoch rule.
- **Words from Keep:** Keep has no public API for phone apps. Read a Keep note labelled `Fox` through Fox Desk on the Mac, or from a Google Doc. Until then, keep the sample `WORDS`.

---

## Quick reference: where each thing lives in the prototype

| Feature | Search the prototype for |
|---|---|
| tokens and fonts | `:root{` (first one), `--fox-ink`, `fonts.googleapis` |
| wallpaper depth | `measure()`, `.wp-front{clip-path` (the same points are in `ridge.json`) |
| home words | `WORDS`, `fitWords`, `.words{` |
| bar | `const LINE`, `wavyBar`, `splashBar`, `drop(` |
| drawer | `setDrawer`, `drawerTick`, `renderDrawer`, `.dr-` |
| liquid shape | `blobTo`, `pillToCard`, `dropIntoBar`, `riseFromBar`, `cardToPill`, `emergePill` |
| dashboard | `setDash`, `dashTick`, `fitDash`, `.d-` |
| ripple shader | `glFrame`, `makeGL` |
