# Deep product review evidence — 2026-09-29

Acceptance is in progress. [Accepted plan](../../../docs/quality/DEEP_PRODUCT_REVIEW_2026-09-29.md). Baseline `04bc58e2`, private phone 0.3.83/code 89. Only synthetic emulator records are used here; no phone installation or data inspection.

## Method

Every routine check is individually bounded by `timeout --kill-after=3s 55s`. Native receipts name one exact method and include the emulator target guard, instrumentation result and elapsed wall time. `verification/run-native.sh` rejects a nonzero command or a missing `OK (1 test)`; Android's shell exit code alone is insufficient. No full/readiness/candidate batch is run.

Original PNG/XML captures are retained in `before/` and `after/`. A capture from a failing fixture is observation only, not a passing journey. Parent and domain reviewers inspect original images; assertion success alone does not establish visual acceptance.

## Build snapshots and unsuccessful attempts

- Baseline debug package and exact shared-page catalog passed before edits.
- `shared-package-1.log`: positional Habit dialog API compile mismatch; restored compatible parameter ordering.
- `shared-package-2.log` / `instrumentation-diagnosis.log`: JaCoCo `MethodTooLargeException` for root `WhipScreen`; extracted Review and Home Habit editor state into feature owners. Coverage remains enabled.
- `shared-package-3/4/5.log`: fixture compile errors corrected (TaskStepDraft named parameters/position and duplicate Gym arguments).
- `shared-package-6.log`: 55-second timeout after Kotlin compilation. Incomplete, never passed. The separately bounded packaging-only `initial-package-phase.log` uses completed compilation outputs and passes in 26 seconds; initial APK/source hashes identify that snapshot.
- `home-preview-native-1.log`: selector targeted a non-merged layout tag. Repaired by addressing the actual accessible completion control; its earlier screenshot remains visual observation.
- `gym-unit-jvm.log`: new pure fixture confused the nested Track point/entry identity. Corrected fixture; exact three-method productivity class subsequently passes in `productivity-jvm.log` (3-second Gradle build). Its XML was overwritten by the next targeted Gradle invocation; the raw success log is retained.
- `final-native-compile-1/2.log`: missing Settings and machine fixture imports; corrected before packaging.
- `final-package-1.log`: fresh app/test build succeeds in 17 seconds with JaCoCo. `final-source.sha256` and `final-apks.sha256` identify the compiled snapshot. Later fixture-only changes, if any, are recorded separately; no silently reused binary is claimed.

## Scope of claims

Source reports assess complete jobs and alternate modes. Exact executed receipts support the selected behaviors only. No full-suite, all-device, TalkBack/locale or measured frame-rate certification is claimed. Content-keyed calculations avoid known unnecessary work; this is not a physical speed benchmark. The owner phone stays on its previously verified release.
