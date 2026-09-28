# Full Whip product audit and implementation plan — 2026-09-28

Status: **Implemented and verified within the requested focused scope**. All 20 accepted groups are complete; 177 source-reviewed workflow dispositions, 14 distinct JVM methods and 15 distinct Android methods. Owner: FB-20260928-005 / VER-20260928-005. Baseline: clean pushed `d737209a` on `main`. Parent plus two GPT-6 Astra/max agents. [Exact receipts and original captures](../../artifacts/product-audit/2026-09-28/README.md).

## Assessment and delivery plan

1. Map all major destinations and cross-cutting workflows against current source: entry/discovery, initial and populated state, authoring/validation, successful and failed execution, editing/history/archive/delete, back/recreation, scope, units/dates, keyboard and accessibility.
2. Inspect current visual evidence and shared owners. Assess hierarchy, terminology, dialog action order, text growth, touch targets, empty/error states and recovery. A source review is not a claim of fresh device execution.
3. Record supported findings with their source, user impact, remedy and small acceptance check. Reuse existing owners when semantics match; keep domain-specific semantics explicit. State justified keeps and untested boundaries instead of manufacturing changes.
4. Implement every accepted remedy in coherent groups, including all affected callers and persistence boundaries. Preserve source records and existing data formats unless a demonstrated defect requires otherwise.
5. Run only selected exact JVM/native checks, each under 55 seconds with a 3-second kill grace. Serialize builds. No full profile, `check --ready`, candidate, exhaustive capture or release campaign. A timeout stays incomplete; adapt the validation approach rather than retrying the same slow command.
6. Review integrated diffs, reconcile the entire plan to concrete outcomes, retain exact evidence and remaining limits, update product memory, and commit/push coherent verified work.

## Coverage ownership

| Owner | Scope | Detailed report |
| --- | --- | --- |
| Productivity agent | Home/Tasks, Habits, Goals, Track; domain rules, repositories, editors, history and analysis | `PRODUCT_AUDIT_2026-09-28_PRODUCTIVITY.md` |
| Gym/Settings agent | Gym, Routines, 5/3/1, libraries/history/progress; preferences, backup/recovery, widgets/notifications/integrations | `PRODUCT_AUDIT_2026-09-28_GYM_SETTINGS.md` |
| Parent | Shared shell/navigation/search, inspectors/dialogs/forms, organization, design system, feedback, architecture and integration | This report |

## Questions and decision criteria

| Owner question | Evidence needed |
| --- | --- |
| Can users easily do what they want? | Main action discoverability, meaningful scope/status, predictable back path, retained drafts and actionable errors. |
| Is the experience coherent and well designed? | Consistent hierarchy, terminology, responsive information, clear primary/secondary/destructive order and readable data meaning. |
| What should be shared or refined for reuse? | Equivalent roles with duplicate layout/behavior; repair an existing component or extract a small owner only when more than one caller benefits. |
| Are visual overhauls warranted? | Demonstrable scanning, hierarchy or reachability problems; no arbitrary palette or navigation replacement. |
| Is the app extensible and uncoupled? | UI versus domain ownership, immutable record identity, query/mutation boundaries and independent feature composition; avoid speculative abstractions. |
| Are there bugs? | Trace concrete invalid input, stale/partial state, identity/date/unit errors and recovery/lifecycle behavior to their root owner. |

## Accepted shared implementation groups

| ID | Source finding and user consequence | Remedy and small acceptance check |
| --- | --- | --- |
| S1 | `UnitSelectionField` disables an empty compatible-unit collection; Custom has no built-in units, so new Habit/Track Custom measurement creation reaches a dead end. Its fallback label can also name a unit the draft has not selected. | Keep creation reachable from an empty collection, show unresolved selection truthfully, preserve selected archived units. Exact native empty-unit creation/selection case. |
| S2 | Area/Tag submission rejection clears request context before the active child reads its error. Color/move/merge dialogs remain interactive during saves. Inline `CreateAreaDialog` restores a saved busy flag although its callback belongs to the lost composition. | Retain failed request ownership, apply the existing saving overlay, restore interrupted inline drafts to an actionable retry, ignore callbacks to disposed dialogs. Exact color-busy and Area restoration cases plus existing taxonomy failure neighbor. |
| S3 | Area/Tag create/rename/merge bodies use non-scrolling columns, inconsistent spacing, long titles and dynamic errors; content can outgrow the available body while fixed actions remain below it. Shared Discard Changes also uses ordinary rather than destructive action styling. | Use the existing shared body and explicit scroll ownership; retain full errors, identity and safe-before-destructive action order. Give Discard Changes the existing destructive role. Inspect selected short/large-text form rendering and retry reachability. |
| S4 | Task Time, optional clock settings and reminder-time creation duplicate native clock dialogs. All lack keyboard-mode choice; their native clock bodies are not scrollable. | Consolidate through the existing `ClockPickerDialog`, with consistent Set/Cancel/Clear and duplicate guidance, a keyboard/clock switch and scrollable body. Preserve 12/24-hour settings and authored minutes; exact switch/draft/duplicate tests and rendered inspection. |

