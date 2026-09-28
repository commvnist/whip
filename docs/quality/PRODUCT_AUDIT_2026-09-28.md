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

Accepted findings, implementation dispositions and exact verification will be added here before broad production changes. Historical audits are reference material; this task has the owner's new bounded verification contract.
