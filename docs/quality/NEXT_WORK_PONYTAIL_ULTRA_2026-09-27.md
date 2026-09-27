# Whip next work — Ponytail Ultra, 2026-09-27

Status: **Proposed**. This is a small queue for a future product pass, not an authorization to restart the [paused whole-product audit](ASTRA_CONTINUATION_2026-09-20.md). The [September 26–27 design overhaul](DESIGN_CONSISTENCY_OVERHAUL_2026-09-26.md) and [reusable-architecture audit](REUSABLE_ARCHITECTURE_UX_AUDIT_2026-09-26.md) are completed within their documented scopes. No app change or new test run is claimed here.

## Selection rule

Keep an item only when a current source or observed journey shows a user-facing problem, or an important supported interaction has one precisely identified verification gap. Prefer one existing owner, a small correction, and a focused acceptance check. A dated audit's “next” wording, file size, similar-looking code, or an unmeasured hypothetical is not enough. Recheck current behavior before implementation; remove an item if the premise no longer holds.

## Ranked next items

| ID | Why it earns a place | Smallest next step | Done when |
| --- | --- | --- | --- |
| N-01 · Track rate meaning | [Current Insights code](../../app/src/main/java/com/whip/app/ui/TrackScreens.kt) calculates `Recent Weekly Rate` from 30 days but presents only the rate. The [original review](ASTRA_TRACKS_REVIEW_2026-09-09.md) identified the hidden denominator; the 7/30/90-day counts beside it do not label which window the rate uses. This is a confirmed copy ambiguity, not a calculation defect. | State the 30-day window in that existing label or its value. Keep the current calculation and shared summary card. | One focused Insights assertion and an ordinary rendered check show the denominator without clipping; the value remains unchanged. |
| N-02 · Spoken accessibility of recent shared changes | The [final design evidence](DESIGN_CONSISTENCY_OVERHAUL_2026-09-26.md) verifies semantics and pixels but explicitly did not measure spoken TalkBack output after changed headings, warnings and disabled destructive actions. These are consequential accessibility roles. No spoken defect is yet claimed. | On one supported Android configuration, traverse one representative heading, warning and disabled destructive confirmation with TalkBack; record what is actually announced. Change the shared owner only if a problem is heard. | A dated spoken transcript or recording and focused correction proof, if needed, are linked from verification. |
| N-03 · Real Count/Timer Habit widget journey | The [owner-pause checkpoint](ASTRA_CONTINUATION_2026-09-20.md) explicitly left this real-launcher journey unexecuted. [Current widget code](../../app/src/main/java/com/whip/app/widget/HabitWidgetRemoteViewsService.kt) exposes increment, start and stop actions, while the real check-off widget was already exercised. This is a bounded verification gap, not a known widget defect. | Exercise one pinned Count Habit increment and one pinned Timer Habit start/stop/review through the existing launcher path, including a return or process-recreation boundary. Fix only a reproduced failure. | Exact actions, saved log/session state and visible widget state agree after return or process recreation; retain the small receipt. |

## How to extend this plan

Add one row with an ID, user consequence, current reproduction or source evidence, the smallest existing code owner, and an observable finish line. Mark it `Proposed`, `Investigating`, `Verified`, or `Rejected` in the product-memory ledgers as work proceeds. Move completed facts to those ledgers and a linked verification receipt; keep this page short.

Do not schedule another complete visual catalog, exhaustive OEM/provider matrix, speculative performance tuning, component framework, or broad aesthetic rewrite from old audit wording alone. Reopen a specific part when a current defect, owner report, or explicit audit authorization supplies a concrete reason. The 167 unfinished rows in the frozen September audit are review debt, not 167 confirmed defects.
