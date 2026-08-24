# Automation UX audit and implemented model

Date: 2026-08-24

## Decision

Whip has one automation system with two user-facing outcomes:

1. **Goal Automations** turn a real source event into auditable Goal progress.
2. **Next-Action Automations** make a follow-up available. They create an in-app prompt unless the user explicitly chooses the narrowly supported automatic Habit Check Off action.

`Metric`, `Link`, `Trigger`, and `Contribution` remain valid implementation terms, but they are not automation categories a user must learn. In particular, the Goal editor must never present the internal `Metric` source type. It presents **Another Goal** and **Health Connect Data** as separate, recognizable sources.

## Problems verified before this pass

- Goal progress was variously called a Progress Source, Link, Connection, or Automation.
- The raw Metric source mixed Goal-owned measurements, Habit-owned measurements, and Health Connect data in one selector.
- Tracks separated “Create Automation” from “Set Goal,” although both are automations from the user's perspective.
- Entry count existed in the domain engine, but the only obvious Track path created a new Goal. Connecting the Track to an existing Goal required leaving Tracks and finding Goal Connections.
- A normal “Watch 50 Movies” Reach Goal commonly used Latest Value. The repository correctly rejected Count Entries for that calculation, but the UI turned that safety rule into a dead end instead of explaining and resolving it.
- `Count Entries` and `Count Matching Entries` were redundant choices because conditions already define which Entries are eligible.
- Advanced transformation controls had the same weight as the source and effect, obscuring the basic cause-and-effect sentence.
- Track mutations relied on a background observer to reconcile Goal progress. The result was eventually correct, but operation-complete feedback could precede the visible effect.

## Implemented interaction hierarchy

Every editor now follows this order:

1. **When / what adds progress** — choose the recognizable event and source item.
2. **How it changes the result** — count Entries, add a Field, use a latest value, prompt an item, or explicitly Check Off a Habit.
3. **Which history applies** — new events only or an explained backfill range.
4. **Conditions and preview** — show which records are eligible and what will happen.
5. **Advanced options** — custom name, context-only links, multiplier, and offset are disclosed in place.

The Track Automation page now gives Goal progress its own card with two direct paths:

- **Create a Goal From Entries**
- **Connect Entries to an Existing Goal**

The default Track measure is **Count Entries**, explained as “Each eligible Entry adds 1 to Goal progress.” Conditions narrow eligibility without introducing a second count concept.

## Goal Automation capability matrix

| User source | Supported result |
| --- | --- |
| Habit Check-In | use its logged value, or add 1 when the Habit succeeds |
| Task Completion | add one completion |
| Subtask Completion | add one for any or one selected Subtask |
| Track Entry | count eligible Entries; total, average, latest, lowest, or highest compatible Number/Scale Field values; or add a fixed amount per Entry |
| Completed Workout | count workouts, use duration, or use volume |
| Exercise Result | estimated 1RM, maximum weight, distance, repetitions, duration, or volume |
| Another Goal | use that Goal's measured progress while rejecting dependency cycles |
| Health Connect Data | use imported provider data and reconcile provider edits/deletions |

Track measures are constrained by the Goal's semantic type. When a valid choice needs a different calculation—for example Count Entries needs a Total instead of Latest Value—Whip explains the consequence before save and changes the Goal calculation as part of creating the Automation. A common count Goal therefore offers Count Entries first even if it began with Latest Value.

Elapsed-time and weighted-milestone Goals remain intentionally separate: elapsed time advances from its start instant, while weighted milestones use explicit milestone completion rather than numeric Track aggregation.

## Next-Action Automation capability matrix

| Cause | Available effect |
| --- | --- |
| Task or Subtask result | prompt a Task, Habit, or structured Track Entry |
| Habit recorded/completed/failed/skipped result | prompt a Task, Habit, or structured Track Entry |
| Completed Workout | prompt a Task or Habit |
| Matching Track Entry | prompt a Task or Habit; explicitly and idempotently Check Off a compatible Habit |

Prompts may have a delay, quiet hours, and optional Android notification. The prompt remains in Whip without notification permission. Track Entry prompts can prefill compatible Fields but never save a structured Entry until the user confirms it.

Whip deliberately does not offer unrestricted silent completion of Tasks or arbitrary automation chains. Those behaviors create hard-to-debug feedback loops and can falsify the user's record. Cycle detection, endpoint validation, stale-reference explanations, and idempotent source-event identities remain enforced below the UI.

## Cause/effect guarantees

- Each eligible source event has one stable contribution or prompt identity.
- Rebuilding is idempotent: the same event cannot increment progress twice.
- Editing or deleting a Track Entry recalculates/removes its derived Goal contribution.
- Restoring an Entry restores exactly one contribution.
- Disabling a Goal Automation removes its generated measurements without deleting the source.
- Overrides and exclusions affect the derived contribution, not the source record.
- Track add, edit, import, delete, restore, and prompt fulfillment now reconcile Track automations before reporting operation success.
- Historical data is included only after an explicit history choice and preview; new-only setup leaves earlier Entries unchanged.

## Verification added and run

- `AutomationUxPolicyTest` protects the user-facing taxonomy, hides internal Goal/Habit metrics from Health data, and verifies generalized Track measures and count-Goal defaults.
- `TrackRepositoryTest.movieEntryCountDrivesAReachGoalAndReconcilesEveryCauseAndEffect` proves three movie Entries produce three `+1` contributions, deletion produces two, restoration produces three, and repeated rebuilds do not duplicate progress.
- `AutomationConfigurationE2ETest` uses the real UI to connect **Movies Watched** to **Watch 50 Movies**, verifies the Count Entries explanation and Latest-to-Total consequence disclosure, then proves two Entries create exactly two Goal measurements.
- The existing `LinkRepositoryTest` cause/effect matrix and the complete `TrackRepositoryTest` automation suite were rerun on a disposable API 34 emulator: 24 focused instrumentation tests passed.

This model is the implementation contract for future automation work. New sources or effects must fit one of the two user outcomes, provide a plain-language cause/effect preview, define history semantics, preserve stable event identity, and add both policy and persisted cause/effect coverage.
