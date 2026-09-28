# Focus timer UX and notification plan

Date: 2026-09-28. Status: Implemented and Verified within the accepted Focus scope. Feedback: FB-20260928-004. Baseline: clean `e799ab02` on `main`. Execution: one Sol/max parent and three explicitly authorized Sol/medium agents.

Owner clarification: This is a Focus timer overhaul and upgrade pass. Accepted work covers the complete duration, start, active, replacement, stop, completion, permissions, recreation and recovery journey. Findings: FND-20260928-005/006; decision: DEC-20260928-002.

## Confirmed baseline behavior

- Home Task actions and Tasks share `TaskActionsDialog`. Its More tab immediately starts 15/25/45/60-minute timers; there is no custom duration. `SettingsViewModel` defaults to 25 and silently clamps minutes to 1–240.
- The shared start callback closes the inspector and requests permission, without saved-start feedback. Only `TaskAreaContent` renders an active countdown; `HomeContent` does not. Its Task title lookup uses scoped tasks, so changing Area can hide the timer identity. The older audit's active notice claim applies to Tasks alone.
- `FocusTimerScheduler` enqueues exact alarms and a durable WorkManager fallback. Only `FocusTimerWorker` publishes a notification, after completion. Start/stop settings writes happen outside the scheduler boundary; asynchronously queued unconditional cancellation can remove a newer timer's schedule.
- Gym custom rest duration is reached through “Adjust”; Task reminder custom offset is an always-visible “Custom Minutes Before” input without a numeric keyboard. Their units/ranges are intentionally different from Focus. Habit timer review edits measured elapsed time, and time-of-day pickers choose clock time; those meanings must remain intact.
- Integration source review corrected the initial clock-format assumption: Task's local picker forces 12-hour mode, and its time label/new Focus finish-time formatter ignore Android's chosen 24-hour format. Reuse the established context formatter/device choice. A tick-derived foreground completion message can race the worker's clear; use a current-process, non-replaying committed completion event.

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

## Integration dispositions

- The shared screen exceeded JaCoCo's instrumented method-size limit when Focus request/state handling was added inline. Extracting that feature owner into `rememberFocusTimerUi` preserves the same request ownership, saved-state and lifecycle behavior; the bounded instrumentation diagnostic now passes without excluding coverage.
- Current-epoch application startup synchronously resolves canonical initialization before Android dispatches notification receivers. No extra wait was added to Stop. Recovery remains gated, and immutable Open/Stop actions retain generation/Task/deadline checks. The external final protocol now executes cold Stop with PID absence, native receiver startup, confirmed clear and no Activity launch.
- The timer deliberately does not complete its Task or add a second history/pause model. Foreground completion comes from a matching confirmed worker clear, rather than a UI tick or a replayed event.
- Native completion capture exposed clipped Home navigation with no accessible name. Keep the all-section reproduction and strict hierarchy check; name status cards and interactive section headings from their complete displayed meaning. Custom Time also needs to appear in Gym's contextual spoken label.
- Android 14 notification enabled status reads runtime permission, so package/UID app-op overrides are invalid permission-denial fixtures. Use real blocked-channel journeys without killing instrumentation and separately verify real runtime denial/recovery through an external device driver. Busy overlays retain the established bounded platform-layout wait and visible-state assertion.
- The accepted 45-method normal campaign exposed a short-screen defect at 1080×1600 and actual 200%: only 131.81dp of Home list. FND-20260928-008 recorded it before the compact metadata/copy repair. Final short acceptance passes 4/4 and keeps all facts, real scaling, 48dp controls, 163.81dp of list and actual scrolling. A final 7/7 Card/Home replay follows all changes.

## Final workflow dispositions

All five accepted workstreams are implemented. The table records the reviewed Focus journey, including supported keeps and platform limits; it does not expand the separate paused whole-product audit.

Evidence families are the accepted FocusTimeSelectionUiTest (Duration), FocusHomeJourneyE2ETest (Home), FocusTimerCardUiTest (Card), FocusTimerMutationIntegrationTest (Mutation), TimerAlarmIntegrationTest (Native), focused JVM policy tests and named neighbors. [Exact method/configuration ledgers and originals](../../artifacts/focus-timer/2026-09-28/README.md) retain the actual proof for each family.

