# Home summary and daily-action improvement

Related: `FB-20260908-006`, `FND-20260908-013`, `FND-20260908-014`, `DEC-20260908-010`.

All images contain disposable emulator fixtures. Production data, schema, backup format, release version, and physical devices were unchanged.

PNG bytes are retained unchanged. The checked-in XML copies normalize CRLF to LF for repository whitespace rules; node content and bounds are unchanged. The external capture manifests hash the original exported files.

| Evidence | What it proves |
| --- | --- |
| [Before populated](before-populated.png) / [hierarchy](before-populated.xml) | Baseline `4f4a5dc` application source: stacked summary links and prominent Review row place the first Task card at y=1232. |
| [After populated](after-populated.png) / [hierarchy](after-populated.xml) | Responsive summaries and the secondary header Review action move that card to y=982 in the same 1080×2400 configuration. Card dimensions and its three actions remain unchanged. |
| [Before skipped](before-skipped.png) / [hierarchy](before-skipped.xml) | One completed and one skipped Habit incorrectly announce `Habit Progress: 1 of 2`. The new regression fails against this source. |
| [After skipped](after-skipped.png) / [hierarchy](after-skipped.xml) | The same scenario announces `1 of 1 complete` plus the separate neutral `1 skipped`. The regression also checks recreation, undo, and navigation to the restored Habit. |
| [Large text / RTL](after-large-rtl.png) / [hierarchy](after-large-rtl.xml) | A 320 dp summary container at 200% text stacks summaries, preserves neutral/timer context, and retains English numeric phrase direction within RTL layout. This is a component configuration, not a claim about all 320 dp application journeys. |
| [Clear day](after-clear-review.png) / [hierarchy](after-clear-review.xml) | A clear day with completed work has one explanatory Review Progress action and no duplicate header action. |

Accepted after evidence was exported by the five shared-family owner tests to `/tmp/whip-astra-home-shared-final2-20260908`; all 26 required PNG/XML pairs passed accounting. Execution: `build/instrumentation-results-xwOcjO`; manifest SHA-256 `ab9b3da61ce7facfaeea057eb2b9db3f5366b220b1486e1c1d6d54a52d9ff9f7`.

The preceding `final` capture is not accepted for clear-day proof: resetting its fixture re-enabled onboarding and an actual welcome dialog covered the intended state despite passing underlying Compose assertions. The fixture now restores completed setup and explicitly rejects that dialog; the corrected original-resolution image above was inspected.

The first large-text fixture also revealed an unpainted background and RTL phrase reordering. Both were corrected; the test now uses a themed Surface and asserts the text layout's paragraph direction.

[System-bar mismatch observation](system-bars-mismatch.png) is a separate open finding (`FND-20260908-015`): Android-light / Whip-dark can leave status icons dark. The catalog forces Android dark and therefore does not validate opposite-theme window contrast. This image is retained as investigation evidence, not an accepted appearance result.
