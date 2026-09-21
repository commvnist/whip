# Timer-delivery audit evidence — 2026-09-21

Scope: Focus and workout Rest timer completion without opening Whip. Disposable API 26, 34 and 37 emulators only. No owner-phone install, signed release or store action.

## Confirmed baseline

- Focus and Rest used delayed WorkManager alone. The native Focus baseline in `build/instrumentation-results-KZ1l2t` failed while Rest posted. The diagnostic replay `build/instrumentation-results-s7EGZQ` showed `Focus deadline=null, active=[], channel=3, enabled=true, permission=true, work=[ENQUEUED]` after the due time.
- `SharedPreferencesSettingsRepository.current()` had filtered out an elapsed Focus deadline. The due worker therefore could not match the persisted timer and never posted, even when its work ran. Opening the app was not a valid delivery dependency or repair.

## Corrected behavior and verification

- Both user-started timers now persist a unique delayed WorkManager fallback before registering an exact `RTC_WAKEUP` alarm when timing access exists. A private receiver promotes due work without an Activity. Startup and boot/time/package/access changes rebuild from the current persisted timer; stale generation/deadline/revision cannot replace a newer timer. Reset/replacement quiesce cancels alarms, and the private alarm registry is recognized by the data-epoch policy.
- The Focus deadline remains readable until the matching worker posts and durably clears it. A restored timer more than 24 hours late is cleared without a surprise alert. The Rest worker retains its post-before-complete retry boundary.
- `TimerAlarmIntegrationTest` passes 8/8 with zero failure/skip on API 34 in `build/instrumentation-results-ErP72d` and through `ANDROID_SERIAL=emulator-5554 ./gradlew --no-configuration-cache :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.whip.app.TimerAlarmIntegrationTest --console=plain` on each API 26 and API 37 AVD. Cases include both real alerts with no Activity launch, stale Focus/Rest wakeups, late obsolete schedule calls, alarm-access reconciliation and long-expired Focus recovery. The API 26/37 lanes use guarded direct Gradle because `qa-targeted` requires API 34+; Gradle's connected-result XML is overwritten by the next direct run.
- Final-source neighboring notification/action/reminder/time-change, timer, settings-persistence, recovery and platform-surface campaign passed 56/56 Android methods on API 34 with zero failure/skip in `build/instrumentation-results-TSlN1e`. `scripts/check --ready` passed in 2m9s (changed-code JVM route, Android-test compilation, lint and debug packaging); a separate `scripts/qa-targeted --all-jvm` passed all 659 methods across 112 classes. `scripts/ui-catalog lint` reports 528 required, zero pending/exception. The adjacent 51 external capture/widget/startup tests in pre-change `build/instrumentation-results-Xqyp1Q` are not claimed as post-change coverage.

Limit: Android may deny exact-alarm or notification access, defer fallback work, apply idle/alarm quotas, or suppress work after explicit force-stop. No implementation can guarantee user-visible delivery against those OS/user controls. This is a scoped platform checkpoint, not final whole-product acceptance.
