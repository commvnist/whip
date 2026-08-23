# Performance and Baseline Profile gate

Whip has a dedicated `:benchmark` module targeting the optimized, debug-signed
`benchmark` build. The harness is separate from debug and release data. Its
exported fixture activity exists only in the benchmark variant and seeds
deterministic 10,000- and 100,000-row datasets without shipping a production
back door.

Coverage includes:

- cold startup with and without a Baseline Profile, plus warm startup;
- primary navigation and expanded-window resize frame timing;
- Home with 10,000 Tasks and 10,000 Habit logs;
- Goal and Gym charts backed by 100,000 points/sets; and
- active-workout inline weight/repetition/save latency.

Build the target and harness on any development machine:

```bash
./gradlew :benchmark:assembleBenchmark
```

Run the full benchmark and Baseline Profile generator on a selected API 35+
device with animations disabled:

```bash
ANDROID_SERIAL=device-serial \
  ./gradlew :benchmark:connectedBenchmarkAndroidTest
```

Run the suite locally on a fixed API 35+ emulator when execution smoke is
needed, and retain the raw benchmark/profile outputs with the audit evidence.
Emulator numbers prove only that scenarios execute; release performance
decisions must come from an otherwise-idle physical reference device, repeated
before and after the same change and build.

## Acceptance budgets

These are regression gates. Physical measurements are recorded below so later
changes can be compared on the same class of device and build:

| Scenario | Provisional physical-device budget |
| --- | --- |
| Cold time to full display, profile installed | median ≤ 1,000 ms |
| Warm time to full display | median ≤ 500 ms |
| Navigation, resize, and dense-list rendering | frame overrun/jank ≤ 3%; p95 frame ≤ 32 ms |
| Active set edit and Save + next | visible response p95 ≤ 100 ms; no lost set |
| 100,000-point graph open/interaction | no ANR; bounded 200-point rendering projection |
| Dense fixture memory | no sustained growth across five repeated journeys |
| Background battery | no global one-second Gym rebuild when no timer is visible |

Record the device model, Android/build version, thermal state, battery mode,
commit, APK hash, and raw output with each accepted result. Move a budget only
when user-visible evidence shows it is unrealistic; do not loosen a gate to
make a regression pass.

## Physical Fold baseline — 2026-08-19

Reference device: Samsung SM-F976W (Fold 8 Ultra), Android 17 / API 37,
build `CP2A.260605.016.F976WVLU1AZGI`, unfolded at 2256×2504 and 480 dpi. The
device was AC-powered at 66%. Android reported thermal status 2 after the full
suite (skin about 41°C), so these figures are conservative rather than a cool,
idle-device best case. Benchmark APK SHA-256:
`14952ac4a5becbad474d1b7bfc994c2909cab093b0cb1cd1578cba4a1ef6d49a`.

The checked-in Whip-only Baseline Profile contains 4,537 generated hot-code
rules. AndroidX/Compose dependency profiles remain supplied by their libraries;
the generator deliberately excludes duplicate framework rules. The packaged
benchmark APK contains binary profile and metadata assets.

| Scenario | Fold result | Assessment |
| --- | --- | --- |
| Cold startup, packaged profile | 217.6 ms median | Pass; 28.7% faster than no compilation |
| Cold startup, no compilation | 305.0 ms median | Comparison baseline |
| Warm startup, packaged profile | 61.8 ms median | Pass |
| Primary navigation | 12.5 ms p95 CPU frame | Pass |
| Forced expanded resize | 33.5 ms p95 CPU frame | 1.5 ms above provisional target; track as a known optimization target |
| Home, 10k tasks + 10k habit logs | 17.7 ms p95 CPU frame | Pass; no crash or Binder exhaustion |
| Goal/Gym, 100k history points | 6.6 ms p95 CPU frame | Pass; rendered through bounded projections |
| Active workout Save + next | 8.8 ms p95 CPU frame | Pass; all iterations persisted the set |

All nine benchmark/profile scenarios passed. Raw Perfetto traces and generated
reports are written by the harness to
`/storage/emulated/0/Android/media/com.whip.benchmark` on the reference device.
The dense Home benchmark currently observes only two measured frames per
iteration, so its p95 is useful as a smoke/regression signal but is not a broad
scroll-jank percentage. Resize is likewise an intentionally abrupt synthetic
`wm size` transition; its small sample should be optimized and remeasured, not
presented as ordinary navigation performance.

References: [Android Macrobenchmark](https://developer.android.com/topic/performance/benchmarking/macrobenchmark-overview)
and [Baseline Profile generation](https://developer.android.com/topic/performance/baselineprofiles/create-baselineprofile).
