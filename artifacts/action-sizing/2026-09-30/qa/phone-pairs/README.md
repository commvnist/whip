# Phone-width action pairs

Accepted source: `47e61384e087692b9a745449eaa7a8133f9bba6e`. Both focused methods passed on emulator-5558 / whip_api34_qa3 at 1080×2520, density 480 (360dp width):

| Exact ActivityActionLayoutUiTest method | Android font scale | Result | Raw evidence |
| --- | --- | --- | --- |
| relatedActivityButtonsShareSizeAndKeepFullLabels | 1.0 | Passed 1/1, 68.297s | [normal](../native/phone-pairs-normal/result.json) |
| largeTextActivityButtonsShareSizeAndKeepFullLabels | 2.0 | Passed 1/1, 110.78s | [large](../native/phone-pairs-large/result.json) |

Normal Duration, Goal, numeric and footer pairs share a row with equal widths and heights. Both runs verify complete characters, glyph/line fitting, no ellipsis, real saved duration 12 and numeric +1, one completion entry, long outside-schedule inspector labels, correction controls and completion Cancel/explanation reachability. Each evidence folder contains ten original PNG/XML pairs and installed-package hashes. No failed attempt or fixture change was needed for this correction.

Frozen app SHA256: `c419686654ba579d951982be717ec89703033e50be1cd354d22e482bcca5f5b2`. Test SHA256: `55d9f2fc5664329606517aadce64362f3816a5ab4f75740c2760a18cf0c5d387`. Owned test source SHA256: `0d35d4ad3e425b184d555fe3ec4c0350a6e8c486ce5a3aa7917c07048bf88c02`.

[Restoration](restored-device.json) confirms physical 1080×2400, density 420, font 1.0. Existing records were preserved. Broad suites, additional devices and additional native methods were not run. Earlier fixed-cell evidence remains preserved in the parent QA index.
