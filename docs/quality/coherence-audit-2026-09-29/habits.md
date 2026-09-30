# Habits coherence audit — 2026-09-29

Baseline: `6d79bd1f`. Component: Habits, including its Home links and shared authoring/inspector controls. This is a review report, with no production or test edits, builds, instrumentation, commits or canonical-memory changes by this reviewer. The coordinating parent owns fresh device acquisition and any later implementation decision.

## Result and evidence limits

Reviewed **64 flow/state dispositions** below. Four current-source findings remain: threshold-ending dates are inconsistently evaluated; numeric quick-action text has an unbounded label in a fixed clipping lane; partially checked Checklists offer an impossible Skip; and today's manual duration opens a past-entry-titled form. H1–H3 are P2; H4 is P3. None is presented as an executed device reproduction. No P0/P1 issue was established in this component review.

The current normal root was personally inspected from the parent-produced fresh original PNG/XML. Ten additional original PNGs and their corresponding XML were inspected as dated historical context. Their appearance is useful evidence of the actual surfaces shown, but their earlier tests/captures do not certify this baseline or unshown journeys. Source review and existing test contracts are distinguished from execution throughout.

The component has **Habits / Today / Insights**, with Archived in More, and Today / History / Insights / Options inside a Habit. There is no root Habit destination named Activity. The requested activity job is covered by History, Recent Activity, and the Insights activity charts rather than an invented missing tab.

Memory read/reconciliation: `AGENTS.md`, the maintain-whip-memory skill, the complete product-memory Index, relevant current feedback/decisions/findings, and the linked fresh productivity, deep review and experience-overhaul reports. In particular, FB-20260929-010's collection-first entry, FB-20260929-005's single toolbar search, FB-20260928-008's retained history, DEC-20260928-001's historical-pause boundary, FND-20260929-027/030/031's completed remedies and FND-20260928-010/012 were rechecked against current owners. The four findings below are not restatements of those completed remedies.

## Source key

- **HS**: `app/src/main/java/com/whip/app/ui/HabitScreens.kt`.
- **HV**: `app/src/main/java/com/whip/app/ui/HabitViewModel.kt`.
- **HR**: `app/src/main/java/com/whip/app/data/HabitRepository.kt`.
- **HM**: `app/src/main/java/com/whip/app/domain/HabitModels.kt`.
- **WA**: `app/src/main/java/com/whip/app/ui/WhipApp.kt`.
- **HE**: `app/src/main/java/com/whip/app/ui/HomeHabitValueEditor.kt`.
- **EI**: `app/src/main/java/com/whip/app/ui/EntityInspector.kt`.
- **PE**: `app/src/main/java/com/whip/app/ui/ProductivityEditorComponents.kt`.
- **PB**: `app/src/main/java/com/whip/app/ui/WhipProductivityItemBuilder.kt`.
- **IC**: `app/src/main/java/com/whip/app/ui/ItemControlPatterns.kt`.
- **RS**: `app/src/main/java/com/whip/app/reminders/HabitReminderScheduler.kt`.

Line numbers refer to the baseline source. **S** = source traced; **F** = personally inspected current parent-produced PNG/XML; **H** = personally inspected historical original PNG/XML; **T** = existing test source inspected, not run. Keep is a justified source/design disposition, not a claim of fresh end-to-end acceptance. Unknown explicitly reserves a runtime/platform boundary.

## Flow coverage