Shared investigation also covers welcome/setup, first-use Home recovery, responsive shell/Area scope, global-search query/partial-state/routing, inspector hierarchy, date/calendar presentation, controls/status/errors, item builders, emoji/color, theme, back/IME handling and deletion boundaries. Their final dispositions follow below.

Productivity findings accepted for domain implementation: compatible Habit unit conversion; recurring Task overdue context; bounded duplicate names across Task/Habit/Goal/Track; unsaved-draft protection for Habit logging/history/pause; effective-date Goal Latest ordering; truthful Review window/milestone outcomes; inclusion of the first partial flexible Habit period; removal of the one-year streak truncation; exact decimal aggregation for ordinary Habit/Track values. The agent report owns exact evidence and all reviewed workflow dispositions.

All seven [Gym/Settings groups](PRODUCT_AUDIT_2026-09-28_GYM_SETTINGS.md) are accepted: contextual numbered-machine record eligibility; retained tracked-record drafts; archived identities in historical summaries/filtering/sharing; correct historical entered-load conversion; a real reminder-scheduling retry; access to older Training Max decisions; explicit saved status/type in Gym CSV. Implement through the existing domain, formatting, request and export owners, with additive CSV fields and unchanged portable-backup format.

Historical audits are reference material; this task has the owner's new bounded verification contract.

## Shared workflow, visual and architecture dispositions

These 24 rows are source/design review, not 24 executed journeys. The two domain reports account for another 153 reviewed workflow rows, making 177 reviewed dispositions in total.

