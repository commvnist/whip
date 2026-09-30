# Review & Trends coherence audit — 2026-09-29

Baseline: `6d79bd1f82da9c1666960834410b3e08ed6d3ea1`. Scope: FB-20260929-017, audit only. This component report changes no application code, tests, canonical memory, device data or release state. Severity is relative to actual harm: P1 false outcome truth; P2 material coherence/recovery failure; P3 smaller presentation issue. “Confirmed source” means the reachable production branches establish the behavior; it does not mean a fresh instrumented reproduction ran.

Review has a coherent core: one presentation projection explains both totals and contributing rows; heterogeneous Track values remain evidence; partial sources do not masquerade as zero; source navigation retains the analytical session. The principal remaining problems are period-based Habit counts, archive-dependent Gym history, and a current-period empty state that hides available 30-day comparisons.

## Evidence and boundaries

- Personally inspected the fresh original [summary PNG](../../../artifacts/coherence-audit/2026-09-29/fresh/shared-shell/catalog/shared.review.summary.png) with `view_image`, and read its paired [hierarchy XML](../../../artifacts/coherence-audit/2026-09-29/fresh/shared-shell/catalog/shared.review.summary.xml). This frame is current baseline evidence. It shows the Review title/close action, collapsed options, exact Sep 23–29 range, Tasks total 1, Habit outcomes 0, Goal progress 0, independently labeled daily scales and reachable card affordances. It does not show below-fold Gym/Tracks/correlations, drilldowns, keyboard operation or source correction.
- Read `AGENTS.md`, the maintain-whip-memory skill and `docs/product-memory/INDEX.md` completely. Reconciled FB-20260929-017, FND-20260929-029/-003, FND-20260929-022, FND-20260928-010/-003 and DEC-20260910-008/-009/-010/-011 against current owners. Previous reports and verification receipts are dated maps, not fresh acceptance.
- Read the full current Review UI/session/availability/projection owners and Review JVM/native tests as source; traced Area filters, the shell host, original-source resolution, persisted state producers, Habit target-period and Goal outcome functions, and correlation mathematics. No builds, tests, emulator actions, reset or commit were performed by this reviewer. Test assertions below describe intended contracts, not executed results.
- Parent owns the already identified Goal baseline defect G1. This report records its Review dependency without assigning it a second root-cause finding.

## Current dependency map