| # | Entry → intended result / relevant states | Owner and source lines | Evidence | Verdict |
|---|---|---|---|---|
| 01 | First Habits navigation → collection, with restored choice respected | HS `HabitAreaContent` 176–183; WA 946–948 | S, F | Keep collection-first entry; no additional landing page. |
| 02 | Habits / Today / Insights → distinct management, execution and evidence jobs | HS 134–139, 360–482 | S, F | Keep three jobs and shared root frame. |
| 03 | Workspace More → Archive → Back to originating destination | HS 183–186, 380–389 | S | Keep saved archive return. External-source Back remains intentional. |
| 04 | Toolbar Search → unified lookup without duplicate collection field | WA toolbar search; HS 360–482, 1492–1666 | S, F | Keep the one top-right entry; History search has a narrower separate job. |
| 05 | Empty collection → Templates or creation | HS 438–461, 1570–1577 | S | Keep useful explanation and Browse Templates route. |
| 06 | Empty Today with saved off-schedule Habits → View All Habits | HS 395–430 | S; T `HomeHabitRecoveryUiTest` | Keep explicit no-due recovery. |
| 07 | Empty selected Area → Show All Areas | HS 395–420, 444–452 | S | Keep meaningful scoped-empty wording and recovery. |
| 08 | Domain load / load failure → truthful placeholder and Retry | HS 187–200; HV 300–329 | S | Keep distinct settled/load/error behavior; fresh failure rendering unknown. |
| 09 | Management card → schedule, Area, availability → detail/Edit | HS `HabitProgressCard` 1116–1130; `managementAvailabilityLabel` 1336–1342 | S, F, H | Keep compact management cards; threshold-ending label gap is H1. |
| 10 | Today pending → direct execution and disclosure | HS 1133–1235 | S, H | Keep direct mode-specific primary action; H2 for numeric label bounds. |
| 11 | Complete/Skip → Finished disclosure, with review/Undo reachable | HS 1461–1490, 1524–1548, 1635–1666 | S; T `HabitSkipJourneyE2ETest` | Keep finished partition, neutral skips and automatic collapse. |
| 12 | All finished → All Set for Today while retained evidence stays below | HS 1581–1587 | S | Keep explicit successful empty work queue. |
| 13 | Reorder → pinned partitions and accessible move controls | HS 381–389, 1539–1564, 1610–1634; HR 233–244 | S | Keep full-collection restriction and Show All Areas recovery. |
| 14 | Add Habit / choose template → editable draft before commit | HS 329–336, 485–516, 1966–2043 | S | Keep templates as drafts and the established shared editor. |
| 15 | Check-off creation → one scheduled check with numeric controls removed | HS 2364–2402, 2480–2500; HM 100–145 | S | Keep dependent settings and normalization. |
| 16 | Checklist creation → named ordered items and independent/automatic parent policy | HS 2408–2469; HM 174–190 | S | Keep item identity, reorder and explicit parent-completion choice. |
| 17 | Count/Decimal creation → positive quick increment/presets and target | HS 2472–2481, 2564–2613, 2730–2748; HM 155–166 | S | Keep quick values progressive; H2 for primary-label sizing. |
| 18 | Duration creation → duration dimension, selected unit and target | HS 2374–2384, 2502–2563; HM 186–188 | S | Keep duration validation and units. |
| 19 | Rating / LogOnly → suitable input and no required numeric target | HS 2374–2384, 2564–2613; HM 100–145 | S | Keep optional target contract; decimal Rating display precision merits later product clarification, no new defect asserted. |
| 20 | Linked measurement definition → read-only manual-entry contract | HS 2368–2400, 1344–1350; HR 901–917; HV 1276–1323 | S | Keep existing source attribution/guards; no new Health setup proposed. |
| 21 | Target AtLeast/AtMost/Exactly/Range/None → active fields only | HS 2206–2268, 2564–2613; HM 100–145, 196–206 | S | Keep one semantic validation owner; hidden irrelevant values do not leak. |
| 22 | Target Day/Occurrence/Week/Month/RollingDays → explained aggregation scope | HS 2590–2613, 3581–3601; HM 685–693 | S | Keep visible period wording and positive rolling window. |
| 23 | Unit change → convert active targets/quick amounts, preserve original log units | HS 2520–2560; HR 155–169; HM 349–361 | S | Keep compatible conversion, finite-draft rejection and affine-zero guard. |
| 24 | Custom measurement dimension/unit → create compatible unit | HS 2503–2563; shared `UnitSelectionField` | S | Keep shared picker; real provider/unit-creation failure unknown in this audit. |
| 25 | Daily / selected weekdays / Every N → expected check-in cadence | HS 2616–2650; HM 537–556 | S | Keep explicit selected weekdays and positive interval. |
| 26 | Flexible week/month → period quota with no artificial daily skip | HS 2623–2638; HM 363–401, 487–534; HR 499–502 | S | Keep flexible quota and activity-vs-target distinction. |
| 27 | Reminder defaults / weekday overrides → structured times, permission request | HS 2654–2730; RS 105–172, 296+ | S | Keep structured editing and scheduler ownership; actual closed-process delivery unknown. |
| 28 | Schedule options → end date / threshold / week-start meaning | HS 2670–2741; HM 204–214, 582–623 | S; T `HabitRulesTest` | Keep inclusive OnDate; H1 for threshold end propagation. |
| 29 | Additional notes/Tags/Area/precision → explicit optional organization | HS 2742–2763, 2257–2268; shared Area/Tag owners | S | Keep progressive details and inherited Area context. |
| 30 | Invalid draft → visible validation / reveal hidden problem / no invalid commit | HS 2253–2314, 2749–2773; HM 147–215 | S | Keep actionable summary and scroll/reveal behavior. |
| 31 | Dirty editor Cancel/Back / save failure / busy → preserve draft and retry | HS 2314–2331, 2353–2364, 2749–2782; HV 453–509 | S, H | Keep dirty fingerprint, request receipt and input blocking. |
| 32 | Existing definition edit → compatible mode/dimension, exact checklist identity | HS 2045–2078, 2785–2792; HR 155–216, 881–900 | S | Keep no measurement-kind reinterpretation and archived item preservation. |
| 33 | Definition edit from inspector → cancel/save returns to same section | HS 203–207, 307–319, 518–544; EI 181–185 | S; H retained History; T `FreshProductivityUiTest` | Keep stable ID-keyed inspector. Previously fixed context defect not reopened. |
| 34 | Check-off primary → complete / undo exact today's occurrence | HS 1148–1160, 1445–1459; HR 448–459, 923–942 | S | Keep check action and live repository contract. |
| 35 | Checklist items → independent checkboxes / optional parent completion | HS 1385–1442, 3209–3227; HR 460–493 | S, H | Keep semantic row checkbox, decorative inner checkbox, 48dp height and explicit policy. |
| 36 | Count/Decimal quick add → additive entry | HS 1176–1177, 1352–1383; HV 798–830; HR 283–350 | S, H | Keep entry addition; H2 for clipped numeric action. |
| 37 | Add Amount vs Set Total → clear delta semantics | HS 2794–2895, 665–677; HV 832–866; HR 352–374 | S, H | Keep additive vs total distinction and exact finite delta tolerance. |
| 38 | Decrement / Undo Last Entry → correct prior evidence without ambiguous reset | HS 1371–1382, 3211–3214; HV 824–830, 867–909 | S, H | Keep bounded decrement and exact log deletion; linked source does not expose these edits. |
| 39 | Rate Today → append latest rating; optional note | HS 2794–2895; HV 840–846; HM 414–431 | S | Keep rating capture distinct from period delta. |
| 40 | LogOnly note-only check-in → authored nullable entry | HS 2830–2874; HR 307–342, 405–435 | S; T `HabitRepositoryTest` | Keep null value, no invented zero measurement. |
| 41 | Malformed/nonfinite current amount → useful input error before commit | HS 2821–2826, 2854–2859, 2882–2888 | S; T `FreshProductivityUiTest` | Keep current validation remedy; no old silent-invalid report. |
| 42 | Manual duration from Today → today's duration entry | HS 584–586, 685–690, 3189–3197, 2939 | S | H4: wrong past-entry title despite today date. |
| 43 | Start timer → session persisted and elapsed shown on Home/All/Today | HS 973–1026, 1116–1133, 1162–1175; HV 935–963; HR 529–607 | S, H wide timer | Keep timer reachable even in management and recovery states. H1 for post-threshold start. |
| 44 | Stop timer → one canonical duration log / exact session boundary | HV 966–984, 1048–1082; HR 609–642, 770–849 | S | Keep idempotent terminal session and canonical-unit log. |
| 45 | Reboot/clock/restore uncertainty → review estimate, continue/log/discard | HS 1028–1091; HV 987–1033; HR 644–747 | S, H short/IME; T `HabitUxPresentationUiTest` | Keep estimated copy and safe clock/session ownership; simultaneous action/busy UX unknown. |
| 46 | Running timer + pause/archive/unit/schedule edit → protect active session | HR 169–194, 223–231; HS 2531–2560 | S | Keep repository protection. Schedule controls can reach save rejection; no demonstrated data corruption. |
| 47 | Skip pending day → neutral saved exception and reminder suppression | HS 605–624, 3095–3100, 3229–3243; HR 495–521; HM 558–579 | S; T `HabitSkipJourneyE2ETest` | Keep neutral semantics; H3 for checked-item prerequisite. |
| 48 | Undo today's/historical skip → exact exception removal in place | HS 565–575, 3288–3294; HV 780–797; HR 524–527 | S | Keep exact-record check and parent inspector continuity. |
| 49 | Pause indefinitely → stop current expectation without rewriting past | HS 3336–3345; HM 403–412, 752–763 | S; H management | Keep distinction from dated pauses and historical explanation. |
| 50 | Schedule/edit/delete dated pause → neutral dates and correction impact | HS 3679–3835, 589–592, 755–789; HR 246–281 | S, H | Keep editable end/no-end date, history-impact text and dirty/save/delete guards. |
| 51 | Add past check-in/rating/note → earlier date with truthful units/outcome | HS 580–583, 2898–3039; HR 283–350 | S | Keep future-date guard and entered unit; H4 also supports date-neutral reusable title. |
| 52 | Edit existing history → original value/unit/status and note/date correction | HS 2913–3000, 714–752; HR 386–446 | S, H | Keep original outcome and nullable meaning; available while paused/archived. |
| 53 | Delete history repeatedly → child closes, parent History/query remains | HS 307–319, 740–752, 3015–3031; EI 181–185 | S, H; T repeated-cleanup fixture | Keep fixed workflow; no forced return to collection. |
| 54 | Search History / no matches / More History → find exact event | HS 3079–3090, 3259–3269, 3305–3310, 3484–3508 | S, H | Keep observable locale and count; final-event deletion with nonblank query leaves no-match Clear route. |
| 55 | Linked measurement History → readable attribution, no synthetic editing | HS 3254, 3280–3286, 3643–3670; HV 1276–1323 | S | Keep source-owned records read-only. |
| 56 | Insights empty/all-time totals/average → truthful sparse evidence | HS 1680–1725 | S, H | Keep no-activity explanation and typed totals/average; archived root scope intentionally separate. |
| 57 | Consistency chart → 8 weeks with gaps/scale/date/spoken observations | HS 1727–1755, 1761–1777, 1876–1917 | S, H | Keep accessible values and activity-vs-success explanation; H1 for post-ending scored dates. |
| 58 | Recent Activity grid → dated states, symbols and accessible names | HS 1780–1872; HM 752–775 | S, H | Keep non-color states and date-qualified semantics; H1 for threshold lifecycle evaluation. |
| 59 | Low-pressure/no-target/flexible evidence → activity without invented success | HS 1694–1695, 1709–1723, 1733–1746; HM 674–683 | S, H | Keep useful evidence without conflating recorded and completed. |
| 60 | Archive/Restore, Duplicate, Pin → preserve definition/history contract | HS 544–546, 593, 3108, 3318–3378; HR 217–231 | S | Keep bounded copied name, no copied history, direct Restore and due-only Home pin semantics. |
| 61 | Permanent deletion → exact reviewed impact, conflict/retry and nonrepeat submit | HS 801–912; HV 332–450, 516–691; deletion coordinator | S | Keep separate danger flow, data-count disclosure, active-timer warning and fresh-review requirements. |
| 62 | Home card/section/deep link → exact Habit inspector or explicit Today | WA 2077–2101, 5272–5336; HE 17–67; HS 337–352 | S, H shared inspector/card | Keep direct Home actions, scoped ID lookup and explicit Today section route. |
| 63 | Normal/enlarged reading / touch / keyboard / short dialog → reachable controls | HS 1389–1418, 2080+, 1028–1091; PB 85–121; PE 458–594; EI 121–192 | S, F, H | Shared text/checkbox/dialog patterns are coherent. H2 numeric-lane exception. Fresh nested 200%/hardware focus unknown. |
| 64 | Wide/split-pane and recreation → pane-aware children and stable state | HS 176–186, 310–319; PE 569–589; EI 104–135, 181–185; WA pane routing | S, H wide | Keep shared pane placement and saved context; fresh current wide/short rendering remains parent scope. |

