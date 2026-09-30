# Habit and Goal activity pairs on normal phones

Normal activity rows now allocate two equal cells from their available width and the existing `WhipSpacing.compact` 12dp gap. Allocation is rounded down in integer pixels so odd-width rows still fit both cells. At native font scale 1.5 or above, rows use one full-width cell only when the half-cell is narrower than 140dp times font scale. Every affected cell uses height 80dp times max(font scale,1), independent of its label. Scoped button padding remains 12dp horizontal and 8dp vertical; unrelated controls retain their defaults.

The existing opt-in context covers Habit/Goal activity cards, numeric inspector controls, leading inspector docks, logging/correction footers and Goal terminal confirmation. Numeric card controls emit directly into the existing activity row, yielding normal ordered pairs +1/+1000000000, Add Amount/Set Total, and decrement/Undo Last Entry for the focused fixture. Primary/secondary styling, checkbox square/gap/role, leading alignment, labels, state guards and callbacks stay intact. The correction dialogs also emit Delete/Cancel directly into their existing outer footer, keeping Save Changes/Delete together at normal scale and Cancel reachable on its next equal cell.

QA owns the frozen actual-Activity assertions and emulator5558; design owns paired phone-matched captures on5554. This lane owns production edits and the shared immutable app/test package. The only tracked pre-existing user receipt remains byte-identical; its SHA is in scope.json. No phone operation, data reset, uninstall, commit or push is part of this lane.

The selected readiness explanation routes only ActivityActionLayoutUiTest: no JVM suite, whole Android suite or static profile. The exact readiness invocation passed in 11.615s; explicit source guards passed in 0.755s. Raw commands/results are retained in ready-explain.log, ready.log, source-guards.log and build-receipt.json. The initial lint/debug/test package passed in 217.036s. The consultant then identified the two nested dismiss rows; after flattening them, only the affected compile/lint/package continuation ran and passed in 200.984s. Its result is retained separately in footer-continuation.log and footer-continuation-receipt.json. Earlier valid suites and native cases were not repeated.


The final production/test source is committed by the integration owner as `47e61384e087692b9a745449eaa7a8133f9bba6e`. No production source changed after freeze. `production-diff.patch` retains the four scoped implementation edits, and `source-manifest.json` hashes all 563 build inputs. Its SHA-256 is `7bdba7d8f2ef8b355d0e2cc82315430da21e79804ae7d44174f67b5d40ae0ea8`.

The immutable pair is under `/root/repos/whip/build/action-sizing/2026-09-30/frozen-phone-fit/`:

| APK | SHA-256 |
| --- | --- |
| app-debug.apk | `c419686654ba579d951982be717ec89703033e50be1cd354d22e482bcca5f5b2` |
| app-debug-androidTest.apk | `55d9f2fc5664329606517aadce64362f3816a5ab4f75740c2760a18cf0c5d387` |

The pair was announced when assembly completed, while corrected lint was still pending; the final frozen receipt now confirms that continuation passed. Frozen APKs equal the finished Gradle outputs, and every source-manifest input still matches. Lint reports zero errors, 90 warnings and 15 informational findings. The pre-existing user receipt SHA remains `05e34843fb7926a88d04b25e3c4f0e926e9eb85a1f5f3a84d85a93f858e6adbe`.

QA owns only the two focused existing sizing methods at the verified 360dp phone viewport: normal and native200%. Design owns the four planned paired screenshots. Those independent results remain in their own lanes; this implementation lane adds no native case, phone operation or evidence expansion. No further build or source edits are planned unless their bounded review identifies a defect.