| Workflow / owner | Disposition |
| --- | --- |
| Welcome, recommended defaults and customization | Keep the two-stage setup, optional preferences and receipt-owned completion in `FirstRunSetupDialog`; notification permission follows confirmed setup. |
| First-use Home, scoped absence and hidden sections | Keep direct capture/destination recovery and the distinction between true absence and Area filtering. |
| Primary destinations and secondary Settings | Keep six primary destinations and a separate Settings action. Current labels/actions consistently match their destinations. |
| Narrow, rail, book-fold and tabletop shell | Keep `AdaptiveLayout`/`WhipWorkspaceLayout` and pane-aware placement. No new adaptive-shell defect substantiated; fresh all-fold execution excluded. |
| Back/IME/transient surfaces and external launch queue | Keep existing priority and private launch/generation gates. Child editors own discard/busy state; do not add a parallel navigation stack. |
| External text capture, widget configuration and recovery host | Keep shared theme/startup recovery and domain-specific input validation. External permission/delivery certification is outside the selected check scope. |
| Area view and temporary scope recovery | Keep canonical Area identity, user-selected scope and temporary Show All Areas rather than rewrite saved preferences. |
| Unified Search entry/scope and query filtering | Keep context-aware starting scope, local all-Areas override and structured query semantics. |
| Search loading/failure/history bounds | Keep explicit incomplete-result disclosure and domain-owned load errors. History caps remain explained rather than claimed exhaustive. |
| Search result navigation | Keep exact domain identities and Task destination precedence; current routing source reflects prior correction. |
| Inspector title, tabs, evidence and fixed primary action | Keep shared `EntityInspector`, bounded long identity and per-section saved state. Current selected-date/history facts remain domain-owned. |
| Loading/error/retry surfaces | Keep `DomainLoadContent`'s bounded-parent scroll rule; wrapping it unconditionally would reintroduce the previously reproduced nested-list crash. |
| Transient success, errors and Undo | Keep request receipts as completion authority and prioritized transient feedback/recovery tokens. A snackbar is not a save receipt. |
| Editor structure and dialog actions | Keep identity → required meaning → schedule → optional organization/detail, fixed actions and explicit dirty close. S3 aligns discard styling; domain fixes add missing dirty protection. |
| Clock and reminder-time selection | S4 unifies repeated dialogs with keyboard/clock choice, actual platform clock format, duplicate explanation and fixed confirmation. |
| Date/calendar selection | Keep shared calendar/wheel choices, selected-date state and caller-specific validation. Track future dates are legitimate and must not inherit Habit/Goal constraints. |
| Unit choice and inline creation | S1 removes the empty-Custom dead end and unselected fallback label. Feature owners retain conversion policy; S1 does not silently reinterpret values. |
| Area create/rename/move/merge/archive/delete | S2/S3 protect pending mutations and restored drafts, keep visible request errors and scroll long forms. Move-and-preserve precedes irreversible deletion. |
| Tag create/rename/merge/archive | S2/S3 reuse the same save and body owners, preserve archived-name restore and explicit cross-feature rename meaning. |
| Color and emoji identity | Keep canonical palette/custom color and saved emoji owners. S2 freezes the shared color dialog during persistence; color choice remains optional identity. |
| Collection, record, setting, execution and summary cards | Keep current shared builders, full-width metadata beneath identity and distinct actual domain roles. Do not force a chart, live Set composer and ordinary record into one universal card abstraction. |
| Spacing, typography, shape and theme | Keep `WhipSpacing`, shared headings, rectangular action wrappers and semantic light/dark tones. S3 removes local taxonomy spacing drift. |
| Accessibility and enlarged text | Preserve native choice roles, named actions, 48dp controls, headings and unclamped font scaling. Selected changed forms receive fresh native checks; no all-screen TalkBack claim. |
| Extensibility and coupling | Keep Room/domain facts outside presentation builders, immutable historical identity and independent feature ViewModels. New reuse is limited to actual repetition: clock dialog, bounded copy names, compatible numeric draft conversion, tracked-record eligibility and reminder retry. Large screen files remain a maintenance cost; splitting them solely by line count would add risk without improving this task's behavior. |

### Visual review basis

Personally inspected the retained current-baseline Home/Focus light rendering, Goal Insights dark rendering, Gym Progress with IME, and Settings reminder diagnostics from the preceding September 28 receipts. These support retaining the current palette, identity/action rows, card hierarchy, spacing and explicit status language. They are historical originals, not new captures for this audit. Fresh changed-form originals and any discovered geometry repairs are recorded with the final exact native receipts.

### Integration and compatibility

Room schema 46, epoch 6, portable-backup format 26, package identity and release version remain unchanged. Gym CSV adds explicit context columns while retaining the existing leading columns. The implementation introduces no dependency or persistence migration. The owner phone remains on its preceding private 0.3.81/code 87 installation.

## Completed implementation and acceptance

All 20 accepted groups have concrete implementation and selected passing evidence. The 177 workflow dispositions comprise 24 shared, 84 productivity and 69 Gym/Settings rows; they are review coverage, not executed test counts.

