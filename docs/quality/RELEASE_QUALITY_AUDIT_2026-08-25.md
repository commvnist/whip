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

### Final measured coverage

The expanded 542-test suite (251 JVM and 291 Android instrumentation tests)
passes without failures or skips. The complete single-run API 34 report records:

| Scope | Baseline | Final | Change |
| --- | ---: | ---: | ---: |
| Product-code lines | 64.74% (24,834 / 38,358) | 66.69% (26,762 / 40,127) | +1.95 pp |
| Product-code branches | 37.53% (11,670 / 31,095) | 39.43% (12,784 / 32,423) | +1.90 pp |
| Repository lines | 80.12% (4,260 / 5,317) | 80.17% (4,323 / 5,392) | +0.05 pp |
| First-class screen lines | 59.67% (10,777 / 18,062) | 62.08% (11,870 / 19,121) | +2.41 pp |
| Track screen lines | 45.38% (1,248 / 2,750) | 49.94% (1,608 / 3,220) | +4.56 pp |
| Gym screen lines | 57.42% (2,755 / 4,798) | 57.52% (2,766 / 4,809) | +0.10 pp |

Denominators changed because the baseline was the last committed passing report
while the checkpoint's attempted report correctly failed before publication.
Every audited floor increased or remained unchanged; the new tests cover
observable Android and policy behavior rather than assertion-free line visits.

## Emulator matrix

| Lane | AVD/device class | API | Size/density | Required states | Status |
| --- | --- | ---: | --- | --- | --- |
| Minimum/small | `whip_api26_small`, Nexus One class | 26 | 480×800 @ 240 dpi | fresh/populated, light/dark, default/150% scale | 27-test compact/minimum-API lane passed |
| Typical phone | `whip_api34`, Pixel 8 class | 34 | 1080×2400 @ 420 dpi | full E2E coverage, fresh/populated, permissions, offline | 291/291 batched gate and single-run coverage passed |
| Target/large | `whip_api37_large`, Pixel C class | 37 | 2560×1800 @ 320 dpi | large-screen navigation, portrait/landscape, light/dark, 150% scale | 40-test expanded/target-API lane passed |
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
- **Status:** verified. The focused regression and final complete 291-test API
  34 gate passed.

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
- **Status:** verified. The focused regression and final complete 291-test API
  34 gate both passed.

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
- **Status:** verified in focused runs, the final complete 291-test gate, and
  the single-run E2E coverage suite.

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
- **Status:** all 12 added tests pass focused and in the final complete 542-test
  gate; the 291-test single-run report also passes with measurable gains.

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
- **Status:** verified across the complete API 34 suite and representative API
  26 compact and API 37 expanded lanes.

### RQ-007 — minimum-API compact workflows are not reliably reachable

- **Evidence:** the first API 26 lane exposed Android-version launch-option
  assumptions, a reference-SystemUI lock-screen crash, and compact content that
  could not scroll to setup, workout, Track-list, Gym-library, picker, and
  confirmation actions. Several tests also selected dense list content by
  brittle visible text rather than stable semantics.
- **Impact/severity:** P1 compatibility and test reliability. The declared
  `minSdk=26` could not complete representative first-class journeys on a small
  viewport.
- **Alternatives considered:** raise minSdk, exclude compact flows, increase
  timeouts, or make production content and observable controls scrollable and
  addressable. The first three options would weaken the supported contract.
- **Decision:** preserve API 26 support; make the affected setup/empty/editor
  content scroll safely, add stable semantic list/action tags, and use plain
  Activity launching on API 26 without legacy lock-screen flags that trigger
  the reference SystemUI defect.
- **Regression:** the selected 27-test API 26 lane plus four compact 200%-font
  tests on API 34.
- **Status:** verified. All selected tests pass on the 480×800 API 26 emulator;
  light, dark, and 150%-font populated states were reviewed.

### RQ-008 — expanded layouts expose ambiguous and stale test routes

- **Evidence:** the API 37 lane found duplicate **Add Name** actions across
  master/detail panes, support-category routing that differs from compact
  navigation, and detail sections rendered directly rather than through the
  compact popup path.
- **Impact/severity:** P1 large-screen coverage. Tests could operate the wrong
  pane or fail before validating actual target-API behavior.
- **Alternatives considered:** force compact navigation on tablets, select the
  first matching label, or expose pane-owned semantics and test the adaptive
  contract. Forcing compact mode discards the supported UI; positional label
  selection is ambiguous.
- **Decision:** add pane/section-specific semantic routes, an unambiguous Track
  add-entry action, and adaptive assertions that follow the rendered layout.
- **Regression:** the selected 40-test API 37 large-screen lane.
- **Status:** verified in landscape and portrait, including light, dark, and
  150%-font states on the 2560×1800 emulator.

### RQ-009 — public schema 27 cannot open in the reset schema-9 build

