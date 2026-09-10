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

## Pilot implementation

`WhipItemBuilders.kt` provides `WhipRecordItem` and a scoped declaration of `context`, `fact`, `edit` and `command`. The caller supplies record identity, title, optional emoji and Open behavior. Commands use the existing menu roles; destructive commands stay last and the renderer closes the menu before dispatch. New composition uses current callbacks, and menu state belongs to record identity.

Track Entries and Activity now use one Track-to-record presentation mapping. Both read title/actions → date and relevant Track context → labeled Field facts. The information spans the card width; a three-line fact preview preserves list scanning while the inspector provides complete values. Their former local Rows, edit glyph definitions and DropdownMenus are removed. The first normal phone pass shows the formerly truncated Activity facts in full and preserves exact actions/history.

Next, inspect the existing productivity-header family at 100% before changing it. Its seven callers already share a renderer; a builder is worthwhile where it removes caller-specific composition or makes action/status ordering enforceable. Do not simply rename that API. Navigation/settings and specialized execution follow their own concrete review. Continue functional defects and broader aesthetics alongside adoption.
