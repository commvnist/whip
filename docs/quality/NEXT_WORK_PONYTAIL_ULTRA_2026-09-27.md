# Whip next work — Ponytail Ultra, 2026-09-27

Status: **Completed** under FB-20260927-002. This bounded pass did not restart the [paused whole-product audit](ASTRA_CONTINUATION_2026-09-20.md). The [September 26–27 design overhaul](DESIGN_CONSISTENCY_OVERHAUL_2026-09-26.md) and [reusable-architecture audit](REUSABLE_ARCHITECTURE_UX_AUDIT_2026-09-26.md) remain completed within their documented scopes.

## Selection rule

Keep an item only when a current source or observed journey shows a user-facing problem, or an important supported interaction has one precisely identified verification gap. Prefer one existing owner, a small correction, and a focused acceptance check. A dated audit's “next” wording, file size, similar-looking code, or an unmeasured hypothetical is not enough. Recheck current behavior before implementation; remove an item if the premise no longer holds.

## Ranked next items

| ID | Why it earns a place | Smallest next step | Done when |
| --- | --- | --- | --- |
| N-01 · Track rate meaning | [Current Insights code](../../app/src/main/java/com/whip/app/ui/TrackScreens.kt) calculates `Recent Weekly Rate` from 30 days but presents only the rate. The [original review](ASTRA_TRACKS_REVIEW_2026-09-09.md) identified the hidden denominator; the 7/30/90-day counts beside it do not label which window the rate uses. This is a confirmed copy ambiguity, not a calculation defect. | State the 30-day window in that existing label or its value. Keep the current calculation and shared summary card. | One focused Insights assertion and an ordinary rendered check show the denominator without clipping; the value remains unchanged. |
| N-02 · Spoken accessibility of recent shared changes | The [final design evidence](DESIGN_CONSISTENCY_OVERHAUL_2026-09-26.md) verifies semantics and pixels but explicitly did not measure spoken TalkBack output after changed headings, warnings and disabled destructive actions. These are consequential accessibility roles. No spoken defect is yet claimed. | On one supported Android configuration, traverse one representative heading, warning and disabled destructive confirmation with TalkBack; record what is actually announced. Change the shared owner only if a problem is heard. | A dated spoken transcript or recording and focused correction proof, if needed, are linked from verification. |
| N-03 · Real Count/Timer Habit widget journey | The [owner-pause checkpoint](ASTRA_CONTINUATION_2026-09-20.md) explicitly left this real-launcher journey unexecuted. [Current widget code](../../app/src/main/java/com/whip/app/widget/HabitWidgetRemoteViewsService.kt) exposes increment, start and stop actions, while the real check-off widget was already exercised. This is a bounded verification gap, not a known widget defect. | Exercise one pinned Count Habit increment and one pinned Timer Habit start/stop/review through the existing launcher path, including a return or process-recreation boundary. Fix only a reproduced failure. | Exact actions, saved log/session state and visible widget state agree after return or process recreation; retain the small receipt. |

## Completion

- **N-01 · Verified:** Track Insights names the fixed 30-day window; the calculation and `0.93 Entries` fixture value are unchanged. The focused journey passes, and the [rendered detail](../../artifacts/quality/2026-09-27/track-rate/tracks.detail.insights.recent-windows.png) shows the two-line label without clipping.
- **N-02 · Verified after one shared fix:** TalkBack spoke the changed heading and disabled destructive button correctly. It initially spoke only “Warning” for the shared warning Card; `WhipNoticeCard` now merges its non-interactive text into the status node. The [spoken-output receipt](../../artifacts/quality/2026-09-27/talkback/README.md) retains before and after evidence and the exact announced words.
- **N-03 · Verified:** A pinned Pixel Launcher Habit widget on API 34 recorded one Count increment and one Timer session through separate managed process deaths, then showed the saved duration in Habit History. The [widget receipt](../../artifacts/astra-audit/2026-09-27/habit-widget-modes/README.md) retains exact Room and visual evidence. No widget product defect reproduced.

## How to extend this plan

Add one row with an ID, user consequence, current reproduction or source evidence, the smallest existing code owner, and an observable finish line. Mark it `Proposed`, `Investigating`, `Verified`, or `Rejected` in the product-memory ledgers as work proceeds. Move completed facts to those ledgers and a linked verification receipt; keep this page short.

Do not schedule another complete visual catalog, exhaustive OEM/provider matrix, speculative performance tuning, component framework, or broad aesthetic rewrite from old audit wording alone. Reopen a specific part when a current defect, owner report, or explicit audit authorization supplies a concrete reason. The 167 unfinished rows in the frozen September audit are review debt, not 167 confirmed defects.
