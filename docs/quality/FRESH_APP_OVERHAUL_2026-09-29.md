# Fresh Whip audit and implementation overhaul — 2026-09-29

Status: Implemented and Verified within the accepted scope. Owner authorization: FB-20260929-014. Baseline: `ba1f7cc5`. Delivery: IMP-20260929-018 / VER-20260929-015.

## Scope and evidence standard

This review starts from current source, complete user jobs and fresh rendered evidence. Prior audits do not establish quality, completion or a keep decision. Repository history supplies only compatibility constraints and explicit owner preferences. Three GPT-6 Astra/high reviewers independently inspected productivity; Tracks/Gym; and shell/Home/Settings/shared experience. At the owner's continuation request, three GPT-6.1 Sol/xHigh agents finish unresolved integration and acceptance using the preserved current evidence. The parent owns cross-app synthesis, visual baselines, design consistency, integration and verification.

Every covered flow records its entry, intended outcome, normal/empty/error or boundary states, current evidence and disposition. A claimed bug requires a reproducible path or direct source proof. A proposed visual improvement requires a clear user benefit and a defined shared treatment. Accepted findings receive implementation and verification evidence; they are not left as recommendations.

## Design and experience pillars

1. **Stable structure.** Equivalent workspace headers, tabs, context controls, content edges and action anchors share geometry. Adaptation follows available space and text scale rather than arbitrary per-page rules.
2. **Obvious next action.** The screen makes its main job and primary action clear. Common work is direct; secondary configuration is progressively disclosed. Every control has one understandable role and bounded scope.
3. **Coherent visual hierarchy.** Identity, actionable status, supporting evidence and secondary controls have consistent typography, spacing, contrast and emphasis. Shared components encode repeated roles; no decorative overhaul may obscure user content.
4. **Continuous context.** Editing, correcting, deleting and returning preserve the parent destination, selection, scroll and useful draft state. Repeated actions do not eject users from their workflow. Navigation exits and destructive operations remain distinguishable.
5. **Truthful and resilient behavior.** Dates, units, counts, progress and completion reflect actual data. Loading, empty, filtered, failure, pending and committed states are distinct. History, undo and recovery protect authored intent.
6. **Accessible speed.** Daily work minimizes unnecessary steps without hiding meaning. Labels and touch targets remain usable with large text, keyboard, short screens and wider panes. Expensive work is scoped to changing data; correctness and accessibility remain requirements.

## Execution plan

1. Establish a fresh current-source workflow inventory and normal/enlarged visual baselines for each workspace and representative nested flows.
2. Independently critique each area; synthesize grounded issues into a single implementation table with owners, user-visible outcomes and acceptance checks.
3. Implement shared visual and interaction primitives first where multiple findings have the same root cause. Implement coordinated feature changes without parallel edits to shared owners.
4. Exercise each repaired workflow with exact short checks and fresh before/after rendering, including relevant error/recovery and large-text states. Review integration for duplicated controls, shifted geometry and lost context.
5. Run one final affected-change readiness batch and selected bounded native checks; record failures and their repairs honestly. Commit and push coherent verified changes. Close only when every accepted remedy has implementation and acceptance evidence.

## Independent review workbooks

- [Tasks, Habits and Goals](FRESH_AUDIT_PRODUCTIVITY_2026-09-29.md)
- [Tracks and Gym](FRESH_AUDIT_TRACKS_GYM_2026-09-29.md)
- [Home, shell, Settings and shared experience](FRESH_AUDIT_SHELL_2026-09-29.md)
- [Cross-app integration and external entry](FRESH_AUDIT_INTEGRATION_2026-09-29.md)

## Findings and implementation

