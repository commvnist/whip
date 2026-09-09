# Short-dialog reading and choice discovery

Scoped evidence for FND-20260909-009 / DEC-20260909-008 / IMP-20260909-009 / VER-20260909-009. Baseline is pushed `4e50bef`. All runtime work used one explicit disposable API 34 emulator; no physical-device or release operation occurred.

Long destructive, template, and backup headings now share their existing content scroll. Actions remain fixed. Templates use concise names and show optional guidance after choices; backup actions appear once with their meaning, retaining record/export context, compatibility, complete metadata, and the second replacement gate. Shared choice-list height scales with text within parent constraints.

The 320×360 dp destructive fixture at actual 200% text gains a 200 dp reading viewport, previously about 59 dp (525 versus 155 pixels at density 2.625). The complete recovery error fits after scrolling. At actual 320% text, the first template is visible before scrolling and the last remains readable. The Area move fixture shows four initial destinations at 200% text.

## Evidence accounting

- `before-*` contains four prior dark-theme PNG/XML pairs from the committed dialog-font-coverage evidence.
- `after-light-*` contains six focused light-theme pairs. `intermediate-light-settings.restore-preview.large` is a seventh inspected intermediate: the final backup preview subsequently restored record count beside its export date and corrected singular metadata grammar.
- Bare surface names are 16 personally reviewed final dark-theme pairs, individually described in [review.tsv](review.tsv). Fifteen have scoped layout acceptance. `organization.area.permanent-delete` remains unaccepted under FND-20260909-011: its ordinary-text impact summary crowds the first choice. Whole Area, backup, Task, accessibility, and platform acceptance is not implied.
- The 38-owner shared/Settings/organization/template capture passed and exported 92 states. Its complete manifest is retained as `capture-manifest.tsv`; only the selected reviewed images are preserved here. Capturing the remaining states does not establish visual acceptance.
- After the backup record/context refinement, both backup owners passed again and exported four replacement states. These four bare backup images come from `backup-final-manifest.tsv`, superseding their earlier capture generation. Other preserved images use the 92-state capture.
- Original PNG bytes and original exported PNG/XML hashes were verified against each corresponding manifest. Preserved XML changes CRLF to LF only. Manifests retain the original export hashes.

## Verification

Nine focused tests and 86 adjacent Android regressions passed with zero failures, skips, or reused batches. They cover complete initial choices/error bounds, stable actions, exact template values, editable template-to-Save callback, dialog themes, deletion guards, backup choices/second gate, organization, and recreation. The new template journey proves UI draft/callback behavior; it does not claim a persisted-save round trip.

The first focused compile rejected a missing test import before instrumentation; it was corrected. The first artifact pull rejected a nonexistent destination directory before export; creating the directory resolved it. Neither failure is counted as runtime acceptance. Exact commands, run paths, readiness outcome, and limits are in VER-20260909-009. Android font scale restored to 1.0.

The Task editor keyboard/header issue (FND-20260909-010), Area deletion neighbor, native-window Search text coverage, and complete whole-product goal remain open.
