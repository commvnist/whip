# Equal action sizing QA — 2026-09-30

**Passed:** both QA sizing methods on API34 `emulator-5558` / `whip_api34_qa3`: compact1.0 (73.903s) and compact2.0 (77.711s). The implementation lane also passed the normal sizing method at wide1920×1200/density240 and retained its earlier200% keyboard/draft pass. Combined scope: four passing executions across three methods. No further tests or captures are planned.

| QA method | Passing evidence |
| --- | --- |
| `ActivityActionLayoutUiTest#relatedActivityButtonsShareSizeAndKeepFullLabels` | [Compact result](native/final-compact-consistency-v2/result.json), ten PNG/XML pairs |
| `ActivityActionLayoutUiTest#largeTextActivityButtonsShareSizeAndKeepFullLabels` | [200% result](native/final-large-replay/result.json), ten PNG/XML pairs |

Actual captured cells measure420×168px at density420/font1.0 (160×64dp), and840×336px at font2.0 (320×128dp). This holds for Start Timer/Enter Duration, the long outside-schedule dock, Save Changes/Delete/Cancel, and terminal Cancel/Complete Goal. Native checks also verify numeric billion siblings, full character/glyph/line/paragraph fitting, no ellipsis, actual Android scale, leading card placement, touch targets, and reachable controls. Real interactions save duration12 and one Goal completion entry, preserve Active status, open correction History, and cancel manual completion. The valid Consistency fixture targets three completions per week across twelve periods.

Representative final200% originals: [long inspector](native/final-large-replay/action-sizing.qa.outside-inspector.large.png), [correction footer](native/final-large-replay/action-sizing.qa.goal-correction-footer.large.png), [completion and explanation](native/final-large-replay/action-sizing.qa.goal-confirmation.large.png).

Production APK is unchanged throughout: `b264e33f81f2d3e6021d5da8f1202bc02797f4e90a4a267d340afb33bbd954a8`. Compact/wide passes use test APK `7f9625e5c208ff527b9f743806eda46d93ac07b64ded76c43f837d86cbeb186f`. Final200% uses `b6ac3e3c1217c48eddbc9ae767a714f17c3ccecaeba622ebcd44bda9932f83b3`; only fixture gestures changed afterward. Final Activity test source: `781a85db858bb4917a059e61c3de81eba8efae1c0cb4ca8702535819494e85cb`.

Six failed QA attempts remain intact under `native/`: initial generic overflow flag; a catalog-validator diagnostic failure; the diagnostic confirming paragraph width356 versus fully fitting glyph width171.832 inside text172; incompatible OpenEndedTrend aggregation; incomplete Consistency setup; and an off-screen numeric tap at200%. These were fixture issues. Full-glyph assertions replace the misleading flag; exact-target scrolling fixes the gesture. Only the two new sizing methods tolerate the known whole-viewport unlabeled-neighbor catalog exception after PNG export; other capture errors propagate. All final QA pairs include XML. No product defect remains in this scope.

[Restored device](restored-device.json): physical1080×2400, density420, font1.0; emulator remains booted. Previous failed restoration metadata is retained with `native/final-large/`. No database clearing, uninstall, global ADB reset, or phone action occurred. Full-suite, TalkBack, locale, and additional-device certification were not run. Prior action-layout passes and unrelated work remain preserved.
