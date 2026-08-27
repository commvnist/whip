# Whip full-product UX/UI/design goal audit

Status: completed with physical-device validation explicitly deferred
Baseline commit: `0394057` (`feat: unify responsive app navigation`)
Evidence dates: 2026-08-25 through 2026-08-26
Runtime evidence: disposable `whip_api34` AVD (Android 14/API 34), plus focused
compatibility smokes on disposable `whip_api26_small` and `whip_api37_large`
AVDs
Physical-device evidence: explicitly deferred; no phone discovery, deployment,
inspection, capture, or testing is part of this goal

## Outcome

Whip must feel like one fast, calm, capable product. A person who learns how to
navigate, inspect, create, edit, save, cancel, filter, disclose, confirm, or
recover in one domain must be able to predict the same interaction everywhere
else. Domain depth remains available, but it must not dominate a first or common
path.

This is the living brief, finding register, decision log, and completion record
for the goal. Older repository audits are useful provenance, not current visual
evidence.

## Product experience principles

1. **Orient, then act.** Every surface makes location, purpose, state, one next
   action, result, and recovery visible in that order.
2. **Learn once, transfer everywhere.** Equivalent decisions use the same
   control, label grammar, placement, feedback, Back behavior, and semantics.
3. **Value before configuration.** First use reaches a useful result before it
   asks for preferences that can safely retain defaults.
4. **Simple by default, complete on demand.** Required and frequent controls are
   direct; optional depth is adjacent, disclosed, and never hidden when active
   or invalid.
5. **Fast means fewer decisions and prompt feedback.** Interaction count,
   continuity, visible acknowledgement, and rendering performance are all part
   of responsiveness.
6. **Recognition beats recall.** Labels describe the user's decision; state,
   scope, units, examples, and consequences remain visible when relevant.
7. **Safe, local, and reversible.** Whip never silently discards work, disguises
   destructive effects, or weakens its local-first privacy promise.
8. **One canvas, many windows.** Compact, rail, dashboard, book-fold, tabletop,
   keyboard, and large-text layouts preserve hierarchy rather than merely fit.
9. **Warm identity, restrained interface.** User-owned emoji identify records;
   interface icons communicate actions. Color has stable semantic roles and
   decoration never competes with content.

## Primary users and jobs

Whip serves one local-first individual through different levels of depth rather
than treating each feature as a separate product.

| User mode | Primary need | Product obligation |
| --- | --- | --- |
| New or returning planner | Understand what Whip is and get one useful item into the system | Explain value in plain language and create the first useful result without setup work |
| Daily operator | See what matters now and complete or log it quickly | Make Today scannable and keep completion/check-in/logging one tap away |
| Outcome builder | Connect repeated action and evidence to a meaningful result | Make Goals, progress, pace, and next actions understandable without exposing storage concepts |
| Structured tracker | Capture reusable typed evidence and automate follow-up | Keep Track depth progressively disclosed, explicit, and auditable |
| Trainee | Log a workout without breaking training flow | Prioritize the next set, repeat entry, recovery, and timer visibility |

The five primary jobs are:

1. Orient to today and choose the next action.
2. Capture an intention without losing context.
3. Complete, check in, measure, or log with minimal interruption.
4. Understand progress and decide what to do next.
5. Configure, protect, export, or recover local data with confidence.

## Journey success targets

Interaction counts begin after the relevant surface is visible and exclude text
entry itself.

| Journey | Target |
| --- | --- |
| First launch to a useful creation path | One primary choice; no preference is required |
| First launch to first saved basic Task | At most two action choices plus title entry |
| Open any primary destination | One direct navigation action |
| Complete a Task or binary Habit from its daily list | One action with immediate visible feedback and Undo where safe |
| Quick-capture a Task from Tasks | Type, then one Add/IME action; interpretation is visible before save when enabled |
| Add a value to an established Habit, Goal, or Track | One direct action from its collection card, then value/save where required |
| Resume after editor cancellation or completion | Return to the invoking destination, scope, and meaningful list position |
| Recover from invalid input | Preserve every valid field and focus the actionable explanation |
| Find an existing record | Search is one action away, starts in the visible domain, and can expand to all Whip |

