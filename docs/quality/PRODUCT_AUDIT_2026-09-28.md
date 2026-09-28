# Full Whip product audit and implementation plan — 2026-09-28

Status: **In progress**. Owner: FB-20260928-005. Baseline: clean pushed `d737209a` on `main`. Parent plus two GPT-6 Astra/max agents.

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
| S3 | Area/Tag create/rename/merge bodies use non-scrolling columns, inconsistent spacing, long titles and dynamic errors; content can outgrow the available body while fixed actions remain below it. | Use the existing shared body and explicit scroll ownership; retain full errors, identity and safe-before-destructive action order. Inspect selected short/large-text form rendering and retry reachability. |
| S4 | Task Time, optional clock settings and reminder-time creation duplicate native clock dialogs. All lack keyboard-mode choice; their native clock bodies are not scrollable. | Consolidate through the existing `ClockPickerDialog`, with consistent Set/Cancel/Clear and duplicate guidance, a keyboard/clock switch and scrollable body. Preserve 12/24-hour settings and authored minutes; exact switch/draft/duplicate tests and rendered inspection. |

Shared investigation also covers welcome/setup, first-use Home recovery, responsive shell/Area scope, global-search query/partial-state/routing, inspector hierarchy, date/calendar presentation, controls/status/errors, item builders, emoji/color, theme, back/IME handling and deletion boundaries. Their detailed dispositions and any additional supported findings follow the integrated review.

Productivity findings accepted for domain implementation: compatible Habit unit conversion; recurring Task overdue context; bounded duplicate names across Task/Habit/Goal/Track; unsaved-draft protection for Habit logging/history/pause. The agent report owns exact evidence and all reviewed workflow dispositions. Gym/Settings findings are being finalized before that domain's implementation.

Historical audits are reference material; this task has the owner's new bounded verification contract.
