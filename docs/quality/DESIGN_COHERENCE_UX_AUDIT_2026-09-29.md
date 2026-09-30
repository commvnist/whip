# Full design consistency, coherence and UX audit — 2026-09-29

**Audit complete; findings were unfixed at delivery.** Product baseline `6d79bd1f82da9c1666960834410b3e08ed6d3ea1`, current source 0.3.90/code 96. Scope: first use through capture, daily execution, inspection, correction, history, analysis, organization, backup/recovery and Android entry points across all supported components. FB-20260929-017 records the owner's request. Eight independent component reviewers used GPT-6.1 Sol/high, one per component; the parent reviewed shared shell/Home/design system, reconciled findings and operated fresh device evidence. No production/test/schema changes or release occurred in the audit itself.

Whip's visual system is broadly coherent. The most consequential inconsistencies are **what an action means, which records it affects, whether a save really completed, and where the user returns afterward**. Another visual overhaul should not precede repairs to false target truth, phase-local tools rewriting other phases, unsettled Routine saves and misleading import outcomes. The complete inventory contains **784 flow/state dispositions and 52 grounded findings: 5 P1, 40 P2, 7 P3**, plus three explicitly optional Review improvements and unresolved runtime probes.

## Reading map and coverage

Subsequent FB-20260929-018 [direct-execution implementation](DIRECT_DAILY_EXECUTION_2026-09-29.md) repairs H2, TASK-02 and TASK-03 with focused native acceptance. The other 49 findings, including all five P1 items, remain unfixed. This report preserves the original audit baseline and counts.

Each report contains a top-to-bottom flow/state inventory, precise current source references, prior-decision reconciliation, concrete triggers, effects, remedies and verification conditions. A disposition is a reviewed flow/state, **not an independently executed end-to-end test**. Source review covers every matrix row; fresh visual/runtime evidence covers the expressly named subset. Unsupported capabilities are identified rather than invented as requirements.

| Component / report | Dispositions | Findings P1/P2/P3 | Scope |
|---|---:|---:|---|
| [Shared shell, Home and design system](coherence-audit-2026-09-29/shared.md) |56|0/2/0| Startup/setup; adaptive chrome/navigation; Add/Search/Area; daily/pinned previews; Focus; common cards/inspectors/buttons/dialogs/identity; drafts/feedback; semantic and responsive design. |
| [Tasks](coherence-audit-2026-09-29/tasks.md) |60|0/4/1| Collection and Today; capture/planning/calendar; recurrence/series edits; subtasks/Focus; occurrence completion/reopen/history; selection/reorder/saved views; archive/delete/search handoffs. |
| [Habits](coherence-audit-2026-09-29/habits.md) |64|0/3/1| Every tracking mode, target/cadence/ending; daily logging/timer/checklist/skip; pauses/backfill/correction; Activity/Insights; templates/organization/archive/delete. |
| [Goals](coherence-audit-2026-09-29/goals.md) |64|1/3/1| Every outcome type; baseline/calculation/units/windows; milestones/elapsed reset; logging/history/correction; target reach/closure/reopen; frozen versus derived evidence; Insights/reminders/lifecycle. |
| [Tracks](coherence-audit-2026-09-29/tracks.md) |128|0/3/1| Every field type; definition/choice evolution/destructive review; Entry creation/duplicates/correction; paged history/search; Activity/Insights/exact evidence; CSV preview/import/export/conflict/draft recovery. |
| [Gym](coherence-audit-2026-09-29/gym.md) |86|2/7/0| Workout execution/rest/composer/sets/finish; Library exercises/machines/categories/tools; static/phased Routine building and launch; equipment/prescriptions; History correction/sharing; Insights/source drilldown; retired compatibility. |
| [Settings and Organization](coherence-audit-2026-09-29/settings.md) |76|1/5/2| Seven categories/search; appearance/Home/planning/units; Gym defaults; Areas/Tags/emoji; notification access/quiet hours; plain/encrypted/portable backup/import/replace/export/retention/delete; identity/diagnostics. |
| [Review & Trends](coherence-audit-2026-09-29/review.md) |102|1/4/1| Options/periods/scopes; normalized outcomes and contributing facts; sparse/loading/error/empty states; correlations; original-source handoffs and retained analytical context. |
| [Widgets and Android entry points](coherence-audit-2026-09-29/platform.md) |148|0/9/0| Widget configuration/resize/Area/cache/execution; exact source and queued capture; reminders/rest/Focus notifications; stale/replaced IDs, permissions/recreation/privacy and external shares/shortcuts. |
| **Total** |**784**|**5/40/7**| Includes supported flows, justified keeps, findings, explicit evidence gaps and absent capabilities. |

