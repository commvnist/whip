# Workspace capture and context consistency — 2026-09-29

Status: Implemented and Verified. All five accepted groups are complete under IMP-20260929-020 / VER-20260929-017. FB-20260929-016 / FND-20260929-033. Baseline: `4ff000ae`; owner phone starts at verified 0.3.89/code 95.

The owner reports that Add Task moves between Tasks and Today. Fresh current-source review confirms a Tasks-only scope/layout row before capture, while Today omits that row. Conditional query/filter/sort/planning/focus content adds further displacement. The previous overhaul's root capture did not compare this particular pair; its general acceptance is not proof of this anchor.

Three Sol/xHigh reviewers perform a focused review of productivity, shared chrome and Track/Gym siblings. The parent owns Task implementation, integration and verification. This is a bounded consistency correction; the existing design pillars and domain contracts remain authoritative.

| Accepted remedy | Delivered behavior / owner | Acceptance |
|---|---|---|
| Common Add Task anchor | `WhipApp` puts the one keyed capture item first after pinned workspace context, before scope/layout, filters, sort, planning and focus information | Exact Tasks/Today capture rectangles and context gap at normal/actual-200% text; ordinary filtered/restored states |
| Consistent secondary controls | Keep capture visible under text filters as under other ordinary filters; scope/layout controls wrap below it; Today states count before long scope | Narrow/enlarged bounds, query and creation/default/context checks; selection/reorder/history exceptions retained |
| Shared Library child context | `GymDestinationHost` reuses `WhipWorkspaceHeader` rather than a shorter custom Back row; preserve explicit Back identity | Exact Library/child context geometry, normal/enlarged Back and return behavior |
| Consistent visible language | Habit/Goal/Track/Gym counts use the existing quantity formatter; empty Gym Insights removes the duplicated retired Progress introduction | Zero/one/multiple formatter contract, native empty/state/header observations and original review |
| Readable active-filter indicator | Enlarged numeric badges become compact dots inside their fixed action; screen readers receive the exact count once | Actual-200% badge/action bounds and complete spoken count, refreshed restored/empty originals |

Capture stays in the existing scrolling owner, preserving per-route scroll and existing draft/IME behavior. This does not introduce another sticky row or search field. Today keeps its current-date creation default; collections keep their existing Area/date behavior. Explicit selection, reorder, history and other noncapture destinations retain their established actions.

[Focused native checks, complete attempt accounting and fourteen accepted original-image pairs](../../artifacts/workspace-anchors/2026-09-29/README.md) verify the delivered behavior. Final affected readiness passes 430 JVM methods; seven current native selectors pass in 26.434–54.158 seconds. Two earlier combined Task invocations remain incomplete at their caps; shorter split scenarios retain the same strict assertions. Final actual-200% badge originals correct a defect discovered in the first rendered pass. No full suite or phone instrumentation. The corrected source and next signed private update are delivered separately; phone installation currently awaits restored connectivity.
