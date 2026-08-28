# WHIP comprehensive product polish

## Status

Discovery complete; product/UX interrogation in progress. No major redesign or styling migration begins until the high-leverage questions below are answered or explicitly converted into documented assumptions.

## Phase 1 — observed product model

### Product purpose

WHIP is a private, local-first Android system for turning intent into action and evidence. It combines:

- finite work in Tasks;
- repeated practice and check-ins in Habits;
- longer outcomes and progress calculations in Goals;
- user-defined structured evidence in Tracks;
- exercise libraries, routines, live workouts, history, records, and tools in Gym;
- cross-cutting Areas, Home, Review & Trends, global search/add, automation, reminders, widgets, Health Connect, and backup/restore.

The strongest existing promise is ownership without fragmentation: each kind of progress retains its domain meaning, while Home, Review, Areas, search, and automations connect the domains without collapsing them into one generic record type.

### Current information architecture

- Persistent peer destinations: Home, Tasks, Habits, Goals, Tracks, Gym.
- App-level workspaces: Settings, Review & Trends, Areas, global Search, global Add.
- Tasks: Inbox, Today, Upcoming, Completed, Archived; List/Agenda/Calendar are view modes, not destinations.
- Gym: Workout, History, Progress, Library; Library leads to Routines, Exercises, Machines, Categories, and Tools.
- Areas provide a persistent productivity scope across Home, Tasks, Habits, Goals, Search, and Review; Gym is deliberately outside that scope.

### Current action model

- The top app bar owns global/contextual Add, Area scope, content-pane expansion, search, and settings.
- Peer destinations own tabs or a labeled Pages overflow.
- Object cards expose an obvious primary state change, direct details, and a trailing edit/action affordance.
- Basic editors progressively disclose advanced configuration.
- Reorder is now an explicit focused mode with full-item direct manipulation and accessible alternatives.
- Obvious local state changes avoid success-notification spam; destructive, recoverable, failed, or non-obvious consequences retain feedback.

### Current responsive model

- Compact: bottom navigation and single content pane.
- Navigation rail: persistent left navigation at medium widths.
- Expanded dashboard: navigation/support context plus a primary content pane.
- Book fold: hinge-aware support and content panes.
- Tabletop fold: vertically separated support and content.
- Destination-sized editors and managers stay inside the active pane; transient dialogs are pane-aware.

### Current design-system foundation

- Material 3 with a warm neutral light palette and near-black/warm-white identity.
- Semantic action, selection, success, warning, destructive, and metadata colors.
- Restrained 4/6/8/10/12 dp radius scale.
- A partially specified typography hierarchy.
- Shared buttons, filter chips, page headers, empty states, sections, settings rows, action lists, item cards/headers, inspectors, editors, dialogs, and reorder primitives.

This is a real foundation, not a blank slate. The next system pass should formalize and enforce it rather than replace it for novelty.

### Preliminary systemic tensions

These are investigation hypotheses, not redesign decisions:

1. **Product center of gravity:** the app calls five domains peers, while onboarding and Home encourage Tasks/Habits first. The intended primary persona determines whether this is purposeful progressive capability or an over-broad first impression.
2. **Home identity:** empty Home currently teaches every module; populated Home is an attention dashboard. We need to decide whether Home is primarily a launchpad, command center, daily brief, or configurable mixture.
3. **Global versus contextual actions:** the same top-right Add location changes scope by destination. This is spatially consistent but can be semantically ambiguous unless the current effect remains unmistakable.
4. **Power without overload:** advanced controls, Tracks, automation, taxonomy, health, and deep Gym configuration are valuable, but discoverability and progressive disclosure need a single policy.
5. **Surface proliferation:** the UI contains many legitimate dialogs, editors, menus, and inspectors. The key question is which jobs deserve a transient overlay, a destination-sized surface, contextual expansion, or a stable page.
6. **Container dependence:** the app already uses shared cards well, but approximately 130 Surface/Card callsites create a continuing risk that containment substitutes for hierarchy.
7. **Token enforcement:** common spacing values dominate, but the UI still contains about 1,190 inline dp literals and the spacing/size vocabulary is not formally tokenized. Many are legitimate geometry; others may encode drift.
8. **Architecture pressure:** `GymScreens.kt`, `WhipApp.kt`, and `TrackScreens.kt` exceed 17,000 lines combined. This does not directly make the UI bad, but it raises the likelihood of locally invented variants and makes design-system enforcement difficult.
9. **Documentation drift:** architecture documentation still names obsolete schema/product assumptions. Product rules need one authoritative living specification so the code, tests, and design language do not diverge.
10. **Responsive intent:** expanded Home currently combines a contextual left summary with a broad instructional/right pane. It is coherent, but the support pane’s enduring job after onboarding needs explicit product intent.

