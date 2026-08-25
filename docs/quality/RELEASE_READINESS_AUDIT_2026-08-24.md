# Release-readiness audit — 2026-08-24

## Assessment

The available local release gate is green. No reproducible P0 or P1 defect
remains in the audited build, and every validated P2 finding below has a
root-cause fix plus regression coverage. The application remains local-first;
this pass added no account, cloud, social, collaboration, or other new product
area.

This assessment covers deterministic tests, Room-backed integration tests,
Compose UI/E2E tests on an isolated API 34 debug emulator, minified release and
benchmark builds, and fresh clean/populated live interaction. It does not claim
a final physical-device, real-hinge, Health Connect system-picker, or API
26/28/35 emulator pass; those configurations were not available to this run.

## Issue register

| Severity | Domain and reproduction | Expected / actual evidence | Root cause and fix | Regression | Status |
| --- | --- | --- | --- | --- | --- |
| P0 | Persistence: install a schema-1 or schema-2 database, then launch the schema-8 app. | Existing data must upgrade intact. The database originally registered only migrations 3→7, so old installs had no valid upgrade path. | Added explicit 1→2 and 2→3 migrations, including the Track/Automation structures and FK-safe Link/Contribution/Trigger table rebuilds, then retained a complete 1→8 chain. Schema 8 adds persisted Task identity emojis with a deterministic legacy default. | `WhipDatabaseMigrationTest` starts from checked-in schemas 1 and 2 and verifies Tasks, Goals, Links, Contributions, triggers, occurrences, Tracks, Fields, Entries, list preferences, Scale values, and source identity after upgrading to 8. | Fixed |
| P1 | Backup/restore: restore a backup written before Track Scale increments, Task identity emojis, or neutral Habit skip occurrences became required. | A supported backup must restore without violating current invariants. | Export version is now 8; v5 through v7 remain explicit supported inputs and supply missing Scale increments, Task emojis, and the Habit Skip table while removing legacy non-measurement Habit rows. Preview and unsupported-version messages state the actual compatibility boundary. | `BackupRepositoryTest` covers v8 round trip, v5 upgrade, Skip round trip, Task emoji preservation/defaulting, and v9 rejection. | Fixed |
| P1 | Editor lifecycle: open a Task/Habit/Goal/Exercise/Machine/Routine editor or a quick-workout Set draft, enter unsaved data, recreate the Activity. | Drafts should survive configuration recreation. Several editors reopened closed or lost input. | A nested saveable-state holder was consuming/dropping editor state. Adaptive content now uses stable movable content with the latest renderer, preserving identity across compact/rail/split movement while allowing normal `rememberSaveable` restoration. | `EditorStateRecreationTest` covers six editors plus workout Set state and back/dismiss behavior. | Fixed |
| P2 | Fold/expanded Settings navigation: enter Settings while a rail is visible. | The rail should remain; Settings instead forced full-content/compact bottom navigation. | Settings was incorrectly included in the content-pane-expanded predicate. Expansion now depends only on an actual supported-pane expansion state. Navigation surfaces have stable test tags. | `AdaptiveWhipScreenTest.bookFoldUsesHingeAwareSupportPaneAndPersistentNavigation` opens Settings from Gym on a separating book fold and asserts rail present/bottom navigation absent. A live 1800×2400 resize repeated the assertion (`rail=1`, `bottom=0`). | Fixed |
| P2 | Color picker reliability: open Custom Color on a software-rendered emulator. | The picker should render and remain interactive. A Compose horizontal-gradient shader triggered a native QEMU RenderThread crash on the available host renderer. | Replaced the runtime shader track with a smooth, deterministic segmented interpolation made from solid Compose colors. Color choice and semantics are unchanged. | The focused color-picker UI test and the complete instrumentation suite pass with host Vulkan disabled. | Fixed |
| P2 | Creation feedback: quick-capture a Task and immediately navigate or use Edit/Undo. | Exactly one feedback surface should appear, its action should work, and it should neither disappear instantly nor replay on each page. The Snackbar could self-cancel because consuming its status invalidated its own `LaunchedEffect`; separate flows could also expose mismatched message/action identity. | Task result, undo message, and quick-added ID are one atomic feedback state. Terminal states are claimed once, while Snackbar display runs in a stable screen scope. The same stable delivery pattern is used across domains. | `ProductivityCreationJourneyE2ETest` waits for the persisted row and separately verifies the rendered Edit action; transient-feedback unit/UI paths cover one-shot consumption. Live quick capture rendered one Task plus Edit and Undo. | Fixed |
| P2 | Adaptive E2E discoverability: reach Goal Insights after selecting Archived. | The test should follow the same adaptive navigation grammar as a user. It assumed Insights was always directly visible, although narrower layouts intentionally place it in Pages. | The journey now opens Pages when required rather than encoding a single-width visual assumption. Product behavior was already correct. | The complete Task → Habit → Goal creation E2E passes on the compact API 34 configuration. | Test gap fixed |

