# Platform and accessibility matrix — 2026-09-02

## Devices

- API 26 small phone: 480 × 800 px at 240 dpi (320 × 533 dp).
- API 37 large display: 2560 × 1800 px at 320 dpi (1280 × 900 dp).

## Findings and remediation

- The emoji picker originally left a searched preset almost entirely clipped when the software keyboard reduced the API 26 window. Search mode now removes redundant explanatory copy, tightens only the result spacing, brings saved matches into view, and explicitly clears focus/hides the keyboard when a choice closes the picker.
- Collapsing or expanding unified-search filters on a short window could retain a deep list offset and hide the active-filter summary. The results list now returns to its disclosure and summary region when that state changes.
- Several end-to-end journeys assumed targets were already composed or visible. They now scroll the owning list before acting and distinguish compact navigation from persistent wide sidebars/master-detail panes.

## Verification

- API 26: all 37 interaction-control tests passed; the eight compact/adaptive large-text and dialog tests selected for the physical viewport passed; 30 core recreation/navigation/settings tests passed after portable scroll targeting; platform-notification surface passed.
- API 26 screenshot/XML: `api26-emoji-search-final.*` shows the complete 52 dp search-result target while the keyboard is open.
- API 37: all 84 interaction/adaptive/visual/Compose-semantics tests passed in one run. The focused 32-test Gym/routine/5/3/1/Track/search set passed across the initial run plus exact wide-pane assertion reruns.
- API 34: the complete `scripts/check --emulator` gate passed all 552 JVM and all 877 Android tests with zero failures or skips. `scripts/check --full` then passed release lint-vital, R8/resource shrinking, signed release-bundle generation, release APK generation, and optimized benchmark-harness assembly.
- Actual TalkBack was enabled through `com.google.android.marvin.talkback/.TalkBackService`. Keyboard focus traversed Home → Tasks → Habits → Goals → Tracks → Gym → Settings, then the Home destination cards in order. Enter on the focused Gym card navigated to Gym and selected Workout. TalkBack was disabled after verification.
- TalkBack evidence: `api37-talkback-home.*`, `api37-talkback-tab1.*`, `api37-talkback-tab12.xml`, and `api37-talkback-enter-gym.*`.
- Release artifacts: unsigned APK SHA-256 `6e7055fb248446f4cac8af3d5dbfc276d25d89b7010301d5546d35a6f11a740d`; signed AAB SHA-256 `bcdd37d9eeabd7b16bfe98cf8df55cef0fc90032f78585d46fa48386eda1e3d5`.

Synthetic fold/desktop tests were not treated as small-phone failures when their requested canvas exceeded the physical 320 dp root. Those contracts were instead run on the 1280 dp API 37 display, where the complete adaptive suite passed.