## Evidence strength

[Exact fresh commands, results and original gallery](../../artifacts/coherence-audit/2026-09-29/README.md): **eight distinct selected native methods passed**, zero failures/errors/skips/reuse/timeouts; **36 fresh original PNG/XML pairs** were inspected. Includes comparable normal roots across all main workspaces, actual200% dense Home and archived Track history/search/IME/recreation/restore, exact expanded content-column geometry, ordinary phased Routine launch, and one widget Area intent. Disposable API34 emulators only. Historical originals and prior successful checks are labelled with their dates and limitations in the component reports.

Seven defects have fresh manual reproductions: TASK-01 Cancel loss, TASK-03 Tomorrow, G1 reduction truth, H2 clipped increment, GYM-03 empty-workout creation, SET-04 index return and SH1 wide Home routing. Source-only findings are explicitly labelled. Several source findings prove deterministic transforms; others identify an unhandled failure/race contract whose precise runtime variant still needs controlled injection. No full regression suite, phone acceptance, physical-fold campaign or comprehensive accessibility certification is claimed.

## Urgent findings

Priority means consequence, not estimated effort. **P1**: false attainment/history or risk to substantial authored work/committed-data recovery. **P2**: material workflow, evidence, availability, continuation or accessible meaning failure. **P3**: smaller presentation/discovery/copy issue. “S” is current source evidence; “D” is a fresh manual reproduction. E executed methods support their named keeps/flows; they do not prove unrelated defect variants.

| Priority / ID | Concrete trigger and user harm | Evidence | First repair and acceptance |
|---|---|---|---|
| **P1 G1** | Reduce target80, blank starting value:90 is reached/112.5%;70 is unreached/87.5%. The ordinary weight template can enter this configuration. | S+D, both observations | Define a valid direction-aware baseline contract, reject incompatible authored intervals, and give existing invalid configurations an honest fallback. Verify card/inspector/Home/Review/closure and templates. |
| **P1 GYM-01** | Viewing Phase2, apply a two-set scheme to six rows: only the first two Phase1 rows remain. Generate Warm-Ups also rewrites all phases from the first global load. Saving persists programming outside the visible scope. | S, exact deterministic before/after | Apply/merge only the declared phase scope, preserve other phases' IDs/fields/order, explicitly handle common sets. Verify save/reopen and instantiated workout. Fresh launch pass proves ordinary phased reachability, not this preservation. |
| **P1 GYM-02** | Routine Save captures a draft while fields/exit remain active; later edits can be cleared on success. A saveable busy Boolean has no recreated receipt owner. | S; delayed/recreation variants unexecuted | Use the existing request/receipt owner, block conflicting edits/navigation or retain a separate subsequent draft, expose Saving/failure/interruption recovery. Verify delayed save, Back, recreation and retry. |
| **P1 SET-01** | Merge commits imported rows, then index/default-Area/background repair fails: UI says Import Not Completed despite imported data. Retrying looks necessary and does not explain the committed state. | S; fault injection pending | Separate committed import receipt from ancillary repair warnings; retain accurate imported/skipped counts and targeted refresh retry. Verify a post-transaction scheduler/index failure, restart and retry. |
| **P1 RV-01** | One discrete weekly/monthly Habit attainment becomes multiple daily rewards, including days before its log. It also feeds End After Completions, which can end a finite Habit prematurely. | S; no native period fixture | One dated outcome per discrete target period; truthful attaining/settled rules by comparison type. Repair the shared owner, including Review, correlations, streaks and ending consumers; preserve rolling/day semantics. |

## Remaining grounded findings, in repair order

Within each group the component report contains the full source trace and required acceptance. These are distinct visible contracts, but several share a repair owner. They should be batched accordingly rather than implemented as independent frameworks.

