# Whip architecture decisions

This document records decisions that affect more than one product domain. The
implementation checklist and completion status live in `PLAN.md`.

## Local-first source of truth

Room is the authoritative local store. UI state is derived from Room flows and
domain calculations. WorkManager, notifications, graphs, summaries, personal
records, streaks, and goal progress are projections or side effects; none of
them replaces the underlying source records.

The 2026-08-22 pre-release cleanup established schema 1 as the public baseline.
Room is now schema 7, with every exported schema and explicit forward migration
from 1 through 7 checked in. Migration tests exercise both the schema-1 baseline
and schema-2 Track baseline through the current schema while preserving records
and relationships. Destructive fallback is not permitted for user data.

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

Habits and goals share typed metric definitions and timestamped metric
entries. Values are normalized to a canonical unit while retaining entry-unit
metadata for display and audit. Missing, zero, skipped, failed, and excused are
distinct. A metric's dimensional type cannot silently change after data exists.
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

## Links

Contribution, context, and trigger links are separate rule types. A derived
contribution is unique by link rule and source-event ID, is unit-compatible, and
retains an audit trail. Link graphs reject cycles. Backfills require a preview
and explicit confirmation.

## Backup envelope

The complete export uses a versioned envelope rather than raw database
files:

```json
{
  "format": "whip-backup",
  "envelopeVersion": 2,
  "databaseVersion": 6,
  "exportedAt": "2026-08-18T00:00:00Z",
  "checksumSha256": "...",
  "tables": {},
  "settings": {}
}
```

The backup data version is intentionally independent of Room's schema version.
The current backup data version is 6; version-5 archives are upgraded during
restore by supplying the deterministic default Track Scale increment. Import is
parse -> authenticate/checksum -> validate -> preview -> recoverable commit.
Unknown future versions fail safely. CSV files are domain-specific
interoperability exports, not complete backups. An optional encrypted envelope
uses a password-based key derivation plus authenticated encryption; the
passphrase is never stored.
Envelope version 2 is the only supported complete-backup format and includes
user preferences. Restore also rebuilds links and replaces Whip-owned reminder/timer
jobs so background state matches the restored records.

Restore first stores an app-private atomic rollback envelope. That marker stays
until Room, preferences, links, reminders, and owned WorkManager jobs
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
A Habit or Goal can explicitly bind to a Health metric. Its UI and link events
then mirror the authoritative source with stable IDs and provenance; provider
updates/deletions deterministically rebuild linked Goal contributions.
