# Whip UI/UX/design consistency audit — 2026-09-07

## Outcome

The current 176-state product is accepted for the private owner-phone release
lane. Gym no longer has a user-selectable set-row dialect: every workout uses
one balanced, information-preserving execution design. The complete visual and
semantic review found two additional well-supported issues, both resolved: the
short next-set jump now meets the 48 dp interaction floor, and selected-Track
analytics is explicitly named **Track Insights** beside the workspace-level
**Insights** destination.

## Review scope

The review treated navigation, pages, cards, editors, menus, dialogs, empty and
populated states, loading/failure/recovery, destructive review, search,
selection, organization, Settings, Gym, Routines, and 5/3/1 as one product.
Every declared catalog surface was reviewed for hierarchy, reading order,
spacing, typography, component and action consistency, terminology, state
truth, target sizing, responsive behavior, accessibility semantics, and visual
balance.

| Family | Accepted surfaces |
| --- | ---: |
| Shared shell and interactions | 23 |
| Tasks | 22 |
| Habits | 19 |
| Goals | 15 |
| Tracks | 21 |
| Gym, Routines, and 5/3/1 | 47 |
| Settings | 14 |
| Organization | 15 |
| **Total** | **176** |

## Findings and decisions

### One Gym execution density

The retired compact branch saved a few pixels by hiding classification,
planned-state, and effort context; the comfortable branch preserved that
context but used looser geometry than Whip's current card system. Keeping the
choice would preserve inconsistent sessions and obsolete state throughout
Settings, preferences, backup, tests, and documentation.

The replacement uses the shared medium `WhipItemCard` surface, 12 dp horizontal
and 10 dp vertical inset, 6 dp direct/set rhythm, semibold primary set text, and
always-visible concise classification/planned/RPE and prescription support.
The focused active-set composer remains visually stronger because it is the
current input workspace. Set completion, editing, reordering, workout groups,
optional work, Routine structure, and 5/3/1 progression are unchanged.

### Frequent targets must not depend on copy length

The ordinary one-line next-set jump measured about 44 dp while the longer 5/3/1
variant exceeded 48 dp only because its text wrapped. A 48 dp minimum height is
now explicit and a Compose geometry regression protects the short state.

### Repeated labels need one owner or explicit scope

Workspace **Insights** summarizes visible Tracks; the nested destination
summarizes one selected Track. Rendering both as “Insights” made position carry
the entire scope distinction. The nested tab and page now say **Track
Insights**, with navigation and visual-catalog regressions covering both labels.

## Deliberate exceptions retained

- The active set remains an emphasized editor rather than a passive card row.
- Metadata chips, charts, calendars, destructive warnings, and domain pickers
  retain role-specific visuals while following the shared surrounding rhythm.
- Repeated per-entity actions such as Log and Reset are legitimate because the
  adjacent card identity supplies their scope.
- The global Search shortcut and local filter/search fields remain distinct
  controls despite related wording.

## QA evidence

- Full JVM behavior: 621 tests passed.
- Broad affected Android acceptance: 594 tests passed across five fresh batches
  on two matched API 34 x86_64 emulators, with zero failures or skips. Evidence:
  `build/instrumentation-results-zp6hcz`.
- Focused correction proof: Settings density absence, Track navigation, and the
  unrelated large CSV performance sentinel passed 3/3 in
  `build/instrumentation-results-tesbdw`.
- Final exact visual/semantic campaign: 65 owning tests passed across seven
  fresh batches on the same two emulators and exported exactly 176 PNG/XML
  pairs. Evidence: `build/instrumentation-results-3kdHZL`; review gallery:
  `/tmp/whip-whole-ui-20260907-final/index.html`.
- Affected replacement campaigns also accepted all 47 Gym surfaces and all 21
  Track surfaces in `build/instrumentation-results-UfpWK9` and
  `build/instrumentation-results-ktK4ik`.
- Final semantic checks found zero `NAF` interactive nodes. The short next-set
  jump measures exactly 48 dp on the catalog device, and the simultaneous Track
  navigation labels are distinct.
- Production/Android-test compilation, `lintDebug`, debug packaging, Play-asset
  validation, source review, retired-identifier scans, and `git diff --check`
  passed.

An earlier broad attempt is excluded: two renamed-flow tests still used stale
navigation and one 100,000-cell timing sentinel exceeded its wall-clock bound
under contention. The tests were corrected, the timing sentinel passed in the
focused replacement, and the entire fresh 594-test broad replacement then
passed. No product defect was waived.

## Compatibility and residual risk

Room schema 46 and data epoch 6 are unchanged. Portable backup version advances
from 25 to exact-match version 26 because the removed setting changes the JSON
contract; older complete archives remain intentionally rejected by Whip's clean
backup boundary. Existing workouts, sets, routines, program history, and local
owner data are not rewritten. This is private-development acceptance, not a
frozen Play Store candidate; store publication still requires `scripts/candidate`.
Subjective spacing and real-use training ergonomics remain open to normal owner
feedback after installation. Whip 0.3.61/code 67 was initially delivered as the
signed private APK because the phone was unavailable; after the user restored
connectivity, that exact artifact installed in place and passed the guarded
device smoke recorded in `VER-20260907-017`. The physical verification remains
release evidence rather than part of the emulator-based audit itself.

## Gold-standard completion addendum

The later whole-product completion run re-audited the same 176-state inventory
from the clean, device-verified Whip 0.3.62/code 68 baseline. It confirmed the
shared card and active-workout authorship fixes and found one additional product
P2: the 5/3/1 builder rendered consequential Four-Day, Beginners, and custom
schedule choices as unexplained chips. They now use one explanatory selected-card
grammar that makes weekly structure, included supplemental work, assistance, and
custom-exercise consequences comparable before selection; long-term plans also
explain the standalone Beginners/Classic boundary.

The final source passed all 621 JVM tests and all 963 Android tests fresh across
two matching disposable emulators. The final exact catalog passed 65/65 owning
tests and exported 176 PNG/XML pairs, all manually reviewed, with manifest
SHA-256 `0e3cfb73b7743c8e686e7c7ea9c6788440cee4b91cd5976867e7c217ea5c547d`.
See `IMP-20260907-022` and `VER-20260907-024` for complete evidence and explicit
exclusions. No additional supported P0/P1/P2 remained after that review.

Whip 0.3.63/code 69 was then built and signed from exact pushed source
`fe943ec`, installed in place on the explicitly selected owner phone, and
verified for signer and installed-hash equality, preserved first-install
identity, cold launch, foreground Activity, live process, and clean bounded
fatal/persistence logs. See `IMP-20260907-023` and `VER-20260907-025`.
