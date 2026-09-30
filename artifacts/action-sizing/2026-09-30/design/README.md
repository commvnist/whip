# Equal action sizing - paired design evidence

Same native fixtures as the action-layout pass: `Design%20Focus` (duration 0 s), `DesignCheckIn` (Pending), and `DesignGoal` (target 100, no observations, active 0%). No timer, entry, check-off or closure was saved during capture; clean entry forms were cancelled.

Exclusively owned device: `emulator-5554` / `whip_api34`. Source HEAD at start: `16f1146859614146798745885f7a7726f482d495`. Both APKs were independently SHA256-checked; final installed in place without clearing data:

- Baseline app: `3cc019a94838fa488ab2b07f7a0950e537b7c963de69ab401909367b3cb50024` (accepted action-layout refinement).
- Final app: `b264e33f81f2d3e6021d5da8f1202bc02797f4e90a4a267d340afb33bbd954a8`, frozen at `build/action-sizing/2026-09-30/frozen/app-debug.apk`.
- Production build source manifest: `98fd19dff604317ff70db50dc2ed8b2028644b9bfc03bf65e1da483e0b4e3ead`, 563 inputs. [Frozen receipt](../implementation/frozen-receipt.json) and [source manifest](../implementation/source-manifest.json) identify that app; later receipt changes concern QA test inputs only.

| Configuration / flow | Before | After |
| --- | --- | --- |
| Compact 1080x2400 / density 420 / font 1.0 - Habits | [Before](before-habits-compact.png) | [After](after-habits-compact.png) |
| Compact - Goal | [Before](before-goal-compact.png) | [After](after-goal-compact.png) |
| Wide 2400x1600 / density 420 / font 1.0 - Habits | [Before](before-habits-wide.png) | [After](after-habits-wide.png) |
| Wide - Goal | [Before](before-goal-wide.png) | [After](after-goal-wide.png) |
| Compact 1080x2400 / density 420 / native font 2.0 - Habits | [Before](before-habits-large.png) | [After](after-habits-large.png) |
| Native 200% - Goal | [Before](before-goal-large.png) | [After](after-goal-large.png) |
| Compact duration entry | [Before](before-duration-form-compact.png) | [After](after-duration-form-compact.png) |
| Compact goal entry | [Before](before-goal-form-compact.png) | [After](after-goal-form-compact.png) |

Supplemental finals: fully scrolled Check In at [wide](after-checkin-wide.png) and [native 200%](after-checkin-large.png), plus [native 200% Goal entry](after-goal-form-large.png). Every screenshot has a same-name saved native XML hierarchy. At 200%, related cells stack; Check In requires scrolling rather than fitting beside all Duration controls in one viewport.

Clickable ancestor bounds are recorded in [baseline CSV](before-button-bounds.csv) and [final CSV](after-button-bounds.csv). Baseline normal cells varied by content: Start Timer 298x126 px, Enter Duration 342x126, Goal Log Progress 326x126, Complete Goal 349x126, Check In 259x126; form Cancel was only 168x126. At native 200%, widths still differed and Check In remained 126 px high versus 133 px for the paired button actions.

All fully visible sampled final cells measure **420x168 px** at normal font and **840x336 px** at native 200%: the approved 160x64 dp control size scaled by max(fontScale, 1), with width bounded by the container. This includes short/long card labels, filled/outlined siblings, form submit/Cancel and the full Check In toggle target. The checkbox glyph itself remains smaller. `after-habits-wide` exposes only 420x95 px of Check In at the viewport edge; its CSV row is explicitly `viewport-clipped` and is excluded from size comparisons. The full [scrolled wide Check In](after-checkin-wide.png) and XML supply the authoritative 420x168 px measurement.

The consultant personally reviewed compact, wide, native 200%, scrolled Check In and large Goal form originals and accepted equal cells, readable labels, leading placement, filled primary/outlined secondary hierarchy, stable status/navigation and preserved toggle meaning. Inspector long-label evidence and the test-reported text-overflow diagnostic belong to the independent QA lane; this index does not claim their outcome or global source acceptance.

Original settings were restored and verified after both matrices: physical 1080x2400, density 420, font_scale 1.0. Fixture values remain 0 s / Pending / active 0%; captures only opened and cancelled clean forms or scrolled. No phone action, data clearing, application/test source edit or commit was performed by this design lane. Native screenshots and saved hierarchies supply bounded visual evidence; automated tests, TalkBack speech and phone behavior are outside this lane.

Final consultant acceptance: all three sizing configurations and the separate 200% keyboard check passed. Root independently accepted the final long-label/Goal/footer originals. No sizing or readability blocker remains.