| Group | Accepted outcome | Owner / acceptance |
|---|---|---|
| P1–P2 | Compact meaningful Habit schedule/availability cards; skipped work counted as finished | Productivity; exact status/summary checks and fresh cards |
| P3–P4 | Concise Task decision information; truthful occurrence history; direct archive restore | Productivity; history state checks and rich/large-text cards |
| P5–P6 | Explain invalid Habit input and unavailable entry actions before failed submission | Productivity; invalid input and paused/archived correction journeys |
| P7 | Definition editing and elapsed reset return to the same inspector context | Productivity; cancel/save/recreation journeys |
| P8 and Goal hierarchy | Obvious milestone/reached-goal action, readable numeric progress and secondary elapsed Reset | Productivity + parent shared builder; normal/enlarged fresh rendering |
| TG1 | Preserve raw invalid Set Details drafts and show actionable validation | Tracks/Gym; raw optional numeric input and discard checks |
| TG2 | Correct historical sets without reopening finished sessions | Tracks/Gym; old-session correction with another active workout and stale boundary |
| TG3 | Open named source entries from Track trend evidence | Tracks/Gym; repeated-date identity and retained analysis context |
| TG4 | Put active set logging ahead of occasional workout setup actions | Tracks/Gym; compact hero/rest, discoverable options, preserved complete metrics and next-set targeting |
| S1 | Stable Home orientation and daily summary/action roles | Shell; final-task/habit completion, empty/loading and enlarged rendering |
| S2–S3 | Review retains Area context and leads with readable, visibly scaled outcome evidence | Shell; source return/recreation and compact/wide 200% cards |
| S4–S5 | Focus-correct search and per-category Settings reading position | Shell; keyboard activation and category return/search/recreation |
| S6–S7 | Restore terminal failures unlock retry; Area query never becomes invisible | Shell; fast failure and query/count transition regressions |
| S8 | Bound arbitrary selected-document reads before restore parsing | Parent; exact boundary/unknown length/UTF-8/error and valid backup compatibility |
| S9 | Retain custom emoji drafts through failure, dismissal and constrained layouts | Shell; receipt, retry and short/200% editor |
| R1 | Shared productivity card identity stays readable at enlarged text | Parent; fresh 200% Goal original truncates an ordinary savings title; shared full-title layout and native text-layout assertion |
| R2 | Primary editor headings stay below system bars with the keyboard open | Parent; fresh 200% Set Details original exposes clipped heading; shared explicit system/IME inset owner and native screen-bound assertions |
| R3 | Enlarged workspace headers keep identical root geometry | Shell continuation; fresh native comparison exposes Gym's larger two-line identity; shared measured identity reservation and exact normal/200% bounds |

The three independent reviews cover 156 current-source workflow dispositions (54 productivity, 53 Tracks/Gym, 49 shell); 20 cross-app integration dispositions bring the total to 176. These are source-review dispositions, not 176 executed journeys. Normal and 200% fresh root captures supplement the review. All 24 accepted remedies (P1–8, TG1–4, S1–9 and R1–3) receive implementation and focused acceptance. No previous audit disposition is carried over as acceptance.

## Implementation and acceptance matrix

All proposed remedies above are implemented. The workbooks preserve original findings, reviewed keeps, exact selectors and failed attempts. This matrix identifies the final user-visible behavior and its acceptance owner; [durable receipts and original images](../../artifacts/fresh-app-overhaul/2026-09-29/README.md) distinguish selected execution from source review.

