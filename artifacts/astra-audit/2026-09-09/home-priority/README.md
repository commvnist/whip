# Home follows the user's starting choices

Related: `FND-20260909-005`, `DEC-20260909-003`.

After choosing Tracks and Gym during setup, Home now introduces those two
tools first. Tasks, Habits, and Goals remain available in the secondary group.
The guide uses the saved order of visible Home sections; choosing all sections
puts all five in the starting group without an empty secondary heading.

| Configuration | Evidence |
| --- | --- |
| Tracks/Gym selected, before | [PNG](../first-run/before-configured-home.png) / [XML](../first-run/before-configured-home.xml) |
| Tracks/Gym selected, after | [PNG](after-tracks-gym-home.png) / [XML](after-tracks-gym-home.xml) |
| All sections selected, after | [PNG](after-all-sections-home.png) / [XML](after-all-sections-home.xml) |

The original persisted setup journey failed its new Home-order assertion in
`build/instrumentation-results-82viwG`. Six focused tests pass after the change
in `build/instrumentation-results-grcYBS`, including real setup and reopening,
reordered selections, a single selected section, all sections, and every
destination callback. Existing card geometry, descriptions, navigation,
settings storage, and domain behavior are preserved.

Both after images were personally inspected. The fresh shared capture passed
12 owners and all 39 PNG/XML pairs in `build/instrumentation-results-dzU3Xa`;
the complete local gallery is `/tmp/whip-astra-home-priority-shared-20260909`.
Manifest SHA-256:
`e62da49c6336098b5e8f02a5ddb607e4450776a5e417d1a34e015f998c12cd73`.
PNGs are unchanged exports; XML line endings were normalized. This scoped
Home-priority acceptance does not close the remaining Home/adaptive or
whole-product audit. No owner phone or release was involved.