Performance regression targets remain defined in `docs/performance.md`. Emulator
numbers prove execution and catch regressions; they are not substituted for the
deferred physical reference-device budgets.

## Information architecture

```text
Home
├── Today overview and direct first/useful actions
├── Review & Trends (secondary; promoted when evidence exists)
└── visible Home sections
Tasks
├── Today
├── Inbox
├── Upcoming
└── History → Completed | Archived
Habits
├── Today
├── All
├── Insights
└── More → Automations | Archived
Goals
├── Active
├── Done
├── Insights
└── Archived
Tracks
├── Tracks → Entries | Rules | Insights | Options
├── Activity
└── Insights
Gym
├── Workout
├── History
├── Progress
├── Library → Exercises | Machines | Categories | Routines
└── Tools
Global/secondary
├── Area scope and Areas management
├── scoped Search → Search All Whip
├── global Add
├── Settings
├── notification/widget/deep-link entry
└── destination-sized editors and managers
```

Home and the five primary domains remain directly reachable. Search, Settings,
Areas, Review, editors, and managers are secondary destinations with an explicit
caller and return path. Gym remains intentionally outside Area scope; its global
header still uses the same Search, Settings, Add, and Home interaction grammar.

## Shared control taxonomy

| Decision | Shared control | Contract |
| --- | --- | --- |
| Primary destination | bottom navigation or labeled rail | Direct, persistent, selected state; posture changes preserve destination |
| Peer page | `DestinationTabBar` | Text tab with selected indicator; real overflow is labeled More/Pages |
| Alternate representation | `SegmentedChoiceBar` | One-of-many view mode; never used for navigation or independent toggles |
| Membership/query refinement | Filter action + `WhipActiveFilterRow` | Machinery is compact while inactive; applied state is visible and removable |
| Optional inline depth | `DisclosureButton` / `DisclosureRow` | Stable label, explicit expanded state, no navigation chevron |
| Child destination | `NavigationRow` | Row/card with forward chevron and Open semantics |
| Primary commit | `WhipButton` | One dominant action in a state; concrete verb-object label |
| Secondary action | `WhipOutlinedButton` or `WhipTonalButton` | Lower emphasis; never duplicates the primary outcome |
| Quiet/dismiss/undo action | `WhipTextButton` | Low-risk, reversible, or dismissal behavior |
| Item state change | domain primary-action lane | Same position in shared productivity cards; feedback is immediate |
| Inspect item | card/row tap | Opens read-only detail or action surface; never silently edits |
| Edit item | labeled pencil/Edit action | Explicit and semantically named for the object |
| Destructive action | separated danger zone + consequence confirmation | Error role, exact impact, recovery or irreversibility stated |
| One value from many | `SelectionField` / menu | Field label remains visible; current value and menu state announced |
| Independent boolean | full-row switch | Label and supporting consequence are one target; dependency follows below |
| Identity | `WhipIdentityEmoji` + shared picker | User-owned identity only; never substitutes for a functional icon |
| Empty collection | `WhipEmptyState` | Concise explanation, one context-correct primary path, at most one quiet alternative |
| Temporary feedback | shared snackbar/operation state | Failures and consequential actions remain visible; destructive or context-moving actions offer recovery when safe. Repetitive direct manipulation—completion, check-in, skip, checklist/milestone toggles, and set completion—uses the changed row/state as confirmation and does not enqueue a redundant snackbar. |
| Transient modal | `PaneAwareAlertDialog` | Active-pane ownership, bounded width, no navigation role |
| Destination/editor | `WhipFullScreenSurface` / root editor host | Owns supplied window, stable header/actions, dirty-draft protection |

Raw Material button/chip primitives are currently absent from domain source;
Whip wrappers own the action shape. Any intentional exception must be recorded
here.

### Intentional interaction exceptions

