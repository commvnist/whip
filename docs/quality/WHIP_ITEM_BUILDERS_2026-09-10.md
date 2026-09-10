# Whip item builders

Owner direction: FB-20260910-001/002. Working investigation: VER-20260910-003. Prioritize the normal 100% app experience.

Builders should let feature code describe an item's content and available actions while the shared renderer owns its reading order, spacing, typography, surfaces and responsive composition. Uniformity should be visible in the app and make common interactions predictable.

## Build on the existing system

Whip already has shared geometry, card surfaces, identity controls, productivity headers, settings rows and inspector patterns. The gap is that callers can still assemble equivalent content with arbitrary Rows, text styles, gaps and action sizes. Consolidate those composition decisions above the primitives; remove each replaced local layout when its migration is verified.

| Layer | Responsibility |
| --- | --- |
| Design tokens and primitives | Color, shape, typography, spacing, accessible controls. |
| Builders and renderers | Named content/action roles, reading order, alignment, optional-content collapse, natural height and available-width behavior. |
| Feature presentation | Names, values, units, dates, state labels, which actions exist and their meaning. |
| Feature state and repositories | Selection, drafts, requests, validation, persistence, history and recovery. |

## Builder families

- **Record items:** title, optional identity, context, supporting facts, direct edit and overflow. First pilot: Track Entries and Activity, which currently render the same recorded facts through different local layouts.
- **Productivity items:** identity/title, primary domain action, persistent status, summary, disclosure and expanded details. Migrate the existing productivity header after the record pilot proves the core arrangement; retain completion, timers and progress as explicit domain roles.
- **Navigation and settings items:** label, explanation, current value, status and one destination/choice action. Consolidate existing patterns when these families are reviewed.
- **Execution items:** workout/set identity, evidence, active inputs and completion. Share common header/fact/action roles while keeping the execution body and mutation semantics specialized.

Forms, dialogs and selection toolbars can use their own builders where repeated evidence justifies them. Do not postpone ordinary experience improvements for a complete framework rewrite.

## First production contract

A record builder declares identity, context/facts and actions without specifying font sizes, padding or Row placement. Its renderer uses the established card geometry and a single title/action header, followed by full-width information. Empty optional lines disappear. Menus have named commands and explicit destructive roles; their state remains keyed to record identity. Natural content height and wrapping belong to the renderer. List scrolling and window insets remain with the owning screen.

The pilot must preserve Open, Edit, Delete review, Open Track, archive read-only behavior and exact saved history. It should improve reading at 100%, especially when record titles, Track names and Field values are long. Do not add icons, containers or duplicated labels just to make the builder look richer.

## Rollout and acceptance

1. Capture a populated native record journey before changing it; compare Entry and Activity reading and actions.
2. Implement the record builder and migrate both callers, removing their duplicated layout and menu plumbing.
3. Verify normal phone and wide rendering, key native actions and relevant regressions. Use a small shared layout contract for responsiveness; avoid a new per-screen 200% campaign.
4. Refine from the rendered result, commit the verified pilot, then move the next equivalent family onto the shared contract.
5. Keep the whole-app design and bug backlog active. Track builder adoption by family, including justified specialized bodies, and remove obsolete APIs once their final caller migrates.

This document proposes a migration direction. It is not evidence that every item family has already been unified.

## Reuse across feature boundaries

The Routine outline increment (DEC/VER-20260910-016) reuses the existing NavigationRow and DisclosureRow contracts: guided program setup declares a destination and explanation, and optional notes declare their disclosure state. Ordinary Add Exercises sits beside the selected day, before its records. The initial phone action moves 764 pixels upward at 100% through hierarchy changes. A new universal form builder is not justified by this one outline; state, programming and specialized placement editing remain feature-owned. FND-20260910-026 records the next substantive opportunity: put ordinary Set prescriptions ahead of conversion and bulk helpers before choosing a shared authoring-record API. Final acceptance is tracked in VER-20260910-016.

The verified Gym Library increment (DEC/IMP/VER-20260910-015) brings Exercises, Machines and Categories into the record family. Features declare complete context/details, optional Open/Edit, one direct action, menu commands and reorder state. The renderer owns natural title height, information width, action geometry and the existing reorder handle; reorder mode hides Open and ordinary actions and resets any open menu. Machine configuration uses complete detail lines while Track facts keep their bounded preview. This removes three local card compositions and the Machine list's separate menu state without introducing a Gym-only framework.

This demonstrates the intended division: a Category declares that Restore is available, a Machine declares which version/settings to show, and an Exercise declares its tracking summary; none chooses Row placement, padding or text styles. Their existing repositories, mutation coordinators, forms and ordering callbacks remain independent. Library navigation already uses consistent shared rows and is retained. Complete native edit/recreation/reorder/archive/restore/version/history and neighboring Track/Review journeys pass on ordinary phone/wide targets, with 95 final-production regressions, twelve final focused methods per platform, 371 JVM readiness checks and 45 reviewed originals. Equipment search also now indexes every linked Exercise and reuses the shared Clear Search input. Evidence: `artifacts/astra-audit/2026-09-10/gym-library/README.md`.

Further adoption should address a demonstrated repeated layout or interaction, then remove its replaced composition. Routine program/execution bodies stay specialized until their full journey review establishes appropriate common roles. Wide master/detail duplication and reading width need a shared screen-level policy; record builders alone do not settle navigation architecture. Continue these and ordinary bug/design work alongside incremental builder adoption.

