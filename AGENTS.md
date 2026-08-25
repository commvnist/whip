# Whip workspace instructions

## Physical-device files

- Never write screenshots, UI hierarchy dumps, test output, traces, helper scripts, or other development artifacts directly to `/sdcard`, `/storage/emulated/0`, or `/data/local/tmp`.
- Use `scripts/device-artifacts capture`, `scripts/device-artifacts ui-dump`, or a subdirectory created by `scripts/device-artifacts prepare`.
- Shared debug artifacts belong under `/storage/emulated/0/whip-debug`; shell-only tooling belongs under `/data/local/tmp/whip-debug`.
- Normal user-initiated release exports belong in the user-selected Android document folder. When using shared local storage, use `/storage/emulated/0/whip` and never the storage root.
- Run destructive instrumentation only on a disposable emulator. Physical-device live inspection must not clear application data or run benchmark/instrumentation tooling.