- Gym's active-set surface keeps repeat/save/rest actions inline because leaving
  the training flow for a general editor would materially slow the job. It still
  uses shared action hierarchy, units, feedback, and confirmation behavior.
- Task Quick Capture is the only persistent inline creation field. It optimizes
  the highest-frequency capture job; `+` remains the shared route for fields
  such as recurrence, reminders, and subtasks.
- Review uses a two-pane controls/overview dashboard at expanded widths because
  the relationship is simultaneous, not hierarchical. Compact Review orders the
  same content and discloses configuration when no outcomes exist.
- User emoji are record identity, while Material/Whip icons remain functional
  controls. They intentionally do not substitute for one another.

## Journey map and friction log

| Journey | Baseline friction | Final path and target | Current verification |
| --- | --- | --- | --- |
| First launch → first useful Task | Preference collection and two competing completion buttons precede value. | One recommended start → `Plan First Task` → Quick Capture/title. Preferences remain optional. | compact and 200% first-run captures; onboarding and task-default tests |
| Orient to today → act | Empty Home foregrounds unavailable review value and vague module guidance. | Home states the next action, offers one direct Task path and one quieter Habit path, then preserves domain discovery. | compact/wide/dark Home captures; navigation tests |
| Establish a repeated behavior/outcome | Habit and Goal first-use copy uses different action grammar and leaves the role of `+` unclear. | Both explain template versus from-scratch creation; templates are dominant where they reduce setup. | compact Habit/Goal captures; editor/default/E2E suites |
| Create structured evidence | Tracks presents a large Create action and a competing FAB for the same empty state. | One labeled `Create First Track` path opens the full editor; the contextual FAB returns once records exist. | compact Track capture; Track workflow, automation, and adaptive tests |
| Find, review, and continue | Search scope label and placeholder disagree; empty Review shows configuration before value. | Search names its actual scope; Review explains how to create evidence, links to sources, then discloses options. | scoped-search and empty-Review captures; navigation/policy/adaptive tests |

The baseline interaction counts and final targets are in **Journey success
targets** above. Automated end-to-end suites also exercise populated-state
creation, editing, completion/logging, invalid input, cancellation, Undo,
archive/restore, reminder/automation configuration, external entry, and process
recreation. No representative human participants were available, so
hesitation and confidence remain cognitive-walkthrough findings rather than
observed usability metrics.

## Content and terminology guide

| Concept | Standard language | Avoid |
| --- | --- | --- |
| Create an object | `Plan First Task`, `Create First Track`, `Create Exercise` | generic `New`, unexplained `Add`, or storage-model language |
| Open a destination | domain noun in primary navigation; `Open …` in supporting actions | using create/edit verbs for navigation |
| Record progress | Task `Complete`; Habit `Check In`/`Log`; Goal `Record Progress`; Track `Add Entry`; Gym `Save Set` | one generic verb that erases domain meaning |
| Reversible removal | `Archive` plus immediate result and restore path | `Delete` for recoverable actions |
| Irreversible removal | `Delete … Permanently` with exact scope and consequence | vague `Remove` or color-only danger |
| Optional depth | concrete disclosure label such as `Review Options` or `More Preferences` | `Advanced` without saying what is inside |
| Scope/state | `Showing Main`, `Scope: Workouts`, visible filter chips and units | hidden modes or requiring recall |
| Errors | explain what is wrong and how to correct it while retaining valid input | blame, codes, or generic `Invalid` |

Use sentence case, concrete verb-object action labels, short explanatory
sentences, contractions only where natural, and the product nouns `Tasks`,
`Habits`, `Goals`, `Tracks`, `Gym`, `Areas`, and `Review & Trends`. A title names
the location; supporting text explains purpose or state rather than repeating
the title. Success feedback names what changed. Confirmations are reserved for
irreversible, bulk, import/restore, or otherwise consequential work; safe
reversible actions prefer Undo.

## Visual-language specification

