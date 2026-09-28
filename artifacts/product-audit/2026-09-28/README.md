# Full product audit: focused acceptance evidence

Owner: FB-20260928-005. [Plan and shared review](../../../docs/quality/PRODUCT_AUDIT_2026-09-28.md), [productivity](../../../docs/quality/PRODUCT_AUDIT_2026-09-28_PRODUCTIVITY.md), [Gym/Settings](../../../docs/quality/PRODUCT_AUDIT_2026-09-28_GYM_SETTINGS.md).

Status: **Accepted within the requested focused scope**, VER-20260928-005. All 20 accepted groups are implemented. The owner explicitly requested minimal selected checks and no full batches. Every check command is bounded by `timeout --kill-after=3s 55s`; command JSON, elapsed time, exit status and original output are retained in `verification/`. No readiness, full profile, candidate, release packaging or phone operation ran. Emulator checks use disposable API 34 `emulator-5554` only.

All 29 distinct selected methods have passing evidence: 14 JVM and 15 Android. Reviewed workflow dispositions (177) are source review, not executed tests. [Selected original gallery](gallery.html) opens the 13 inspected acceptance states; original images and native hierarchies remain unmodified in `visuals/`.

## Calculation and build receipts

| Receipt | Result |
| --- | --- |
| `jvm-launch-environment.log` | The optional `/usr/bin/time` executable was absent; no build/test started. Replaced the timing wrapper with the available Python standard library. |
| `jvm-focused` | Incomplete: 55.002s timeout during the initial cold compilation path. Production Kotlin compiled; no executed-test claim. |
| `jvm-test-compile` | Passed in 6.011s. Compilation only. |
| `jvm-accepted` | Passed in 2.457s: 14 exact JVM methods, zero failures/errors/skips. Original XML and method ledger retained. |
| `debug-assembly` | Passed in 20.073s. An intermediate debug build before final fallback-copy/overflow integration; not a release artifact. |
| `jvm-numeric-overflow` | Passed in 6.150s: final exact Track decimal method also checks nonfinite converted-display input without throwing. Original XML retained separately. |
| `jvm-numeric-final` | Passed in 5.694s: exact Habit and Track decimal methods after preserving previous nonfinite eligibility at the three aggregate callers. |
| `android-compile` | Failed in 3.062s before execution: the new duplicate fixture referenced nonexistent TaskStep `uuid`. Corrected to its actual `id`; no production failure. |
| `native-assembly` | Passed in 8.532s after that fixture correction. Compilation/assembly only. Later native commands rebuild affected source as needed. |

The accepted JVM set has 14 distinct methods and 17 passing executions, zero failures/errors/skips. `jvm-methods.tsv` records the initial selection; [all execution ledger](verification/jvm-all-executions.tsv) distinguishes the two later affected-only repeats. Original XML is retained in `jvm-xml/`, `jvm-overflow-xml/` and `jvm-final-xml/`. No older audit receipt is represented as fresh verification.

## Native and visual acceptance

Each receipt prefix below has `.command.json`, `.time.json`, `.log` and `-results/` with the original XML, expected/executed selectors and runner aggregate. [Per-method ledger](verification/native-methods.tsv) preserves both failures and successes. Counting XML yields **21 executions, 18 passes, three fixture failures, zero skips; 15 distinct methods, all with a subsequent or original pass**. Successful methods inside a failed two-method command retain their own XML result; the failed command is never called green.

| Receipt | Elapsed / outcome | Scope and disposition |
| --- | --- | --- |
| `android-shared-large` | 31.258s; 2/2 pass | Area Create/Move long failure and clock keyboard/switch/recreation/duplicate. Actual font 2.0 at 1080×1600, density 420. |
| `android-draft-large` | 38.361s; 2/2 pass | Final shared color-field readability in Area Create/Move and dirty Habit Value/History/Pause restoration. Same short actual-200% configuration. |
| `android-shared-recovery` | 26.722s; 2 pass, 1 fail | Empty Custom unit and interrupted Area creation pass. Color fixture chose nonexistent palette label Red; corrected to actual Rose. |
| `android-color-tag` | 27.563s; 1 pass, 1 fail | Corrected color saving/failure check passes. Existing Tag fixture waited on the root error even though the repaired child correctly owned it. |
| `android-tag-duplicates` | 20.328s; 1 pass, 1 fail | Real duplicate persistence across four repositories passes. Tag fixture's raw error matcher was ambiguous between root and child; scoped it to the active rename dialog. |
| `android-tag-goal` | 31.677s; 2/2 pass | Final Tag owned-failure/draft fixture and Goal effective-date/backfill/recreation. |
| `android-units` | 46.111s; 2/2 pass | Shared custom receipt deliberately arrives before projection, with recreation; exact selection waits for the definition. Habit unit configuration persists without rewriting original history. |
| `android-gym-records` | 28.610s; 2/2 pass | Numbered record selection/draft/recreation/save/source, archived Progress volume and historical entered-unit conversion. |
| `android-gym-settings-export` | 31.141s; 3/3 pass | Historical filters and 21st Training Max decision, Settings scheduling refresh, real Room Gym CSV status/type snapshot. |
| `android-gym-final-copy` | 41.625s; 1/1 pass | Affected-only archive-volume journey after final singular/plural copy repair. |

