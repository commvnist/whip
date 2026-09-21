# Whip whole-product audit continuation — 2026-09-20

Status: **In progress** under FB-20260920-001. This is a new owner-authorized continuation of the [September 8 whole-product audit](ASTRA_PRODUCT_AUDIT_2026-09-08.md). The [September 10 closeout](ASTRA_GOAL_CLOSEOUT_2026-09-10.md) remains historically closed; its 534-row [surface matrix](astra-surface-review-2026-09-08.tsv) is frozen rather than silently rewritten.

## Current-source baseline

- Opening `main` and `origin/main`: `4b0ade52`; clean worktree before the new feedback record. Installed private release is 0.3.72/code 78. Room schema 46, data epoch 6 and backup format 26 are unchanged. No owner-phone operation or store release is part of this continuation.
- Opening UI catalog lint passed with 541 required captures, zero pending selectors or platform exceptions. Seven newer IDs belonged to later 5/3/1 supplemental setup/edit/workout states. The owner subsequently retired that feature: the worktree catalog now has 520 required captures (23 retired feature states removed and two ordinary phased-Routine states added), with zero pending selectors or platform exceptions. The historical matrix remains frozen.
- Frozen audit review status: 367 Verified, 125 Investigating, 42 In progress. The 167 unfinished rows are review/work debt, **not** 167 confirmed product defects. By family, unfinished: shared 20, Tasks 26, Habits 16, Goals 14, Tracks 23, Gym 48, Settings 18, Organization 2. Earlier Verified rows retain their dated evidence but do not replace a fresh final-source campaign.
- Current-source complete JVM development run: `scripts/qa-targeted --all-jvm` passed on September 20 after the owner-directed removal, before the final duplication/copy refinement. A fresh, exact two-emulator 541-state capture was cancelled after about 2 of 20 batches when the owner prioritized removal of 5/3/1; it is **not** an acceptance result. The changed 520-state catalog still needs complete exact recapture and final audit reconciliation. Its Gym family has a current 35-method/86-state capture and three later refined source screenshots.

## Completion contract and fast lane

Work from high-risk cause/effect journeys into UI families, using source and existing scoped evidence to avoid rerunning settled investigations. A new finding needs a current reproduction or source-backed failure path, a recorded user consequence, and a focused regression. During development use the narrow `scripts/qa-targeted` / `scripts/check` route, then affected emulator journeys and family recapture. Commit/push each independently verified chunk. Preserve the original single-agent and at-most-two-disposable-emulator boundary.

The work is complete only after:

1. Every unfinished frozen row and later-added state has a substantive current-source disposition linked to source/journey and visual/semantic evidence, or a specific retired-feature disposition where the state no longer exists. Newly discovered consequential states are inventoried too.
2. Complete cross-feature journeys cover backup/recovery and provider failure, large Track histories and specialized analytics, Gym/Routine advanced authoring/execution and legacy-program conversion, Task/Habit/Goal interruption and history, widget/external capture, reminders/timers, Settings and global navigation. No confirmed P0/P1 remains, and material P2 decisions are implemented or explicitly justified.
3. Final source passes a **fresh**, complete JVM/Android matrix, static/lint/build gate, full current-catalog exact capture and individual/cross-family visual review, plus proportionate API 26/34/37, compact/wide/fold, keyboard, actual large text, RTL, TalkBack and realistic-data performance checks. Historical scoped passes and test-source counts must not be represented as final execution.
4. Durable feedback/findings/decisions/implementation/verification and the index agree with source and evidence; all verified chunks are normally committed and pushed. A phone install or Play Store publication requires a separate owner request under the original Astra scope.

## First risk triage

- Backup: native encrypted-file delivery, provider failure/revoked access, portable-folder recovery and replacement/reset error injection were explicit closeout gaps. `SettingsScreens` currently holds an encrypted-export passphrase only in `remember` while Android's document picker is open; recreation is a specific failure hypothesis to reproduce before remediation.
- Tracks: specialized filter/analytics combinations, actual reorder/bulk-failure recovery, realistic large-history latency, and residual accessibility/design states remain open despite extensive authoring/history/CSV scoped acceptance.
- Gym/Routines: advanced configurations, exact navigation to later Sets within groups and setup guidance remain open. The retired 5/3/1 states require a recorded removal disposition; the replacement phased-Routine states need final-current-source review.
- Platform: widget/external-capture, reminder/notification/timer and startup recovery journeys need whole-product treatment. The September 15 reminder fix has exact scoped evidence; it does not itself close the full platform matrix.
- New owner priority: FB-20260920-002 / FND-20260920-001 removes the 5/3/1 feature, folds reusable program controls into ordinary Routines, and requires a compatibility disposition for saved routines and active workouts before the paused matrix resumes.

## Evidence log

| Date | Scope | Result and limit |
| --- | --- | --- |
| 2026-09-20 | Catalog/source baseline | `scripts/ui-catalog lint` passed, 541 required, zero exceptions/pending. `scripts/ui-catalog source-snapshot` reports 240 discovered owners. |
| 2026-09-20 | JVM baseline | `scripts/qa-targeted --all-jvm` passed; Android tests were not executed by this command. |
| 2026-09-20 | Full-catalog attempt | Cancelled deliberately after about 2/20 batches due to the owner-directed product change; no capture or visual acceptance is claimed. |
| 2026-09-20 | Feature-retirement catalog | 520 required states, zero pending/exception; source discovery has 238 owners after the wizard and generator are removed. Generic phased-Routine builder and workout selectors replace the retired capture surfaces. An earlier 87-state Gym capture exposed an obsolete, unreachable legacy builder state, which was removed. Its 35-method/86-state replacement passed with zero missing/duplicate pixels or NAF nodes. After a small generic-copy correction, two affected builder tests passed and three exact refined PNG/XML pairs were inspected: outline, phase structure and primary progression. Complete 520-state final audit capture is still outstanding. |
| 2026-09-20 | Feature-retirement regression | Exact final-source `ANDROID_SERIAL=emulator-5554 scripts/qa-targeted gym gymphased --emulator` passes 173/173 Android methods with no failure/skip; `scripts/qa-targeted --all-jvm` passes 656/656. This is affected-family coverage, not the 1069-method complete Android inventory or a release. |
| 2026-09-20 | Feature-retirement readiness | `scripts/check --ready` passes in 2m33s: route/harness fixtures, JVM, Android test compilation, lint and debug packaging. No frozen candidate, phone installation or store publication was made. The complete audit contract above remains open. |

Do not mark this continuation complete merely because the full test inventory runs. The uncovered user journeys and each inventoried UI state require a justified disposition.
