# Whip complete product-experience overhaul — 2026-09-28

Status: **Investigating**. FB-20260928-007. Clean pushed baseline `9014937f`; current private phone release 0.3.82/code 88. This is a new goal adopting the owner's comprehensive prompt, with the explicit change to two additional GPT-6 Astra/high agents.

## Required outcome

Independently audit the current app from discovery through successful use and recovery, identify substantial improvements to experience, visual design/language, clarity, speed and quality, then implement every recommendation. Earlier audits guide navigation to owners but do not prove current optimality. The intended result is a complete implemented improvement plan, not a finding list or arbitrary quota of small fixes.

## Work sequence and ownership

1. Recheck current source, durable decisions and representative running workflows; map all major areas and cross-feature transitions. Distinguish source review, measurement and observed rendering.
2. Publish prioritized findings before production changes, with impact, intended before/after, all affected owners, compatibility and a focused acceptance check. Investigate uncertainty before accepting a remedy.
3. Implement coherent groups, including underlying state/persistence, accessibility and recovery. Coordinate shared owners and reuse actual equivalent behavior.
4. Run only useful exact methods/fixtures under 55 seconds with 3-second kill grace; inspect fresh changed-state originals and repair confirmed followthrough. Do not run full batches, affected-readiness or candidate campaigns. Treat timeouts as incomplete and avoid repeated blind retries.
5. Reconcile each accepted recommendation and each goal requirement against actual implementation and evidence, update memory, and commit/push verified work. Report exact limits without substituting them for completion of accepted work.

| Owner | Scope | Detailed report |
| --- | --- | --- |
| Parent | Onboarding, Home, Tasks, shared navigation/search/organization, cross-feature consistency, shared controls, architecture, integration and performance evidence | This report |
| Astra/high productivity agent | Habits, Goals and Tracks; complete authoring/action/history/analysis/import/export journeys and underlying rules | `EXPERIENCE_OVERHAUL_2026-09-28_PRODUCTIVITY.md` |
| Astra/high Gym/Settings agent | Gym/Library/Routines/workout/history/progress; Settings, backup/recovery, units, notifications/timers/widgets and external boundaries | `EXPERIENCE_OVERHAUL_2026-09-28_GYM_SETTINGS.md` |

The parent serializes builds and emulator work. The physical phone remains on its verified release; this goal uses synthetic disposable emulator data. No schema, backup-format or release-version change is assumed necessary.

## Completion requirements and evidence ownership

| Requirement | Evidence needed before completion |
| --- | --- |
| Full product and connected user journeys | Current-source disposition of every major area, entry/authoring/action/history/correction/archive/delete/recovery, with current running examples. |
| Substantial experience improvement | Concrete before/after for accepted changes to interaction effort, discoverability, hierarchy and clear next actions; no arbitrary small finding limit. |
| Visual language and accessible interaction | Existing/new shared role ownership, fresh changed-state originals and relevant light/dark, keyboard, enlarged-text and compact/larger layout checks. |
| Clarity and trustworthy meaning | Consistent terms, units/dates, state/error/success explanation and domain calculations across affected consumers. |
| Speed and responsiveness | Evidence-based assessment of startup/navigation/rendering/search/calculation/database/large-history hot paths; measured remedies where problems are substantiated. |
| Extensibility and quality | Actual shared reuse, explicit state/domain boundaries, persistence/history protection and tested recovery for materially changed behavior. |
| Complete implementation | Every accepted group mapped to delivered source and passing selected checks; no recommendation silently deferred. |
| Efficient verification | Exact commands/results/timings, fresh versus historical evidence, failed attempts and explicit scope limits. |
| Durable delivery | Coherent commits/pushes, canonical memory, final completed plan and reviewable visual evidence. |

## Accepted implementation groups

The first eight groups below are accepted before production changes. Investigation of remaining workflows continues. A justified keep is an assessment outcome; it is not a postponed recommendation.

| ID / priority | Current problem and intended improvement | Owners / compatibility | Focused acceptance |
| --- | --- | --- | --- |
| EP1 / high | Weighted Goal inspector displays milestones but cannot execute them; the user must leave and expand a collection card. Put the same actionable milestone checklist inside Overview and keep committed progress live. | GoalScreens and existing milestone boundary; closed/archived/stale contexts retain explicit read-only behavior. | Toggle and persist from inspector, keep it open, verify progress/source and protected stale/closed behavior. |
| EP2 / high | A reached finite Goal hides explicit completion in Options. Offer completion at the moment the target is reached while preserving manual choice and ongoing Goal semantics. | Goal inspector and existing completion receipt; no automatic completion or reinterpretation of MaintainRange/trend/elapsed behavior. | Eligible reached versus incomplete/ongoing/closed Goals, confirmed persisted completion and feedback. |
| EP3 / high | Habit Today inspector describes a checklist but does not let the user work through it. Reuse actionable checklist rows inside the inspector. | HabitScreens and existing checklist callback; parent completion remains explicit and skip/pause/off-schedule rules remain authoritative. | Toggle/reopen/recreate and verify actual item state without implicit parent completion. |
| EP4 / high | Track creation starts with an unexplained schema and provides no entry-form preview. Offer editable starter logs plus a preview through the actual entry-field renderer, so users can understand and refine what they are building before saving. | Track editor/field renderer; existing supported field types, no template auto-save or preview-value persistence. | Choose/customize starter, preview actual required/ordered fields, save/reopen; blank creation remains supported. |
| EP5 / high | Finding an old Habit/Goal event requires sequential Show More and scrolling. Add saved local History search over displayed date/value/unit/note and relevant neutral events, result/clear recovery, and lazy Habit history composition. | Existing loaded historical facts; pagination after filtering, no schema/index change or rewritten history. | Oldest match immediately reachable, edit exact source, clear/query recreation, no-match recovery; assess large-history work. |
| EGS1 / high | Returning Gym users see an inert no-workout state rather than useful starting choices. Provide a compact launchpad with meaningful routine choices, exact next-day/static-day behavior, Browse All and the latest completed workout's source. | Existing workout/routine/history commands; preserve first use, empty workout creation and active-session recovery. | Start correct programmed/static day, deduplicated bounded choices, exact history navigation and active-workout guard. |
| EGS2 / medium | The Routine library lacks search despite potentially large named day/exercise structures. Add remembered search, truthful counts and clear/no-match recovery. | RoutineContent; names/notes/day/exercise lookup, focused routing and constrained reorder protection. | Find routine through nested content, clear/recreate, prevent reorder from changing unseen items. |
| EGS3 / high | Data & Privacy places bulky folder administration ahead of backup/restore and exposes every CSV action at once. Lead with truthful protection status and core backup/restore actions; progressively disclose folder administration and spreadsheet exports. | Settings presentation, unchanged backup verification/preview/request owners; errors and busy state remain explicit. | Configured/unconfigured/error/protected states, direct actions, disclosure recovery and constrained rendering. |

Detailed domain reports retain current-source traces, all reviewed workflows and exact fixture selectors. Every accepted row must end with implementation and inspected evidence; additional supported findings will be added before their changes.