The shell passes Area-filtered Task/Habit/Goal states and global Gym state to `ReviewAppRoute`; Track evidence explicitly receives the unscoped Track state ([WhipApp.kt:2587](../../../app/src/main/java/com/whip/app/ui/WhipApp.kt#L2587), [AreaScopeFilters.kt:35](../../../app/src/main/java/com/whip/app/ui/AreaScopeFilters.kt#L35)). Period/sections are saved Settings preferences. The date anchor is `taskState.currentDate`, and Task completion timestamps and milestone timestamps use the configured Settings zone ([ReviewAppRoute.kt:75](../../../app/src/main/java/com/whip/app/ui/ReviewAppRoute.kt#L75), [ReviewDialog.kt:120](../../../app/src/main/java/com/whip/app/ui/ReviewDialog.kt#L120)).

Availability selects only ready included outcome domains, plus Tracks independently. Review computes outcomes for at least the last 30 days; visible totals/drilldowns use the selected seven-day or month-to-date dates. Correlations always use the last 30 calendar days. Current formulas are: one completed Task occurrence, one successful Habit outcome date, positive normalized Goal contribution, and one finished Workout ([ReviewOutcomes.kt:37](../../../app/src/main/java/com/whip/app/ui/ReviewOutcomes.kt#L37)). The same rows supply daily values and details; the screen does not persist a second history payload.

Content keys deliberately exclude live projected Goal progress, timer ticks and rest countdowns, but retain current Goal definitions, observations and milestones ([ReviewSourceContent.kt:29](../../../app/src/main/java/com/whip/app/ui/ReviewSourceContent.kt#L29)). This is a current-definition recalculation. Completed Goal overview values can instead be frozen closure facts ([GoalViewModel.kt:702](../../../app/src/main/java/com/whip/app/ui/GoalViewModel.kt#L702)). These are distinct facts, not interchangeable numbers.

`ReviewSession` saves open/return status, originating Area and nested presentation state. Visiting a source hides Review and shows a return affordance; returning restores Area and details/list context; explicit close removes the Review saved-state bucket ([ReviewSession.kt:32](../../../app/src/main/java/com/whip/app/ui/ReviewSession.kt#L32)). Domain editing remains owned by the original inspector/repository.

## Findings and concrete remedies

### RV-01 — Weekly/monthly Habit targets become repeated, retrospectively backfilled daily outcomes

**P1 · confirmed source · analytical truth.** Configure a numeric Habit with a Daily schedule and a Per week target, then record the complete target on Friday. A Review through Friday counts Monday–Friday as five successful Habit outcomes, even when Monday–Thursday have no logs. For a closed full week it can count seven. A Per month target can similarly become many daily outcomes. This is a supported editor configuration: all `TargetPeriod.entries` are offered, and copy says “All entries in the week count toward one target” / “All entries in the month count toward one target” ([HabitScreens.kt:2599](../../../app/src/main/java/com/whip/app/ui/HabitScreens.kt#L2599), [HabitScreens.kt:3912](../../../app/src/main/java/com/whip/app/ui/HabitScreens.kt#L3912)).

**Root cause:** The non-flexible branch of `successfulPeriodOutcomeDates` iterates each scheduled day and asks `outcomeForPeriod` for the same whole week/month ([HabitModels.kt:635](../../../app/src/main/java/com/whip/app/domain/HabitModels.kt#L635)). `valueForPeriod` / `hasData` read the whole `periodBounds(date)`, including later logs within that period ([HabitModels.kt:414](../../../app/src/main/java/com/whip/app/domain/HabitModels.kt#L414), [HabitModels.kt:433](../../../app/src/main/java/com/whip/app/domain/HabitModels.kt#L433), [HabitModels.kt:685](../../../app/src/main/java/com/whip/app/domain/HabitModels.kt#L685)). Review emits one unit contribution for every returned date ([ReviewOutcomes.kt:49](../../../app/src/main/java/com/whip/app/ui/ReviewOutcomes.kt#L49)). This changes reward count and creates false nonzero days that can satisfy the seven-day correlation threshold. It is distinct from the Habit audit's threshold-ended scheduling issue H1.

**Why this is a defect rather than an alternate daily success metric:** The UI names Habit “outcomes,” each detail row says “Habit target reached,” domain copy explicitly calls this “one target period,” and the editor says one weekly/monthly target. Flexible weekly/monthly schedules already emit one period reward on the achieving date. The non-flexible branch turns the same discrete period into repeated rewards and uses evidence recorded later to claim earlier attainment. Daily/Occurrence targets and continuously overlapping rolling-window targets require their own semantics and are not included in this finding. Parent additionally traced `HabitModels.kt:591–597`: End After Completions uses this same outcome-date count, so one weekly attainment can prematurely fulfill a multi-completion ending. This extends the source-proven impact beyond Review; no native threshold-ending variant was executed.

**Minimal remedy:** At the shared Habit outcome owner, emit one outcome per discrete Week/Month target period and choose an honest dated attainment/period-close rule. AtLeast can use the first qualifying accumulated observation date; AtMost/Exactly/range need a settled-period rule or explicitly provisional status because later entries can change success. Do not patch only Review counts while streak/end-condition users keep inconsistent periods. Preserve Day/Occurrence and deliberate rolling-window accounting. Future-dated observations must not award an earlier outcome in a Review ending before them.

**Acceptance:** Source-level cases for Daily+Week and Daily+Month, one target attained late in period, sparse logs, multi-log attainment, future-dated logs, week-start variants, AtLeast/AtMost/Exactly/range, and partial start/end periods. One attained discrete period contributes once; no day preceding attainment is labeled reached. Native Review shows matching total/chart/drilldown; correlation cannot pass its observed-day gate from a single weekly attainment. Existing flexible period and neutral-date behavior must remain. Current `HabitRulesTest` covers flexible one-period rewards and period bounds, not this non-flexible combination (`:284`, `:369`); `ReviewOutcomesTest:65` covers daily numeric increments only.

### RV-02 — Archiving a finished Workout removes its earned Review outcome

**P2 · confirmed source · cross-domain history coherence.** Finish a Workout, see its Review contribution, then archive the session. Review's workout total and contributing rows decrease, and its correlations may change, although the original session remains saved. Tasks/Habits/Goals retain archived outcomes and identify them as Archived. Gym does not.

**Root cause:** Production `GymUiState.history` includes only Finished and **not archived** sessions; archived sessions go to `archivedWorkouts`, while `allSessions` retains both ([GymViewModel.kt:2734](../../../app/src/main/java/com/whip/app/ui/GymViewModel.kt#L2734)). Review reads only `gym.history` ([ReviewOutcomes.kt:69](../../../app/src/main/java/com/whip/app/ui/ReviewOutcomes.kt#L69)), and its memoization input keys only that list ([ReviewSourceContent.kt:34](../../../app/src/main/java/com/whip/app/ui/ReviewSourceContent.kt#L34)). The Review row's `archived = workout.archived` looks capable of archive support but cannot become true from this production producer.

**Minimal remedy:** Derive Review workout evidence from finished `allSessions`, or from distinct-ID union of history and archivedWorkouts, with the same set in `ReviewSourceContent`. Continue excluding Active/Abandoned sessions and permanently deleted records. Reuse the existing Workout ID route and Archived detail context.

**Acceptance:** Finish → Review → archive → Review still shows the same count/date/session identity, with Archived context. Restore does not double count. Include mixed active/finished/abandoned/archived sessions, recreation, Area-scoped Review and correlation invariance across archive/restore. Open archived outcome resolves the exact retained session. Current `ReviewOutcomeJourneyE2ETest` archives the other three domains but its Gym journey finishes without archiving (`:104–108`); it therefore cannot certify this state.

**Prior decision reconciliation:** DEC-20260910-008 preserves archive history and explicitly treats cleanup as separate from progress; it names Task/Habit evidence and immutable Gym facts. It does not authorize a silent archive-dependent score policy. This is a current producer/consumer inconsistency, not evidence that the older native receipt failed.

### RV-03 — Empty selected periods hide valid 30-day correlations

**P2 · confirmed source · analytical reachability.** Have at least seven contributing days in each of two included domains during the last 30 days, with none in the last seven. Select Weekly. Valid 30-day comparisons are calculated, but only the empty selected-view guidance/source actions render. The “30-Day Correlations” disclosure disappears. A month-to-date period early in a new month can fail the same way.

**Root cause:** Review intentionally requests at least 30 days of outcomes and computes correlations independently of visible period ([ReviewDialog.kt:128](../../../app/src/main/java/com/whip/app/ui/ReviewDialog.kt#L128), `:142–163`). `hasReviewData` reflects only visible selected-period signals (`:164`), then the empty overview branch returns before the correlations disclosure (`:478–500`, `:539`). The visibility predicate conflates selected-period outcome presence with fixed-window comparison availability.

**Minimal remedy:** Keep the selected-period empty state, but let the independent 30-day comparison block render afterward when meaningful. Clearly separate its exact date range from the selected range. Avoid fabricating empty outcome cards merely to reach it.

**Acceptance:** Weekly zero outcomes + qualifying older 30-day series still exposes the same coefficient/sample as Monthly when both domains are included/ready. Changing only the visible period must not hide fixed-window comparisons. First-of-month, unavailable/hidden sections, Area/Gym exclusion, insufficient variation and complete no-history cases remain truthful. `ReviewAvailabilityUiTest#correlationsUseReadySourcesEvenWhenOtherEvidenceIsUnavailable` exercises ready-source gating with ten recent days; it does not cover zero visible-period days.

### RV-04 — Missing original-source handoffs settle differently and can fail silently

**P2 · confirmed source branches; exact timing/runtime reproduction pending · recovery.** A selected outcome can become unavailable between rendering and opening because its source is deleted, reopened or temporarily absent. The Review host always closes into source navigation after attempting resolution ([ReviewAppRoute.kt:84](../../../app/src/main/java/com/whip/app/ui/ReviewAppRoute.kt#L84)). Missing archived Tasks produce a null action key; missing completed Tasks are silently cleared by `CompletedTaskRouteEffect`; missing Habit/Workout IDs are left pending with no failure message. Goals already consume and report the unavailable request.

**Root cause:** Archived Task lookup assigns nullable result then navigates ([ReviewAppRoute.kt:28](../../../app/src/main/java/com/whip/app/ui/ReviewAppRoute.kt#L28)); settled missing completed Task clears only (`WhipRouteEffects.kt:25`). Habit request resolution has only active/archived success branches ([HabitScreens.kt:332](../../../app/src/main/java/com/whip/app/ui/HabitScreens.kt#L332)). Gym missing-session resolution returns before request consumption ([GymScreens.kt:1248](../../../app/src/main/java/com/whip/app/ui/GymScreens.kt#L1248)). Goal has an explicit “no longer available” terminal branch ([GoalScreens.kt:399](../../../app/src/main/java/com/whip/app/ui/GoalScreens.kt#L399)).

**Harm:** A source tap can look successful while showing an unrelated/general collection; lingering requests may reopen later if the ID reappears. Return to Review usually remains available, reducing severity. This is not a claim that ordinary existing-source routing is broken.

**Minimal remedy:** Make a settled unavailable request a consumed, named result at the existing source owner. Keep loading requests pending until source readiness is known. Preserve the return route and give clear local feedback; optionally keep Review open if the route can synchronously establish absence. Do not create another navigation controller.

**Acceptance:** Controlled source removed after outcome listing but before activation, for all four domains; ready absence gives one named unavailable result, no unintended inspector and no latent request. Loading absence waits rather than failing. Retry/return/recreation retains the chosen section/scope; source correction causing an outcome to disappear updates rows on return. Native timing remains an explicit parent probe.

### RV-05 — Empty Review sends “Open Tasks” to Completed, away from the suggested work

**P2 · confirmed source · action/destination coherence.** The empty state explains that completing a Task will add an outcome and offers “Open Tasks” ([ReviewDialog.kt:478](../../../app/src/main/java/com/whip/app/ui/ReviewDialog.kt#L478), `:492`). Its navigation callback forces `TaskDestination.Completed` ([ReviewAppRoute.kt:47](../../../app/src/main/java/com/whip/app/ui/ReviewAppRoute.kt#L47)). A new/empty user lands on completed history rather than the collection where pending Tasks can be found or created. Other empty-state source buttons preserve potentially unrelated chosen tabs, so the stated recovery intent is also weakly specified.

**Minimal remedy:** For this empty-state CTA, use the established Task collection destination, or rename a deliberately analytical route “Open Completed Tasks” and separately expose the existing work/creation route. Prefer the collection route to fit collection-first navigation. Keep actual outcome-row routing to Completed unchanged.

**Acceptance:** First-use/selected-empty Review → Open Tasks lands on the collection with its ordinary Add Task anchor and pending work. A real completed outcome still opens its completed occurrence. Existing chosen destination behavior for Habit/Goal/Gym needs an explicit source-action disposition rather than accidental saved tab inheritance.

### RV-06 — A one-day month-to-date trend does not render its observation

**P3 · confirmed draw path; visual reproduction pending · chart state.** On the first day of the month with a positive outcome, Monthly produces one date/value. `ReviewLineChart` sets step to zero, moves its path to one point, and never adds a line segment or marker ([ReviewDialog.kt:663](../../../app/src/main/java/com/whip/app/ui/ReviewDialog.kt#L663)). The baseline renders, but the positive observation has no visible mark. The numeric total is still truthful.

**Minimal remedy:** Draw an explicit point for a one-observation series; retain date/scale labels and accessible numeric text. The neighboring `TrackRecordedTrend` already handles single observations with circles (`TrackReviewExperience.kt:102`), but reuse should remain proportionate.

**Acceptance:** Monthly on day one, positive/zero data and large text: the single observation is visibly represented, total/date/scale and spoken description agree. No invalid line or division edge on zero/one/two points. Inspect the original rendered frame rather than only asserting a Canvas exists.

## Grounded improvements, separate from defects

**RV-I1 · P2 improvement — make scoring rules and historical basis inspectable.** Goal copy currently says only “Normalized progress score; partial progress counts proportionally” (`ReviewDialog.kt:630`) and row copy repeats the score (`ReviewOutcomeDetails.kt:109`). Actual rules differ: positive capped gains for numeric targets, observation-days for OpenEndedTrend, a fraction/latest success for MaintainRange, weighted earned fraction for milestones, no ElapsedSince contribution (`GoalModels.kt:855–897`). Target-based Goal scores use current definitions, while completed Goal overview can show frozen closure progress (`GoalViewModel.kt:702–720`). This is an intended distinction, not proof of numerical corruption. Add a compact “How outcomes count” disclosure that names these rules, the current-definition basis, exclusions and positive-gain meaning. Show enough type/context in Goal rows to explain the contribution without relabeling a score as a percent or completion. Acceptance: users can explain a 0.005 contribution, unchanged elapsed timer, declining then recovered target, and a frozen completed value without guessing. G1 baseline repair remains the parent/Goals finding, and must update Review's dependent scoring acceptance there.

**RV-I2 · P3 improvement — explain correlation eligibility precisely.** `pearsonCorrelation` requires at least seven **nonzero outcome days in each series**, rejects no variation, and reports all 30 calendar points (`CrossDomainInsights.kt:18–34`). UI says “at least seven observed days,” “Not enough observations yet,” and `n=30` (`ReviewDialog.kt:541–558`). Two complete constant series have sufficient observations but no estimable coefficient. Keep the mathematical guards; distinguish insufficient contributing days from no variation and explain that calendar days without outcomes are zero. Include the actual last-30-day range. Do not claim seven paired measured records or treat the 30 as logged-entry count. Acceptance: sparse, constant, anti-correlated, separate-date and ready-source cases have accurate eligibility text; association disclaimer remains.

**RV-I3 · P3 improvement — offer contextual source inspection without replacing entity navigation.** Current outcome text promises the original **entity**, not an exact Entry; DEC-20260910-011 deliberately reuses mature inspectors. Thus entity-ID navigation is a justified contract, not a correctness defect. However, active Habit opens Today by default (`HabitScreens.kt:3092`), Goal opens Overview (`GoalScreens.kt:2628`), and archived recurring Task routes its definition (`ReviewAppRoute.kt:30`); an older contributing date is not carried. Offer a small optional History/date landing hint, preserving existing entity actions and full return context. Do not silently edit today's check-in when the user is tracing an old contribution. Acceptance: an old outcome reaches that entity's retained History with its date discoverable, and Return restores Review; exact event editor need not open automatically.

## Exhaustive flow/state disposition matrix

Legend: **Keep** = source supports current design; **Change** = numbered finding; **Improve** = grounded optional improvement; **Probe** = current runtime acceptance missing, not a confirmed defect. Sources are current baseline line anchors; ranges in prose refer to inspected adjacent branches.

| # | Flow/state | Current evidence and disposition |
|---:|---|---|
| 1 | Home discovery with authored evidence | `WhipApp.kt:2133` opens the session; `ReviewJourneyE2ETest:145` specifies Track-only discovery. **Keep** existing Review action; fresh summary proves entry endpoint only. |
| 2 | Open fresh Review | `ReviewSession.kt:32–36`, `ReviewDialog.kt:116–118`. **Keep** new presentation state and origin capture; settings period/sections persist separately. |
| 3 | Compact initial options | `ReviewDialog.kt:338–347`. **Keep** summary-first disclosure; fresh original shows current scope without expanded controls. |
| 4 | Expanded compact options | `ReviewDialog.kt:347`, `:399–447`. **Keep** in-place controls inside scrolling content; **Probe** current normal/200% options original. |
| 5 | Wide dedicated controls | `ReviewDialog.kt:243–310`. **Keep** fixed control pane with independent scrolling and reading-width main pane. |
| 6 | Weekly boundary | `ReviewDialog.kt:86–88`. **Keep** rolling seven inclusive local dates, not an unexplained calendar-week claim; explicit range visible. |
| 7 | Monthly boundary | `ReviewDialog.kt:88`. **Keep** month-to-date inclusive range; **Improve** say Month to Date if users equate Monthly with 30 Days. |
| 8 | Historical/custom date window | `ReviewPeriod` only Weekly/Monthly; `ReviewDialog.kt:120–124`. **Keep** current bounded scope; arbitrary older period/previous-period comparison is absent capability, not a broken control. Source histories provide retained evidence. |
| 9 | Granularity | `ReviewDialog.kt:123`, `:637–649`. **Keep** daily aggregation with visible scale/endpoints; no weekly/monthly bucketing control is falsely advertised. |
| 10 | Period preference persistence | `ReviewAppRoute.kt:77–80`. **Keep** Settings owner, no duplicate Review draft. `ReviewJourneyE2ETest:62–73` specifies recreation. |
| 11 | Included domains and display order | `ReviewDialog.kt:91`, `:425–433`. **Keep** stable enum order rather than mutation-order cards. |
| 12 | Last selected domain | `ReviewDialog.kt:429–430`. **Keep** prevents empty set; **Improve** disable the last chip or explain refusal if needed, instead of silent ignored tap. |
| 13 | Domain changes during details | `ReviewDialog.kt:176`, `:198`. **Keep** selection changes reset selected detail. |
| 14 | Invalid restored/empty sections | `reviewAvailability`, Settings contract. **Probe** malformed preference/legacy backup: UI assumes valid nonempty settings; no new proof of corruption. |
| 15 | Area-scoped productivity | `WhipApp.kt:2588`; `AreaScopeFilters.kt:35–82`. **Keep** Task/Habit/Goal evidence filtered before calculation, including archives and related logs. |
| 16 | All Areas | `ReviewEvidence.kt:8–11`. **Keep** null special scope label; detail explicitly says All Areas. |
| 17 | Named Area label | `ReviewEvidence.kt:11`, `ReviewDialog.kt:228`. **Keep** current name/fallback plus summary visibility. |
| 18 | Gym global under Area | `ReviewDialog.kt:137`, `:141`, `:441`; details `:59`. **Keep** labeled all-Gym data and excluded Area correlation. |
| 19 | Track global under Area | `WhipApp.kt:2589`, `ReviewDialog.kt:574–580`. **Keep** All Tracks evidence with explicit scoring exclusion. |
| 20 | Area removal/rename during Review | `ReviewSession.kt:33`; `AreaScopeFilters.kt:9–17`. **Probe** scope validation/fallback across removal and recreation; no current fresh edge capture. |
| 21 | Originating Area after Track visit | `ReviewSession.kt:33–34`, `ReviewAppRoute.kt:89`. **Keep** repaired origin restoration; reconcile old SHELL-02 as addressed in source. |
| 22 | Summary populated | `ReviewDialog.kt:471–537`. **Keep** primary outcome cards before Track prose; fresh original confirms hierarchy. |
| 23 | Mixed populated and zero domains | `ReviewDialog.kt:164`, `:509`. **Keep** ready zero-domain cards while any selected outcome exists; provides honest comparative presence. |
| 24 | All selected domains empty | `ReviewDialog.kt:478–500`. **Keep** selected-view language and options guidance; **Change RV-03/RV-05** correlation/action reachability. |
| 25 | Hidden section contains history | `ReviewDialog.kt:125–140`. **Keep** hidden source does not invent visible outcomes; empty guidance suggests including sections. |
| 26 | Track-only selected period | `ReviewDialog.kt:484–486`. **Keep** Track evidence alongside zero-productivity message; no normalized productivity invented. |
| 27 | Historical evidence outside visible period | `ReviewDialog.kt:130`, `:164`. **Change RV-03** independent comparison block must remain reachable. |
| 28 | Source loading, all empty-looking | `ReviewAvailability.kt:48–57`, `ReviewDialog.kt:478`. **Keep** incomplete notice prevents false no-outcomes claim. |
| 29 | Partial source failure | `ReviewAvailability.kt:20–24`, `ReviewDialog.kt:140`. **Keep** ready cards/results remain, failed source excluded. |
| 30 | Tracks failure with outcome data | `ReviewAvailability.kt:58`, `ReviewDialog.kt:165–167`. **Keep** outcomes remain valid; global evidence availability still named. |
| 31 | Hidden domain failure | `ReviewAvailability.kt:58`. **Keep** excluded domain not reported/retried; Tracks remains independently global. |
| 32 | Error + loading same domain | `ReviewAvailability.kt:48–50`. **Keep** failure precedence, not a misleading spinner. |
| 33 | Retry sources | `ReviewAvailability.kt:26–36`, `:75–76`. **Keep** targets only failed included/global evidence sources. |
| 34 | Recovery into current results | `ReviewDialog.kt:128`, `ReviewAvailability.kt:21`. **Keep** content/readiness rekeys; stale failed values not displayed. |
| 35 | Selected detail fails after entry | `ReviewDialog.kt:198–205`, `ReviewOutcomeDetails.kt:79–84`. **Keep** selected section retained, unavailable notice rather than zero rows claimed. |
| 36 | Selected detail recovers | Same source as 35; `ReviewAvailabilityUiTest` selected-outcomes contract. **Keep** local recovery/retry, no forced overview reset. |
| 37 | Task once/undated completion | `ReviewOutcomes.kt:39–43`. **Keep** completion timestamp supplies local outcome date; no scheduled date required. |
| 38 | Task recurring identity | `ReviewOutcomes.kt:42`, `ReviewEvidence.kt:15–29`. **Keep** source occurrence key and completion date distinct. |
| 39 | Task moved occurrence | `ReviewOutcomeDetails.kt:102–104`. **Keep** scheduled and original date both explain the contributing event. |
| 40 | Archived Task evidence | `ReviewEvidence.kt:15–29`. **Keep** once/recurring completion preserved, distinct stable keys prevent duplication. |
| 41 | Skipped/open Task evidence | `ReviewEvidence.kt:18`, `ReviewOutcomes.kt:38`. **Keep** no outcome credited. |
| 42 | Task original source existing | `ReviewAppRoute.kt:27–35`, `TaskNavigationIndex.kt:7–10`. **Keep** nonarchived exact completed key resolves source. |
| 43 | Archived recurring Task source | `ReviewAppRoute.kt:30`. **Keep** original entity contract; **Improve RV-I3** optional dated History context. |
| 44 | Task reopened/deleted before source open | `WhipRouteEffects.kt:25–29`. **Change RV-04** named consumed unavailable result. |
| 45 | Habit daily target raw increments | `ReviewOutcomes.kt:49`, `HabitModels.kt:433`. **Keep** one successful Day/Occurrence rather than count each increment. |
| 46 | Habit nonflex Week/Month target | `HabitModels.kt:635–639`, `:414–444`. **Change RV-01** discrete target counted repeatedly/backfilled. |
| 47 | Habit flexible week/month | `HabitModels.kt:641–669`. **Keep** one reaching-day period reward, with custom week start and target eligibility. `HabitRulesTest:284` source asserts one monthly reward. |
| 48 | Habit rolling target window | `HabitModels.kt:692`, `:635`. **Keep** each evaluated rolling day can legitimately differ; do not collapse like discrete Week/Month. **Probe** explain window semantics in rows. |
| 49 | Habit pauses/skips | `HabitModels.kt:638`, `:403–411`. **Keep** dated neutral exclusions; current paused flag does not rewrite dated schedule evidence (`:540`). |
| 50 | Archived Habit evidence | `ReviewOutcomes.kt:48–52`. **Keep** current archive flag does not remove outcome history; ID deduplication handles unusual active+archive overlap. |
| 51 | Optional/log-only Habit | `targetSatisfied`, `HabitModels.kt:682`. **Keep** no target means no target-reached reward; **Improve RV-I1** rules should make exclusions visible. |
| 52 | Active Habit source | `HabitScreens.kt:334–340`, `:3092`. **Keep** entity opens; **Improve RV-I3** Today default creates extra effort for old evidence. |
| 53 | Archived Habit source | `HabitScreens.kt:342–346`, `:3093`. **Keep** archived entity defaults History, preserve correction capability. |
| 54 | Missing Habit request | `HabitScreens.kt:332–348`. **Change RV-04** settled missing ID needs named consumption. |
| 55 | Goal positive target gain | `GoalModels.kt:892–897`. **Keep** positive normalized daily gain bounded at one; scores do not claim raw Entry count. G1 baseline dependency is parent-owned. |
| 56 | Goal multiple entries on one date | `ReviewOutcomes.kt:59–63`, `GoalModels.kt:892`. **Keep** one aggregate daily contribution. |
| 57 | Goal beyond target/negative movement | `GoalModels.kt:897`. **Keep** Review cap separate from uncapped live progress; negative change is not a negative productivity reward. **Improve RV-I1** explain positive-gain meaning. |
| 58 | Goal open-ended recorded day | `GoalModels.kt:875–880`. **Keep** one finite recorded day, including valid numeric zero; outside authored date range excluded. |
| 59 | Goal MaintainRange | `GoalModels.kt:881–890`. **Keep** latest observed in-range or observed fraction for TimeInRange; score not physical-unit percentage. |
| 60 | Goal Consistency | `GoalModels.kt:892–897`, `:900`. **Keep** normalized gained consistency progress; explain fraction rather than label completed Goal. |
| 61 | Goal WeightedMilestones | `GoalModels.kt:864–873`. **Keep** completed dated weight/positive total and configured-zone date; zero-weight optional items excluded. |
| 62 | Goal ElapsedSince | `GoalModels.kt:863`. **Keep** no ticking outcome inflation; **Improve RV-I1** excludes elapsed timer from scoring visibly. |
| 63 | Goal tiny positive score | `ReviewOutcomeDetails.kt:118–123`. **Keep** up to three decimals / <0.001 rather than rounding earned progress to zero. |
| 64 | Goal paused/completed/abandoned/archive | `ReviewOutcomes.kt:58`; `GoalViewModel.kt:725–727`. **Keep** retained observed evidence across lifecycle. Closing alone is not the same as normalized gain. |
| 65 | Goal current vs frozen facts | `ReviewSourceContent.kt:33`, `GoalViewModel.kt:702–720`. **Keep** distinct analytical/current-definition versus frozen closure contracts; **Improve RV-I1** disclose distinction. |
| 66 | Goal source correct destination | `GoalScreens.kt:404–410`. **Keep** Active/Completed/Archived resolution from source membership. |
| 67 | Goal missing source | `GoalScreens.kt:399–402`. **Keep** explicit unavailable message/consume as pattern for RV-04; **Probe** loading-after-recreation guard. |
| 68 | Gym finished outcome date | `ReviewOutcomes.kt:69–71`. **Keep** session localDate and one contribution; completing many Sets is not many workouts. |
| 69 | Gym Active/Abandoned | `ReviewOutcomes.kt:69`, `GymViewModel.kt:2735`. **Keep** only Finished counts, no abandoned-work reward. |
| 70 | Gym finished archive/restore | `GymViewModel.kt:2734–2738`. **Change RV-02** current history-only consumer drops retained outcome. |
| 71 | Gym original exact session | `ReviewAppRoute.kt:39–42`, `GymScreens.kt:1248–1255`. **Keep** exact ID-focused History; no date/name ambiguous route. |
| 72 | Gym missing session | `GymScreens.kt:1249`. **Change RV-04** named settled absence; retain return context. |
| 73 | Track evidence count/period | `ReviewDialog.kt:72–83`, `:165–167`. **Keep** selected range entries/distinct touched Tracks, not numeric values or productivity scores. |
| 74 | Track no selected Entries | `trackReviewEvidence` returns null. **Keep** no fake zero-score Track card; empty branch retains source navigation. |
| 75 | Open all Tracks from scoped Review | `ReviewAppRoute.kt:88–91`, `ReviewSession.kt:33`. **Keep** temporary All Areas then origin restoration, saved user Area unchanged. |
| 76 | Track evidence/source precise matching rows | `ReviewAppRoute.kt:90`. **Keep** current contract says Open Tracks, not exact period-filtered Entries; detailed Track analysis belongs to Track component audit. |
| 77 | Correlation selected/ready sources | `ReviewDialog.kt:140–149`. **Keep** only included ready signals; Track excluded; Area Gym excluded. |
| 78 | Correlation sparse/constant series | `CrossDomainInsights.kt:18–34`. **Keep** seven nonzero-day and variance guards; **Improve RV-I2** truthful empty reason. |
| 79 | Correlation 30-day zero representation | `ReviewOutcomes.kt:76–78`, `ReviewDialog.kt:142`. **Keep** calendar-day no-outcome zero; **Improve RV-I2** sample explanation. |
| 80 | Compare/top insights | `ReviewDialog.kt:143–162`, `:539–560`. **Keep** deterministic pair comparisons, association disclaimer; no ranked top-insight, causality or previous-period control exists to audit as functional. |
| 81 | Outcome totals → details | `ReviewDialog.kt:191–205`. **Keep** in-context explanation first, same row projection as charts, exact sum/count. |
| 82 | Detail row identity/archive/date | `ReviewOutcomeDetails.kt:86–112`. **Keep** shared record grammar, authored name/icon, contribution/date/archive context. |
| 83 | Detail back to overview | `ReviewOutcomeDetails.kt:47`, `ReviewDialog.kt:205`. **Keep** single analytical back action; overview scroll state survives branch. |
| 84 | Outcome list large data | `ReviewOutcomeDetails.kt:51`, `:86`. **Keep** keyed LazyColumn rather than composing all rows. **Probe** query/search/date grouping opportunity only after realistic dense-history measurements. |
| 85 | Live clock recomposition | `ReviewSourceContent.kt:15–34`, `ReviewDialog.kt:128`. **Keep** expensive outcome calculation keys exclude live projection clocks. Structural key construction/equality still touches content; see runtime probe. |
| 86 | Large historical computation | `ReviewOutcomes.kt:48–64`, `GoalModels.kt:892–895`. **Probe** main-thread repeated per-goal/date history projection and Habit log scans; no measured jank or timing claim. |
| 87 | Source visit → return | `ReviewSession.kt:38`, `:42`, `ReviewReturnBar.kt:19–26`. **Keep** explicit Return and dismiss affordances, no duplicate destination state. |
| 88 | Source correction/deletion → return | `ReviewDialog.kt:128`, `ReviewSession.kt:42`. **Keep** current evidence recomputed, same chosen section retained even if now empty; **Probe** mutation-heavy end-to-end acceptance. |
| 89 | Activity/process reconstruction | `ReviewSession.kt:55–60`, `ReviewDialog.kt:116–118`. **Keep** saveable UI state/source IDs; **Probe** fresh cold process restart distinct from Activity recreation tests. |
| 90 | System Back while Review details | `ReviewDialog.kt:209`. **Keep** details → overview before closing dashboard. |
| 91 | System Back while source | `WhipApp.kt:1129–1133`, source archive handlers. **Keep** retained Review precedence; FND-20260929-022 repaired callback path. **Probe** fresh all-domain nested-child Back. |
| 92 | Explicit close / return-bar dismiss | `ReviewSession.kt:39`. **Keep** end inspection session and clear saved content; source remains ordinary destination. |
| 93 | Return while keyboard/editor active | `WhipApp.kt:2020`. **Keep** return bar suppressed while keyboard/routine-editor/focused collection; **Probe** keyboard dismissal restores it and Back does not discard dirty editor. |
| 94 | Enlarged text adaptive layout | `ReviewDialog.kt:212`, `:510–515`. **Keep** density-independent readable thresholds include fontScale, wrapped FlowRows and stacked title/total. Fresh summary is not 200% proof. |
| 95 | Hinge/wide readability | `ReviewDialog.kt:253–285`, `ReviewOutcomeDetails.kt:43–52`. **Keep** real hinge gutter and bounded detail measure; **Probe** current fold and resized-window originals. |
| 96 | Short viewport/header | `ReviewDialog.kt:324–349`. **Keep** persistent title/close with scrolling body. **Probe** actual 200% short viewport still leaves usable list space. |
| 97 | Chart empty/flat/multiple values | `ReviewDialog.kt:657–673`. **Keep** zero baseline and explicit scale. **Change RV-06** lone positive observation absent. |
| 98 | Chart accessibility | `ReviewDialog.kt:615–617`, `:639`. **Keep** all daily numeric values in semantics; **Probe** spoken date mapping and long 31-value announcement usability. |
| 99 | Card action/heading semantics | `ReviewDialog.kt:622`, `:627`, `:372`; detail heading `:48`. **Keep** named action/headings; **Probe** icon description plus click label duplication and TalkBack focus grouping. |
| 100 | Keyboard/switch traversal | Standard clickable/chip/button owners. **Probe** focus order, Enter/Space on selected cards/chips, detail Back and return bar; XML is not hardware-keyboard verification. |
| 101 | RTL / localized dates/numbers | `ReviewDialog.kt:119`, `:595–602`; auto-mirrored Next `:627`, number formatter `ReviewOutcomeDetails.kt:118`. **Keep** locale-aware numbers/dates and directional icon; **Probe** chronological chart interpretation in RTL. |
| 102 | Theme/contrast/reduced motion | Material semantic palette in card/chart owners; no Review animation-dependent truth. **Keep** normal dark original readable; **Probe** light/dynamic palette and actual contrast/large-pointer conditions. |

## Justified keeps and prior-decision reconciliation

1. Keep the single outcome projection and domain-owned scoring. Domain corrections belong to those owners; a second Review-only counting implementation would split truth. RV-01 targets the shared Habit outcome rule; G1 remains the existing Goal root cause.
2. Keep in-context contributing rows before original entity navigation (DEC-20260910-011). The extra step makes numeric totals inspectable. Entity-only navigation is intentional; optional date landing is RV-I3, not an invented exact-Entry promise.
3. Keep Track evidence global and separate from productivity scores/correlations (DEC-20260910-008). Entry count plus touched-Track count is meaningful for heterogeneous data. Calling it a fifth normalized score would be false equivalence.
4. Keep usable partial evidence with a single named notice and existing targeted retry (DEC-20260910-009). A blanket loader would hide ready domains; displaying failed stale numbers would weaken truth.
5. Keep scoped-empty language, options-first guidance and no false onboarding (DEC-20260910-010). RV-03 corrects one premature return without discarding that design.
6. Keep selected period/sections as Settings preferences and origin Area as Review-session context. Current `ReviewSession` repairs the old Area loss; the old report is not proof the bug persists.
7. Keep explicit per-card daily scale and font-scaled layout thresholds. Fresh original confirms the repaired stacked title/total and outcome-first hierarchy. Identically shaped slopes can still have different scales; the labels make that deliberate relative-trend view inspectable. An interactive full chart is not justified solely by the small summary sparkline.
8. Keep bounded Review scores separate from uncapped Goal live progress (DEC-20260927-006 / Goal celebration decision). A score is neither completed Goal count nor physical-unit reading. RV-I1 adds needed interpretation.
9. Keep frozen completed Goal facts in their original closure owner. Review currently recalculates day-level scores from current definition/evidence; no old snapshot contains enough historical target metadata to reconstruct an entirely frozen daily series. Explain the distinction rather than silently invent historical configuration.
10. Keep exact Workout ID source navigation and immutable performed snapshots. RV-02 restores archive-invariant outcome inclusion; it does not modify the historical Workout or inflate count from Sets.

## Fresh runtime probes for the coordinating parent

These are not passed tests or confirmed render failures. Prior test/source evidence establishes useful targets but cannot certify the baseline's current native appearance.

| Probe | Purpose / acceptance evidence |
|---|---|
| RP-01 | Native non-flexible numeric Daily+Week/Month Review fixture; confirm RV-01 visible count/dates and distinguish provisional AtMost outcomes. Use seeded evidence, not a destructive reset on the owner phone. |
| RP-02 | Finished Workout archive/restore while retaining Review: count/details/correlation invariant, Archived row, exact source ID; confirms RV-02 production producer issue. |
| RP-03 | Older last-30-day paired series with zero weekly/month-to-date outcomes: correlation disclosure stays reachable; proves RV-03 independently of stale source state. |
| RP-04 | Controlled source deletion/reopen between row display and activation; all four domain outcomes and loading/settled distinction; confirms named recovery/consumption for RV-04. |
| RP-05 | Monthly on first day: positive single observation marker plus accessible dates/total/scale; original PNG rather than Canvas-presence assertion. |
| RP-06 | Normal/actual-200% short compact overview/options/detail; wide/fold main/controls; strict complete text bounds, minimum useful viewport, close/back reachability, exact saved state after return. |
| RP-07 | Hardware keyboard/TalkBack traversal through options → card → detail → source → return; named single action, no ambiguous duplicate icon announcement, dates understandable when values announced. |
| RP-08 | Large retained dataset trace: opening Review, changing period/sections, returning after one correction, live clock ticking. Measure frame time/allocations on representative Goals/Habits/Tasks/Workouts rather than infer acceptable speed from lazy rows. |
| RP-09 | Real process death in source with selected detail, scrolled outcome list, expanded options/correlations and originating Area; reopen/return preserves analytical context and recomputes current evidence. |
| RP-10 | Correct/delete a contributing Habit/Goal measurement and Task completion, return without closing Review; updated totals/row removals plus sensible keyed list position. Nested dirty editor Back must retain its normal discard/working boundary. |

## Source-test accounting

Read as source, not executed: `ReviewOutcomesTest`, `ReviewEvidenceTest`, `ReviewCopyTest`, `ReviewAvailabilityTest`, `CrossDomainInsightsTest`; relevant complete Habit/Goal outcome functions and adjacent `HabitRulesTest`/`GoalRulesTest` cases; `ReviewJourneyE2ETest`, `ReviewOutcomeJourneyE2ETest`, `ReviewAvailabilityUiTest`. These cover domain exclusion/readiness, daily target normalization, archive preservation for productivity, exact current/archived entity source routes, recreation, partial/recovered states, Track global Area behavior and large-text label contracts. They leave the specific RV-01 nonflexible-period combination, RV-02 archived Workout, RV-03 zero-current-period/valid-30-day comparison and RV-06 single-observation rendering uncovered in the inspected source.

Fresh rendered evidence in this reviewer scope is exactly one personally inspected Review summary PNG/XML pair. No claim of full native/accessibility/performance acceptance, original-source visual acceptance or passing test execution is made here.
