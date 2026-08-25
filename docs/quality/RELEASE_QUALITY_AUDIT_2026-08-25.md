# Release-quality audit — 2026-08-25

## Objective and evidence rules

This is the active audit record for the release-quality objective. It supersedes
older device-specific acceptance claims when they conflict with the current
emulator-only requirement. Source inspection and disposable-emulator evidence
are both required; historical screenshots are context only.

The detailed product-surface register is
[`release-quality-coverage-2026-08-25.tsv`](release-quality-coverage-2026-08-25.tsv).
Each credible finding is recorded below before remediation with observable
evidence, impact, alternatives, the selected decision, regression coverage, and
verification status.

## Baseline checkpoint

- Pre-existing work checkpoint: `3b80fb5ba483653dce76055ea30fc2264cb20f7f`
- Branch/upstream: `main` → `origin/main`
- Remote verification: local `HEAD`, `origin/main`, and `git ls-remote` all
  resolved to the checkpoint before audit edits began.
- Sensitive-data review: no credential, private-key, keystore, environment,
  credential-bearing URL, or provider-token additions were found. Apparent
  `sk-` matches were the benign substring in `tasks-tab-*` test tags.

### Measured pre-remediation coverage

Checkpoint `3b80fb5` produced the following deterministic report:

| Scope | Baseline |
| --- | ---: |
| Domain lines | 78.43% (2,535 / 3,232) |
| Domain branches | 51.80% (1,382 / 2,668) |
| Core settings/policy lines | 63.16% (324 / 513) |

The committed E2E baseline report records product lines 64.74% (24,834 /
38,358), product branches 37.53% (11,670 / 31,095), repository lines 80.12%
(4,260 / 5,317), first-class screen lines 59.67% (10,777 / 18,062), Track
screen lines 45.38% (1,248 / 2,750), and Gym screen lines 57.42% (2,755 /
4,798). A fresh single-run attempt discovered all 282 tests but failed two
tests, so it correctly did not emit a replacement passing report. Those
failures are findings RQ-001 and RQ-002.

### First remediation coverage

The expanded 541-test suite (251 JVM and 290 Android instrumentation tests)
passes without skips. The complete single-run API 34 report now records:

| Scope | Baseline | After first remediation | Change |
| --- | ---: | ---: | ---: |
| Product-code lines | 64.74% (24,834 / 38,358) | 66.49% (26,537 / 39,914) | +1.75 pp |
| Product-code branches | 37.53% (11,670 / 31,095) | 39.38% (12,758 / 32,395) | +1.85 pp |
| Repository lines | 80.12% (4,260 / 5,317) | 80.17% (4,323 / 5,392) | +0.05 pp |
| First-class screen lines | 59.67% (10,777 / 18,062) | 62.00% (11,846 / 19,105) | +2.33 pp |
| Track screen lines | 45.38% (1,248 / 2,750) | 50.50% (1,624 / 3,216) | +5.12 pp |
| Gym screen lines | 57.42% (2,755 / 4,798) | 57.42% (2,755 / 4,798) | unchanged |

Denominators changed because the baseline was the last committed passing report
while the checkpoint's attempted report correctly failed before publication.
Every audited floor increased or remained unchanged; the new tests cover
observable Android and policy behavior rather than assertion-free line visits.

## Emulator matrix

| Lane | AVD/device class | API | Size/density | Required states | Status |
| --- | --- | ---: | --- | --- | --- |
| Minimum/small | `whip_api26_small`, Nexus One class | 26 | recorded at runtime | fresh/populated, light/dark, default/large scale | Provisioned |
| Typical phone | `whip_api34`, Pixel 8 class | 34 | 1080×2400 @ 420 dpi | full E2E coverage, fresh/populated, permissions, offline | 290/290 gate and single-run coverage passed |
| Target/large | `whip_api37_large`, Pixel C class | 37 | recorded at runtime | large-screen navigation, light/dark, default/large scale | Provisioned |
| Synthetic posture | Compose window-layout fixtures | 34+ runner | compact, rail, expanded, book/tabletop | RTL, 200% font, state recreation | Complete suite passed on API 34 |

No physical device is an evidence lane for this objective. Every ADB command is
pinned to an `emulator-*` serial and destructive operations are guarded by
`ro.boot.qemu=1`.

## Structured findings

### RQ-001 — completed-Habit E2E transition is not synchronized

- **Evidence:** on API 34, both the complete 282-test coverage run and focused
  reruns failed at `ProductivityCreationJourneyE2ETest.kt:129`. After `+1` is
  persisted, the Habit moves from the remaining list into the auto-expanded
  **Done** section. The failure dump showed `rowCount=3` and zero scroll range:
  only the header, all-done message, and collapsed Done disclosure existed.
  A first completion could arrive before `completionTrackingReady` was set, so
  the intended transition expansion was skipped.
- **Impact/severity:** P1 release-gate reliability. A critical happy-path test
  fails deterministically and prevents a coverage report even though the
  product provides the correct edit action after the state transition.
- **Alternatives considered:** keep the current assertion; stop moving completed
  Habits; navigate to All; wait longer; require the user to expand Done; or make
  new-completion detection deterministic. Changing information architecture is
  unsupported by the UX evidence, and waiting cannot repair a skipped state
  transition.
- **Decision:** seed the known Done IDs from initial content, then expand for
  every subsequently new Done ID without a separate readiness race. Give the
  Today list a stable semantic test tag and scroll to the exact accessible edit
  action, since an expanded lazy card can still be below a compact viewport.
- **Regression:** the existing first-class productivity E2E journey.
- **Status:** verified. The focused regression passed and the complete 282-test
  API 34 gate passed across six isolated batches before coverage expansion.

