# Content-height action pairs

Source `2bdf490639a7a81f29730bb6389b63abe443b590` passed both approved methods on emulator-5558 / whip_api34_qa3 at 1080×2520, density 480 (360dp width).

| Exact ActivityActionLayoutUiTest method | Android font scale | Result | Raw evidence |
| --- | --- | --- | --- |
| relatedActivityButtonsShareSizeAndKeepFullLabels | 1.0 | Passed 1/1, 90.581s | [normal](../native/phone-content-normal/result.json) |
| largeTextActivityButtonsShareSizeAndKeepFullLabels | 2.0 | Passed 1/1, 131.243s | [large](../native/phone-content-large/result.json) |

The known single-line normal Duration pair measures 48dp. Siblings within each group have equal widths/heights; normal ordered pairs share a row. Both journeys retain complete-character, glyph/line-fitting, no-ellipsis, 48dp touch minimum and actual font-scale checks, including long outside-schedule inspector labels. Real duration 12, numeric +1 and one Goal completion entry persisted; correction and completion Cancel/explanation remained reachable. The valid fixture was unchanged. No failed attempt or replay was needed.

Each raw folder includes ten fresh original PNG/XML pairs and installed-package hashes. App SHA256: `6119f70fdfdf64fbcf09942fb9be7d4b8b80b0b28fad035ee5c1b9340bd82ea5`. Test SHA256: `4369517a3f6e3956cc3879f72a9df4fe5b1c3ac1a411cafc204f5de846cb08ad`. QA source SHA256: `bf771907335954cc9f5b17f5f27e0331824f41929e27cbc9d138823aa3d87051`.

[Restoration](restored-device.json) confirms physical 1080×2400, density 420, font 1.0. Existing records were preserved. No additional native methods or broad suites ran. Earlier fixed-height passes remain preserved with their original source provenance.