- **Identity:** exact brand ink `#090909` and warm white `#F5F3EA` anchor the
  fixed palette. New installs use it by default; Android dynamic color remains
  an explicit Appearance option.
- **Semantic color:** ink/warm white express primary action and canvas; green is
  success, amber is warning, red is destructive, and a quiet lavender container
  marks selection. Meaning always has a label, icon, shape, or state cue.
- **Surfaces:** tonal container steps create hierarchy without decorative
  gradients. Outlines separate inputs and boundaries; elevation is reserved for
  transient or floating controls.
- **Typography:** page titles, section headings, labels, body, and metadata use
  the shared Material type scale. Hierarchy comes from size/weight/space, never
  from ad hoc feature colors.
- **Shape and spacing:** restrained 4/6/8/10/12 dp corner roles and shared
  `WhipSpacing` increments keep controls compact but touch targets at least 48
  dp. Full pills are reserved for genuinely pill-like status or compact chips.
- **Icons and identity:** familiar interface icons always receive semantic
  names; user-selected emoji identify records. Unfamiliar or consequential
  global actions are labeled.
- **Motion:** short state/spatial transitions may clarify continuity, but
  essential information never depends on animation or a time-limited effect.
  Reduced-motion preferences and deterministic tests take priority over delight.

## Accessibility and adaptive-layout rules

- Interactive targets are at least 48 dp, expose role/state/action semantics,
  use one merged descriptive target where label and switch/card action belong
  together, and remain reachable with the IME open.
- Focus follows visual reading order: destination and page hierarchy, content,
  primary action, secondary actions, then destructive/advanced controls. Modal
  and full-screen surfaces hide the underlying shell from accessibility.
- Text and essential controls reflow at 200% font without relying on truncation;
  scrollable bodies keep commit/cancel available. Color, motion, and gesture are
  never the sole signal. Charts retain textual summaries and source routes.
- Compact uses labeled bottom navigation and one content column. Rail layouts
  preserve the same destination order. Dashboard/book layouts use support and
  content panes for complementary context, respect hinge gutters, and allow the
  content pane to expand without changing destination or editor ownership.
- Rotation, resize, folding, process recreation, system Back, and external entry
  preserve meaningful destination, scope, draft, active workout, and recovery
  behavior as protected by the adaptive, recreation, platform-entry, and editor
  suites.

## Screen, state, and entry-point inventory

| Surface family | Audited states and transitions | Current evidence |
| --- | --- | --- |
| First run | default, customization, optional preferences, completion | fresh compact capture/XML `00-first-run`; `ProductivityDefaultsUiTest`; source |
| Home | empty, populated sections, Area scope, support pane, Review route | fresh compact/wide captures `01-home-empty`, `10-wide-home`; Home and adaptive tests |
| Tasks | Today, Inbox, Upcoming list/agenda/calendar, History completed/archived, quick capture, filters, selection, detail, editor, deletion | fresh compact `02-tasks`; task journey/editor/selection/deletion/adaptive suites; source |
| Habits | Today/All/Insights/Automations/Archived, templates, all logging modes, skip/undo, detail, editor, pause, links | fresh compact `03-habits`; Habit journey/productivity-card/editor/adaptive suites; source |
| Goals | Active/Done/Insights/Archived, templates, measurement, elapsed reset, milestones, detail, editor, links | fresh compact `04-goals`; goal/productivity-card/editor/adaptive suites; source |
| Tracks | list/detail, Activity, Insights, Entries, Rules, Options, prompts, CSV, filters, editors, automation | fresh compact `05-tracks`; Track workspace/automation/E2E/adaptive suites; source |
| Gym | Workout, History, Progress, Library, Tools, exercise/machine/category/routine flows, set and timer states | fresh compact `06-gym`; Gym power input/routine/E2E/adaptive suites; source |
| Search | scoped and all-product search, empty/results, filters, keyboard, routing | fresh compact `07-search-workouts`; global routing/keyboard/adaptive suites; source |
| Review | weekly/monthly, included sections, saved views, empty/populated dashboard, wide/book layouts, source routing | fresh compact `08-review-empty`; review/adaptive/navigation tests; source |
| Areas | scope selection, list/detail, create, rename/color, move, merge, archive, delete/last-area guard | Area UI/domain/repository/adaptive suites; source |
| Settings | landing and six categories, compact/detail and wide master/detail, automatic saves, diagnostics and platform pickers | fresh compact `09-settings`; settings behavior/cause-effect/platform suites; source |
| Platform entry | widget, notifications/actions, permission prompts, share/deep links, document picker, Health Connect | platform-entry, notification, widget, Health, settings and backup suites; manifest/source |
| Cross-cutting states | loading/error/retry, validation, disabled reasons, snackbars/Undo, dirty drafts, IME, Back, recreation, light/dark, large text, RTL | shared pattern source; accessibility, interaction, recreation, visual matrix and E2E suites |
| Adaptive windows | compact, rail, dashboard, flat/book fold, tabletop, expanded editor, resize | fresh compact/wide captures; `AdaptiveWhipScreenTest`; `VisualAcceptanceMatrixTest` |
| Performance/scale | launch, navigation, resize, 10k collections, 100k charts, active set entry | 9/9 benchmark tests passed on API 34; raw results retained under final evidence; `docs/performance.md` |