| ID | Workflow | Final disposition and evidence |
| --- | --- | --- |
| F01 | Home and Tasks entry | Shared inspector and saved start path; both destinations remain reachable. Duration/Home and WhipNavigation neighbors pass. |
| F02 | Preset selection | Direct start at exactly 15/30/45/60; all four deadlines verified. Duration. |
| F03 | Default invocation | Default 30 minutes at the confirmed mutation owner. Mutation. |
| F04 | Custom duration | Custom Time opens explicit minutes, 1–240; exact 37-minute example commits without rounding/clamping. Duration/Mutation. |
| F05 | Invalid custom draft | Blank, zero, negative, decimal, text and over-range input remain visible with local errors and numeric keyboard. Duration/Native validation. |
| F06 | Custom cancel | No timer starts on cancel. Duration. |
| F07 | Draft recreation | Raw custom draft survives Activity recreation. Duration. |
| F08 | Busy start | Editing/dismiss/Back and duplicate submissions stay blocked until the owned result. Duration. |
| F09 | Failed start | No false success; preserve draft and explicit correction/retry. Duration/Mutation. |
| F10 | Active replacement | Name current/new Task and duration, require explicit replacement consent and expected pair. Duration/Native. |
| F11 | Keep current session | Keep/cancel preserves existing deadline and returns to the authored custom draft. Duration. |
| F12 | Replacement races | Old Stop, generation mismatch and late old schedules cannot alter replacement state/work. Native. |
| F13 | Home saved acknowledgement | Focus started confirmation follows the persisted receipt; no Tasks visit needed. Home. |
| F14 | Scrolled Home | Active card is above the LazyColumn and stays visible after a lower Task starts or list scrolls. Home/short replay. |
| F15 | Area scope changes | Resolve focused Task from the unscoped list; title survives temporary/current Area changes. Home. |
| F16 | Open focused Task | In-app Open retains Home destination/scope and shows the exact Task inspector. Home. |
| F17 | Stop input/failure | Disable duplicate actions, show saving/error state and retain usable recovery on failure. Card. |
| F18 | Confirmed Stop | Only matching owner consumes the result; stale Stop acknowledges no change, current Stop clears then acknowledges. Mutation/Native/external cleanup. |
| F19 | Remaining-time presentation | Ceil positive seconds, support hours, clamp at zero; no negative display or minute misstatement. JVM/Card. |
| F20 | Due interval | Show completion state while waiting for confirmed delivery; do not infer Task completion or success from a tick. Card. |
| F21 | Foreground completion | Once-only non-replaying event after matching durable worker clear; Task stays open. Native/Home. |
| F22 | Running notification | Silent ongoing native countdown with exact deadline, timeout, Task identity and visible Open/Stop. Native drawer ticks and metadata pass. |
| F23 | Notification Stop | Private immutable receiver checks generation/Task/deadline. Actual visible drawer Stop and external cold-process Stop pass. |
| F24 | Notification Open | Direct Activity intent with current generation/active-pair validation; completion Open uses the confirmed generation. Metadata/source guards are reviewed, with direct external Open follow-through recorded in platform evidence. |
| F25 | Completed notification | Replaces ID 40001 with the completion alert, without ongoing/chronometer/Stop or silent running group. Native delivery. |
| F26 | Fresh runtime denial | Android prompt Don’t allow leaves durable timing and Home warning/Alerts usable; fresh denied publication has no native record. External protocol. |
| F27 | Permission recovery | Home Alerts opens actual Android app notification settings; grant and return clears warning and restores same Task/deadline countdown. External protocol. |
| F28 | Channel recovery | Blocked channel is explained; actual Android channel enable/return refreshes card and notification. Home/Native. |
| F29 | Presentation refresh | Refresh republishes notification without replacing WorkManager UUID or deadline; operation boundary avoids stale publication. Native. |
| F30 | Missing/archived Task | Reject stale ownership before confirming a pair. Native validation and recovery gate. |
| F31 | Permanent deletion | Compare identity within the confirmed write; cancel only the pair actually cleared. NotificationActionIntegrity neighbor and source ownership review. |
| F32 | Queued cancel/rebuild | Cancellation queued for an old clear cannot remove newer pair/work. Future/due recovery reuses exact/fallback reconciliation. Native. |
| F33 | Ancient timer and exact access | Old expired Focus does not resurrect alerts; exact-access return rebuilds persisted alarms. Native Focus/Rest neighbors. |
| F34 | Data recovery boundary | Blocked canonical recovery denies native action/delivery/data access. RecoveryBoundary neighbor; no duplicate cold-start wait. |
| F35 | Device clock format | Task clock selection/labels and Focus finish time respect Android 12/24-hour format and configured zone. Duration clock test and JVM formatter policy. |
| F36 | Shared Custom Time vocabulary | Equivalent Focus, reminder and Gym affordances use Custom Time visibly and in contextual spoken labels; minutes/seconds/offsets keep explicit ranges. Duration, Gym catalog and Rest journey. |
| F37 | Short actual 200% layout | Full clock/finish/status, 48dp actions and at least 160dp Home list, real scroll and complete later Task. Final short 4/4 and normal Card/Home 7/7. |
| F38 | Clipped Home accessibility | Name status cards and interactive section headings from complete displayed meaning, retaining heading/role/action. Original all-section completion trigger passes strict capture. |
| F39 | Native exported rectangles | Compose uncovered-node pruning explains shrunken exported bounds after scroll; physical layout/targets and existing LazyColumn clip remain correct. Investigated keep; no speculative clipping change. |
| F40 | User/OS delivery limits | Keep notification/exact access denial, force-stop, OEM deferral and explicit dismissal truthful. No service, pause/history model or unconditional delivery guarantee. |

## Final acceptance and delivery

[Current final readiness](../../artifacts/focus-timer/2026-09-28/readiness-current.log) freshly passes 412 JVM methods in 46 classes plus compilation, JaCoCo instrumentation, lint, debug assembly and static checks. The 300-second final bound accommodates the measured 2m54s build/lint gate; earlier 55-second timeouts remain incomplete historical attempts.

Accepted native evidence is 45 distinct methods across 56 executions (normal 45, constrained 4, final Card/Home 7), zero failures/skips/reuse. All four new JVM and 28 new Android methods are included. The external driver separately verifies true runtime permission denial/grant, unchanged deadlines, actual 200% denied Home and both native controls from cold processes: Stop clears without opening UI, and Open launches the exact Task inspector. Exact receipts distinguish source/configuration and failed attempts from acceptance.

Room 46, epoch 6, backup 26, dependencies, package/signing and release version remain unchanged. Implementation and verification are delivered through the normal source commit/push. The owner phone remains on the preceding private 0.3.81/code 87 update; no new phone install or Play candidate/publication belongs to this task. Subjective appearance and actual TalkBack speech remain outside the automated evidence.
