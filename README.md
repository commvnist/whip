# Whip

`Whip` is a local-first Android task, habit, goal, and workout tracker built
with Kotlin and Jetpack Compose.

Whip does not collect or transmit personal data. See the
[Privacy Policy](PRIVACY.md) for details.

The full product roadmap, including tasks, user-defined habits, goals, linking,
and gym/workout tracking, is maintained in [`PLAN.md`](PLAN.md).

Tasks include recurrence, subtasks, equal-weight progress, reminders,
occurrence history, an Inbox, planning capacity, saved filters, and a focus
timer. Generic habits cover check-offs, quantities, timers,
ratings, checklists, flexible schedules, and streaks. Goals cover values,
totals, ranges, averages, consistency, and weighted milestones. The gym area
starts with an empty user-owned exercise library and supports detailed sets,
immutable routine prescriptions, supersets/circuits, equipment/machine
versions, rest timers, PRs, graphs, estimated 1RM, volume, and workout sharing.
Its scalable routine composer separates the selected
outline from the searchable library, supports structured rep ranges and day
templates, copies prior workouts, and can create exercises or machines inline
without losing the draft. Explicit load meaning keeps total, per-hand, per-side,
bodyweight, assisted, displayed-mass, and ordinal-machine history honest.

First-class productivity Areas organize Tasks, Habits, and Goals into durable,
user-named contexts. A global All/Unassigned/Area scope
stays consistent across Home, the three productivity destinations, Search, and
Review; Gym remains explicitly unscoped. Areas have stable identities, colors,
ordering, archive/merge management, permanent deletion with preserve-or-delete
item choices, editor pickers, and tappable record badges.

All records live in Room and work offline. Explicit contribution and trigger
links connect domains without merging their meaning. Optional read-only Health
Connect sync can be selected as a Habit or Goal source. Complete versioned
backup/restore, optional authenticated encrypted archives, a remembered
portable-backup folder with crash-safe verified daily copies and retention,
per-domain CSV export, widgets, notification actions, unified search, and
weekly/monthly reviews are included. First-run setup supports a focused simple
start with explicit one-tap defaults while every area remains available.
The interface adapts live across compact phones, tablets, and folding postures.
The Fold's flat inner display, book/tabletop postures, and other expanded
windows use the extra pane for navigation and contextual task, day, habit,
goal, review, or live-workout detail while keeping controls clear of the hinge.
The content pane can always expand to full screen.

## Toolchain

- Android Gradle Plugin 9.3.1
- Gradle 9.5 (through the checked-in wrapper)
- JDK 17
- Android API 37 (`minSdk` 26)
- Kotlin/Compose compiler 2.4.10
- Jetpack Compose BOM 2026.08.00

## Run it

Open this directory in Android Studio Quail 2 (2026.1.2) or newer, let Gradle
sync, select an API 26+ emulator or device, and run the `app` configuration.

From a configured terminal:

```bash
./scripts/check
# Also run persisted-data, migration, UI, and fold-layout tests on a device:
./scripts/check --device
# CI/release gate also assembles the optimized benchmark target and harness:
./scripts/check --ci
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`. See
[`docs/testing.md`](docs/testing.md) for device tests and environment-based
release signing, and [`docs/performance.md`](docs/performance.md) for the
Macrobenchmark/Baseline Profile workflow.

## Deploy to a phone over Wi-Fi

Wireless debugging requires Android 11 or newer and a phone on the same Wi-Fi
network as this machine. On the phone, enable **Developer options > Wireless
debugging**, then choose **Pair device with pairing code**.

The pairing popup and the main Wireless debugging screen show different ports.
Use each in order:

```bash
# Use the endpoint from the pairing-code popup, then enter its six-digit code.
./scripts/device pair 192.168.1.50:37121

# Use the endpoint on the main Wireless debugging screen.
./scripts/device connect 192.168.1.50:37123

# Build, replace the isolated `Whip Dev` app, and launch it without touching release data.
./scripts/device deploy

# Build, update, and launch the signed non-debug release configured on this workstation.
./scripts/device release-deploy
```

Pairing is normally needed only once. The debug port can change when wireless
debugging or the network restarts, so rerun `connect` with the currently shown
port. Use the explicit `dev-run` / `dev-logs` or `release-run` /
`release-logs` commands so QA cannot accidentally launch or inspect the wrong
variant. Every deploy verifies the installed APK hash, package/version details,
and resumed activity. `devices` and `disconnect` cover the other common
operations. If more than one device is attached, select one with
`WHIP_DEVICE=IP:PORT ./scripts/device deploy`.

This workstation keeps the Whip release key and its generated password outside
the repository under `/root/.android/whip`, both mode `600`. Debug
(`commvne.com.whip.app.debug`, shown as “Whip Dev”) and release
(`commvne.com.whip.app`, shown as “Whip”) are
separate packages and can coexist; neither needs to be uninstalled when testing
the other. Subsequent `release-deploy` runs use the same release key and update
the installed release in place without clearing its data. Back up that key
directory separately: losing the signing key prevents
future APKs from updating the installed release.

## WSL2 + Arch Linux

Install a compatible JDK inside WSL:

```bash
sudo pacman -Syu --needed jdk17-openjdk android-tools
sudo archlinux-java set java-17-openjdk
```

`android-tools` provides `adb` and `fastboot`; Arch's official repositories do
not package the full Android SDK. For command-line builds inside WSL, install
the Linux Android SDK command-line tools separately, export `ANDROID_HOME` to
that SDK, and install `platform-tools`, `platforms;android-37.0`, and
`build-tools;36.0.0` with the SDK manager. With the current Android CLI, that
command is:

```bash
android --sdk="$ANDROID_HOME" sdk install \
  platform-tools 'platforms;android-37.0' 'build-tools;36.0.0'
```

Keep
`local.properties` machine-local (it is ignored by Git):

```properties
sdk.dir=/absolute/linux/path/to/Android/Sdk
```

The cleanest all-WSL setup is Android Studio for Linux under WSLg, using that same
Linux SDK. A Windows Android Studio/SDK is a separate toolchain: it can open the
project through `\\wsl.localhost`, but do not point Linux Gradle at the Windows
SDK because its build-tool executables target a different OS.

## Project layout

```text
app/src/main/java/com/whip/app/
├── MainActivity.kt              # Activity and notification permission
├── WhipApplication.kt           # Repositories, channels, projection rebuilds
├── core/                         # Clock, settings, IDs, shared policies
├── data/                         # Room entities, DAO, database, repository
├── domain/                       # Rules and projections for every area
├── health/                       # Optional Health Connect reader
├── reminders/                    # WorkManager scheduling and notifications
├── ui/                           # Compose screens, editors, ViewModels, theme
└── widget/                       # Summary and quick-add home-screen widget
```

Room schemas are exported to `app/schemas`. Dependency versions live in
`gradle/libs.versions.toml`.

Further documentation:

- [`docs/user-guide.md`](docs/user-guide.md) — behavior, formulas, links, imports,
  and data lifecycle.
- [`docs/privacy.md`](docs/privacy.md) — local storage, Health Connect, backups,
  and the health/fitness disclaimer.
- [`docs/architecture.md`](docs/architecture.md) — persistence and projection
  decisions.
