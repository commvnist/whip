# Content-sized Habit and Goal action groups

Activity controls now use a shared natural height within their group: max(48dp, the tallest direct child's native minimum intrinsic height at the actual cell width). Each action is measured once at that common width/height. Existing constant 12dp horizontal and 8dp vertical button padding remains; no fixed 64/80dp reserve or height multiplied by font scale remains.

Widths retain the accepted responsive rule. Normal phone rows use equal halves of available width minus the 12dp compact gap, rounded down in integer pixels. At font scale 1.5 or above, a group uses one full-width column only if its half width is below 140dp times font scale. Vertical gaps remain micro 4dp. The existing WhipActivityActions helper owns the small native Compose Layout; its card, numeric-inspector and opted-in dialog hosts emit actions directly. Disabled hosts retain their prior layouts and callback ordering. Terminal Goal confirmation keeps end alignment and Cancel before its emphasized confirm action. Checkbox square, gap, checked role and behavior remain.

There is no Home/card spacing change. WhipItemCardBody already uses 12dp horizontal and 10dp vertical insets with 6dp content spacing. Builder summary-to-action spacing is 2dp plus micro 4dp, and Home sibling spacing is 8dp. The oversized 80dp activity reserve was the concrete density defect.

The integration owner committed stable source as2bdf490639a7a81f29730bb6389b63abe443b590. production-diff.patch records the four focused production files. source-manifest.json hashes 563 build inputs; its SHA-256 is342ae7f2774073e554be5e572f2aa20890a32c00ed7417b42d4fe1d0caf64b0c. Comparing with the preceding 47e61384 pair changes exactly those four production inputs and ActivityActionLayoutUiTest.kt. The QA fixture is frozen at bf771907335954cc9f5b17f5f27e0331824f41929e27cbc9d138823aa3d87051, with its validated Goal data unchanged. It checks local sibling equality, full glyphs, normal same-row pairs and persistence; the short normal Duration pair must measure 48dp, and cross-host equality is removed.

Only one stable affected batch ran. scripts/check --ready --path app/src/androidTest/java/com/whip/app/ui/ActivityActionLayoutUiTest.kt passed in 11.604 s. Its explained route selects no JVM suite, whole Android suite or static profile. Explicit source guards passed in 0.647 s. The same single lint/debug/test package invocation passed in 198.575 s. Lint reports zero errors, 90 warnings and 15 hints. No old valid native/JVM case, suite or speculative unit test was repeated.

The immutable pair is under /root/repos/whip/build/action-sizing/2026-09-30/frozen-content-height/:

| APK | SHA-256 |
| --- | --- |
| app-debug.apk | 6119f70fdfdf64fbcf09942fb9be7d4b8b80b0b28fad035ee5c1b9340bd82ea5 |
| app-debug-androidTest.apk | 4369517a3f6e3956cc3879f72a9df4fe5b1c3ac1a411cafc204f5de846cb08ad |

QA owns the two existing phone-matched normal/native200% sizing calls on 5558, and design owns the bounded visual captures on 5554. The pair was announced when assembly finished so those checks could proceed while lint completed. This lane adds no native case or phone action. No source change, additional build, uninstall, data reset, commit or push is planned from this lane. The existing personal receipt remains byte-identical at 05e34843fb7926a88d04b25e3c4f0e926e9eb85a1f5f3a84d85a93f858e6adbe. The final build/frozen receipts confirm the single invocation passed; every source input and both finished Gradle outputs still match the immutable pair.
