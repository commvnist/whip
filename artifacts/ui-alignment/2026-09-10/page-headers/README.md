# Shared page-header spacing — 2026-09-10

FB-20260910-005 / FND-20260910-039 / DEC-20260910-029 / IMP, VER-20260910-030.

The existing `WhipPageHeader` now measures actual supporting text and leaves
inter-item gaps to its parent. `whipPagePadding` shares page edges between full
lists, fixed Task controls and narrower Track browser panes. Equivalent Habit,
Goal and Track collection pages use the existing 8 dp sibling rhythm.

## Observed result

On the matched 1080×2400/420 dpi phone, Tasks and Habits both retain their Today
title at `[53,503][250,589]` and their subtitle origin at `[53,620]`. Short
subtitle bounds shrink from 112 to 49 pixels high. The first Task title moves
from y=1092 to y=965 (127 pixels upward); the first Habit title moves from y=839
to y=755 (84 pixels upward). Cards, navigation and title/action alignment remain
intact. Task capture retains its existing floating-label visibility allowance.

The 1800×1200/160 dpi native wide review retains aligned Tasks/Habits/Goals
headings and content in their shared reading column. Tracks keeps its existing
list/detail browser with a narrower list inset. Natural wrapping remains
visible; longer descriptions may occupy more lines.

## Verification

- `scripts/check --ready`: 384 JVM tests in 44 suites, zero failures/errors/skips;
  Android test compilation, lint and debug packaging pass. The final readiness
  rerun also passes after the geometry assertion correction.
- `scripts/ui-catalog lint`: 534 required surfaces, zero pending selectors or
  platform exceptions. `git diff --check` passes.
- Phone `6O32gI`: two header component methods pass, then 18/19 neighboring
  methods pass. The sole failure is the new page assertion omitting the existing
  8 dp input-label allowance from `FocusedInputVisibility`.
- Final phone `25Eoup`: corrected workspace geometry and both explicit Task
  primary/history selection methods pass 3/3, zero failures/skips. Together the
  phone evidence covers 23 distinct passing methods after that exact replacement.
  Runtime production is unchanged between the original and replacement batches.
- Wide `xg7MaG`: all four Task/Habit/Goal/Track native page journeys pass 4/4,
  zero failures/skips. This is viewport coverage on API 34, not another API claim.

Phone command:

```sh
ANDROID_SERIAL=emulator-5554 scripts/qa-targeted \
  --android com.whip.app.ui.InteractionControlUiTest#pageHeaderActionsDoNotShiftTitleOrSupportingText \
  --android com.whip.app.ui.InteractionControlUiTest#pageHeaderKeepsFollowingContentVisuallySeparated \
  --android com.whip.app.AdaptiveWhipScreenTest#everyPrimaryWorkspaceUsesTheSameHeaderAndNavigationGeometry \
  --android com.whip.app.TrackWorkspaceUiTest \
  --android com.whip.app.InlineTaskCaptureE2ETest \
  --android com.whip.app.ProductivityItemBuilderJourneyE2ETest \
  --android com.whip.app.VisualCatalogPagesTest

ANDROID_SERIAL=emulator-5554 scripts/qa-targeted \
  --android com.whip.app.AdaptiveWhipScreenTest#everyPrimaryWorkspaceUsesTheSameHeaderAndNavigationGeometry \
  --android com.whip.app.TaskBulkSelectionUiTest#activeSelectionKeepsPrimaryActionsVisibleAndSecondaryActionsInOverflow \
  --android com.whip.app.TaskBulkSelectionUiTest#historySelectionOffersRelevantActionsForCompletedAndArchivedTasks
```

Wide command, after explicitly setting only the disposable emulator to
1800×1200 and density 160:

```sh
ANDROID_SERIAL=emulator-5554 scripts/qa-targeted \
  --android com.whip.app.VisualCatalogPagesTest#captureTaskPageCatalog \
  --android com.whip.app.VisualCatalogPagesTest#captureHabitPageCatalog \
  --android com.whip.app.VisualCatalogPagesTest#captureGoalPageCatalog \
  --android com.whip.app.VisualCatalogPagesTest#captureTrackPageCatalog
```

The emulator's display overrides were reset and the task-created emulator was
shut down afterward. No owner-phone, release, version, schema or backup change.

## Visual review and exclusions

All fourteen retained original PNGs were inspected. Their XML
hierarchies accompany them with line endings and trailing whitespace normalized;
PNG bytes are unchanged. The eight final phone images cover Tasks/Habits
Today, Goals, Tracks, Habit/Track Insights, Gym Library and Settings Planning;
the four wide images cover Tasks/Habits Today, Goals and the Track browser.
Reviews accept readable titles/copy, compact header-to-content rhythm, preserved
actions and intentional content-driven wrapping. This is a bounded header
review, not a fresh acceptance of every captured dialog or the whole catalog.

The two `before` images are unchanged-source comparison from the initial four
catalog methods. That campaign completed its methods but was rejected by the
source-drift guard because editing started after the baseline APK was built.
It is visual comparison only, not test acceptance. The copied originals were
the newest `tasks.today.populated (3)` and `habits.today.populated (3)` exports;
old device exports were cleared before final phone/wide captures.

The initial source assertion expected the old literal padding declaration; its
replacement reads the shared padding's actual value. One interim compilation
missed the `assertEquals` import and was corrected. These failed intermediate
runs and the first shell assertion are excluded from green acceptance. The
retained failed neighboring result is named explicitly in `verification/`.

Subjective normal-use appearance remains awaiting owner validation. No complete
product suite, store qualification or physical-device deployment is claimed.