## Baseline evidence

Repository-owned fresh evidence is under
`artifacts/ux-audit/2026-08-25-goal-baseline/`. Captures and hierarchy dumps were
created only through `scripts/device-artifacts` against explicitly selected
`emulator-5580`, after `ro.boot.qemu=1` and AVD name `whip_api34` were confirmed.

The pre-change baseline local gate passed with 266 JVM tests, lint, Android-test
compilation, debug assembly, and every coverage ratchet. The final implementation
passes 270 JVM tests, all 318 Android instrumentation tests on API 34, focused
min-SDK and latest-SDK compatibility smokes, the optimized release gate, and all
9 benchmark scenarios. Full results are summarized in the completion record and
`docs/performance.md`.

## Finding register

| ID | Priority | Evidence and cause | Decision and acceptance | Status |
| --- | --- | --- | --- | --- |
| G-01 | P1 | Fresh first run makes Home-section curation, advanced-control exposure, weight units, and optional preferences the first product experience. `Use Defaults` and `Start Using Whip` compete without explaining their different consequence. | Lead with value and one recommended start. Put customization behind an explicit secondary path. No preference is required; privacy is clear; first useful creation is the next emphasized step. | Implemented and verified |
| G-02 | P1 | Empty Home promotes Review & Trends before any outcome exists, then says to open a module without presenting one dominant creation path. Wide support content repeats four zero/ready summaries while the main pane remains mostly empty. | Empty/new Home becomes a first-value launchpad with one primary Task path, a quiet Habit/template alternative, and domain exploration. Review becomes prominent only once evidence exists, while a quiet preview keeps it discoverable. Wide support content teaches the same next actions instead of repeating empty metrics. | Implemented and verified |
| G-03 | P1 | Fresh domain captures show five empty-state grammars: Quick Capture with no empty CTA, template CTA, missing/low template CTA, duplicated Track CTA+FAB, and Gym-specific two-action hierarchy. | Define first-use versus temporarily-empty states. Each first-use collection exposes one direct domain-appropriate primary action and at most one quiet alternative; do not duplicate the global/contextual Add affordance with an equally weighted action. | Implemented and verified |
| G-04 | P1 | Scoped workout search says `Scope: Workout`, while its placeholder advertises Tasks, habits, goals, and tracks. The label and hint describe incompatible search universes. | Preserve the named `Workouts` scope supplied by the entry point and make the placeholder scope-aware. Expanding to all Whip updates both label and hint. Add a regression assertion. | Implemented and verified |
| G-05 | P2 | New installs default to wallpaper-derived dynamic color; fresh evidence has little connection to Whip's near-black/warm-white brand and visual identity changes with the device. The fixed scheme supplies only a small subset of roles and begins from the Material sample purple. | Make the refined Whip palette the stable new-install default while preserving optional dynamic color. Define complete light/dark surface, outline, action, selection, success, warning, and destructive roles; verify contrast and both modes. | Implemented and verified |
| G-06 | P2 | Wide empty Home reserves a large support pane for zero summaries and compresses the useful pane, weakening hierarchy. | Empty support provides concise domain and privacy orientation without duplicating the main pane's actions; populated support keeps actionable aggregate shortcuts. | Implemented and verified |
| G-07 | P2 | Review exposes period, four section toggles, and saved-filter creation before an empty user has any reviewable evidence. | In empty Review, show the explanation and source actions first; keep advanced view configuration available through disclosure or after data exists. | Implemented and verified |
| G-08 | P2 | Historical architecture and current tests are extensive, but no single current artifact connects users/jobs, journey targets, control taxonomy, fresh evidence, accepted findings, implementation, and deferred phone validation. | Keep this document current and finish with requirement-by-requirement evidence plus a separate phone checklist. | Implemented and verified |

