# Actual dialog font scaling and inspector identity

Related: `FND-20260909-006/007`, `DEC-20260909-005/006`, `IMP/VER-20260909-007`.

The outer Compose density override did not enlarge native-dialog text. Three baseline assertions measured 1.0 rather than 2.0. The corrected shared rule sets Android's font scale before activity creation, checks rendered dialog TextLayoutResults, and restores the original setting after teardown.

Actual 200% text exposed a crowded inspector header: even “Barbell row” and “Available” broke mid-word, and the Exercise context and Task title were truncated. The responsive header now gives identity its own full-width row below emoji and trailing actions. The existing body scroll reaches the Task's Subtask conversion action.

- `before-shared.inspector.*`: original header at actual Android 200%, from `lRrKkv`.
- `after-light-shared.inspector.*`: matching light-theme header comparison from `DOsO2c` (Task) and `svdm2B` (Exercise). These runs exposed an overly strict status-badge assertion, subsequently corrected to check painted line bounds. The production layout matches the final source.
- `after-shared.inspector.task-large`, `compact-large`, and `subtask-large`: final dark-theme top and scrolled states at actual Android 200%.
- `after-shared.choice.large` and `nested-large`: final parent/child dialog font and action reachability evidence.
- `after-shared.inspector.base`: normal-scale Habit shell retaining the roomy header arrangement.

All six final states were personally inspected in the fresh shared catalog. The final capture command was `ANDROID_SERIAL=emulator-5554 scripts/ui-catalog capture --family shared /tmp/whip-commit-dialog-font-shared`: 17 owning tests and 50 exact PNG/XML pairs passed with no failures/skips/reuse (`build/instrumentation-results-WUtbw4`). Manifest SHA-256: `e9f70f676eef2a04039b1eabf9072cd3046569eaf8b709518d01e20d833ef9b1`.

PNG files preserve original exported bytes. XML only normalizes CRLF to LF. The 31-test focused regression passed in `1ZkxKY`; full commands and readiness results are in `VER-20260909-007`. The broader 28-fixture font-verification backlog remains in `docs/quality/astra-dialog-font-review-2026-09-09.tsv`; this evidence does not establish whole-product accessibility acceptance.
