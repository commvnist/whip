# Whip Habit and Goal action layout — 2026-09-30

Status: final independent design and interaction acceptance complete; focused validation passed. This cohesive source/evidence change is ready for verified delivery.

## Team and scope

One integration lead/design consultant and exactly three workers requested at gpt-6.1-sol/xhigh: design inventory, implementation, independent QA. No further agents. Workers own exclusive API34 emulator serials 5554, 5556 and 5558 respectively; the consultant reviews saved originals. Scope is Habit/Goal activity actions and their logging/inspection siblings, using existing components. Prior source/receipt baseline is 4b4bf66d3c3dc644d2b9c2fe64d31a4b3adae27d.

## Independent initial critique

Actual native screens resolve the wording slip: Duration Habit has Start in the trailing title area and Enter Duration below on the left; Complete Goal belongs to GoalCard and its centered full-width footer, while Log occupies the trailing header. Existing log forms submit on the right. The Duration trigger opens a generic Add Amount form, obscuring the duration-specific action.

The accepted proposal uses a shared leading activity row after stable status/progress and before optional expanded detail. Navigation/edit/disclosure retain their header role. Timer actions explicitly say Start Timer, Stop & Log or Review Timer; manual entry says Enter Duration and submits with Log Duration. Primary logging has stronger treatment than secondary Enter Duration or Complete Goal. Check-in keeps a single readable toggle with checked state. Wrapping begins again at the leading edge, with minimum 48dp interactive targets.

Consultant-required corrections before implementation:

1. Preserve manual Complete Goal for every currently executable goal, even before attainment; retain confirmation and saved outcome.
2. Move new-log submit actions into the same leading theme, rather than only aligning card margins. Preserve correction Save Changes and unsaved/saving/error guards.
3. Preserve numeric add-versus-set semantics; labels and explanatory text must distinguish adding an entry from setting a period total.
4. Give Duration entry a specific title/field/submit label; include timer-review Stop & Log as a sibling logging flow.
5. Scope component opt-ins to the affected Habit/Goal flows; do not redesign unrelated inspectors or dialogs.
6. Preserve manual-entry availability separately from timer recovery: a paused/archived/unscheduled running timer may be stopped, while manual duration logging remains unavailable. Independent diff review found the initially broadened guard; implementation restored the exact original restriction, and QA extended its native regression.
7. Measure numeric quick-action padding using the actual filled control padding, preserving the readable fallback and complete accessible amount.

Reviewed baseline originals: [Duration card](../../artifacts/action-layout/2026-09-30/design/before-duration-card.png), [manual entry](../../artifacts/action-layout/2026-09-30/design/before-duration-entry.png), [manual entry with keyboard](../../artifacts/action-layout/2026-09-30/design/before-duration-entry-keyboard.png).

Additional reviewed originals: [Goal card](../../artifacts/action-layout/2026-09-30/design/before-goal-card.png), [Goal log form](../../artifacts/action-layout/2026-09-30/design/before-goal-log.png), [Goal inspector](../../artifacts/action-layout/2026-09-30/design/before-goal-inspector.png), [Habit check-in cards](../../artifacts/action-layout/2026-09-30/design/before-habit-checkin.png), [check-in inspector](../../artifacts/action-layout/2026-09-30/design/before-checkin-inspector.png). These confirm that the live collection check-off checkbox was also trailing, and both inspectors centered their activity docks. The consultant rejected an earlier misnamed Goal draft capture; only the settled Goal card is accepted as baseline evidence.

## Final visual/interaction critique

Accepted after personal review of original compact before/refined pairs, wide/native 200% layouts, actual keyboard forms, inspectors, draft guard and completion confirmation. The consultant required a compact-token checkbox/label gap correction after the initial final capture; it is present and accepted in the refined originals. Original compact viewport/navigation bounds were restored for the final card/inspector pairs.