## Confirmed current-source findings

### H1 — P2: threshold-ended Habits keep accumulating misses and look actionable in the inspector

**Source proof.** HV `buildProgress` explicitly calls `hasEnded` at **1369** and suppresses `scheduled` at **1378**, but separately calls `dayStateOn` at **1387**. HM `dayStateOn` **752–775** uses `followsScheduleOn` and never evaluates the threshold ending. `followsScheduleOn` **541–556** knows only start date and OnDate ending. HM `completionRateOverRecentPeriods` **499–506** uses that same cadence-only predicate. HS `scheduledCompletionRateForDays` **1769–1775** and `HabitActivityGrid` **1811–1830** consume the inconsistent state. The inspector primary falls through to regular Check In at **3118**, and `inspectorStatus` **3521–3532** says Ready today. The Count/Check-off inspector summary also reads as normal current work. For Duration, HR `startTimer` **545–549** guards only OnDate ending.

**Reproduction to execute.** Create a Daily check-off Habit with End After Completions = 1 and a start date of September 28. Record its one success on September 28; inspect it on September 29 and again September 30. Today correctly omits it. Habits correctly says no check-in is expected, but the inspector says Ready today/Check In. At September 30, September 29 becomes Missed in Recent Activity and the regular recent completion rate includes a false failed day (one success / two closed outcomes = 50%). Equivalent endings after streak or total follow the same cadence-only state path. Existing `HabitRulesTest#thresholdEndingsStayEndedAndSuppressReminders` tests ended/reminder behavior, not this state/chart/rate gap.

