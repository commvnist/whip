# Whip development checks

- Batch related edits. During implementation, run a focused check only when its result can guide the next edit. Use an exact JVM class or method with `scripts/qa-targeted --jvm PATTERN --jvm-only`, or the relevant `scripts/test-*` fixture for harness changes. Avoid broad checks after every edit.
- Bound each routine check with `timeout --kill-after=3s 55s COMMAND...`. For example: `timeout --kill-after=3s 55s scripts/qa-targeted --jvm com.whip.app.domain.GoalRulesTest --jvm-only`. A timeout or forced kill is incomplete, never passed; do not retry it repeatedly during implementation.
- After the implementation is stable, run one affected-change `scripts/check --ready` batch and selected Android emulator tests if device behavior changed. Reserve `scripts/candidate` for a frozen Play release. Follow an explicit user request for different verification scope or timing.