| Priority / IDs | Inconsistency and consequence | Concrete direction |
|---|---|---|
| P2 H1 | Threshold-ended Habit is absent from Today but inspector can say Ready/Check In; subsequent expected-day calculations accumulate false misses. | Resolve ending cutoff consistently while retaining the threshold-reaching outcome and genuine history. Distinct from RV-01's repeated discrete-period accounting. |
| P2 G2 | Goal Insights uses physical date while live progress uses Whip's logical cutoff day. | Use the same authoritative logical date across current windows, quality/rate/forecast and details. |
| P2 RV-02 | Archiving a finished Workout removes its Review count/chart/correlation contribution; other domains keep archived evidence. | Derive finished Review sessions and memo keys from the complete retained source, retain Archived context and avoid double counts. |
| P2 GYM-07 | Workout shared text includes prefilled unperformed sets without status, making prescription read as performed work. | Label completed/planned/skipped explicitly or state performed-only scope; keep saved units/equipment. |
| P2 TASK-05 | Completed Task says checklist evidence was preserved, but shows only totals without saved named subtasks/notes. | Render stored historical checklist facts, without attaching current definition controls to old evidence. |
| P2 G4 | Closed Goal juxtaposes frozen outcome and a trend recalculated with current definition without an adjacent distinction. | Label the basis/date of each view; preserve genuine frozen closure and current-derived facts. |
| P2 GYM-06 | Selecting a pound plate preset silently relabels an authored kilogram target; dropdown already uses Convert/Keep/Reset. | Reuse the existing meaning-change guard for cross-unit preset application. |
| P2 TASK-02 | Tasks collection Agenda/Calendar omit today's Schedule-anchored recurring occurrence while other schedule kinds include today. | Give dated presentations one complete, distinct stable occurrence set; preserve owner-directed collection semantics. |
| P2 RV-03 | Weekly/month-to-date zero outcomes hides valid independent last30-day correlations. | Render the selected-view empty guidance and independent comparison block with their separate exact date windows. |
| P2 TASK-01 | Text visible in New Subtask is omitted from dirty/save draft until its Add icon is pressed. Cancel loss reproduced. | Include pending raw text in dirty state and resolve it on parent Save/Save & New without duplicates or silent drop. |
| P2 G3 | Switching numeric Goal to milestones can hide invalid decimal precision while validation still rejects Save. | Normalize type-specific inactive fields or reveal a repairable validation path. |
| P2 GYM-05 | Set/Workout/cycle-review fields remain editable after submitting the captured draft, then close as if latest values saved. | Adopt existing `inputBlocked` and request ownership; retain raw failed draft. |
| P2 SET-02 / GYM-08 | Taxonomy and several secondary Gym authoring dialogs dismiss changed drafts inconsistently with primary editors. | Apply existing changed-draft Keep Editing/Discard protection to authoring, not unchanged/simple pickers. |
| P2 SH2 / SET-06 | Shared emoji Save & Use closes without durable receipt; several Settings controls and Area follow-ups also have no confirmed persistence failure result. | Reuse typed confirmed settings mutations, retain authored drafts and accurate committed-success warnings. Shared picker and immediate settings controls are separate callers of the same systemic contract. |
| P2 TASK-04 / TR-01 | Completed Task definition edit and Track Entry correction from Entries/Activity discard the inspector context; analogous active/Insights routes retain it. | Keep the source inspector's ID/section/query/loaded-history context through child save/cancel. |
| P2 GYM-04 | Insights→source workout disposes the analysis selections/range/comparison and lacks a precise return owner. | Retain per-destination analysis state and the drilldown origin; return to identical scope. |
| P2 SET-03 / SET-04 | Compact Area detail and lower Settings category visits recreate the parent list at the top. SET-04 reproduced at200%. | Hoist one saved list state per parent, separate from detail states and intentional search reset. |
| P2 SH1 | Wide Home daily support tiles restore remembered collection/library tabs instead of Today/Workout. Tasks reproduced. | Route explicit daily intent through the same callbacks as main Home; preserve ordinary root-navigation restoration. |
| P2 PLAT-01 | Out-of-Area active/review timer remains visible in its widget, but widget Area filters the exact inspector out on Open/Review. | Resolve exact entity scope or admit the authoritative unscoped inspector with explicit context; preserve the safety visibility exception. |
| P2 PLAT-02 | Hide-completed filtering can remove an active/review timer after prior period completion. | Preserve every unsettled timer through completed-visibility filtering. |
| P2 PLAT-03 | Rest-complete alert drops session ID and restores whichever Gym tab was last selected. | Explicit Workout intent with original-session validation and truthful ended-session fallback. |
| P2 PLAT-08 | Overnight deferred Habit notification actions retain prior logical date, but body/Open drops it and opens Today. | Carry and explain the reminder date through app logging/inspection. Preserve scheduling semantics. |
| P2 GYM-03 | Empty active workout's Create New Exercise saves a library item but leaves the workout empty; copy says workout-only. | Reuse atomic create-and-add with the current workout boundary; state reusable-library scope accurately. |
| P2 RV-04 | Settled missing original-source requests silently clear or remain pending by domain; Goals already gives a named result. | Consume ready absence once with local feedback and retain Return to Review; keep genuinely loading requests pending. |
| P2 RV-05 | Empty Review suggests complete a Task, but Open Tasks routes to Completed history. | Separate empty recovery intent from analytical history drilldown, selecting a useful capture/execution job. |
| P2 TR-02 | Existing Entry edit's duplicate-match recovery calls its draft a New Entry and misstates discard scope. | Use edit-specific identities and Keep Editing wording; preserve allowed duplicates and correct target switching. |
| P2 H3 | Partial Checklist offers Skip Today, then repository rejects because checked items remain. | Share eligibility/prerequisite explanation; do not silently erase partial work to permit skipping. |
| P2 SET-05 | Equal quiet start/end shows enabled protection although the policy has no quiet interval. | Explain/reject the zero-length interval with retained draft; preserve ordinary overnight/daytime behavior. |
| P2 H2 | Direct Count/Measurement increment clips the amount;1000000000 shows only+100 at200%. | Measured/bounded action width or truthful short Add label with full visible/accessible amount. Never shrink enlarged text. |
| P2 PLAT-04 | Widget+0 can record0.001; subtitle omits precision/unit/comparison/upper-bound/target-period meaning. | Reuse one compact precision/locale/unit/target summary describing the actual mutation. |
| P2 PLAT-05 | Stopped Duration widget shows Start as both metadata and action, hiding saved duration/target progress. | Show recorded period evidence separately from the next timer control. |
| P2 PLAT-06 | Elapsed widget text looks live but is a refresh-time snapshot that can stay stale. | Use a supported live timer display or explicitly name snapshot/update time; avoid battery-intensive polling. |
| P2 PLAT-07 | Extreme-text widgets hide metadata without preserving promised date/progress/target semantics on accessible owners. | Retain compact geometry and put essential state on body/action descriptions; verify actual spoken launcher traversal. |
| P2 PLAT-09 | Direct widget write failures are swallowed; successful refresh shows unchanged rows with no failure explanation. | Generation-bound local mutation result, clear Retry/Open recovery; distinguish stale rejection and post-commit ancillary warning. |
| P2 TR-03 | Oversized CSV recovery instructs deleting long text/older Entries to make a backup/export fit. | Preserve the output ceiling, state data is intact, direct to portable backup; distinguish size from provider-write failure. |
| P2 GYM-09 | Add from Workout silently shows only latest50, excluding valid older sessions from existing Routine-day reuse. | Existing paged/searchable source picker or explicit Show More; keep selected-day import semantics. |
| P3 TASK-03 | Tomorrow selects today in Tasks collection Calendar. | Anchor the shortcut to logical tomorrow, separate from collection planning-window start. Fresh repro. |
| P3 H4 | Enter Duration Manually from Today opens Log an Earlier Day. | Use actual selected date/intent in the common log form title. |
| P3 G5 | Time-in-range wording implies elapsed-time exposure, but calculation uses observations. | Name observation-based share; keep the actual scoring rule. |
| P3 TR-04 | Permanent-delete preparation claims integration verification although current impact covers only its own Track graph. | Name actual reviewed fields/history/values; do not revive retired integrations. |
| P3 SET-07 | Custom-unit Archive lacks the new-version dialog's explanation of picker versus historical usage. | Brief persistent consequence guidance plus existing Restore/Undo; keep historical contract immutable. |
| P3 SET-08 | Settings Search omits supported merge/move/color/emoji-rename aliases. | Add purpose vocabulary to existing manager entries and anchors. |
| P3 RV-06 | One-day month-to-date trend draws no observation because only multi-point lines are painted. | Draw the sole point and preserve textual count/date; no invented connecting trend. |