## Fresh live evidence

The release audit used only a freshly built live application and current source;
historical screenshots were not used as evidence.

| State/configuration | Evidence observed |
| --- | --- |
| Clean debug data, API 34, 1080×2400, default theme/font | Setup explained that Home visibility does not remove destinations; `Main` was created; Tasks and Habits were the concise default Home modules; all five destinations remained in navigation; no notification-permission prompt interrupted first use. |
| Populated debug data, API 34, 1080×2400 | Quick Capture created `Release audit task`; the live hierarchy changed from 0 to 1 Task, showed the exact record, completion/edit affordances, and one Edit/Undo feedback surface. |
| Expanded window, API 34, 1800×2400 | Tasks used the adaptive rail. Opening Settings preserved `adaptive-navigation-rail` exactly once and rendered no `adaptive-bottom-navigation`. |
| Synthetic separating book fold, 1768×2208 | Compose instrumentation exercises hinge-aware support/content panes, navigation persistence through Settings, pane expansion, editor containment, and duplicate-header prevention. |
| Dark/light, RTL, 200% font, keyboard, accessibility | Covered by instrumentation policies and interaction/accessibility suites; these were not all repeated as manual physical-device observations. |

The emulator was `emulator-5554`, API 34 Google APIs x86_64, launched headless
with software rendering and host Vulkan disabled. The debug package
`commvne.com.whip.app.debug` was cleared; the release package and personal data
were never used by instrumentation.

## Verification

Commands and final outcomes:

```text
./scripts/check                         PASS (246 JVM tests, Android-test compile, lint, debug build)
./scripts/check --emulator equivalent   PASS (275 instrumentation tests in bounded, emulator-pinned batches)
./scripts/check --full                  PASS (local gate plus minified release and benchmark builds)
git diff --check                        PASS
```

Focused verification performed during the fixes also passed:

- database schema 1→8 and 2→8 migration tests;
- backup v5/v6→v7 compatibility and current-version round trips;
- twelve editor recreation/back-state tests;
- book-fold Settings rail persistence;
- custom-color picker interaction on the stable software renderer; and
- the complete productivity creation E2E journey.

The exact generated reports are `app/build/reports/lint-results-debug.html` and
`app/build/reports/androidTests/connected/debug/index.html`. Build outputs are
generated locally and intentionally ignored by Git.

## Coverage and modified files

`docs/testing.md` is the authoritative feature-to-test coverage map. This
release train changes production code under `app/src/main`, instrumentation
coverage under `app/src/androidTest`, deterministic coverage under
`app/src/test`, checked-in Room schemas 2–7, benchmark seed/profile sources,
release/user/architecture documentation, and the local `scripts/check` and
`scripts/device` tooling. Implementation commit `363079c` preserves the
complete file-level inventory (`git show --name-status 363079c`), avoiding a
manually duplicated list that can drift from the committed tree.

## Remaining limitations and risks

- Only the installed API 34 system image was available. API 26, 28, and 35
  compatibility runs remain release-matrix work, not known failures.
- A synthetic fold and an expanded live window were exercised; no final real
  hinge transition was performed in this run.
- Notification, Health Connect, document-picker, battery, reboot, timezone/DST,
  keyboard/mouse/stylus, and TalkBack platform surfaces still require the
  physical smoke matrix in `docs/testing.md`. Their app-owned persistence,
  scheduling, semantics, denial, and recovery paths are automated.
- Software rendering was selected because the available QEMU Vulkan path had a
  native renderer failure. The application-side shader that exposed that path
  was removed, and the full suite is stable; physical GPU performance remains a
  separate smoke/benchmark concern.

These are unavailable-platform checks, not accepted product defects. Any newly
observed P0–P2 failure in those configurations blocks release until reproduced,
fixed, and added to the appropriate regression layer.