| Group | Delivered behavior | Focused acceptance |
| --- | --- | --- |
| S1 | Empty Custom units can be created; unresolved selection is truthful. A saved creation receipt waits for the actual unit definition before selecting or converting a draft, including after recreation. | Shared empty-unit creation/receipt-before-projection native method. |
| S2 | Area/Tag failures remain owned by the open child; busy forms cannot accept conflicting edits; interrupted Area creation restores a retryable draft and ignores disposed callbacks. | Shared color-save, Area-interruption and Tag-rename failure native methods. |
| S3 | Taxonomy forms scroll inside fixed-action dialogs; long identity/error content stays reachable. Discard uses the shared destructive action. The shared color field uses a dropdown arrow so its value remains readable at 200%. | Area Create/Move and Habit dirty-dialog native checks at actual 200%, short height; inspected originals. |
| S4 | Task Time, optional time and reminders share one scrollable native clock/keyboard dialog with preserved time, 12/24-hour policy and duplicate guidance. | Clock-mode/restoration/duplicate native method and keyboard original. |
| P-01 | Compatible Habit unit edits convert target, quick-action and ending configuration together; incomplete drafts and unsupported additive affine conversions stay explicit. | Numeric conversion JVM method; real Habit save/reopen native journey retaining original history. |
| P-02 | All active Task schedule types expose overdue deadlines through the same projection. | Exact overdue projection JVM method. |
| P-03 | Task/Habit/Goal/Track copies respect name bounds and Unicode while keeping independent structure and excluding source history. | Copy-name JVM method plus all-four-owner Room duplicate native method. |
| P-04 | Habit Value/History/Pause retain dirty drafts through Back, confirmation and recreation. | Actual 200% three-dialog native journey. |
| P-05 | Goal Latest, current value, trend and History agree on effective local date before recording time. | Goal ordering JVM method and full-app backfill/recreation native journey. |
| P-06 | Review respects saved evidence windows, finite values and dated weighted milestones. | Exact Goal Review and milestone JVM methods. |
| P-07 | Flexible Habit completion rates include the first partial week/month. | Exact partial-period JVM method. |
| P-08 | Earned Habit streaks retain history beyond one year and older neutral days. | Exact long-streak JVM method. |
| P-09 | Habit/Track decimal totals and means share the existing Goal decimal accumulator, preserving small values and finite means while retaining prior nonfinite propagation. | Exact Habit/Track decimal JVM methods, repeated only after their final integration changes. |
| PGS-1 | Numbered-machine tracked records use one context-aware capability owner at every consumer. | Eligibility JVM method and choose/save/reopen/source native journey. |
| PGS-2 | Tracked-record selection/order survives recreation and accidental dismissal. | Same native journey checks unsaved and committed drafts. |
| PGS-3 | Archived identities retain historical volume, filters, sharing and meaningful Progress states; weekly count language is grammatical. | Archived-share JVM method, archived-volume/filter native journeys and final copy original. |
| PGS-4 | Historical entered mass converts from its saved unit before adding Per Hand/other qualifiers. | Exact formatter JVM method and saved-history native journey. |
| PGS-5 | Refresh Notification Status actually retries Task/Habit/Goal scheduling with independent failure reporting and truthful terminal feedback. | Scheduler partial-failure JVM method and native Settings refresh. |
| PGS-6 | Older Training Max decisions are reachable through Show All and retain current history filters. | Native 21st-decision/filter/recreation journey. |
| PGS-7 | Gym CSV preserves saved tracking type and appends explicit workout/archive/placement/completion context. | Real Room CSV export native method. |

### Design and architecture result

The established navigation, palette, cards and inspectors remain coherent. The demonstrated visual problems called for shared form repair: scrolling content with fixed actions, clear destructive ordering, clock-mode choice, readable unit/color values and request-owned errors. Thirteen selected changed-state originals were inspected, including short layouts at actual 200% text, the keyboard, dark Goal history, archived Gym data and Settings recovery. The evidence gallery identifies final versus superseded captures.

Reuse now follows actual shared behavior: one time picker; existing form, saving and discard owners; shared bounded copy naming; compatible numeric draft conversion; exact decimal aggregation; contextual Gym record eligibility; and the existing Settings request boundary for retries. Domain-specific editors, historical facts and conversion policies remain at their owners. No additional universal card, editor framework or module split was justified by this investigation.

### Verification and remaining boundaries

All 29 distinct selected methods have passing evidence: 14 JVM methods across 17 passing executions, and 15 Android methods across 21 executions (18 passes and three initial fixture failures, each corrected and subsequently passed). No test was skipped or reused. Native commands completed in 20.328–46.111 seconds. One cold JVM command timed out at 55.002 seconds during compilation and remains incomplete; separate bounded compilation and selected execution then succeeded. One native fixture compile error was corrected before execution. Exact commands, timings, raw failures, XML and method ledgers are retained in the linked receipt.

No full batch, affected-readiness batch, candidate, release packaging or owner-phone operation was run. Source review and these selected emulator checks do not certify every device, locale, accessibility service, external integration, large imported history or Android/OEM notification policy. Subjective appearance remains open to normal owner feedback. Every accepted implementation and requested focused acceptance item in this plan is complete; historical paused audits retain their original status.
