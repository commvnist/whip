# Whip architecture decisions

This document records decisions that affect more than one product domain. The
implementation checklist and completion status live in `PLAN.md`.

## Local-first source of truth

Room is the authoritative local store. UI state is derived from Room flows and
domain calculations. WorkManager, notifications, graphs, summaries, personal
records, streaks, and goal progress are projections or side effects; none of
them replaces the underlying source records.

The current product data contract is schema 46 and data epoch 6. This is a
deliberate clean boundary: an update from an earlier epoch is locked until the
person explicitly confirms a fresh start twice. Whip then removes only its own
local data and creates the canonical schema. It never attempts to reinterpret
an earlier schema or silently fall back to destructive database opening.

## Time and identity

Domain writes obtain time from `WhipClock`, not directly from the system. This
keeps date boundaries and tests deterministic. Persist instants as epoch
milliseconds and calendar occurrences as ISO-local epoch days. Records that
can be exported or referenced across tables receive a stable UUID from
`WhipIdGenerator`; Room-generated integer IDs remain valid internal keys.
The active time zone follows the device by default and can be pinned to an IANA
zone in Settings. Changing it affects future grouping and reminders; saved
entries retain their original local date, zone, and UTC offset. A configurable
late-night cutoff can assign early-morning manual entries to the previous day.

## Task occurrences and subtasks

Task definitions are separate from occurrence state. Recurring occurrence
identity remains anchored to the original scheduled date even when that
occurrence is moved. A task step is a reusable definition; its completion state
is keyed by step ID plus task occurrence key. This prevents Monday's step state
from leaking into Tuesday's occurrence.

## Measurements

Habits and goals share typed measurement definitions and timestamped
measurement entries. Values are normalized to a canonical unit while retaining entry-unit
metadata for display and audit. Zero and failed measurements remain explicit;
an absent habit check-in is derived as missed only after its scheduled day
closes. A user skip is stored separately in `habit_skips`, never as a value or
measurement entry, so it cannot inflate totals. A measurement's
dimensional type cannot silently change after data exists.
Custom units store a name, symbol, dimension, and conversion factor, and use
the same canonical conversion path as built-in units.

Gym machine profiles are exercise-specific, versioned resistance contexts. A
workout exercise snapshots load interpretation and multiplier, exercise and
bodyweight/e1RM policies, machine scale/unit/configuration group and version,
seat/back/attachment details, pulley ratio, stack topology, add-on plate, and
mass mapping. Sets separately preserve prescribed and performed values. Mass
stacks additionally store canonical kilograms. Ordinal settings never receive a
fabricated mass value; therefore they cannot enter volume or estimated-1RM
calculations. Previous-set, record, routine-copy, and chart queries include
compatible immutable machine scope.

## Projections

Task percentage, habit streaks, workout statistics, personal records, and goal
progress are calculated from source records. Expensive results may be cached,
but caches must be rebuildable and invalidated when historical sources change.

Editor saves commit the authoritative Room change before reporting success.
Dialogs receive that result directly; transient snackbar delivery is never a
save-completion signal. Derived work is dependency-aware: presentation-only
changes such as an emoji or description do not reschedule reminders, rewrite
unchanged child rows, or rebuild Track entry search.
Changes to cadence, reminder timing, Track Fields, or searchable Track metadata
still invalidate the corresponding derived state.

Queued Task, Habit, and Goal reminder work is never authoritative. Each request
carries a versioned claim for one stable entity, logical day, exact trigger,
delivery kind, and semantic fingerprint. Immediately before posting or acting,
the domain resolver loads current occurrence/progress/settings state and rejects
missing, malformed, early, or stale work. Production reminder-relevant commits
and resolve/post decisions share a non-reentrant state boundary; code that also
uses a per-entity scheduler lock always acquires entity then state. Raw repository
delegates exist only below one explicit outer boundary so Room transaction
coroutine changes cannot create reentrant-lock deadlocks. A private durable
cleanup journal bridges permanent deletion across Room and Android notification
state without altering backed-up user data or historical records.

