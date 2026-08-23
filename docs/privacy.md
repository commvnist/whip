# Whip privacy and health-data notes

Whip is local-first. It does not require an account, does not include an
Internet permission, and does not send task, habit, goal, workout, or health
data to a Whip server. Android system backup is disabled so the app database is
not silently copied to a cloud backup provider.

The user can explicitly export a complete JSON backup or domain CSV files with
Android's system document picker. A user-selected portable-backup folder can
also receive verified, retained daily copies. Whip persists only the Android
URI permission the user grants; it does not gain general filesystem access.
These files may contain sensitive personal and health information. One-off
complete backups can be saved either as interoperable plaintext JSON or as an
authenticated, passphrase-encrypted Whip archive. The passphrase is never
stored. Plaintext backups and CSV exports depend on the destination for
confidentiality; encrypted backups still reveal their filename and approximate
size. Daily portable-folder backups use plaintext JSON so another Whip install
can inspect and recover them without a retained secret.

Backup creation uses an unmistakable staging document, closes and reads it
back, validates its checksum and row count, then renames it to the final visible
name and verifies it again. Interrupted staging files are cleaned on recovery,
and corrupt or unreadable archives do not count against retention. Restore is
previewed before it starts. Whip keeps an app-private recovery snapshot until
the database, preferences, reminders, geofences, and background jobs have all
been replaced successfully; failure rolls back immediately or at next launch.
**Delete all local data** in Settings
clears every internal Whip table after confirmation but deliberately leaves
external backup files untouched.

Health Connect is optional. When enabled, Whip requests read-only access only
for the categories selected in Settings: weight, steps, distance, hydration,
sleep, and/or exercise. Imported records retain provider and record identifiers
so a subsequent reconciliation updates changed values and removes provider
records deleted within the selected window. Habits and Goals can explicitly
bind to a supported Health metric; those records show their provenance and do
not expose misleading manual controls. Disabling the integration stops future
sync; imported local entries remain until they are deleted or all local data is
cleared. Manual tracking never requires Health Connect.

Whip's estimated one-repetition maximums, pace estimates, correlations, and
trend summaries are informational. They are not medical advice, a diagnosis,
or a guarantee that a load or workout is safe. Correlation does not establish
causation.

Notifications are scheduled locally through Android WorkManager. Whip uses no
continuous background service. A workout screen can opt into keeping the
display awake only while that screen is visible. Rest timers use a single
replaceable delayed job per active workout.