## Phase 2 — product and UX questions

The questions are grouped by decision, not by screen. The most consequential questions are marked **Core**.

### Users, promise, and success

1. **Core:** Is WHIP primarily for a broad individual who wants one calm place to manage life, or for a power user/quantified-self user who actively configures sophisticated systems? Is one the default persona and the other an opt-in mode?
2. Are there meaningfully distinct personas—for example casual planner, habit builder, serious lifter, data tracker, and automation-heavy power user—or should the product deliberately optimize for one blended person?
3. **Core:** Within ten seconds of first launch, which promise should dominate: “know what to do today,” “manage every kind of personal progress,” “own your private data,” or something else?
4. What are the five most common workflows in actual use, in order? Which actions happen dozens of times in one session?
5. What are the rare but critical workflows whose reliability matters more than discoverability—for example restore, history correction, machine configuration, or automation repair?
6. What does a successful everyday session look like: clear Today, complete/check in, log evidence, run a workout, review progress, plan tomorrow, or some combination?

### Information architecture and navigation

7. **Core:** Should Tasks, Habits, Goals, Tracks, and Gym remain equal permanent peers, or are Tracks and/or Gym specialized workspaces that should visually recede until used?
8. **Core:** What is Home’s primary job after onboarding: daily command center, cross-domain summary, customizable launchpad, coaching/next-best-action surface, or a deliberate combination? Which role wins when they conflict?
9. Should Review & Trends become a persistent peer destination for users with enough history, remain a Home/app-level action, or be treated as a secondary analytical workspace?
10. Is the global Area scope a central daily mental model users should always see, or an organizational power feature that should become quiet when only one Area exists?
11. Should Gym remain completely outside Areas, or are real use cases such as “Home gym,” “Rehab,” or “Sport training” evidence that Gym eventually needs a separate but analogous context model?
12. Where do you personally experience backtracking or wonder whether a feature belongs in Settings, a domain page, an item inspector, or a contextual menu?

### Actions, object behavior, and context preservation

13. **Core:** When a user taps a Task, Habit, Goal, Track, exercise, or routine row, should the default be a quick inspector, full editor, domain detail page, or domain-specific behavior? How strongly should WHIP standardize this?
14. Is the top-right `+` best understood as “create something relevant here” or as a global creation menu whose first option adapts to context? Should it always display a nearby textual cue when its consequence is not obvious?
15. Which actions must stay beside the object they affect, and which should live in overflow? Are there any current one-tap actions you consider non-negotiable?
16. Which operations should use Undo instead of confirmation? Which destructive actions are consequential enough that confirmation is mandatory even when recovery exists?
17. Should edits autosave, explicitly Save, or follow the current explicit-save model by domain? Are there places where the current model makes you wonder whether something persisted?
18. Are long press, swipe actions, double tap, or context menus desirable expert accelerators, or should WHIP avoid hidden gesture vocabularies except drag/reorder?

### Density, progressive disclosure, and expert use

19. **Core:** Should the recommended setup default to comfortable cards or compact list rows? Is compact mode an expert preference, a phone-size behavior, or the eventual default once users have data?
20. Which pages should feel dense and operational—Today, active workout, task planning, Track entries—and which should remain reflective or spacious—Home, insights, progress, review?
21. Should advanced controls be hidden by one global preference, decided per editor, remembered per surface, or revealed automatically when a user begins using advanced features?
22. Do you want keyboard shortcuts and hardware-keyboard workflows treated as first-class on an open Fold/tablet, or primarily as accessibility/fallback behavior?
23. Which advanced features are currently too hard to discover? Conversely, which features occupy permanent attention despite being rarely used?

### Search, filters, selection, and saved state

