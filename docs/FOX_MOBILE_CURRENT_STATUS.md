# Fox Mobile — Current Status

Updated: 2026-11-10
Workspace: `apps/fox-mobile`
New product reference: `/Users/enoch/Desktop/Glass Main Archi Folder/Claude outputs/Fox Line v40.html`

## Product direction

The v40 prototype is the latest product direction and supersedes the v30-only design lock in `docs/FOX_MOBILE_PLAN.md`. The Android implementation should keep the approved depth wallpaper, work-first home, Fox Line, and fast idle behavior while adopting v40's dashboard and more meaningful workflows. Demo interactions should feel active, but actions that require the Mac must remain clearly simulated until the Fox Desk connection is implemented.

Obsidian is useful for navigating the local vault and progress graph. The v40 HTML is already available as a local file, so no network fetch is needed to inspect it.

## Reported issue: Android navigation UI overlaps Fox

`MainActivity` already uses edge-to-edge layout, transparent system bars, and hides Android's navigation bars on create, resume, and focus. It uses transient bars on swipe, so Android/HyperOS is allowed to show its navigation UI temporarily over the bottom area. The Fox line is a 56dp bottom touch strip, which makes it vulnerable while those bars are visible. Apps cannot replace Android's Home gesture or guarantee that system navigation affordances never appear.

The working change tracks the system gesture-safe inset and moves Fox's bottom controls up when HyperOS reports its gesture indicator enabled. On the connected POCO C71, `navigation_mode=2` and `Settings.System.show_navigationbar=1`. HyperOS therefore still renders its own white gesture handle while Fox renders its own bottom line. This reads as two navigation bars. Code now reads the HyperOS setting and can collapse the Fox clearance when the user enables “Hide full screen indicator”; that device setting has not been changed. Preserve Android's Home gesture; Fox cannot replace it.

## Work completed in this session

- Reviewed `Fox Line v40.html`, current Android code, `PROGRESS.md`, and the Obsidian file map.
- Reviewed the attached 87-second phone recording against v40. It demonstrates four key behaviors: upward drag on the capsule reveals apps, the home shows the saved quote/work summary, a downward drag parts the painting to reveal the dashboard, and the Fox Line changes as activities happen. The largest missing screen was the ridge-parting dashboard; the previous report overstated how closely the running app matched the reference.
- Confirmed Phases 0–5 are recorded as complete in the repo. Existing device measurements are from prior work; they were not repeated in this session.
- Added launcher intent visibility in `AndroidManifest.xml` so Android 11+ can enumerate `MAIN`/`LAUNCHER` apps.
- Replaced the old Spotlight placeholder with an initial native launcher drawer: installed app query/launch, category filters, recent apps, list/grid options, search filtering, drag-to-open/close, and Back-to-close.
- Mounted the drawer from `MainActivity`.
- Added system navigation inset tracking and moved Fox's bottom touch strip above the navigation area when Android reveals it.
- Increased clearance after the first screenshot showed the Fox line still coincident with the OS gesture handle. The follow-up screenshot shows two distinct bars: Fox's control above and Android's handle below.
- Added the v40 drawer transition: the home layer scales to 0.975 and dims to 50% while the drawer opens.
- Corrected the interaction model: tapping/focusing the search capsule opens a people/conversation search panel; installed apps remain hidden until an upward drag on the capsule. The search field is labeled “Search contacts, chats and files.”
- Fixed grid grouping so app icons are rendered in unique rows of four instead of repeating across rows.
- Fixed compile errors found by the first build attempt (Compose state delegate imports and `FoxContact` references).
- Connected POCO C71 (`25028PC03I`, Android 15) over ADB; `./gradlew installDebug` compiled and installed successfully after the fixes.
- Tapped the search capsule on-device: the app drawer opened and listed installed apps. Back closed the drawer after dismissing the keyboard.
- Tapped an installed app from the drawer; the Android Clock app launched, and it appeared in the drawer's recents when returning to Fox.
- Rebuilt and reinstalled after the home transition and grid fixes; `installDebug` passed.
- Checked the `AndroidRuntime` log after launch; no crash entries were present.
- Rebuilt and reinstalled the split search/app browsing implementation on the POCO C71; `installDebug` passed.
- On-device check: tapping the capsule showed recent conversations without apps; a subsequent upward drag revealed app categories, recents, and installed apps. The Clock app was visible in the drawer.
- Confirmed HyperOS `show_navigationbar=1`, explaining why the OS gesture handle and Fox's custom line can both appear.
- Added and integrated the v40 dashboard surface with Mac connection status, Continue card/details, Needs You test/alarm/fix items, and Spaces. It uses the existing wallpaper progress provider to move the sky and clock up and the front ridge down, while the home work block moves down and fades.
- Made the dashboard pull gesture update the split progress while dragging, then settle open/closed; Back and tapping outside close it. The test rerun action now requires an 850ms hold, Continue Details expands, and fix dismiss/apply update their visible state.
- The first device capture showed the dashboard cards drawing above the wallpaper. Corrected the z-order: dashboard content now sits between the back and front wallpaper layers, so the clock and ridge mask it as they part. The list begins near the top of the revealed gap, matching the recording's composition.
- Installed the integrated dashboard on the POCO and verified a downward swipe beginning over the clock opens it. Latest screenshot confirms the cards now sit behind the parted wallpaper and the Mac status row clears the clock. The screenshot still needs visual refinement against v40, but the dashboard is no longer missing or drawn over the clock.

