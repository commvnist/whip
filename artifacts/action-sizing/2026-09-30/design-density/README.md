# Content-sized sibling actions - focused design evidence

This correction supersedes the 80dp cells in source `47e61384e087692b9a745449eaa7a8133f9bba6e`. Responsive equal widths remain; common sibling height is the tallest natural child at that width, with a 48dp touch minimum and fixed padding. Text scales; empty padding does not.

Synthetic device: `emulator-5554` / `whip_api34`, 1080x2520 / density480 / font1.0 (360x840dp). Fixtures unchanged: Duration0s, Check InPending, Goalactive0%. Personal images excluded.

- Before APK: `c419686654ba579d951982be717ec89703033e50be1cd354d22e482bcca5f5b2`.
- Corrected source: `2bdf490639a7a81f29730bb6389b63abe443b590`.
- Corrected APK: `6119f70fdfdf64fbcf09942fb9be7d4b8b80b0b28fad035ee5c1b9340bd82ea5`, independently checked before in-place emulator installation.
- Manifest: `342ae7f2774073e554be5e572f2aa20890a32c00ed7417b42d4fe1d0caf64b0c`; [immutable receipt](../content-height/implementation/frozen-receipt.json).

| Flow | Superseded 80dp before | Content-sized after |
| --- | --- | --- |
| Habit tab | [Before](../design-correction/after-habits-phone.png) | [After](after-habits-phone.png) |
| Goal tab | [Before](../design-correction/after-goal-phone.png) | [After](after-goal-phone.png) |
| Direct Home | [Before](../design-correction/after-home-phone.png) | [After](after-home-phone.png) |
| Wide Goal,2400x1600/d420/font1 | [Before](../design-correction/after-goal-wide.png) | [After](after-goal-wide.png) |

Each final screenshot has companion native XML. [Observed bounds](after-button-bounds.csv): routine controls/Check In are **426x144px (142x48dp)** versus426x240px before. Height falls32dp/40%. The same Home Duration card shrinks510→414px (170→138dp); its action block falls47%→35% of card height. Tab cards shrink558→462px. Both full Habit cards now fit one unscrolled Home frame. Home shows Habits only; Goal is directly reviewed in its tab, with no filter changes.

Independent visual review found equal siblings, full labels, compact one-line height, stable leading hierarchy/status/navigation and preserved checkbox spacing. QA's normal/native200 journeys passed separately1/1 each. Fresh reviewed originals show content-sized large controls and the longest inspector action fully wrapped in3 normal/2 large lines:

- [Normal long label](<../qa/native/phone-content-normal/action-sizing.qa.outside-inspector.normal (3).png>)
- [Native200 Duration](<../qa/native/phone-content-large/action-sizing.qa.duration-card.large (3).png>) and [Goal](<../qa/native/phone-content-large/action-sizing.qa.goal-card.large (2).png>)
- [Native200 long label](<../qa/native/phone-content-large/action-sizing.qa.outside-inspector.large (2).png>)

One current wide Goal frame verifies new layout placement: equal **572x126px (48dp)** siblings,32px gap, full labels and stable leading/status/navigation; before572x210px. Consultant accepted normal/Home/native200 originals. No further captures, automated tests or TalkBack speech check by this lane.

Original settings restored/verified:1080x2400 / density420 / font1.0. Fixtures remain0s/Pending/active0%; navigation only. No phone action, data clearing, source edit or shared build.
