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

Current-source investigation is underway. Concrete groups will be recorded here before their production changes. A justified keep is an assessment outcome; it is not a postponed recommendation.