The verified Organization lifecycle increment (DEC/IMP/VER-20260910-014) adds a shared search input role. `WhipSearchField` owns label/value composition, single-line query editing and the conditional Clear Search action; the Area manager, Area picker and Tag manager retain their query limits, filtering and saved state. A feature may omit unused search below its count threshold, but an entered query must remain visible until cleared. Existing-identity recovery also declares the saved color as read-only through the existing color-field roles, so the form exposes only choices the operation will honor. These contracts improve ordinary recovery while keeping repository and transaction policies outside the renderers. Complete native Area lifecycle/history and controlled picker transitions pass on API 34/37, with 86 final-production regressions, final focused cohorts, 371 JVM readiness checks and 48 original reviews; see `artifacts/astra-audit/2026-09-10/area-lifecycle/README.md`.

The verified Organization increment (DEC/IMP/VER-20260910-013) extends the role-based approach to destination headers. Areas and Tags declare title, complete supporting text, optional Create/Back and trailing Close through `WhipManagementHeader`; features no longer place those controls or choose a separate wrapping branch. Root lists lose the repeated introductory heading, while Area count/reorder, Tag search and every child operation remain domain-owned. This is a bounded header contract; it does not force manager records or transactional forms into the record builder. Acceptance includes 51 API 34 checks, four API 37 methods, 371 JVM readiness checks and 23 personally inspected original images, with real cross-domain Tag history and Area rename/reopen journeys. See `artifacts/astra-audit/2026-09-10/organization/README.md`; normal Area lifecycle is now verified in VER-20260910-014; shared wide reading measure and broader design review remain open.

The Review outcome increment (DEC/IMP/VER-20260910-011) reuses the record family for contributing Tasks, successful Habit periods, normalized Goal progress and finished Workouts. Each feature supplies exact source identity, dates, archive context and contribution; `WhipRecordItem` supplies the same reading order, typography, information width and interaction surface. No Review-specific card builder or domain scoring framework is introduced. The owning Review list supplies scrolling and a bounded reading width inside its existing compact or wide pane.

The data projection is separate from the visual builder. It feeds daily totals, correlations and contributing rows using existing domain calculations, so the visible total can be explained by its records. Existing feature inspectors retain editing and history behavior. This is the intended architectural payoff: new cross-feature experiences inherit coherent item composition while preserving each domain's meaning.

## Pilot implementation

`WhipItemBuilders.kt` provides `WhipRecordItem` and a scoped declaration of `context`, `fact`, `edit` and `command`. The caller supplies record identity, title, optional emoji and Open behavior. Commands use the existing menu roles; destructive commands stay last and the renderer closes the menu before dispatch. New composition uses current callbacks, and menu state belongs to record identity.

Track Entries and Activity now use one Track-to-record presentation mapping. Both read title/actions → date and relevant Track context → labeled Field facts. The information spans the card width; a three-line fact preview preserves list scanning while the inspector provides complete values. Their former local Rows, edit glyph definitions and DropdownMenus are removed. The first normal phone pass shows the formerly truncated Activity facts in full and preserves exact actions/history.

The productivity family is verified under FND/DEC/IMP/VER-20260910-004. Native 100% review confirmed duplicated running-timer information and Edit interrupting deeper details. `WhipProductivityItemContent` replaces all seven header calls with declared summary/details text, Area, notice, disclosure, primary action and specialized expanded content. It owns full-width text and places the disclosed Edit footer after the complete body. The old header implementation and unused persistent-summary branch are removed. Caller-owned WhipItemCard gestures/colors and domain controls remain explicit. Accepted API 34 batches cover 90 shared/product checks and five final catalog methods; API 37 passes eight focused/catalog methods plus Home. Readiness and 33 original image reviews support this bounded adoption; see artifacts/astra-audit/2026-09-10/productivity-builders/README.md.

Settings adoption is verified under FND/DEC/IMP/VER-20260910-006. The normal native Planning/Appearance review found independent dropdown, toggle and typed-value hierarchies. `WhipSettingItem` declares one choice, toggle or typed-edit role and an optional explanation; the renderer owns label/control alignment, complete value text, full-width explanations and choice-menu identity. Typed editors retain their explicit durable Save coordinator, and choices retain immediate callbacks. Actual category/navigation groups were reviewed and retained: changing their API alone would add no demonstrated benefit.

Specialized execution follows its own concrete review. Continue functional defects and broader aesthetics alongside adoption. Further primary-action sizing consolidation should be driven by observed ordinary-scale cases; the productivity builder currently preserves each domain control's existing width requirement.

Settings acceptance includes 147 fresh API 34 checks, five API 37 methods, 346 JVM readiness checks and 25 reviewed original images. A native choice/draft/recreation/Save/reopen journey verifies the real production route on phone and wide layouts. Evidence: `artifacts/astra-audit/2026-09-10/settings-builders/README.md`. Execution-item acceptance follows below; full-product gates remain open. Preserve specialized workout bodies while looking for demonstrated duplication in identity, facts and actions; do not turn this into a universal form framework.

Execution adoption is verified under FND-20260910-008/009 / DEC/IMP/VER-20260910-007. Four Set renderers now declare identity, values, outcome, target, additional evidence, exact actions and specialized inputs through `WhipExecutionItem`. It retains the established passive/active appearance and one domain outcome mapping; the native baseline proves that omitted optional work previously lost its Optional identity and displayed Removed instead of Skipped. The new full journey checks Undo, exact Set identity, draft recreation, quick-save, unchanged neighboring facts and History. Final Gym/5/3/1 and neighboring journeys pass all 177 API 34 checks; API 37 passes four selected methods and readiness passes 346 JVM checks, compilation/lint/debug packaging. Fourteen original reviews and exact evidence are retained in `artifacts/astra-audit/2026-09-10/execution-builders/README.md`. A controlled normal-scroll regression also proves that Add Exercise/Arrange retain their names when only 4dp remains exposed below the sticky execution panel. This family deliberately owns Set composition without importing program calculations, repositories or draft lifecycle into the builder.