The normal emulator configuration was 1080×2400, density 420, font scale 1.0. The two large-text commands used a real Android font scale of 2.0 and shorter 1080×1600 screen; the rule restored font scale and the parent restored normal size. No local fake font scale, skipped tests or reused native result was used. These incremental checks are intentionally small; unchanged checks were not rerun merely to turn an older failed command green.

### Inspected acceptance originals

| Original under `visuals/` | Inspection result |
| --- | --- |
| `product-audit.shared.area-error (1).png` | At actual 200%, full failure and identity scroll; Cancel/Create remain reachable. Shared Color now shows complete Default and dropdown arrow. |
| `product-audit.shared.area-move (1).png` | Long identity and 12 destinations scroll; Destination 12 remains selected and fixed Move 2 Items is reachable. |
| `product-audit.shared.clock-keyboard.png` | Native digits and AM/PM fit with the keyboard; fixed Cancel/Add remain reachable at 200%. |
| `product-audit.shared.tag-save-failure.png` | Request-owned error appears inside the open rename dialog with the retained Reviewed draft and fixed actions. |
| `product-audit.habits.discard-draft.large.png` | Full explanation, safe Keep Editing first and red Discard Changes fit at actual 200%. |
| `product-audit.habits.converted-unit.png` | L/minimum 2/maximum 3/quick 0.5 agree; historical-unit explanation, scrolling and Save remain clear. |
| `product-audit.goals.backfilled-current.png` | Dark current/trend/table agree on effective dates (80 yesterday, 75 today), with truthful downward trend and reachable Log Progress. |
| `product-audit.gym.tracked-record-draft.png` | Restored two-record draft, reorder/choice controls and Save fit. |
| `product-audit.gym.machine-record.png` | Exact machine Setting 3 and source identity remain visible. Synthetic 1969 timestamp is fixture data. |
| `product-audit.gym.archived-progress (1).png` | Archived-only volume stays 200 kg·rep; exercise selection and graph remain. Final weekly counts use grammatical singular text. |
| `product-audit.gym.archived-history-units (1).png` | Retained exercise/workout identity and 44.09 lb per hand (88.18 total) agree. |
| `product-audit.gym.training-max-history.png` | The oldest selected Training Max decision is reachable after recreation. Synthetic 1970 dates are fixture data. |
| `product-audit.settings.reminder-refresh.png` | Schedule-refresh success is separate from the still-visible Android notification permission blocker; no delivery promise. |

Images were exported with `scripts/device-artifacts` and personally inspected. `(1)` files are the final captures from repeated MediaStore names; the unsuffixed Area/Gym originals retain the prior rendering. `visuals/before/product-audit.shared.area-error.*` is a preserved copy of the initial Color-label rendering. `ux-upgrades.settings.reminder-result.*` is an additional current diagnostic capture from the reused Settings fixture, inspected and correctly showing unavailable notification testing. `tag-retry-failure.*` was captured after failed-test teardown and shows the emulator launcher: it is retained as an excluded diagnostic, **not app/UI acceptance evidence**. No edited image or crop substitutes for these originals.

## Integrity and limits

`source-sha256.tsv` identifies every changed production/test file in the accepted implementation. `evidence-sha256.tsv` covers the retained receipt files other than itself; final validation checks them against the filesystem. Scoped `.gitattributes` preserve native XML CRLF and raw compiler log spacing while the normal source/document whitespace checks remain in force.

Fresh checks are limited to the selected paths and configurations above. No full suite, lint/readiness/candidate gate, all-device/locale/TalkBack campaign, external Android delivery guarantee or physical-phone certification follows from this receipt. Room 46, epoch 6, backup 26 and release 0.3.81/code 87 are unchanged. Gym CSV adds four status/context columns and retains its existing leading columns. All accepted work in this audit is complete; historical paused audits keep their original status.
