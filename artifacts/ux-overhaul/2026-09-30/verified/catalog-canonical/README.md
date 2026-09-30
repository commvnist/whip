# Canonical page catalog native acceptance

The repaired full VisualCatalogPagesTest class passes **10/10 in272.748s**, zero failures/skips, on disposable emulator-5558 in one instrumentation process. Canonical capture ownership, the process-wide duplicate guard and every existing geometry assertion remain active. Five redundant overview captures were removed; their dedicated page journeys still produce the same canonical IDs.

Both Home Review actions are retained and tested: persistent Review & Trends and contextual clear-day Review Progress each open/close the same review surface. The old assertion that the header must be absent was replaced with exact one-header/one-contextual-action checks plus actual navigation.

Ten original PNG/XML pairs were personally inspected by the owning goals/habits lane; original device hashes are retained in provenance.tsv; repository text exports useLF and parsed-equivalent XML. All ten hierarchies parse, identify Whip and contain no NAF nodes. Task/Habit/Goal/Track/Gym frames show common chrome and visible bottom navigation; native geometry checks pass. Common Tasks and Goals tab bounds are180,2169-360,2337 and540,2169-720,2337 respectively.

Capture limit: shared.home.clear-review.png shows valid Home header/body and both Review actions, but bottom-navigation pixels were not drawn in that captured frame. Its hierarchy retains expected navigation bounds; this frame alone does not certify navigation pixels. Other representative frames have visible navigation.

- [Tasks](originals/tasks.collection.png), [Habits](originals/habits.all.populated.png), [Goals](originals/goals.active.populated.png)
- [Tracks](originals/tracks.all.populated.png), [Gym](originals/gym.workout.populated.png), [Settings](originals/settings.overview.png)
- [Home priorities](originals/shared.home.populated.png), [clear-day Review](originals/shared.home.clear-review.png)
- [Native original log](instrumentation.log), [receipt](receipt.txt), [provenance](provenance.tsv)

The exact isolated pair is app6922aff3bb79aec2fe974c19908bc76670dbac55179d0561830026e776613850 / test67ca5dff0d0c86a959f580f5c05d1e14121a94a81780fdf20ef1bec6764b2dcd. It includes the unchanged shared DAO normalization repair and diagnostic-only timer logging. These selected native passes are separate from complete-campaign acceptance and the separately authorized phone release.


[Export normalization](export-normalization.json) records original versus repository text hashes. Only CRLF line endings changed; native results and parsed hierarchies are equivalent, PNG pixels unchanged.