**Impact.** Users who chose a finite habit are told they failed days after fulfilling it. The same Habit has incompatible availability across collection, inspector and Insights, and a Duration can offer Start Timer after its threshold ending. This undermines both long-term evidence and the reason to choose an ending rule. The selected ending-reaching date must still retain its earned completion.

**Remedy.** Resolve threshold-ending cutoff through the existing domain owner and apply it consistently to expected dates, current availability and analytical denominator/state. Preserve genuine history and the completion that reaches the threshold; allow any intentional later entry through explicitly outside-schedule wording if that is the desired product rule. Avoid directly inserting `hasEnded` into its own completion/streak calculation chain: `hasEnded` currently depends on `successfulPeriodOutcomeDates`. Add narrow contracts for all three threshold rules, reaching day, following days, sparse/neutral periods and corrections that legitimately alter the ending evidence. Freshly inspect collection → inspector → Insights for the same Habit. No migration is needed merely to correct derived evaluation.

### H2 — P2: valid numeric quick increments can be silently clipped in the primary action

**Source proof.** HS **1176–1177** renders `+${editableNumericValue(habit.quickIncrement)}`. HS **1233–1235** fixes Count/Decimal action width at 64dp. PB **120–121** assigns that exact lane. IC `ItemPrimaryTextButton` **1031–1049** forces a single nonwrapping line with `TextOverflow.Clip`. HM **156–162** only requires a positive finite increment; it has no label-length limit. NumericSequence's `editableNumericValue` preserves an ordinary whole amount as its complete number string. Goal's recently repaired measured action width does not reach this Habit caller.