| Remedy | Delivered behavior | Focused acceptance |
|---|---|---|
| P1 | Habit management cards show concrete schedule, Area, availability and pause state in the shared compact summary | `HabitPresentationTest`; management Habit native check; normal/200% originals |
| P2 | Skipped Habits count as finished rather than remaining | `CompactCollectionStatusTest` skipped/completed/status cases |
| P3 | Collapsed Tasks show concise timing; expanded details retain complete evidence | `TaskCollectionPresentationTest`; full-width Task card native check |
| P4 | Same-date reopened history says Open; archived Task recovery uses Restore as the primary action | Task presentation/state checks and responsive shared action source review |
| P5 | Invalid current and historical Habit amounts explain the error before Save | Two exact invalid-amount native methods, with no persisted invalid entry |
| P6 | Paused/archived Habit actions explain the prerequisite while historical correction remains available | Archived prerequisite native method and definition/entry source boundaries |
| P7 | Habit/Goal child definition edits and elapsed reset return to the selected inspector section; successful Goal edits refresh once | Cancel, persisted save/unchanged save/external-edit native methods; repeated persisted history cleanup; shared rename/scroll native method |
| P8 | Reached Goals offer Review; milestone Goals offer Items; numeric progress is readable with bounded visual fill and truthful above-target values | Reached Goal and actual-200% Review/Items native methods; fresh normal/200% Goal originals |
| TG1 | Set Details preserves malformed raw text, explains numeric/RPE errors, disables invalid Save and protects the draft | Exact native malformed optional-input/recreation/discard/correction method; `GymUxRulesTest` |
| TG2 | Historical Set editing corrects the owning finished workout without resuming it or changing an unrelated active workout | Exact old-session/recreation/stale-edit native method |
| TG3 | Recorded trend evidence names and opens the exact Entry, retaining Insights scope and return context | Exact repeated-date Entry identity/correction native method |
| TG4 | Active workout gives logging priority; occasional setup and complete metrics remain available through labelled options | Exact compact logging/options/totals/next-set native method; normal/200% Gym originals |
| S1 | Home retains readable daily summary slots through loading, zero and final completion, with stable heading/action roles | Home geometry, enlarged RTL and final originals |
| S2 | Review restores its originating Area after all-Track evidence and recreation | Exact Work/Monthly/section/Task-total/all-Track return native method |
| S3 | Review leads with outcomes, separates long identity from totals, labels chart scale/dates and discloses correlations | Actual-200% long-title/zero/chart native method and correlation native method |
| S4 | Enter activates the focused search control; only query submission runs the selected result | Actual hardware TAB/ENTER focus check plus search IME check |
| S5 | Settings restores each category's reading position and exact search anchors | Category-return/recreation and actual-200% search native methods |
| S6 | Restore failure produces a terminal request receipt, unlocking Retry and Cancel; duplicate submissions remain blocked | Two exact fast-failure/recreation/duplicate native methods |
| S7 | A nonblank Area query stays visible when the list shrinks, with Clear and truthful no-match state | Exact count transition/query/recreation native method |
| S8 | A selected backup is bounded before parsing; a failed new selection cannot leave an old candidate actionable | Three `BackupDocumentReadTest` boundary/UTF-8/error cases; invalid selection and plain/encrypted roundtrip native methods |
| S9 | Custom emoji drafts survive dirty dismissal, fast failure and recreation, with durable retry and reachable controls | Exact actual-200% custom emoji native method |
| R1 | Shared productivity identities remain complete at enlarged text, including Goal action labels | Shared full-title text-layout and Goal Review/Items text-layout native assertions |
| R2 | Primary editor title and complete Save stay clear of system bars and the IME | Actual-200% Set Details screen-bound checks and shared light-on-dark dialog theme native check |
| R3 | Tracks and Gym share exact toolbar, search and context rectangles at normal and actual-200% text | Unrelaxed native geometry comparisons in both supporting overview executions |

Small integration refinements belong to the same owners: stable entity UUID inspector keys, a neutral Goal progress track, measured Goal action labels, and singular/plural Track collection counts. Existing domain repositories and persistence formats remain the mutation owners; no competing design system or generic state framework was added.

Final affected readiness passes 450 JVM methods in 49 suites with zero failures/errors/skips, Android-test compilation, debug lint and debug assembly. Thirty-nine distinct selected native methods have passing evidence (37 behavioral plus two root overview methods), with each native invocation bounded to 55 seconds. Fresh final roots are captured and inspected at normal and actual Android 200% text. The [exact receipts](../../artifacts/fresh-app-overhaul/2026-09-29/README.md) retain failed and incomplete attempts, source hashes, remaining non-error lint diagnostics and timing limits. All accepted remedies above are closed; none is left as an unimplemented suggestion.

## Practical limits

Acceptance covers the implemented remedies and selected boundary states; it does not claim that the complete app test inventory or every possible device/state combination ran. Original images use deterministic synthetic records. Subjective appearance still benefits from owner use on their phone. At audit acceptance the phone was at 0.3.88/code 94; subsequent explicit FB-20260929-015 authorization releases this source privately as 0.3.89/code 95 under VER-20260929-016. No Play publication occurred.

Selected backup import has an explicit 32 MiB limit, including encrypted envelopes. Export and private recovery are uncapped. This bounds the provider stream before parsing, rather than promising that every smaller JSON object graph is safe under all memory conditions. No schema, data epoch or portable-backup format changes were introduced.
