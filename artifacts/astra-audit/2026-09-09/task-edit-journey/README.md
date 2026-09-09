# Existing Task editing through interruption

Baseline application source: clean pushed `40892ff`. Production behavior is unchanged by this chunk.

The earlier API 26 status-only frame showed a keyboard with the title below the body viewport. Fresh, settled Home → Task inspector → Edit journeys do not reproduce that as a persistent defect. API 26 automatically focuses the title; API 34 and the reviewed API 37 ordinary state open without the keyboard. Both behaviors preserve a readable field. A speculative keyboard-closed requirement was rejected.

Two real-app tests cover ordinary and actual Android 200% text: initial native field visibility, explicit keyboard input, draft recreation, durable save, and recreation/reopening. Exact entity comparison permits only the authored title and update timestamp to change. Complete native label/field bounds supplement title/exit and actual-font assertions.

- API 26: both tests pass, including the final additional reopened-field assertion (`astra-task-edit-api26-final`). The eight reviewed images are from the preceding successful two-test run on identical production; final test XML adds explicit reopened-bound verification.
- API 34: both tests pass in `eTgwbN`; the canonical final capture reruns the two owners in `PpUe4s` and exports all eight states, individually reviewed under `final/`.
- API 37: both tests pass in `KyCbyq`; ordinary opening and enlarged recreated-keyboard frames are individually reviewed under `api37/`.
- Readiness: 344 JVM tests in 34 XML suites, zero failures/errors/skips, Android-test compilation, debug packaging, lint and static checks pass. This is scoped acceptance, not full Android-suite execution or release qualification.

The 18 preserved images have individual scope notes in `review.tsv`. PNG bytes are unchanged; XML is normalized only to LF. `capture-manifest.tsv` is the original API 34 export (SHA-256 `9d7ed14f95d6a9ad7c9d77e672666a17cfdb40e5e6416e117762f9fbb54c8fe0`); `SHA256SUMS` covers the preserved tree.

The complete Task flow, wide-form composition, and whole-product audit remain open. In particular, the wide form still separates labels and trailing controls across a long horizontal span; this native edit-visibility check does not accept that design. No physical-device or release action occurred. See `IMP/VER-20260909-015`.
