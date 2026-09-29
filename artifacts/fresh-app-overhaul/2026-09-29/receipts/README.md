# Fresh overhaul native acceptance receipts

Status: **Verified** native receipt accounting after the final three Track/Gym acceptance invocations. Parent owns overall acceptance and the affected readiness receipts stored alongside these files.

Baseline/source context: the fresh audit starts from `ba1f7cc5`. Four `before-*.log` invocations capture baseline productivity and supporting workspaces at normal and actual Android 200% text. Other native logs record implementation, repair replays or after overviews across successive built APKs; the native output does not independently identify each APK/source revision. A passing old attempt is historical evidence, not certification that every method was rerun on one final APK.

## Accounting

The parser inspected `build/fresh-app-overhaul-20260929/*.log`: **64 native invocations**, **68 method attempts**, **53 passed**, **14 failed**, **1 incomplete**. Invocation summaries are separately **49 passed**, **14 failed**, **1 incomplete**. Fifteen build/install/emulator/readiness/JVM logs without Android method-status records are excluded from native totals.

Deduplicating only passing rows by the exact fully qualified `class#method` gives **39 distinct passed methods**: **37 behavior checks plus 2 overview capture methods**. Excluding all four baseline invocations still gives 39, because both overview methods also have passing after runs. Normal/enlarged runs and repaired repeats are separate attempts but count once in this distinct-method total. No failed or incomplete attempt is removed from the receipts. All nine distinct method IDs with a failed attempt also have a passing attempt elsewhere; this does not change the failed invocations' status.

These are focused acceptance counts, not full-suite execution or an executed count for every source-review disposition.

## Files and parsing contract

- [native-results.tsv](native-results.tsv) contains every method attempt, including source-log path/hash, exact method identity, terminal status code and source line, invocation result/time/footer, baseline/after context and supported environment provenance.
- [failure-attempts.tsv](failure-attempts.tsv) is the exact subset of failed or incomplete method rows. The incomplete historical-set attempt started but has no terminal method status, `Time`, `OK` or failure footer; its cause and shell exit status are not recorded.
- [failure-snippets.md](failure-snippets.md) retains exact first failure blocks and aggregate footers from all failed invocations, plus the complete short incomplete output. Blocks longer than 35 source lines are explicitly bounded. The logs contain disposable synthetic fixtures; credential-pattern inspection found no credentials needing redaction.
- [accounting.json](accounting.json) stores the totals above for machine reconciliation.
- `raw/` preserves 29 exact native logs: the original 20 selected shared-card/backup, productivity History/action, overview, training, shell and mixed search/Review receipts, plus all remaining Track/Gym attempts added at final handoff. Other attempts remain represented by complete structured status rows, source hashes and failure excerpts; their original full logs remain in the build directory. The final installed-byte proof is also preserved as `raw/tracks-gym-final-installed.txt`.

For each source log, the parser reads `INSTRUMENTATION_STATUS` key/value bundles and associates class/test/current with the following status code. Code `1` starts a method; code `0` is a passing terminal record; code `-2` is a failed terminal record. A start without a terminal record becomes incomplete. Source line columns refer to the status-code lines, using one-based numbering. Each available native log contains one invocation; the parser rejects multiple final invocation footers rather than silently combining them.

`Time` belongs to the whole invocation, not each method. `OK` or `FAILURES!!!` determines the separate aggregate status. Advertised method counts, terminal counts and JUnit failure totals were cross-checked. The final `INSTRUMENTATION_CODE: -1` appears after both successful and failed AndroidJUnitRunner runs; it is preserved and is not interpreted as an individual method failure.

Two mixed invocations are intentionally represented without granting command-wide acceptance: `shell-search-review-native-replay.log` passes focused keyboard search and fails Review; `shell-review-return-correlations-replay.log` fails Review and passes correlation handling. Their passing methods contribute to the distinct-method total while both invocation statuses remain failed.

The final continuation adds three passing invocations without adding a distinct method: [strengthened Set Details](raw/tracks-gym-malformed200-complete.log), 12.161 s; [supporting overview at normal text](raw/tracks-gym-supporting-normal-complete.log), 13.718 s; [supporting overview at actual Android 200% text](raw/tracks-gym-supporting-native200-complete.log), 13.124 s. These fresh supporting captures follow the final Tracks singular-count copy repair. The parent handoff explicitly identifies normal and actual-200% profiles; the raw native output prints neither configuration nor serial, so that provenance is distinguished in the TSV.

Device/profile fields name a serial or profile only where the workbook or fixture provides explicit support. The provenance columns identify that support. A current `@AndroidFontScale` annotation is marked **fixture-declared**, not mistaken for a runtime configuration line in the raw log. The Home RTL test uses synthetic Compose font scale. Other unsupported serial, SDK, density, locale and runtime-scale facts remain unrecorded; filename order does not establish chronology. No command or timeout outcome is invented from an absent footer.

Receipt consistency checks confirmed every stored source hash, every passing status-code line, the failure subset and the byte-for-byte selected raw copies. Parent retains overall source, readiness, visual gallery and delivery accounting.