**Fresh parent reproduction after source review.** Create Count with Quick increment `1000000000`, then open Today. The parent personally inspected [the current normal original](../../../artifacts/coherence-audit/2026-09-29/fresh/manual/habits.large-increment.normal.png) and matching XML: XML retains `+1000000000`, while the visible action clips the amount to a short prefix. At actual system font scale2.0, [the current enlarged original](../../../artifacts/coherence-audit/2026-09-29/fresh/manual/habits.large-increment.native200.png) visibly reads only `+100`. Both originals were inspected; font scale was restored. The action attempts to place eleven visible characters inside a 64dp slot minus padding. This is fresh manual evidence, not another instrumentation pass. Shorter everyday amounts remain a separate probe; measure `+5000` rather than assuming an exact clipping threshold from source. Habits management has no daily quick button and therefore does not expose this particular defect.

**Impact.** The primary action may display only a prefix while recording the full amount. Users cannot visually establish the size of the action they are about to take, precisely where a direct daily action should be fastest and clearest. Semantics retaining the full text does not repair the visible amount.

**Remedy.** Reuse the shared measured action-width strategy, with a bounded maximum and an explicit short label such as Add/Log when a numeric amount cannot fit; retain full accessible amount/unit and show the amount in supporting evidence or the existing expanded actions. Avoid shrinking text or weakening large-text identity. Verify a typical increment, a longer valid increment, decimal/localized values and actual 200% text on Today/Home, using `TextLayoutResult` completeness and fresh originals.