Habit occurrence state has one neutral user action: **Skip Today**. A skip is
visible in the Today card, History, Insights, and exports; it suppresses that
day's reminder, is excluded from completion-rate
denominators, and bridges rather than increments a streak. **Undo Skip** removes
the occurrence. Missing is not writable state: past scheduled occurrences with
no check-in or skip are derived as missed. The epoch-6 schema stores one skip
occurrence per habit/day and has no separate missing-value record.

## Productivity collection design

Tasks, Habits, and Goals share one collection-card grammar implemented by
`ProductivityItemCard` and `ProductivityItemHeader`: identity emoji, title and
context, an optional fixed-width primary-action lane, then a trailing edit
action. Card inset, shape, color, headline typography, area placement, and
vertical spacing are owned by those primitives. Progress and expanded content
follow beneath the header. Home, planning, active/completed/archived lists, and
insight cards reuse the same hierarchy; a domain may omit an action, but may not
reorder the remaining elements.

## Adaptive presentation and visual semantics

Whip treats a Fold or tablet as a composed workspace, not as a stretched phone.
Each first-class destination may own an actionable support pane; support panes
must contain useful navigation or context for that destination rather than
generic dashboard filler. Primary content uses bounded readable widths, while
review dashboards may use the wider dashboard bound.

Transient dialogs are placed by `PaneAwareAlertDialog` inside the active
content pane. Destination-sized editors and managers instead use
`WhipFullScreenSurface`: its opaque surface owns the complete bounds supplied by
the root, while safe-drawing insets apply only to its child content. This keeps
the Fold hinge and status-bar background intentional and prevents a small
dialog from straddling two panes.

Color has semantic roles across domains. Primary is action/selection, secondary
is success/completion, tertiary is warning/skip, and error is destructive.
Features do not repurpose these roles as decorative identity colors. Shared
navigation budgets elevated font scale and label length, fills available direct
destination capacity, and reserves **More** only for genuine overflow.

## Backup envelope

The complete export uses a versioned envelope rather than raw database
files:

```json
{
  "format": "whip-backup",
  "envelopeVersion": 3,
  "dataModelEpoch": 6,
  "databaseVersion": 23,
  "exportedAt": "2026-08-18T00:00:00Z",
  "checksumSha256": "...",
  "tables": {},
  "settings": {}
}
```

The backup data version is intentionally independent of Room's schema version.
Only envelope 3, data epoch 6, and backup data version 23 are accepted. Older
and future complete archives are rejected before their tables are interpreted;
the clean boundary deliberately provides no archive upgrade path. Import is
parse -> authenticate/checksum -> validate -> preview -> recoverable commit.
CSV files are domain-specific
interoperability exports, not complete backups. An optional encrypted envelope
uses a password-based key derivation plus authenticated encryption; the
passphrase is never stored.
Envelope version 3 is the only supported complete-backup format and includes
user preferences. Restore replaces Whip-owned reminder/timer
jobs so background state matches the restored records.

Restore first stores an app-private atomic rollback envelope. That marker stays
until Room, preferences, reminders, and owned WorkManager jobs
have been replaced/rebuilt; failure or process death rolls back immediately or
on next launch.

Portable folder backups use Android's Storage Access Framework rather than a
filesystem path. The persisted tree-URI grant is device-local metadata and is
not included in the portable archive. Each write uses an `INCOMPLETE` staging
document, is reopened and validated, renamed to its final name, then reopened
and validated again before success metadata is saved or retention runs.
Retention counts only readable, checksum-valid candidates. A corrupt partial
write is deleted, unrelated files are ignored, and automatic work skips an
empty source database. WorkManager owns one uniquely named periodic request so
toggling the feature or restarting the process cannot create duplicate
schedules.

## Health Connect

Health Connect is an optional read-only source. Selected record types are
normalized into the same measurement ledger as manual entries and retain stable
provider record IDs. Aggregate types such as steps and distance are read as
daily totals to avoid double counting. A bounded sync rebuild removes stale
Health Connect entries inside the requested window; manual data is independent.
A Habit or Goal can explicitly bind to a Health measurement. Its UI and derived records
then mirror the authoritative source with stable IDs and provenance.