The same 0-second Duration, Pending CheckOff and 0% Goal fixtures establish the placement change without changed progress. Actions lead below visible status, with filled primary and outlined secondary treatment. Wide actions and native 200% wrapping preserve this order. Input and Log Duration remain reachable above the actual IME. Inspectors retain selected domain/status and leading activity docks; large Habit tabs expose horizontal scrolling.

Cancel/Keep Editing retains the duration draft; explicit discard returns without logging. QA verified real repository values/counts, rapid submit, Start/recreate/Stop logging, additive amount versus Set Total, Check In/Undo, Goal correction/History restoration and manual completion below target. Timer review correction/Continue/Stop/Discard passed, including native 200% keyboard. Terminal completion deliberately keeps ordinary Cancel/confirm ordering with a filled confirmation and explicit saved outcome; its secondary card trigger is leading. This semantic distinction is accepted.

[Before/after index](../../artifacts/action-layout/2026-09-30/README.md) identifies accepted originals. A misnamed baseline Goal draft and an unproven IME capture are excluded. A QA check-in capture painted Pending during a transition; persistence/undo assertions establish that interaction, and the image is not claimed as checked-state proof.

## Validation and delivery

41 JVM methods in five affected suites passed once, with brand/migration/diff guards. Final Android compilation, lint and debug/test packaging passed; lint has 0 errors, 90 warnings and 15 hints. Seven affected sibling native methods passed on the initial pair; two timer-review methods passed on the refined pair. Independent QA passed the complete refined compact Habit journey, native 200% duration/IME/draft journey, and four direct-action/availability/large-amount checks. Compact Goal logging/correction/manual completion passed on the initial pair.

The wide persistence pair initially stopped before product assertions because its helper matched both navigation and workspace lists. The one test-only selector correction compiled/packaged in 3 seconds; only those two failed methods were rerun. Both passed (Habit 43.165s; Goal 43.064s) with unchanged production bytes. The consultant personally reviewed the resulting wide Goal correction and completed 30% History originals. All emulator settings were restored. Overall: 18 successful native executions / 16 unique methods, across documented APK/test phases; independent QA contributed 9 executions / 7 unique methods. Three original fixture-failed native attempts are retained. The [QA receipt](../../artifacts/action-layout/2026-09-30/qa/README.md) identifies every result.

Original failures are retained: one test-only GoalDraft argument compilation error, the first compact Habit lazy-card lookup, and the wide helper ambiguity. No product failure is attributed to these fixture errors. Corrected compact/build/wide checks passed. Two whole-database-clearing fixtures were compiled but not run: DeepProductivityJourneyTest#numericHabitActionsStayInDetailsAndPreserveFacts and HabitDirectExecutionUiTest#collectionEntriesSaveThroughFocusedDialogsWithoutInspector. New non-destructive journeys cover affected persistence.

[Implementation evidence](../../artifacts/action-layout/2026-09-30/implementation/README.md) retains exact methods, runner/JVM logs, lint and frozen manifests. Results are phased across explicitly identified APK pairs. Refined app SHA256 is 3cc019a94838fa488ab2b07f7a0950e537b7c963de69ab401909367b3cb50024. The integration lead verified all 564 frozen inputs, then verified that only the one QA helper changed afterward and every production input stayed unchanged. Final test-v3 SHA256 is a8dfe2db7ebda2036b9acf0338279cfe85e7f1c2476fca10d6cc6980bea1ec55. Original capture CRLF bytes are preserved; staged whitespace verification recognizes those line endings. No full-suite rerun, public release or phone installation is part of this task.

## Preservation and limits

The preexisting phone AAB receipt edit remains outside this change; opening SHA256 is 05e34843fb7926a88d04b25e3c4f0e926e9eb85a1f5f3a84d85a93f858e6adbe. Older untracked UX captures are preserved. No uninstall, data clear or global adb reset occurred. Actual TalkBack speech, Switch Access and physical folding/OEM hardware are not certified by emulator evidence. Automatic approval review rejected an optional escalated lookup of protected /root/.codex; that lookup was abandoned without retry or bypass. Repository work continued using authorized paths.