### H3 — P2: a partially checked Checklist offers Skip Today even though it cannot be skipped

**Source proof.** HS `skipAvailable` **3095–3100** checks Pending, source and flexible mode but does not inspect checked items. HR `toggleChecklistItem` **475–491** stores item state without recording parent completion until the configured parent-completion condition. HM `outcomeForPeriod` **433–445** therefore remains null while only an item is checked, and HM `dayStateOn` **771–774** yields Pending. HS **3229–3243** offers Skip Today; confirmation **605–624** promises a neutral skipped day. HR **507–508** rejects the operation with “Clear the completed checklist items before skipping this day.” This happens with auto-complete enabled until the final item; with auto-complete disabled it can happen even after all items are checked.

**Reproduction to execute.** Create a Daily Checklist with two items. Check one item without completing the parent Habit. Open Today detail → Skip Today → confirm. The user is led through a promise of success and only then receives the repository error. Existing source contracts test ordinary skip/undo and finished checklist behavior, not this prerequisite mismatch.

**Impact.** An apparently available action is impossible in the current state. The prerequisite is especially confusing because the parent still says Ready today and the checkbox list looks like ordinary in-progress work. Authored item state is protected by the repository, so this is a preventable failed interaction rather than silent data loss.

**Remedy.** Include completed checklist state in the existing skip availability predicate, and show the clear-items prerequisite beside a disabled or omitted Skip control. Preserve checked items unless the user explicitly clears them; do not silently turn partial work into a skip. Use the same eligibility explanation in confirmation and repository. Verify both auto-complete policies and an ordinary empty checklist skip followed by Undo.

### H4 — P3: Enter Duration Manually from Today opens a form titled Log an Earlier Day

**Source proof.** HS **584–586** sets `historicalLogForToday = true`. The reused child chooses today's date at **690** but calls the same `HabitHistoryLogDialog`. Its title at **2939** calls `historyDialogTitle(editing = false)`, and Duration takes the generic “Log an Earlier Day” branch at **3624**. The visible action **3189–3197** instead promises to record a duration when the timer was forgotten.

**Reproduction to execute.** Open an available Duration Habit → Today → Enter Duration Manually. The form is correctly dated Today, while its heading says Log an Earlier Day. Record a duration and confirm that persistence uses the selected date; this report identifies misleading orientation rather than an asserted wrong-date write.

**Impact.** Users checking in today must reconcile conflicting intent/date language and may unnecessarily change the date or abandon recording. The shared form is appropriate; its presentation has lost the caller's purpose.

**Remedy.** Give the reusable child a date-neutral heading such as Record Duration/Record Check-In, or pass the existing current-vs-history intent through to the title. Retain the date picker and the selected date. Verify current manual duration, historical duration and existing-log editing titles without adding another form owner.

## Original-image inspection

Fresh evidence available to this reviewer: `artifacts/coherence-audit/2026-09-29/fresh/productivity/habits.all.populated.png` and `.xml`, produced by the parent for this audit. The image shows a clear Habits-selected three-tab root, common toolbar, two compact management cards, full identities, concrete Everyday cadence and Pending state. XML has 123 nodes and zero `NAF="true"` flags. Add Habit, Search Habits and More Habit Actions are explicitly named. This pair is a limited seeded overview, not evidence for H1–H4 or every Habit state.

The following **historical originals** were personally opened using `view_image`, and paired XML was parsed to check the represented state/control names. Each inspected XML contains zero NAF flags; this alone is not a TalkBack or focus-order test.

