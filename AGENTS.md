# Whip workspace instructions

## Durable product memory

- For every non-trivial Whip investigation, user-feedback item, design decision, implementation, bug fix, migration, QA campaign, or release, use the personal `$maintain-whip-memory` skill when available.
- Begin by reading `docs/product-memory/INDEX.md` and searching the linked ledgers for relevant features, identifiers, files, and user language.
- Record new user feedback and acceptance criteria before implementation. Update findings, decisions, implementation evidence, verification, release state, residual risks, and the index before concluding.
- Treat the memory as a durable map, not as proof. Reconcile it against current code, tests, schemas, and observed device behavior. Preserve superseded history instead of silently rewriting it.
- If the personal skill is unavailable, follow the same protocol directly from `docs/product-memory/INDEX.md` and its linked ledgers.
- After each coherent, independently understandable and revertible chunk is verified, update its memory, stage only files/hunks belonging to that chunk, create a focused commit, and push it to the configured upstream before starting substantially different work. Do not batch unrelated work into a final mega-commit.
- Never absorb unrelated dirty-worktree changes, force-push, rewrite history, bypass hooks, or claim delivery when commit/push failed. Report authentication or upstream blockers with the local commit preserved.

## Physical-device files

- Never write screenshots, UI hierarchy dumps, test output, traces, helper scripts, or other development artifacts directly to `/sdcard`, `/storage/emulated/0`, or `/data/local/tmp`.
- Use `scripts/device-artifacts capture`, `scripts/device-artifacts ui-dump`, or a subdirectory created by `scripts/device-artifacts prepare`.
- Shared debug artifacts belong under `/storage/emulated/0/whip-debug`; shell-only tooling belongs under `/data/local/tmp/whip-debug`.
- Normal user-initiated release exports belong in the user-selected Android document folder. When using shared local storage, use `/storage/emulated/0/whip` and never the storage root.
- Run destructive instrumentation only on a disposable emulator. Physical-device live inspection must not clear application data or run benchmark/instrumentation tooling.
