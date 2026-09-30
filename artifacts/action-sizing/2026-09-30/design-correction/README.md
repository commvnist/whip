> Superseded after Nick rejected the80dp empty-space reserve; retained as before evidence for the compact-height redesign.

# Phone-width action pairing - focused design evidence

Fixed 160dp cells stack at the verified 360dp phone width. Corrected normal actions split the available row width less a 12dp gap equally, with common 80dp height. Large text stacks only when needed; height scales with font size.

Unchanged synthetic fixtures: `Design%20Focus` (0s), `DesignCheckIn` (Pending), `DesignGoal` (target100, no observations, active0%). Personal phone imagery is excluded.

Phone-equivalent emulator viewport: 1080x2520, density 480, font_scale 1.0 (360x840dp), exclusively `emulator-5554` / `whip_api34`.

- Baseline APK SHA256: `b264e33f81f2d3e6021d5da8f1202bc02797f4e90a4a267d340afb33bbd954a8`, independently verified from the installed package.
- Corrected source: `47e61384e087692b9a745449eaa7a8133f9bba6e`.
- Corrected APK SHA256: `c419686654ba579d951982be717ec89703033e50be1cd354d22e482bcca5f5b2`, independently checked before in-place emulator installation.
- Source manifest SHA256: `7bdba7d8f2ef8b355d0e2cc82315430da21e79804ae7d44174f67b5d40ae0ea8` (563 inputs), identified by the [immutable build receipt](../phone-fit/implementation/frozen-receipt.json).

| Synthetic flow | Before | Corrected final |
| --- | --- | --- |
| Phone Habits | [Before](before-habits-phone.png) | [After](after-habits-phone.png) |
| Phone Goal | [Before](before-goal-phone.png) | [After](after-goal-phone.png) |
| Wide Goal, 2400x1600 / density 420 / font 1.0 | [Reused baseline](../design/after-goal-wide.png) | [After](after-goal-wide.png) |

Each screenshot has companion native XML. [Before bounds](before-button-bounds.csv): 480x192px cells stack inside 888px phone content. [Corrected bounds](after-button-bounds.csv): phone siblings and Check In target **426x240px**, paired with 36px gap; wide Goal siblings **572x210px**, paired with 32px rounded gap.

Direct Home: [entry](after-home-phone.png) and [scrolled](after-home-phone-scrolled.png). Scrolled controls are complete; entry's 426x116px Check In is `viewport-clipped` and excluded from size comparisons. This Home configuration shows Habits only; direct Home Goal evidence is unavailable. Filters/data were preserved.

The consultant independently viewed and accepted corrected phone, Home, wide and native200 originals: equal geometry, readable labels, balanced 80dp proportion, leading hierarchy, stable status/navigation and checkbox spacing/role.

Independent QA's normal/native200 sizing methods passed separately on this source/app pair. Actual longest action `Mark Today Complete Outside Schedule` fits in three normal and two native200 lines. Large text requires scrolling for full card context. Reviewed QA originals:

- [Normal long inspector label](<../qa/native/phone-pairs-normal/action-sizing.qa.outside-inspector.normal (2).png>)
- [Native 200% Duration pair](<../qa/native/phone-pairs-large/action-sizing.qa.duration-card.large (2).png>) and [Goal pair](<../qa/native/phone-pairs-large/action-sizing.qa.goal-card.large (1).png>)
- [Native 200% long inspector label](<../qa/native/phone-pairs-large/action-sizing.qa.outside-inspector.large (1).png>)

Prior large-text captures describe the superseded layout. This lane does not claim TalkBack speech or phone behavior.

Original settings restored/verified: 1080x2400, density420, font1.0. Fixtures unchanged; navigation/scrolling only. No phone action, data clearing, source edit or build by this lane.