24. **Core:** Should global search start in the current module and visibly offer All WHIP, or always search everything? What do you instinctively expect from the header search icon?
25. Should filters update immediately, or should complex filter panels require Apply? Which filters should persist across destination changes or app restarts?
26. Should multi-selection survive navigation/filter changes, or should leaving the owning list always clear it? Is preserving selection more valuable than preventing hidden bulk scope?
27. Is a saved filter/view part of your normal workflow, or is it configuration weight that should exist only where many recurring queries justify it?

### Responsive behavior and devices

28. **Core:** Rank the real target contexts: closed Fold/phone, open Fold, ordinary phone, tablet, desktop-like window. Which one is primary for repeated daily use?
29. On the open Fold, should the left/support pane primarily provide navigation, selected-item context, Today summary, or domain-specific tools? Should it ever be independently scrollable/actionable?
30. Must every capability remain directly available on closed-phone layouts, or may rare configuration move to deeper pages while common workflows stay shallow?
31. Are split views expected to preserve simultaneous context (master/detail), or is the extra pane mainly a convenience that should disappear whenever it cannot add a genuinely useful second task?
32. What minimum supported window size and maximum text scale matter in practice beyond the current Android/API contract?

### Voice, motivation, and visual personality

33. **Core:** Should WHIP feel primarily calm/utilitarian, technical/precise, warm/encouraging, athletic/energetic, playful, or intentionally different by context? What three adjectives should every screen share?
34. Are there products whose interaction quality—not merely visual style—should be benchmarks? Which specific behaviors do you admire in them?
35. Should the app’s neutral chrome dominate while user content carries personality through emoji/Area color, or should WHIP itself have a more expressive visual presence?
36. How should motivational language behave? Should WHIP celebrate progress, remain matter-of-fact, adapt through low-pressure mode, or avoid coaching language entirely?
37. Should animation be nearly invisible and functional, or can selected moments such as completion, progress, and workout transitions be more expressive? Is reduced motion important to you personally?

### Trust, state, errors, and performance

38. Which asynchronous actions genuinely take long enough that users need queued/processing/success stages? Which current status messages feel like implementation leakage?
39. What failure modes have you encountered in real use—lost draft, unclear save, stale screen, wrong scope, slow list, notification failure, import ambiguity, or something else?
40. How large do you expect real datasets to become: Tasks, Track entries, workout history, exercises, routines, and automation rules? Which lists must remain instant at thousands or tens of thousands of records?
41. Should offline/local-first status remain mostly invisible because it is the normal state, or should privacy/storage/backup health have a persistent trust surface?

### Accessibility and release constraints

42. Are TalkBack, switch/keyboard access, color-vision differences, large text, reduced motion, or one-handed use known user requirements, or should WCAG/Android best practice be the baseline in absence of specific personas?
43. Are there workflows where 48 dp targets or expanded labels would conflict with the density you need? If so, which expert surfaces should use a deliberate dense-but-keyboard-accessible exception?
44. Which existing visual/layout choices are intentionally part of WHIP’s identity and should not be materially changed?
45. Are there upcoming features or releases this refinement must leave architectural room for?

## Provisional defaults if unanswered

To avoid blocking indefinitely, unanswered questions will initially use these assumptions:

- One broad individual persona is primary; sophisticated configuration is progressive, not a separate product mode.
- Home is a daily command center first, cross-domain summary second, and onboarding launchpad only when empty.
- The five domains remain peers, but unused advanced domains do not compete for Home attention.
- Tap opens context/details; explicit Edit changes data; visible primary actions perform the most frequent state change.
- Current-module search is the default with an unmistakable All WHIP expansion.
- Filters update immediately when cheap; destructive/bulk scope requires explicit visibility; selection is local to its list.
- Comfortable density is the onboarding default, with compact behavior user-controlled rather than silently breakpoint-controlled.
- Closed phone and open Fold both receive full capability, but their composition differs rather than merely shrinking.
- Open Fold support panes must provide real navigation or context; otherwise content should be allowed to expand.
- Visual language is calm, precise, warm-neutral, and functional; content and user identity carry most expressive color.
- Motion is restrained, causal, fast, and reduced-motion aware.
- Accessibility best practices are baseline product quality, not an optional persona.

## Next decision gate

After the user response, convert answers and accepted assumptions into:

1. a concise UX model;
2. enforceable product/design principles;
3. a semantic design-system specification;
4. a severity-ranked systemic/local audit;
5. an implementation sequence with explicit before/after acceptance criteria.