No P0 data, trust, accessibility-blocking, or core-completion defect was found.
All accepted P1 and P2 findings are implemented and verified.

## Structural proposals and cognitive walkthrough

### First value

```text
Welcome/value/privacy
├── Start with Recommended Setup → Home → Plan First Task → Quick Capture
└── Customize First → Home sections/advanced/units → optional preferences → Save
```

The rejected alternative kept the old preference-first form but improved its
copy. It was lower risk but still required a new user to understand product
structure before receiving value. A multi-page tutorial was also rejected: it
increased time, Back complexity, and instruction without demonstrating a real
result. In the selected flow, a new user can predict that the primary action
uses safe defaults, can locate customization, and can change every choice later.

### Empty Home

```text
Location/date
→ one recommended action: Plan First Task
→ one lower-emphasis route: Explore Habits
→ visible configured domains
→ quiet Review preview until evidence makes Review useful
```

A metrics dashboard and a six-equal-action launcher were rejected because both
make an empty user interpret unavailable value or choose among equally weighted
domains. Hiding advanced domains entirely was rejected because it damages
discoverability and experienced-user speed. The selected hierarchy creates one
answer to “what next?” while preserving the existing direct navigation.

### First-use domain grammar

```text
Where/purpose → why collection is empty → one domain-appropriate start
Tasks: Quick Capture | Habits/Goals: template | Tracks: create | Gym: exercise
```

The surfaces retain meaningful domain differences: templates reduce setup for
repeatable behavior/outcomes, while a Track must define reusable evidence and a
Workout benefits from reusable exercises. The transferable rule is hierarchy,
not identical wording or components.

### Review and search

Search preserves its caller-provided named scope and can explicitly expand to
all Whip. Empty Review starts with explanation and source routes, then discloses
period/section/saved-view options. A universal default search and permanently
visible Review configuration were rejected because they discard entry context
and present optional machinery as the user's first decision.

## Implementation roadmap

1. **Completed:** repair first-value experience: first run, empty Home,
   first-use domain actions, and wide empty support.
2. **Completed:** correct scoped-search content and regression coverage.
3. **Completed:** establish the stable Whip palette/new-install default with
   light/dark and semantic-role verification.
4. **Completed:** reduce empty Review configuration dominance while preserving
   all advanced controls.
5. **Completed:** run focused tests and inspect compact/wide, 100%/200% text,
   light/dark, and editor/IME states.
6. **Completed:** run the complete emulator suite, local full release gate,
   benchmark execution, compatibility smokes, and artifact inventory.
7. **Completed:** finish the decision/evidence record and deferred phone
   checklist.

## Decision log