All 52 grounded findings are represented above. Grouped rows preserve separate component IDs while identifying a shared repair owner; optional improvements and unconfirmed probes are excluded from the finding count.

## Cross-component rules to make explicit

1. **An action affects the scope named beside it.** Selected phase, selected date, selected Entry and selected Area must flow to the authoritative operation. A summary of Today must open Today's job. Physical conversion must not silently become relabeling.
2. **Visible authored input belongs to the draft contract.** Pending text is dirty; inactive fields cannot secretly block saving; a submitted draft has one settled receipt owner. Navigation and input during Save must have deliberate semantics.
3. **Corrections preserve the inspection context.** Definition editors and child corrections return to the same relevant section/query/range/loaded history. An analytical source visit preserves the analysis that led to it.
4. **History survives organizational cleanup.** Archive is reversible visibility, not withdrawal of earned progress. Frozen closure, current definition, performed set and unperformed prescription are distinct facts and must be named accordingly.
5. **Recovery text distinguishes committed, failed and partially repaired.** A committed import with scheduling trouble is a saved-with-warning state. Export advice must preserve source evidence. A stale source tap must settle with a named result.
6. **Responsive changes preserve actionable meaning.** Actual large text can make rows taller. A clipped numeric action is unacceptable; hiding optional metadata must preserve its essential alternative semantics. One screenshot or semantic XML cannot certify all spoken/focus behavior.

