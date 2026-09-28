# Major-component UX overhaul evidence

The [accepted plan](../../../docs/quality/MAJOR_COMPONENT_UX_UPGRADES_2026-09-27.md) and four review matrices define the implemented scope. [Browse original screenshots](gallery.html). [VER-20260927-025](../../../docs/product-memory/VERIFICATION.md) records the authoritative final outcome.

## Scope and accounting

Three Astra/high agents reviewed roughly 100 domain screen/workflow rows plus shared interactions. All 19 accepted workstreams and the reproduced short-screen repairs were implemented. The source inventory grew by six JVM and 29 Android methods: **668 JVM + 1,151 Android = 1,819**. Inventory is distinct from executed checks.

- Affected readiness executes **409 JVM tests in 48 suites**, Android-test compilation, debug lint, assembly and static/diff checks. The final receipt is retained in `verification/readiness.log`.
- **171 distinct selected Android methods** are tracked in [latest method outcomes](verification/android-latest-method-results.tsv). This includes 38 Habit repository methods, both backup round-trips, ordinary/large-text authoring, history, recovery, Settings and shared semantics. [Every attempt](verification/android-attempts.tsv) and its original XML remain available; failed batches are never represented as wholly passing.
- Four additional constrained-layout scenarios were exercised: Task filter header, dense Calendar, Habit timer with IME, and active Gym lane. Their accepted runs are short-2 (Task/Timer), short-3 (Gym), and short-7 (Calendar). Some methods overlap the normal-height selection.
- Catalog lint reports **524 required captures, zero platform exceptions and zero pending selectors**. This is inventory integrity, not 524 fresh visual acceptances.

## Reproduction

All runs use synthetic data and an explicit disposable Android API 34 emulator. Ordinary journeys use its original 1080×2400 display at density 420. The four constrained cases use 1080×1600 pixels (about 411×610 dp) and actual Android 200% text. Overrides are restored after each run; normal/large-text methods manage font scale through the existing test rule.

Readiness command: `scripts/check --ready`. Native batches: `ANDROID_SERIAL=emulator-5554 scripts/qa-targeted --android CLASS[#METHOD] ...`, using the retained `verification/ux-overhaul-native-*.txt` selector files. Later files contain only failed or not-yet-executed selections. Final exact cleanup/copy check: `com.whip.app.WhipComposeSemanticsTest#gymConfigurationControlsUseProgressiveDisclosure`. Catalog command: `timeout --kill-after=3s 55s scripts/ui-catalog lint`. Routine diagnostic timeouts were incomplete, never accepted.

## Original captures

- `before/`: initial failed Task filter, Habit timer and Gym active-lane images. The first Task image still shows the dismissing filter dialog and is diagnostic only. The missing initial Timer XML reflects its failed native unnamed-interaction guard.
- `diagnostic/`: rejected digit-wrapped Calendar and Gym chart-scroll diagnostics. The Calendar initially passed a weak assertion but failed direct visual review; final assertions require one complete line and no text overflow. Gym diagnostics proved the chip was below the vertical viewport and that its nearest horizontal scroll owner could not reveal it; a real vertical swipe enabled the strict touch-and-workout drill-down test.
- `after-short/`: four accepted, personally inspected constrained layouts with paired accessibility hierarchies.
- `after/`: fresh domain and shared-control captures. Isolated chart/choice fixtures and component-editor captures show the component under test; they are not whole-app layout screenshots. Some long forms are captured mid-scroll or with IME open. Native tests separately establish offscreen-action reachability.

PNGs are unmodified. XML files are original accessibility hierarchies. [Capture provenance](capture-provenance.tsv) records original host filenames and hashes, including MediaStore duplicate suffixes. The gallery preserves diagnostic distinctions. Representative originals across every area were inspected directly; this does not claim every retained image or every catalog state received a new visual audit.

## Important outcomes and limits

The UI-authored blank-amount Habit note now persists as a true nullable Habit log, never a fabricated zero. Numeric↔note transitions, stable identity/provenance, repository recreation, undo, strict numeric validation, backup restoration and CSV note inclusion pass. Calendar dates/counts at 200% remain single-line; future one-off Tasks beyond the calendar projection remain reachable in Upcoming List. Historical Goal closures, selected units, Track field identity and legacy Routine controls remain intact.

This is affected-scope verification, not a complete Android inventory, full visual catalog, Play candidate or owner-phone release. Notification-settings return refresh is source-reviewed; an OS permission-return journey was not run. Room schema, backup format and release version remain unchanged. Subjective design acceptance awaits normal owner use.