| Decision | Rationale | Exceptions |
| --- | --- | --- |
| Keep direct navigation to Tasks, Habits, Goals, Tracks, and Gym | Product breadth is intentional and recent navigation testing proves the labels fit at supported compact widths. Hiding domains would slow experienced use. | Settings and Review remain secondary destinations. |
| Keep user identity emoji separate from interface icons | Preserves warmth without giving functional controls unstable meaning or metrics. | None. |
| Keep dynamic color as an option, not the new-install default | Stable brand is necessary for a coherent visual language; personalization remains available. | Existing saved preferences and restored backups are respected. |
| Prefer one recommended start over first-run preference collection | Defaults are safe and all preferences remain in Settings; early configuration delays value. | A clearly labeled customization path remains available. |
| Do not use physical-device evidence in this goal | The phone is unavailable and requires later explicit authorization. | None; emulator and host evidence are the completion gate. |

## Evidence limitations

- No representative human research participants are available through the
  repository. Findings use cognitive walkthroughs, current rendered behavior,
  deterministic journeys, accessibility checks, and established product rules;
  these are not described as user research.
- Emulator performance is execution/regression evidence only. It cannot replace
  later measurements on an otherwise-idle physical reference device.
- Physical-device validation is deferred rather than passed.

## Completion record

### Implemented outcome

- First run now explains Whip's value, local ownership, and recommended setup
  before preferences. `Customize First` preserves direct access to Home
  composition, advanced-control, unit, reminder, and low-pressure choices.
- Empty Home now recommends one useful Task action, offers a quieter Habit route,
  preserves configured-domain discovery, and delays Review emphasis until
  evidence exists. Its expanded support pane provides complementary orientation
  instead of duplicated zero metrics.
- Tasks, Habits, Goals, and Tracks use a shared first-use hierarchy with concrete
  labels and one dominant domain-appropriate path. The empty Track screen no
  longer competes with a duplicate FAB.
- Empty Review puts outcome explanation and source routes ahead of optional view
  machinery; populated Review retains direct controls. Workout search now uses
  matching scope and placeholder language.
- New installs use Whip's exact ink/warm-white palette with complete light/dark
  semantic roles. Dynamic color remains available and existing saved/restored
  preferences are respected.
- The control taxonomy, domain exceptions, information architecture, content
  guide, visual specification, accessibility rules, and journey targets above
  now form the shared implementation contract.

### Requirement-to-evidence matrix

| Goal requirement | Result | Direct evidence |
| --- | --- | --- |
| Audit every user-facing surface, state, and entry point | Passed | Screen/state inventory above; source review; 318-test instrumentation suite covering productivity, Tracks, Gym, Areas, automation, Settings, platform entry, recovery, and adaptive states |
| Exercise primary journeys with empty and populated data | Passed | Journey/friction map; first-class workflow, defaults, editor, domain E2E, platform-entry, recreation, and populated-state suites |
| Resolve all accepted P1 and P2 findings | Passed | G-01 through G-08 are implemented and verified; no P0 was found |
| Coherent navigation, controls, language, and feedback | Passed | Shared control taxonomy and content guide; cross-workspace navigation, productivity-card, editor, settings, and interaction-contract tests |
| Fast first value and efficient frequent work | Passed | One-choice recommended setup; direct first-create paths; journey targets above; 9/9 benchmark scenarios passed |
| Preserve advanced capability and local-first trust | Passed | Advanced preferences and Review options remain disclosed; existing saved settings remain respected; no analytics or network behavior added |
| Continuity, recovery, and external entry | Passed | Interaction, editor, process-recreation, notification, widget, deep-link, document-picker, backup, and Health Connect test suites |
| Accessibility, large text, keyboard, and touch targets | Passed with recorded residual risk | Accessibility and visual-acceptance instrumentation passed; rendered 200% first-run/customization evidence retains scroll and fixed actions; semantics and 48 dp contracts remain enforced |
| Compact, expanded, Fold, RTL, and posture behavior | Passed | API 34 full adaptive/visual matrix; compact and expanded captures; focused API 37 large-screen adaptive smoke |
| Minimum and latest Android compatibility | Passed | Focused primary-navigation smoke on API 26 and expanded-layout smoke on API 37, both on wiped disposable AVDs |
| Realistic scale and performance | Passed for emulator regression evidence | 9/9 macrobenchmarks: cold/warm launch, navigation, resize, 10k collections, 100k charts, and active workout entry; full values in `docs/performance.md` |
| Automated and optimized release gates | Passed | 270 JVM tests; 318/318 API 34 instrumentation tests; lint; debug/test builds; coverage ratchets; R8 release APK; signed AAB; vital release lint; benchmark assembly |
| Rendered before/after evidence | Passed | `artifacts/ux-audit/2026-08-25-goal-baseline/` and `artifacts/ux-audit/2026-08-25-goal-final/` |
| Phone constraint and later validation | Deferred as required | No phone discovery, connection, deployment, inspection, capture, waiting, or test attempt; separate `PHYSICAL_DEVICE_VALIDATION_CHECKLIST.md` is ready for later explicit authorization |