These rules have existing owners in Whip. Adoption at remaining callers is preferable to another navigation, persistence, validation or design system.

## What should remain

Keep the owner-directed collection-first Task/Habit entry, one top workspace Search, meaningful per-component tabs, stable shared chrome, orthogonal archive, default collapsed optional controls and normal-density cards with readable enlarged identities. Keep the warm neutral visual system and identity color separate from semantic success/warning/destruction. No current evidence warrants a global typography, spacing or corner-radius replacement.

Keep completed Goal closure snapshots and original entered-unit evidence; typed conversion/versioning; exact destructive reviews and stale revision guards; independent paged histories; retained Review session; explicit Track evidence alongside normalized productivity; optional no-target Habits; original entity inspectors; and retired 5/3/1/link compatibility constraints. Missing cloud sync, weekend quiet overrides or arbitrary new navigation tabs are not defects under the current product contract.

The Review report additionally records three **optional improvements**, not confirmed correctness bugs: explain scoring/exclusions, explain correlation contributing-day/no-variation eligibility, and offer contextual History/date hints while preserving original-entity navigation. Track enlarged selection/reorder title and long Scale endpoint risks remain probes, not promoted defects without layout evidence.

## Recommended remediation sequence

**First:** fix the five P1 contracts at existing domain/persistence owners, with focused deterministic cases and controlled failure receipts. Freeze no release until phase preservation, direction-aware target truth, discrete Habit periods and committed-import reporting are verified. Routine delayed/recreated Save needs an owned receipt and complete draft protection.

**Second:** repair truth/availability consumers (Habit ending/current state, Goal cutoff, archived Review workouts, performed sharing, historical checklists, unit changes), then batch continuation/route restoration across Tasks/Tracks/Gym/Settings/Home/platform. Reuse common navigation/context owners while retaining each job's deliberate behavior.

**Third:** close draft/secondary-dialog/persistence gaps, impossible actions and harmful recovery copy. Add focused normal/actual200% originals where the visible promise changed. Finish smaller discovery/copy/chart details in a single bounded pass after behavioral contracts settle.

Acceptance should be affected-change based: one stable `scripts/check --ready` batch after implementation, selected exact emulator journeys, and original inspection for changed device behavior. During edits, exact checks under `timeout --kill-after=3s 55s`; a cap is incomplete and must not be silently counted as passed. This audit does not authorize implementation or a frozen Play candidate.

## Remaining runtime/visual uncertainty

The audit is complete within its declared source and sampled-native scope. The following are unresolved evidence, not an assertion that every configuration passed:

- Actual200% **short-height + lower-field IME** across maximal primary/secondary editors, especially active Gym next/rest lane, phased builder, Track Choice/Scale/condition editors, Goal hidden-field/type changes and Settings password/unit/emoji forms.
- Long names and extreme valid values in every selection/reorder/detail/support branch; Track's bespoke two-line selection title and competing Scale endpoints need current visual evidence.
- Physical book/tabletop folds, short landscape, wide200%, populated RTL and locale/timezone/DST/cutoff travel cases. Exact emulator column bounds cannot establish hinge/OEM behavior.
- Full current spoken TalkBack and Switch Access traversal: focus entry/return, separate nested actions, reading order, error/live announcements, disabled conditions, reorder alternatives and widget current state. XML proves labels/bounds, not speech.
- Controlled delayed/failing saves, killed-process pending Routine/child operations, post-commit merge ancillary failure, provider revocation/readback/size boundaries and recovery across app replacement.
- Actual launcher pin/resize/refresh/recreation and notification delivery/action return under permission loss, blocked channels, exact-alarm restrictions, quiet hours and OEM background policy. The fresh sole-Area intent check covers one narrow route only.

Remedies are proposed; none are marked implemented by this report. Canonical findings and the current product-memory INDEX link this audit so the next implementation can start from the present evidence rather than treating older overhauls as a blanket pass.
