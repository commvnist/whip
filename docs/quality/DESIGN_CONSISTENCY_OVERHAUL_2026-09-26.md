# Whip top-down design consistency overhaul — 2026-09-26

Owner request: FB-20260926-003. This audit starts from private-release source `a42a30d9` on `main` and rechecks the September 26 reusable-architecture audit against current code and rendered states. The earlier 523-state catalog is a baseline, not proof that every viewport and interaction was sound.

## Method and acceptance

Review the shell, seven main destinations, major editors/details/dialogs, narrow and expanded layouts, 200% text, RTL, and both themes. Compare equivalent visual and semantic roles with their actual shared owners. Mark domain-specific composition as intentional when it carries different information or behavior. Confirm an inconsistency in source and, where layout matters, rendered pixels or an owning native UI test before redesigning it.

Implement every confirmed finding below through existing shared roles where possible. Preserve persistence and domain behavior. Run focused checks while editing; batch final source, Android and visual verification on at most two disposable emulators. Never select the connected owner phone for tests.

## Ranked findings and implementation plan

| Rank | Finding | Evidence | Resolution and focused proof |
| --- | --- | --- | --- |
| P1 | Folded support content can leave later rows unreachable | In actual Task/Habit/Goal navigation, `DestinationSupportPane` fixes a title and description above a weighted inner list; Track/Settings repeat the same shape. At 200% tabletop height, the header can consume nearly the whole pane. `FoldContextPane` for Home/Gym had branch-specific scrolling. | Give each support pane one native scroll owner over its title, description and rows, bounded by the allocated pane; keep tabletop navigation fixed. Seed more Task rows than fit at 200% and reach the last row, with the title accessible by scrolling back. |
| P2 | Gym permanent-deletion reviews hide the reason for a disabled action at large text | All four reviews pin long titles above scrollable impact. The current 320 dp/200% Machine original initially shows Removed/Kept, while its Active Workout blocker sits after other details. | Put all four headings in the native scroll content using the established `WhipDialogHeading` pattern; put Machine's blocker before impact details. Keep action buttons pinned. Check initial blocked reason and reachable final impact at 200%. |
| P2 | Settings save warnings look and announce like neutral information | The `Setting Saved with Warnings` result passes `WhipStatusKind.Status`, which selects informative purple and semantic state “Status.” | Map actual warnings to a shared warning status/tone and verify the spoken state and rendered tone. |
| P2 | Shared navigation rows silently change authored item names | The populated 200% Task test could not find `Pane task 6` because `NavigationRow` title-cased it. Caller review also found one-day Home Routine and active Workout names changing while multi-day Routine names did not; Area-scoped empty-status titles could change Area casing. | Preserve authored casing in those support and Home rows through an explicit shared row option; keep existing static navigation wording. Assert exact original names in real adaptive and Home journeys. |
| P2 | Equivalent group/section labels have inconsistent heading semantics | Track groups use `WhipSectionHeading`; Task groups, Task/Habit/Goal reorder groups, several Settings groups, and the adaptive support title are raw text. | Give equivalent labels heading semantics through existing presentation roles, retaining current type sizes and authored casing for dynamic labels. Verify heading traversal in focused native UI. |
| P2 | Wide Settings categories change between Material drawer pills and Whip support selection cards | [Retained current 900 dp pair](../../artifacts/design-consistency/2026-09-26/baseline/README.md): split mode shows descriptive rectangular `SettingsSupportPane` rows; opening Settings from expanded Home at the same width shows bare green `NavigationDrawerItem` pills for the same six choices. | Converge both renderers on the same selection grammar while preserving the compact phone index and narrow sidebar fit. Recheck both 900 dp modes. |
| P3 | English setup copy gets detached punctuation in RTL | Current 200% RTL first-run optional/error originals show sentence-ending periods at the beginning of wrapped lines; this changes reading order without translation. | Isolate English text direction in the first-run copy while retaining the RTL layout and control order. Recheck both exact 200% RTL states. |

## Deliberate variants and nonfindings

- Gym's `Whip / Gym` header identifies a destination with no Area scope; its Add/Search/Settings controls and destination tabs match the shared shell. No redesign is warranted by the current source and ordinary captures.
- Specialized Gym execution, charts, calendars, destructive impact content, and domain-specific day choices retain their own content model. A shared outer role is appropriate only where rendered hierarchy truly differs.
- Task and Habit widget RemoteViews rows and headers are matched; no widget design change is supported.
- One-day pinned Routines have a single whole-row start action; multi-day Routines show explicit day choices. The differing action shape is intentional. No populated visual comparison supports changing their card shell in this pass.

## Implementation and verification ledger

All seven confirmed findings have source changes: whole-pane native support scrolling, authored-name preservation, heading semantics, scrollable Gym deletion titles and early Machine blocker, warning state/tone, unified 900 dp Settings category cards, and content-directed first-run English copy in RTL. Domain mutation and persistence formats are unchanged.

Focused native checks pass 1/1 populated 200% tabletop, 1/1 Home authored-name journey, 4/4 Gym deletion reviews, and 3/3 wide Settings/RTL journeys on disposable API 34 emulators. The earlier in-flight compile and shared Gradle-output race stopped before product assertions; the corrected focused runs passed. `scripts/ui-catalog lint` still reports 523 required states and no pending/exception rows. A current-source full catalog and frozen candidate are next; final evidence and image review will be recorded here.