### RQ-002 — navigation test asserts retired Habit copy

- **Evidence:** `WhipNavigationTest.kt:66` expects “Check in, log a value, or
  continue a timer for habits due today.” The current Today page intentionally
  says “Check in, log a value, or continue a timer. Completed habits move to
  Done for confirmation or undo.” Both the full and focused API 34 runs fail.
- **Impact/severity:** P2 test correctness. The release gate rejects the current
  accessible page despite the newer copy explaining recovery more clearly.
- **Alternatives considered:** revert the product copy, loosen the test to a
  generic substring, remove the assertion, or assert the current complete
  explanation. Reverting loses useful recovery guidance; a vague or absent
  assertion weakens navigation evidence.
- **Decision:** assert the current complete text.
- **Regression:** `primaryAreasAndAccessibleTopActionsAreReachable`.
- **Status:** verified. The focused regression and complete 282-test API 34 gate
  both passed.

### RQ-003 — emulator gate cannot select a safe emulator when another device is visible

- **Evidence:** `scripts/coverage --emulator` honors `ANDROID_SERIAL`, validates
  `ro.boot.qemu=1`, and safely ignores unrelated ADB targets. `scripts/check
  --emulator` instead requires exactly one connected target and cannot run in
  the current environment even when `ANDROID_SERIAL=emulator-5554` is explicit.
- **Impact/severity:** P1 release-tooling reliability. The authoritative gate is
  unnecessarily blocked, encouraging manual command subsets.
- **Alternatives considered:** disconnect unrelated devices, retain the current
  behavior, or align selection with the coverage script while retaining the
  QEMU/API guards. External device state is not a reproducible CI contract.
- **Decision:** honor an explicit `ANDROID_SERIAL`; otherwise continue requiring
  exactly one connected target. Always validate connection, QEMU identity, and
  API level before destructive instrumentation.
- **Regression:** shell-level focused verification plus the complete emulator
  gate pinned to each supported lane.
- **Status:** verified. `ANDROID_SERIAL=emulator-5554 scripts/check --emulator`
  passed all 282 then-current instrumentation tests while an unrelated ADB
  target remained visible and untouched.

### RQ-004 — programmatic widget refresh dereferences a missing broadcast result

- **Evidence:** the new real widget-configuration test selected an Area and
  pressed **Add widget**. `WhipWidgetProvider.update()` directly calls
  `onUpdate`; outside `BroadcastReceiver.onReceive`, `goAsync()` returns null.
  The refresh completed its coroutine and then crashed at `pending.finish()`.
- **Impact/severity:** P1 functional/reliability. Adding or refreshing a widget
  from in-app code can produce an uncaught asynchronous NPE after a successful
  configuration.
- **Alternatives considered:** remove programmatic refreshes, synthesize a
  broadcast, split broadcast and direct update pipelines, or finish only a
  platform-owned pending result. Removing refresh breaks visible correctness;
  a synthetic broadcast adds lifecycle and export complexity. A complete
  pipeline split is unnecessary while rendering behavior is shared.
- **Decision:** retain one rendering pipeline and null-safely finish the pending
  result only when Android dispatched a broadcast.
- **Regression:**
  `PlatformEntrySurfaceE2ETest#widgetConfigurationPersistsTheChosenAreaAndReturnsSuccess`.
- **Status:** verified in focused runs, the complete 290-test gate, and the
  single-run E2E coverage suite.

### RQ-005 — platform entry and privacy contracts rely on manual claims

- **Evidence:** the cause/effect register left the launcher-widget
  configuration, real document picker, notification shade, and Health rationale
  as manual-only cells. ACTION_SEND and release manifest/backup invariants also
  lacked direct behavioral contracts.
- **Impact/severity:** P1 coverage/release risk. External entry points and
  privacy declarations can regress while repository and Compose-only suites
  remain green.
- **Alternatives considered:** preserve manual checklist entries; assert private
  helpers; add screenshot-only tests; or exercise observable Android-owned
  surfaces and parse the authored release policy inputs. Manual-only checks are
  not reproducible, and helper assertions would not verify platform handoff.
- **Decision:** add six external/widget/Health E2E tests, a real DocumentsUI
  cancel/resume test, a real notification-shade test, and four JVM manifest and
  backup-policy tests. All assert user-visible state or release policy rather
  than implementation trivia.
- **Regression:** `PlatformEntrySurfaceE2ETest`,
  `ZPlatformNotificationSurfaceE2ETest`,
  `SettingsBehaviorUiTest#plainJsonBackupUsesAndroidsDocumentProviderAndReturnsSafelyOnCancel`,
  and `ReleasePrivacyPolicyTest`.
- **Status:** all 12 added tests pass focused and in the complete 541-test gate;
  the 290-test single-run report also passes with measurable coverage gains.

### RQ-006 — authoritative test guidance treats physical hardware as mandatory

- **Evidence:** `docs/testing.md` required a current physical device and a
  Physical Fold 8 Ultra matrix even though repository tooling refuses physical
  hardware for destructive tests and this release objective is emulator-only.
- **Impact/severity:** P2 release-process clarity. The written acceptance gate is
  impossible in the authorized environment and conflicts with safe tooling.
- **Alternatives considered:** retain the checklist as aspirational, waive it
  only in this audit, or define representative emulator lanes from minSdk,
  targetSdk, and supported window classes. A silent waiver leaves the next gate
  ambiguous.
- **Decision:** make API 26 small, API 34 typical, and API 37 large emulators the
  authoritative release matrix. Physical evidence remains optional only.
- **Regression:** documentation baseline count is machine-checked by
  `scripts/check`; all destructive tooling continues enforcing QEMU identity.
- **Status:** implemented; emulator-matrix verification pending.
