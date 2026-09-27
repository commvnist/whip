# Whip testing speed plan — 2026-09-27

## Fastest proven path

| When | Command | Evidence bought |
| --- | --- | --- |
| During an edit | `scripts/check` | Changed-path JVM behavior and small source guards. Use `scripts/check --path PATH` for one independent change in a dirty shared tree. |
| When affected UI or persistence behavior needs a device | `ANDROID_SERIAL=emulator-5554 scripts/check --emulator` | Routed Android classes on a disposable emulator. Add an explicit matching secondary emulator for a large selection. |
| Once before handoff | `scripts/check --ready` | Affected Android-test compilation, lint and debug packaging in addition to the fast checks. |
| Only for a frozen Play candidate | `ANDROID_SERIAL=emulator-5554 WHIP_ANDROID_SECONDARY_SERIAL=emulator-5556 scripts/candidate` | Complete JVM coverage, fresh Android execution, lint and signed build evidence. Verify and use the exact accepted artifacts. |

Private owner-phone releases follow the separately authorized `scripts/device release-deploy` lane. No physical phone is an instrumentation target. An explicit visual-design campaign uses `scripts/ui-catalog capture`; it does not run after every edit.

## Measured baseline

- The last accepted two-emulator candidate, before the current in-progress Goal edits, executed **659 JVM and 1,113 Android methods**. Its Android portion ran **16 fresh batches in 50.1 minutes** of wall time; Android XML sums to **92.2 test-minutes**. This is device execution time, not the whole candidate duration. The ordinary batches ranged from **2.5 to 14.0 minutes**.
- The accepted visual catalog ran **193 methods and 523 captures** in **34.9 minutes** of wall time on two emulators. Visual capture is a likely cost center, but its incremental cost inside the candidate has not been measured.
- The old quality-file route selected the entire `all` profile: **45 JVM and 76 Android classes**, even for Markdown. The new route selects **one JVM class** for each known TSV and treats quality Markdown as docs-only. The two owning contract classes passed **3/3 methods in a 4-second warm Gradle run**. Gradle now tracks all three TSVs as test inputs. Known TSVs still flag a fresh Play candidate; unowned quality data still takes the broad route.
- The current shared worktree contains unfinished Goal changes. `--path` narrows check selection but Gradle still compiles all dirty source; reconcile the source test count in `docs/testing.md` when that work is finalized.

## Next experiments, in order

1. **Keep the proven change ladder.** Record edit-to-result time for five representative changes (JVM-only, production feature, Android-test-only, shared core, and quality TSV). Run affected emulator tests once after the implementation stabilizes, readiness once before handoff, and a fresh candidate once per frozen Play release source. Target: median ordinary edit feedback under one minute on a warm build; report actual times before changing another gate.
2. **Profile visual capture before changing it.** Time the same capture-heavy methods on the same emulator/source before and after removing demonstrably redundant synchronization or output work. Keep PNG, hierarchy, distinct-state and `NAF` checks intact. Adopt a change only if repeated runs show at least a 10% candidate Android wall-time gain with zero new missing/stale captures on API 26, 34 and 37 focused checks.
3. **Revisit batch placement only if it becomes material.** Replaying the latest batch durations through a simple next-free-worker queue predicts about **48.7 versus 49.0 minutes** for ordinary work, so scheduler code is not justified now. First target the slow Track capture journeys if profiling shows repeatable waste; preserve their persistence, large-text and recovery assertions.

Do not add a test framework, third emulator, generalized cache, or new CI lane without a measured bottleneck that the existing scripts cannot address. Cached Android results remain development evidence; only the fresh candidate can qualify a Play release.