## Verification state

- `./gradlew :app:compileDebugKotlin` passes on the latest source; Kotlin emits only deprecation warnings for system bar color properties. An earlier dashboard integration installed successfully on the POCO and its content was observed on-device.
- The search-first / drag-up-for-apps behavior is visually confirmed on the POCO. The OS handle and Fox's line remain a visual mismatch while HyperOS's indicator setting is enabled; toggle “Hide full screen indicator” on the phone to use the single Fox line presentation.
- The app list appears after an upward drag. One app launch and its recent-app entry were verified on the previous installed build; latest build's list and categories were visually checked. Grid, category accuracy, live contacts/action/file search, rotation, screen-reader behavior, and transient system-bar reveal remain unverified.
- Latest `installDebug` passed and installed to the POCO. Down-swipe-from-clock and dashboard rendering were checked. Performance benchmark and unit tests were not run in this check.
- No phase commit was made for this work-in-progress.

## Progress estimate

| Area | State | Remaining |
|---|---|---|
| Phases 0–5 (foundation, theme, depth, home, speed, Fox Line) | Previously completed and committed | Recheck against v40 where its vision changed |
| Phase 6 drawer | Compiles; open/list/app launch/recents verified on device | Visually check grid, then finish contacts/actions/files, panel physics and v40 matching |
| Navigation overlap | Root cause identified: HyperOS indicator enabled, so OS handle and Fox line both render | User can hide the full-screen indicator; then verify Fox's line placement. Check transient-bar reveal and 3-button mode |
| Phase 7 send/receive | Not started | v40 one-shape transfer animation and interaction flow |
| Phase 8 dashboard | Initial screen integrated; latest ridge-layer ordering and down-swipe-from-clock checked on POCO | Refine card density/clipping against video; verify dismiss, alarm, test hold, fix and Spaces actions; connect real Mac actions later |
| Phases 9–12 polish/performance/demo | Not started | Device QA, benchmark, screen recording |
| Phase 13 real Mac/Keep data | Not started | Fox Desk link and data source integration |

Most of the visual foundation and primary line interaction is already implemented. The app is **not finished**: drawer validation, dashboard, transfer morph, robust navigation behavior, user-facing activity entry points, and Mac integration remain. A defensible percentage is not available because the remaining phases include substantial interaction and device-validation work.

## v40 priorities

1. Finish navigation QA on the POCO C71. The system gesture indicator is currently enabled; evaluate the Fox-only line presentation after the user enables HyperOS “Hide full screen indicator.”
2. Finish the drawer against v40: wire real contact/chat sources, complete actions and file search, check app list/grid, keyboard, open/close physics, and close-on-line/outside behavior.
3. Refine the dashboard workflow against the recording: confirm ridge clipping and drag-from-clock, then verify Continue, Needs You, alarm, diff review, and Spaces at device size.
4. Add the v40 send/receive flow using one continuous moving shape.
5. Move meaningful triggers out of the debug-only menu while leaving simulated Mac actions clearly separate from later real integration.
6. Run performance/device checks after each feature phase; the phone is now connected for this work session.

## Remaining phone checks

The POCO is connected. Continue checking:

- current system navigation mode (gesture or 3-button),
- whether the bar appears on launch or only after edge swipes,
- whether Fox controls remain tappable while the bar is transient,
- swipe-up Home and back-edge gestures,
- app drawer inventory and launching,
- rotation and keyboard behavior.

No additional connection is needed for the next round of device checks.
