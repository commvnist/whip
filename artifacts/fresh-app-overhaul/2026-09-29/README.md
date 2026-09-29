# Fresh Whip overhaul: implementation and acceptance

Baseline: `ba1f7cc5`, 2026-09-29. [Completed implementation plan and six design pillars](../../../docs/quality/FRESH_APP_OVERHAUL_2026-09-29.md). Three GPT-6 Astra/high reviewers began the independent audit; three GPT-6.1 Sol/xHigh agents finished integration and acceptance at the owner's request. Prior audits were not used as current-quality evidence.

All **24 accepted remedies** are implemented across Home, Tasks, Habits, Goals, Tracks, Gym, Review, Search, Settings and shared editors/inspectors. The four linked workbooks distinguish **176 source-review dispositions** from executed checks. Main improvements include concise meaningful daily cards, visible Goal progress and next actions, preserved inspector/history context, direct historical workout correction, actionable exact Track evidence, stable shared headers and resilient settings/restore flows.

## Original visual evidence

Open the [before-and-after gallery](index.html) for seven main screens at normal and actual Android 200% text, plus representative repaired child workflows. PNGs are original device captures; accompanying XML records native accessibility hierarchy. Fixtures contain synthetic records, without owner-phone content. Comparable root states use the same overview fixtures before and after; time-dependent values can differ.

- `before/productivity` and `before/supporting`: normal baseline roots.
- `before/large-productivity` and `before/large-supporting`: baseline roots at Android font scale 2.0.
- `before/primary-editor`: enlarged Set Details with the native keyboard, exposing the system-bar overlap.
- `after/productivity`, `after/large-productivity`, `after/supporting-final`, `after/large-supporting-final`: accepted root originals.
- `after/productivity-detail`, `after/tracks-gym`, `after/shell-final`, `after/shell`, `after/dialog-theme`: selected error, correction, return, enlarged and theme originals.
- Intermediate `after/supporting` and `after/large-supporting` preserve earlier integration captures, including the actual enlarged-header failure; they are not final acceptance images.

Reviewers inspected original normal/enlarged images, including keyboard and error states. Exact native assertions verify full Goal identity/action text, editor screen bounds, and equal Tracks/Gym toolbar/search/context rectangles. Appearance remains subject to owner judgment and does not certify every device or possible state.

The [original capture manifest](receipts/original-captures.tsv) records all PNG/XML file sizes and hashes, including retained intermediate evidence. Final [installed APK byte proof](receipts/raw/tracks-gym-final-installed.txt) confirms the final debug/test pair matches the built artifacts on the acceptance emulator.

Directory-local Git attributes preserve original native CRLF bytes and meaningful empty final TSV fields. These evidence-format exceptions apply only to this artifact directory; production/source patch whitespace remains checked normally.

## Executed acceptance

- **450 affected JVM methods**, 49 suites, zero failures/errors/skips. Final readiness uses the change router's relevant product profiles and exact new classes, rather than all JVM tests. Test execution totals 3.453 seconds in the XML; its Gradle stage reports 12 seconds. [Suite receipt](receipts/jvm-suites.tsv), [method receipt](receipts/jvm-methods.tsv).
- **39 distinct native methods with passing evidence**: 37 behavioral methods and two overview methods, including both normal and actual-200% executions of each overview. Duplicate/repaired/baseline attempts do not increase this count. [Native receipts and attempt accounting](receipts/README.md).
- Each selected native invocation uses `timeout --kill-after=3s 55s`; successful runner status and `OK` are required. Some individual methods pass inside a failed two-method invocation; those per-method successes are distinguished from the failing containing command. Failures and incomplete attempts remain recorded rather than erased.
- Final `scripts/check --ready` passes affected JVM checks, brand/static rules, Android-test compilation, debug lint, debug assembly and patch integrity. Its static/build stage reports **2m54s**, separate from routine bounded test runs. Debug lint has zero errors, 90 warnings and 14 informational hints; [issue receipt](receipts/lint-issues.tsv) records the remaining diagnostics. [Exact readiness log](receipts/final-ready.log).
- Incremental `timeout --kill-after=3s 55s ./gradlew :app:assembleDebugAndroidTest --console=plain` passes in **2s**. [Packaging receipt](receipts/final-test-apk.log). Native instrumentation is restricted to guarded disposable API 34 emulators; no phone tests or install occurred.

[Accepted application/test/resource hashes](receipts/accepted-source.tsv) record the final source. The current source inventory is 736 JVM plus 1,294 Android declarations, **2,030 total**; the selected counts above are the executed scope. No full suite, candidate or complete visual-catalog campaign ran.

## Failure and recovery integrity

The audit found real enlarged Goal identity/action clipping, keyboard editor overlap and a five-pixel enlarged Gym header shift; shared repairs retain strict assertions. Initial fixture failures included ambiguous title/scroll selectors, inaccessible Cancel assumptions, asynchronous workout cleanup, and disabled Compose controls lacking a native Button ancestor. Repairs target actual controls and stable identity without weakening data or geometry checks. A started historical invocation without a terminal result remains incomplete. [Failure attempt receipt](receipts/failure-attempts.tsv) and the workbooks retain their history.

The repeated persisted Habit cleanup and stale Goal deletion/correction journey passes in 46.957 seconds, retaining History, query and exact saved evidence through recreation. Historical workout correction preserves finished-session dates/state and the unrelated active session while rejecting a stale edit. Malformed Set Details text survives recreation/discard review until the user corrects it.

## Delivery boundaries

No domain schema, data epoch, portable-backup format or dependency was changed. Selected backup import is explicitly limited to 32 MiB including encrypted envelopes; export and private recovery remain uncapped. Bounding input does not promise that every smaller JSON object graph is memory-safe.

At development acceptance the owner phone remained **0.3.88/code 94**; this record is not Play qualification. Source and durable evidence are committed and pushed together. The subsequent explicitly authorized [0.3.89/code 95 phone release](../../phone-releases/2026-09-29/0.3.89/README.md) is Verified under VER-20260929-016.
