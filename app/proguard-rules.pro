# Whip deliberately persists enum names in Room, preferences, portable backups,
# and immutable workout snapshots. Their member names are part of the on-device
# data format and must remain stable across minified builds.
-keepclassmembers enum com.whip.app.** {
    *;
}