### Regression contracts added or updated

- First run protects value-first recommended setup and optional customization.
- Home protects the first-action hierarchy, Review promotion only when evidence
  exists, and configured-domain visibility.
- Track protects a single empty-state creation path and adaptive editor routing.
- Review protects empty-versus-populated control hierarchy.
- Search policy protects scope-aware labels and placeholders.
- Theme tests protect exact brand tokens, contrast, and new-install dynamic-color
  defaults. Existing navigation, editing, undo, validation, recreation,
  accessibility, platform-entry, data-integrity, and performance coverage remains
  in force.
- Transient-feedback tests protect quiet inline completion for Tasks and Habits,
  while failures and consequential snackbar actions remain visible.

### Verification summary

- `./scripts/check`: passed with 270 JVM tests, lint, debug build, Android-test
  compilation, and all coverage ratchets.
- `ANDROID_SERIAL=emulator-5580 ./scripts/check --emulator`: passed all 318
  instrumentation tests in seven batches with zero failures or skips.
- `./scripts/check --full`: passed optimized release APK/AAB, R8, vital release
  lint, benchmark assembly, artifact integrity, and coverage.
- `ANDROID_SERIAL=emulator-5580 ./gradlew :benchmark:connectedBenchmarkAndroidTest`:
  passed all 9 tests. Representative emulator medians/p95 values are 1020.3 ms
  cold launch, 101.9 ms warm launch, 43.9 ms primary-navigation CPU, 65.5 ms
  resize, 27.2 ms Home at 10k/10k, 21.9 ms Goal/Gym charts at 100k, and
  141.5 ms active-workout inline entry.
- Focused connected smokes passed on API 26 small-screen and API 37 large-screen
  wiped AVDs. All target serials and AVD names were explicit; the phone was not
  enumerated or contacted.
- Final screenshots and hierarchy dumps were captured only through
  `scripts/device-artifacts`; device-root inventory was clean after repository
  helper organization.

### Deferred, rejected, and remaining risk

- **Deferred:** physical-phone rendering, one-handed feel, touch latency, thermal
  behavior, OEM font/rendering differences, notifications, widgets, Health
  Connect, document pickers, and release-install continuity. These are neither
  claimed nor required to pass this emulator-only goal; use the dedicated
  checklist only after explicit authorization.
- **Rejected:** preference-first onboarding, a tutorial carousel, an equal-weight
  six-action Home launcher, hidden advanced domains, universal unscoped search,
  and permanently exposed empty-Review configuration. Their tradeoffs are
  recorded in the walkthrough and decision log.
- **Residual risk:** compact six-destination shell labels can visually ellipsize
  at 200% font scaling when the underlying shell is visible. Full semantics and
  direct targets remain available, and all active first-run/editor actions
  reflow and pass. A future navigation study should compare a two-row or
  overflow model without weakening one-action domain access.
- **Evidence limitation:** cognitive walkthrough and deterministic automation do
  not replace representative human usability research. A later task-based study
  should measure hesitation, confidence, and one-handed comfort against the
  journey targets without adding behavioral tracking.

The audit-and-execution goal is complete under its emulator/on-system constraint.
Physical-device validation remains a separate, explicitly gated follow-up.
