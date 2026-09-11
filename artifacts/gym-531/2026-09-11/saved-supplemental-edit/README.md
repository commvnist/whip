# Saved 5/3/1 supplemental edits

FB-20260911-003 / FND, DEC-20260911-002 / IMP, VER-20260911-004. Baseline: clean pushed `8acd2520241d8b888ac1358bac0c3cff56776834`, installed Whip 0.3.70/code 76.

## Reproduction and fix

The existing Program Structure selector changed only the selected phase. The native baseline saves BBB successfully for week one, then fails because week two still contains FSL sets (`EPFaoA`, assertion “Week 2 must use the saved BBB choice”). This is an editing-scope inconsistency; the repository correctly persisted the submitted one-week change.

Supplemental Work now defaults to every training phase with the selected Standard, Leader or Anchor role. Existing shared filter chips expose that scope and an explicit This Phase Only override; accompanying text identifies the affected phase numbers. Protocol phases remain individually scoped. A shared helper uses the existing phase generator, preserving the original Main/Joker set objects and their ordering while replacing only supplemental work. Existing saved routines require an explicit selection and Save; no installation-time rewrite or persistence migration is introduced.

## Verification

- Focused Gym/5/3/1 JVM profile: 88 methods across six suites pass, including two new exact tests for BBB/FSL sets and percentages, retained Main/Joker notes/rest/order, untouched other exercise and protocol work, one-week overrides, independent Leader/Anchor roles and alternate-BBB phase removal.
- `ANDROID_SERIAL=emulator-5554 scripts/qa-targeted --profile gym531 --emulator`: all 89 Android methods pass with zero failures/skips/reuse in `IHyJgR` (3m37s).
- Native saved-edit journey completes both original week-one workouts, edits Bench during week two, saves BBB across all three training weeks, recreates/reopens the editor and starts the next workout. Bench receives five sets of ten at 50 kg; Zercher retains its original FSL prescriptions. Non-supplemental prescriptions, program phase/day/cycle position and every previously performed set remain exact.
- Wide `G2ntQ7` passes the complete journey. Final fixture additions distinguish the saved all-weeks view from the explicit current-phase override and verify override state restoration. Exact replacements `umuc4v` (wide, 27s) and `fWk1l5` (phone, 30s) pass. Only that Android fixture changes after the 89-method run; production inputs remain identical.
- Final `scripts/check --ready` exits zero: 389 JVM methods/44 suites, zero failures/errors/skips, Android compilation, lint and debug packaging. The lint/build stage takes 8m10s; the original run finishes without interruption or further source changes. Inventory is 657 JVM + 1079 Android = 1736 tests; catalog lint passes 541 required states and zero pending selectors.

## Failed checks and evidence limits

The first two baseline attempts used an incomplete or incorrectly capitalized selector description (`7vBeOF`, `I7u6JS`); the third asserted the asynchronous saved receipt too early. Correcting the exact label and waiting for the receipt exposes the real week-two regression in `EPFaoA`. The first fixed run `LerBRT` passes persistence/history/next-workout checks, then fails a display assertion against a tag absent from the compact Set branch; its replacement checks the shared rendered Set card. All initial failures and accepted replacements are retained in compressed logs and `android-results.json`.

An initial saved-versus-unsaved inspector pair was pixel-identical, as its displayed prescriptions were unchanged. It is superseded by the final distinct scope/override and reopened-state captures; no PNG is edited. `phone/` contains three final originals at 1080×2520/480 dpi (360 dp); `wide/` contains three at 1800×1200/160 dpi. All six are personally reviewed: complete scope controls and affected-week text, correct selected exercise and BBB prescription, then the visible 50 kg × 10 supplemental Set. Scrolled context above/below these controls is outside this bounded visual review. XML has normalized whitespace only, is valid and has no NAF nodes. Capture hashes are recorded; all 514 final implementation/test/build/harness input hashes match.

The task emulator's display overrides are reset and it is stopped. No physical phone, release, owner data, schema 46/data epoch 6/backup 26 change or closed-audit resumption occurs. Phone 0.3.70 remains the previous release; this fix is verified source for a subsequent requested release.