- **Evidence:** every public 0.3.0–0.3.7 release shipped Room schema 27. The
  audited current build declared schema 9, so installing it over 0.3.7 failed
  on first database open with `Migration from 27 to 9 required`. This was
  reproduced with an old signed 0.3.7 APK and a populated sentinel record.
- **Impact/severity:** P0 data availability. An ordinary public upgrade crashes
  at launch and prevents access to existing local data.
- **Alternatives considered:** destructive fallback, renumber current schema
  above 27 while dropping legacy-only tables, or migrate every persisted public
  structure losslessly. Destructive fallback violates the release contract;
  silently dropping tables would still lose public data.
- **Decision:** advance to schema 28, add an explicit atomic 27→28 migration,
  preserve compatibility entities for `entity_tag_links` and
  `goal_completion_snapshots`, and retain the complete 1–9→28 forward path.
- **Regression:** four migration tests pass on API 26 and API 34, including
  public schema 27 and current pre-release schema 9 paths.
- **Status:** verified with a real signed upgrade. `Upgrade_Sentinel_037`
  created by public 0.3.7 remained visible after installing the signed current
  release with `adb install -r`; cold launch produced no migration exception.

### RQ-010 — optimized benchmark coverage is stale and violates artifact policy

- **Evidence:** the first full benchmark run found a retired Home selector, a
  100k-Goal fixture missing schema-28 elapsed fields, an undersized load
  timeout, a resize scenario that generated no frames, and a status file
  written directly to shared-storage root paths outside `device-artifacts`.
- **Impact/severity:** P1 release evidence. Four of nine performance scenarios
  could not produce trustworthy optimized-build metrics, and the harness
  violated the workspace's device-artifact boundary.
- **Alternatives considered:** report only passing scenarios, retain direct
  status files, or repair the observable routes, fixtures, workload, and host
  reporting. Partial reporting would conceal gaps.
- **Decision:** report completion through UI state, include benchmark sources in
  the static device-path scan, update current navigation and schema fixtures,
  generate resize frames explicitly, and bound the dense-Goal wait to 60s.
- **Regression/status:** all nine optimized benchmark/profile scenarios pass
  together with zero failures or skips. Median/p95 emulator metrics and hashes
  are recorded in `docs/performance.md`; they are engineering evidence, not
  retail-device claims.

## Final release validation

- `scripts/check --full`: passed the minified APK/App Bundle build, lint,
  static-policy checks, all 251 JVM tests, Play Store asset validation, and all
  checked coverage floors.
- `ANDROID_SERIAL=emulator-5554 scripts/check --emulator`: passed all 291
  Android tests in seven isolated batches.
- `ANDROID_SERIAL=emulator-5554 scripts/coverage --emulator`: passed all 291
  Android tests in one run and published the final coverage table above.
- Optimized benchmark/profile suite: all nine scenarios passed together.
- Signed release: minified APK and App Bundle built with an ephemeral audit key;
  package `commvne.com.whip.app`, version code 15, version name 0.3.9, min API
  26, target API 37. The merged/output manifest has backup and cleartext traffic
  disabled and only the intended exported components and permissions. The APK
  verifies under signature scheme v2 with certificate SHA-256
  `88ec0a9406b9c7d5f05861a7ffc03c4a012bacec43d05f0800f2dcf44f17d1ae`;
  artifact SHA-256 values are
  `dfd0dc069fe6e9164ae636a34448ae042c5b03b1411348b7c67750ab01ec6035`
  (APK) and
  `cc4b275f9729689ee8e59dd885c7ff5c8b2fb7308cfbccdbe9dea32e4c7026c5`
  (AAB).
- Fresh signed-install smoke: uninstall/install succeeded on the QEMU-verified
  API 34 emulator; setup, Home, all five primary destinations, Settings, and a
  prefilled ACTION_SEND Task editor rendered. A forced cold relaunch returned
  to Home in 525 ms with no app crash or ANR in the error log.
- External entry smoke: ACTION_SEND, launcher-widget configuration, Health
  rationale, DocumentsUI cancel/resume, and a real notification shade were
  exercised on the representative API 34 emulator.

## Second-audit conclusion and limitations

All in-scope P0/P1/P2 findings above are remediated and covered by regression
evidence. No unresolved release-blocking finding remains in the repository or
authorized emulator matrix. The final full non-device gate and fresh signed
install smoke are recorded at the concluding audit checkpoint.

The API 26 reference image crashes its own SystemUI notification shade and
letterboxes forced rotation, so API 34 supplies platform-shade evidence and API
37 supplies valid rotation evidence. API 26 DocumentsUI returns to Launcher
after cancel while API 34 verifies app resume. Benchmark numbers are emulator
comparisons only. Physical-device assistive-technology checks and Play Console
submission remain optional/external owner activities and are not claimed here.
