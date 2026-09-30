# Native before/after originals - 2026-09-30

These original PNG/XML pairs came from explicitly assigned disposable API34 emulators. Agents inspected original pixels; hierarchies supplement the screenshots and do not certify spoken accessibility.

| Surface | Before | After | Visible change |
| --- | --- | --- | --- |
| Empty Habits, same integration device/configuration | [PNG](before/habits.png), [XML](before/habits.xml) | [PNG](after/habits.png), [XML](after/habits.xml) | Existing shared chrome and bounds retained; direct Create Habit action matches Goals' template/direct-create hierarchy |
| Legacy Reduce Goal, target75/current90 | [PNG](before/before-reduce-90.png), [XML](before/before-reduce-90.xml) | [PNG](after/after-legacy-reduce-90.png), [XML](after/after-legacy-reduce-90.xml) | Removes false Target reached/120%; retains measurements and honest value without inventing a missing baseline |
| Valid authored Reduce baseline100/target75/current70 | Missing/incompatible baseline before above | [PNG](after/after-authored-reduce-70.png), [XML](after/after-authored-reduce-70.xml) | Correct120% and Target Reached suggestion; manual closure remains a distinct choice |
| Typed pending subtask Cancel | [PNG](before/pending-subtask-cancelled.png), [XML](before/pending-subtask-cancelled.xml) | [PNG](after/pending-subtask-discard.png), [XML](after/pending-subtask-discard.xml) | Cancel now prompts Keep Editing/Discard rather than silently losing authored input |

The before app is the dated pre-resume0.3.90-debug/code96 artifact; after is the immutable resumed0.3.91-debug/code97 pair, not a release. The manual Task gesture also changed Priority during touch scrolling, so it establishes retained-draft behavior; the isolated native pending-subtask test is required to prove that pending text alone owns dirty state.

The full local evidence and first-failure logs remain under ignored `build/qa-overhaul/2026-09-30/` and `build/overhaul-evidence/routines/`. This selected repository gallery preserves reviewable originals without committing hundreds of test captures or APK binaries. See [acceptance and limitations](../../../docs/quality/UX_OVERHAUL_2026-09-30.md).

## Final verified surfaces

Same emulator-5554, normal system font scale1.0, actual visible native tab taps: [Home](verified/tabs/home.png), [Tasks](verified/tabs/tasks.png), [Habits](verified/tabs/habits.png), [Goals](verified/tabs/goals.png), [Tracks](verified/tabs/tracks.png), [Gym](verified/tabs/gym.png). Each has matching XML in that folder. Personally inspected roots confirm common positions and component styling; Gym's unscoped identity remains deliberate.

[Completed Task selected-section return](verified/completed-task-edit-return.png) follows actual Edit Series and Cancel; [atomic empty-workout creation](verified/empty-workout-atomic-create.png) belongs to the original session. [Actual200% editor with keyboard](verified/habit-editor-200-keyboard.png) was captured after native input-method state reported shown; header/exit remain visible. The separate before-IME original is labelled accordingly. Matching XML accompanies each PNG.

[Goal/Habit final selected originals](after/goals-habits/) include exact endpoint percentages, finite flexible2weeks, Home1of1, next logical-date empty state and open weekly upper-bound Pending/Ready evidence. Composite method counts and original failures are in the [Goal/Habit receipt](../../../docs/quality/GOALS_HABITS_ACCEPTANCE_2026-09-30.md) and [Routine/Gym/Task receipt](../../../docs/quality/ROUTINE_GYM_TASK_ACCEPTANCE_2026-09-30.md).

Repository XML copies use LF line endings; device metadata/content and original PNG pixels are preserved. Raw device XML remains in the ignored evidence directories.

## Follow-up acceptance originals

[Actual200% Settings category return](verified/parent-return/settings-parent-return.png) follows About Whip, actual Activity recreation and Back; [Area parent return](verified/parent-return/area-parent-return.png) follows Create/Cancel, details and Compose saved-state restoration. Native bounds retain the same parent offset, and before/after PNG bytes are identical in each round trip. Matching XML and raw/copy SHA256 provenance accompany them; both returned originals personally inspected. These are restoration acceptance pairs, not invented old-build visual redesign screenshots.

[Accepted persisted Gym journeys](verified/gym-follow-up/README.md) preserve exact source/phase/unit evidence and separate first failed attempts from accepted replays.

[Review and Track follow-up originals](verified/review-track-follow-up/README.md) preserve the visible inspector-over-editor defect, corrected foreground drafts and accepted saved-context return, alongside truthful Review count/chart evidence and separate failed attempts.

[Fresh grouped Calendar/Task regression originals](verified/grouped-regression/README.md) accompany the exact eight-method repair replay; acceptance frames are distinct from visual redesign before/after evidence.

[Card/routine repair originals](verified/card-routine-repair/README.md): one collapsed checklist progress count with direct rows retained, native200% Reset sizing, phased session persistence and actual details journeys. Focused23/23 passed; full campaign remains separate.


[Canonical creation baseline/repaired native proof](verified/canonical-creation/README.md) and [full canonical page catalog10/10](verified/catalog-canonical/README.md) preserve the bounded follow-up repairs and current cross-tab geometry evidence. The clear-review capture limit is explicitly recorded.
