# Focus timer UX and notification plan

Date: 2026-09-28. Status: In progress. Feedback: FB-20260928-004. Baseline: clean `e799ab02` on `main`. Execution: one Sol/max parent and three explicitly authorized Sol/medium agents.

Owner clarification: This is a Focus timer overhaul and upgrade pass. Accepted work covers the complete duration, start, active, replacement, stop, completion, permissions, recreation and recovery journey. Findings: FND-20260928-005/006; decision: DEC-20260928-002.

## Confirmed current behavior

- Home Task actions and Tasks share `TaskActionsDialog`. Its More tab immediately starts 15/25/45/60-minute timers; there is no custom duration. `SettingsViewModel` defaults to 25 and silently clamps minutes to 1–240.
- The shared start callback closes the inspector and requests permission, without saved-start feedback. Only `TaskAreaContent` renders an active countdown; `HomeContent` does not. Its Task title lookup uses scoped tasks, so changing Area can hide the timer identity. The older audit's active notice claim applies to Tasks alone.
- `FocusTimerScheduler` enqueues exact alarms and a durable WorkManager fallback. Only `FocusTimerWorker` publishes a notification, after completion. Start/stop settings writes happen outside the scheduler boundary; asynchronously queued unconditional cancellation can remove a newer timer's schedule.
- Gym custom rest duration is reached through “Adjust”; Task reminder custom offset is an always-visible “Custom Minutes Before” input without a numeric keyboard. Their units/ranges are intentionally different from Focus. Habit timer review edits measured elapsed time, and time-of-day pickers choose clock time; those meanings must remain intact.

## Accepted implementation

| Workstream | Implementation and completion criteria | Owner |
| --- | --- | --- |
| Duration and custom entry | Direct-start 15/30/45/60 minute buttons, Custom Time button, saveable custom Focus dialog with explicit minutes and 1–240 validation. No silent clamping. Match Custom Time affordance on Gym rest and Task reminder custom offset; numeric keyboards, explicit units/ranges and adaptive layouts. Preserve measured elapsed decimals and native clock pickers. | Duration agent; parent validates shared meaning |
| Native running timer | Publish an ongoing silent native countdown for a persisted current Focus timer with Task identity, Open and Stop. Completion replaces running presentation under the same notification identity and retains its completion alert. Reuse exact alarms/fallback and rebuild; handle permission/channel denial honestly. No ticker service/dependency. | Notification agent |
| Lifecycle integrity | Serialize confirmed start/stop writes with scheduling/delivery; reject missing/archived Task and invalid durations. Recheck identity after suspending operations. Match generation/Task/deadline for notification Stop; old actions and queued cancellation cannot alter a replacement. Keep deletion/recovery/ancient-deadline behavior. | Notification agent; parent ViewModel integration |
| Home and Tasks feedback | Visible shared Focus card above the Home list, so it appears even when the list is scrolled. Immediate request-owned saved-start feedback; failure does not claim success or discard a custom draft. Show Task title, countdown, finish time and Open/Stop; resolve identity from unscoped tasks. Reuse on Tasks. Refresh system availability on resume, and explain missing drawer presentation while in-app timing remains usable. | Home agent; parent ViewModel integration |
| Full Focus interaction pass | Ask before replacing an active timer; preserve the old session if replacement is canceled. Acknowledge confirmed stop. Keep due-state presentation truthful rather than showing a negative countdown; completion does not automatically complete the Task. Review archived/completed/deleted Task ownership, permission-return publication, process/recreation, clock changes and custom-draft failure recovery. Fix reproduced issues and retain explicit dispositions/tests for supported keeps. | Parent and three agents |

## Verification contract

Batch implementation first. Use a bounded exact JVM check only if it guides an edit, then one affected `scripts/check --ready` batch. Run selected disposable-emulator journeys for presets/custom valid/invalid/cancel/recreation, Home start and pinned feedback, Area scope, running chronometer metadata and actual drawer rendering, Stop/stale Stop, completion replacement, denied access and rebuild/deletion/recovery neighbors. Inspect normal and short 200% layouts with explicit accessible actions. Freeze production/tests/APKs during each native campaign. Record exact execution counts separately from source inventory, preserve failed attempts and honest platform limits, then close memory and push coherent changes. No owner-phone instrumentation or reset.

Android controls native countdown rendering and delivery. Explicit force-stop, denied notification/exact-alarm access, OEM deferral and user dismissal remain platform/user boundaries. Verification must not present these as unconditional delivery guarantees.
