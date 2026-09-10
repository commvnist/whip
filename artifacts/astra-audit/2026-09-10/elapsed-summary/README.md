# Complete visible elapsed Goal summaries — 2026-09-10

FND/DEC/IMP/VER-20260910-005. Opening source: `dabab4b`. Goal collection and Home summaries now use the complete authored elapsed label in the existing small full-width text role. The old three-part overview formatter is removed. Expanded/Insights metrics, exact instants, calendar calculations, selected-unit persistence, resets, history, versions and schemas are unchanged.

## Evidence and acceptance

- Baseline Z5SzSq runs the new normal-text test on unchanged production. The complete contentDescription assertion passes, but the complete visible Text assertion fails. Existing normal native Goal and wide Home originals show the same omission; they are retained under before-api34/37 from VER-20260910-004.
- Final 0slSQl passes 46 fresh API 34 Android methods, zero failures/skips/reuse: GoalRepositoryTest, GoalSecondaryMutationUiTest, ElapsedGoalTimeUiTest (normal and strengthened existing large-text visibility, editor and reset), 15 ProductivityCardDesignUiTest neighbors, and real Goal/Home page catalogs. The command also passes 34 JVM tests in GoalRulesTest/UiDesignArchitectureTest.
- API 37 direct instrumentation passes the two visible-summary contracts and real Goal/Home catalogs in 42.355s, `OK (4 tests)`. Both platform runs use the same unchanged production/tests. Native catalog captures use actual system font scale 1.0; the scoped Compose contract also verifies existing 2.0 local text behavior. This adds no per-screen enlarged-resolution campaign.
- `scripts/check --ready` passes 37 fresh affected JVM tests in four suites, zero failures/errors/skips, compilation/lint/debug packaging in 2m26. The prior broader builder chunk's 346-test readiness is separate history. No whole-suite or release-candidate claim.
- `scripts/ui-catalog lint` passes: unchanged 389 states, no exceptions/pending selectors. Declared source counts: 632 JVM + 1,046 Android = 1,678; actual-font inventory remains 53.

## Commands

```sh
ANDROID_SERIAL=emulator-5554 scripts/qa-targeted \
  --jvm com.whip.app.domain.GoalRulesTest \
  --jvm com.whip.app.ui.UiDesignArchitectureTest \
  --android com.whip.app.ui.ElapsedGoalTimeUiTest \
  --android com.whip.app.ProductivityCardDesignUiTest \
  --android com.whip.app.GoalRepositoryTest \
  --android com.whip.app.ui.GoalSecondaryMutationUiTest \
  --android com.whip.app.VisualCatalogPagesTest#captureGoalPageCatalog \
  --android com.whip.app.VisualCatalogPagesTest#captureSharedPageCatalog --emulator
```

Baseline uses the same wrapper with only `--android com.whip.app.ui.ElapsedGoalTimeUiTest#collapsedElapsedGoalCardKeepsEveryConfiguredUnitVisibleAtNormalText --emulator`.

API 37 installs the debug and debug-test APKs with `adb -s emulator-5556 install -r`, then runs `adb -s emulator-5556 shell am instrument -w -r -e class '<comma-separated selectors>' commvne.com.whip.app.debug.test/androidx.test.runner.AndroidJUnitRunner`. Selectors are the normal/large `collapsedElapsedGoalCardKeepsEveryConfiguredUnitVisible...` methods and both catalog methods above; api37.log records exact executed names.

## Visual review and limits

All twelve retained originals were personally inspected: six before and six final. Complete six-unit status fits the ordinary Goal row on both phone and wide layouts; wide Home now agrees. Expanded content/controls remain continuous and unchanged. The phone Home capture's Goal status lies below the initial viewport, so it proves adjacent layout only. Before/final fixtures have different current event timestamps; they demonstrate presentation, not a mutation comparison.

PNG bytes are unchanged; signature/chunk CRCs/zlib/IEND and paired XML parsing pass. Capture manifests retain native filenames/hashes. Text-normalization.json records original/retained hashes where line endings/trailing whitespace were normalized. Native XML, failed/final logs, focused/readiness JVM XML and final source/test/APK hashes are included. Zero NAF nodes do not establish TalkBack acceptance.

Two disposable emulators only: API 34 1080×2400/420dpi and API 37 2560×1800/320dpi. No physical-device operations, release or publication. Navigation/settings/execution builders and the broader app-quality objective remain open. Git history records normal main/upstream delivery.