| Historical original PNG (XML at same path stem) | Review observation and present limit |
|---|---|
| `artifacts/fresh-app-overhaul/2026-09-29/after/productivity/habits.all.populated.png` | Compact management identity/schedule/availability rows are clear. Fresh root above corroborates the current seeded layout. |
| `artifacts/fresh-app-overhaul/2026-09-29/after/large-productivity/habits.all.populated.png` | Ordinary two-card enlarged root reads completely, with common stacked bottom navigation. Current enlarged reproduction remains parent-owned. |
| `artifacts/fresh-app-overhaul/2026-09-29/after/productivity-detail/fresh-productivity.habit-management.png` | Selected Tue/Fri schedule plus paused/no-check-in state are explicit, avoiding the earlier management information deficit. Isolated light-theme component frame. |
| `artifacts/deep-product-review/2026-09-29/after/deep.habits.numeric-details.png` | Today leads with exact current/target/unit/date, then Add Amount/Set Total/decrement/Undo. Daily action dock is obvious. Numeric example is not the long primary label in H2. |
| `artifacts/deep-product-review/2026-09-29/after/deep.habits.execution.large.png` | Enlarged Checklist inspector has full title, scrollable body and visible primary completion dock. Horizontal section overflow advertises access to Options. |
| `artifacts/deep-product-review/2026-09-29/after/deep.habits.focused-insights.large.png` | Enlarged analytical hierarchy separates count/streak/30-day result from chart. Lower chart continues through body scrolling rather than an asserted clipped layout. |
| `artifacts/deep-product-review/2026-09-29/after/experience.habits.retained-history.png` | History/query/count/current record remain together after cleanup. Single remaining note entry is not relabeled as numeric zero. |
| `artifacts/ux-overhaul/2026-09-27/after-short/ux-upgrades.habits-timer-review.large.png` | Short enlarged timer review with keyboard shows estimate input and complete Continue/Stop actions. Earlier fixture; no fresh busy/error proof. |
| `artifacts/astra-audit/2026-09-09/task-ime/final/habits.editor.ime-large.png` | Historical enlarged Create Habit has visible heading/Save above keyboard and a scrollable authoring body. Current shared safe-drawing/IME owner was rechecked in source. |
| `artifacts/astra-audit/2026-09-10/workspace-layout/wide/habits.productivity-builder.timer-expanded.png` | Wide running-timer card and pane routing were visually coherent in that dated build. Old four-tab labels differ from the current three-tab contract and are not current UI findings. |

The personally inspected images are **one fresh Habit root plus ten historical Habit originals**, eleven total. Evidence acquisition or later parent image review can extend this list without retroactively changing this review's execution claims.

## Justified keeps and remaining unknowns

The collection and execution views have different jobs. It is reasonable that Habits management prioritizes Edit and schedules while Today/Home prioritize check-in. The fresh normal overview supports retaining the compact management rows rather than another aesthetic redesign. Single toolbar search, common tab/header geometry, shared pane-aware forms, common identity/status/action hierarchy and full large-text identities are suitable existing owners. H2 is a concrete bounded-lane exception, not a reason to fork the card system.

Historical corrections should remain local. Existing History uses an observable locale, saved query/pagination and a stable Habit ID key; successful authored mutations close only the completed child. Definition editing likewise retains inspector state. Parent History should not be forcibly closed after each correction. Whole-Habit archive/delete intentionally leave a context whose availability changed, and Restore is already an obvious archived primary action.

Units and outcomes should keep their current meaning. Set Total records a delta; Add Amount appends an entry; quantitative logs retain entered units and canonical values; note-only entries remain nullable authored facts. Linked measurement data is attributed and read-only in Habit History. Independent Checklist completion is explicitly explained. Low-pressure/no-target/flexible charts label recorded activity rather than manufacturing completion. Current pause flag affects current availability, and dated pauses govern historical neutrality. These constraints matter when fixing H1.

No fresh complete Habit journey, process-death/boot timer, actual closed-app reminder, launcher widget, backup restore, TalkBack spoken-order, hardware keyboard, RTL, current short/200% nested form or current wide-pane journey was executed by this reviewer. Existing test source and historical receipts suggest useful acceptance selectors but cannot substitute for fresh execution. In particular, timer review has no dialog-local busy/error parameter and the ViewModel permits competing launched resolution requests; repository session-state guards prevent straightforward duplicate logging, but whether a user can see a stale retry/error during a real slow operation needs a current runtime reproduction before promoting it to a finding. Existing running-timer schedule-edit rejection is safe but can be made clearer only after establishing the desired edit availability policy.

The next proportionate acceptance set is H1's reaching/end-following day calculations and a shared inspector/Insights rendering; H2's exact numeric text layout on Today/Home normal/200%; H3's partial Checklist skip prerequisite; and H4's current/manual/history titles. Parent-produced broad root captures alone do not close these four findings. No broad test/candidate run was requested or performed by this reviewer.
