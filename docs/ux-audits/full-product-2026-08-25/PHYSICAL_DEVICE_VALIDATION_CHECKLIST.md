# Deferred physical-device validation checklist

Status: deferred until the user explicitly enables phone testing after the
full-product UX/UI/design goal

This checklist is not evidence for the current goal. Do not discover, connect
to, deploy to, inspect, capture from, or test a physical phone until the user
explicitly authorizes that work in a later task.

## Authorization and safety

- [ ] Record the user's explicit authorization and the exact phone selected.
- [ ] Confirm whether the installed Whip data must be preserved; assume yes.
- [ ] Create or verify a user-controlled backup before any update when possible.
- [ ] Do not clear app data, uninstall the app, run instrumentation, run a
  benchmark harness, or perform destructive setup on the phone.
- [ ] Keep debug captures under `/storage/emulated/0/whip-debug` through
  `scripts/device-artifacts`; keep normal user exports in the folder selected
  through Android's document picker.
- [ ] Record model, Android version, display size/density, font scale, theme,
  navigation mode, app version, APK hash, battery level, and thermal state.

## Install and continuity

- [ ] Confirm the intended signed build and SHA-256 before deployment.
- [ ] Update in place and verify existing Tasks, Habits, Goals, Tracks, workouts,
  settings, backups, and widgets remain intact.
- [ ] Verify cold launch, warm launch, resume from Recents, process recreation,
  device rotation, and Back behavior without losing an active draft.
- [ ] Confirm notification permission and scheduled reminders retain their
  prior state; do not manufacture or delete production reminders unnecessarily.

## Visual and interaction matrix

- [ ] Inspect Home and all five primary domains in light and dark mode.
- [ ] Inspect the compact navigation, top actions, selected states, empty and
  populated cards, editors, confirmation surfaces, snackbars, and Undo.
- [ ] Verify 100%, 130%, and 200% text; display-size changes; keyboard open;
  gesture and three-button navigation; edge-to-edge/system bars; and landscape.
- [ ] Verify touch targets, focus order, TalkBack names/states/actions, contrast,
  non-color cues, reduced-motion behavior, and external-keyboard navigation if
  the phone supports them.
- [ ] Confirm the fixed Whip palette is the default for a new preference state
  and dynamic color remains an explicit, reversible Appearance option.

## End-to-end journeys

- [ ] Complete first-run recommended and custom paths only on a disposable user
  profile or other non-destructive environment; otherwise review existing setup.
- [ ] Create, edit, complete/check in/log, undo, archive, restore, and safely
  cancel one representative item in every applicable domain.
- [ ] Verify Home first-value guidance, scoped and all-Whip search, Review empty
  and populated states, Areas, Settings, backup/export/import selection, widget,
  notifications, deep links, and Health Connect where available.
- [ ] Verify active workout set entry, rest timer continuity, interruption and
  resume, and finish flow without disrupting the user's real training history.
- [ ] Confirm Android document-provider cancellation returns safely to Whip and
  that no export is written to shared-storage roots.

## Physical-only quality observations

- [ ] Assess perceived launch, destination switching, list scrolling, keyboard
  response, resize/rotation, and set entry on an otherwise idle device.
- [ ] Use non-destructive system profiling only when separately authorized;
  compare observations with `docs/performance.md` and identify emulator-only
  results as such.
- [ ] Check haptics, notification delivery timing, widget refresh, battery
  behavior, screen cutout/insets, OEM document picker, and OEM font rendering.
- [ ] Record every issue with build hash, exact state, steps, expected/actual,
  severity, screenshot or hierarchy evidence, and whether user data was involved.

## Exit

- [ ] Restore the user's preferred font scale, display size, theme, navigation
  mode, notification state, and active destination.
- [ ] Confirm no temporary data, reminder, workout, export, or debug artifact was
  left outside its approved location.
- [ ] Add the phone evidence as a dated supplement to `GOAL_AUDIT.md`; do not
  rewrite the emulator-only completion record as if phone validation had been
  part of the original goal.
