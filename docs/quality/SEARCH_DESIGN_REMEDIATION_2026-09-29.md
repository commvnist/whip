# Search and workspace consistency correction — 2026-09-29

Status: **Implemented and privately released in 0.3.85/code 91.** The subsequent owner release request authorized signed packaging and installation/launch checks, which pass under VER-20260929-006. Tests, full lint and fresh rendered verification remain paused. Related: FB-20260929-005/006, FND-20260929-011, DEC-20260929-002, IMP-20260929-007/008.

The owner rejects the always-visible Habit/Goal find boxes and duplicated Task magnifying glass. Main collection lookup belongs to the existing contextual top-right search. This newer requirement supersedes the local collection-search portions of the September 29 deep review; it does not undo retained history correction, execution improvements or factual safeguards.

## Implemented corrections

| Current source finding | Correction |
| --- | --- |
| Habit/Goal search fields sit between tabs and headings, adding an extra fixed row. | Remove their fields, query state, filtered empty states and clear-search reorder branches. Collections and Insights retain their existing area scope and page layout. |
| Task page adds a second search icon and another find row. | Remove both. Keep contextual toolbar search and saved-filter text criteria. Show text criteria in the active-filter chips and count so a restored saved view cannot silently hide Tasks. |
| Track collection puts its new find field before the heading. | Remove its field and collection-query state. Retain bounded selection targets, area scope, pinned ordering and collection position. |
| Gym Exercise/Machine/Routine collections keep persistent find fields. | Remove them. Preserve lookup through unified search: equipment/muscles/linked machines, machine location/version/linked exercises, and routine notes/day/exercise names. Keep concise result display text separate from searchable metadata. |
| Routine search caching previously omitted day/exercise assignments. | Include authored routine days and placements in the existing content key; workout clock ticks still do not invalidate it. |
| Track Activity exposes a second magnifying glass beside its heading. | Retain text narrowing inside Activity Filters, with a truthful active count and collapsed summary. Entry history searches remain contextual. |
| Habit/Goal/Task header actions alternate between text buttons and overflow icons as counts or sections change. | Use the same overflow anchor, with available commands inside it. Empty-state primary actions remain visible. |
| Settings search lives one row below the global search position; its section action bypasses the adaptive header. | Route Settings purpose lookup through the shared toolbar anchor and Ctrl+K. Its feature owner reports availability and retains busy/editor guards. Standalone Settings surfaces use the shared adaptive header/action with the same page padding. |

## Main workspace source review

| Workspace/pages inspected | Geometry and ownership disposition |
| --- | --- |
| Home dashboard | Shared root toolbar, primary navigation and page edges. Its daily overview intentionally has no second-level destination tabs. Existing bounded domain previews and continuation routes retained. |
| Tasks Today/Inbox/Upcoming/History | Shared destination bar and page padding; remove duplicate search; stable Filter/More anchors. Planning/history subsections and explicit selection/reorder modes retain their intentional controls. |
| Habits Today/All/Archived/Insights | Shared destination bar, adaptive page heading and page edges; remove extra row; stable overflow. Today finished disclosure and inspector/history continuity retained. |
| Goals Active/History/Archived/Insights | Same destination/header/edge owners; remove extra row; stable overflow. Reorder and original measurement history retained. |
| Tracks/Archived/Activity/Insights | Same tab/header/edge owners; remove pre-heading find row and Activity duplicate icon. Compact detail Back and wide master/detail are intentional hierarchy differences. |
| Gym Workout/History/Library/Progress/Tools and Library Exercise/Machine/Category/Routine pages | Shared page headings/edges and destination bar. Remove library find rows; contextual toolbar scope already selects appropriate search domains. Workout execution, history-option filters and authoring pickers retain their distinct jobs. |
| Settings categories/sections and wide navigation | Shared root search position; feature-owned purpose results, disabled-state guards, adaptive page header and edges. Category/section navigation intentionally differs from collection tabs. |
| Cross-workspace root frame | One Scaffold toolbar, 52dp root actions, shared primary navigation and shared destination-tab owner. Responsive tabs measure every label before choosing scrolling; root navigation measures all labels independently of selection. No additional per-destination frame implementation found in these source paths. |

This is a source review, not a new screenshot/device pass. It cannot certify every font size, fold posture, OEM or rendered transition while verification is paused.

## Regression coverage prepared for resumption

Existing methods are updated rather than adding mirrored per-screen suites:

- `DeepSharedJourneyTest#workspaceSearchAndSavedFilterChildRetainConsistentChrome`: toolbar/search bounds across Tasks/Habits/Goals/Tracks/Gym; page-title alignment; one Settings search action at the same root position; contextual Task result routing; retained saved-filter child and visible query criteria.
- `DeepProductivityJourneyTest#habitAndGoalSearchUsesTheWorkspaceActionAndKeepsCollectionsClear` and `#trackWorkspaceSearchAndSelectionShowConfiguredDetails`: shared query recreation/routing, archived Habit lookup, clean collections, exact Goal count and Track selection/configured facts.
- Existing Gym library journeys and the dense Machine fixture use unified search with linked metadata, archive status and recreation, preserving exact routine-day launch and equipment-filter assertions.
- Existing Track Activity and Task bulk selection fixtures follow the new shared controls. Architecture and pure search contracts guard absence of collection find controls, metadata matching and authored-content invalidation.

None of these updated checks has been executed or compiled after the pause. UI discovery hashes and affected visual-catalog selectors also require reconciliation when verification resumes. The subsequent [0.3.85 release](../../artifacts/phone-releases/2026-09-29/0.3.85/README.md) compiles production source and verifies packaging/install/launch only. Schema, data epoch and backup format remain unchanged.
