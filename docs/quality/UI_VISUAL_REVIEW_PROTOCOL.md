# Whip systematic visual-review protocol

## Purpose

Use one reproducible emulator-only process to find design drift across every
declared page, dialog, menu, system handoff, and meaningful widget state. The
catalog establishes completeness, the gallery makes cross-family comparison
fast, and the durable finding ledger keeps critique tied to implementation and
verification. The physical owner phone is reserved for the final signed
deployment and is never part of screenshot collection or instrumentation.

## Produce an exact review set

1. Run `scripts/ui-catalog lint`. Any source-discovery drift, invalid row,
   duplicate ID, exception without disposition, or pending selector fails the
   review before capture.
2. On one explicitly selected disposable emulator, run:

   ```bash
   ANDROID_SERIAL=emulator-5554 scripts/ui-catalog capture /tmp/whip-ui-review
   ```

   To shorten a complete capture, opt into one matching secondary emulator;
   both targets remain explicit and emulator-only:

   ```bash
   ANDROID_SERIAL=emulator-5554 WHIP_ANDROID_SECONDARY_SERIAL=emulator-5556 \
     scripts/ui-catalog capture /tmp/whip-ui-review
   ```

3. Accept the set only when instrumentation reports zero failures/skips and the
   exporter reports the exact catalog count with no missing or unexpected PNG
   or semantics XML files.
4. Open `/tmp/whip-ui-review/index.html`. Use its search and family/kind filters
   to compare related surfaces. `scripts/ui-catalog report` can recreate only
   the gallery from an existing accepted capture.

## Review every card

For each surface, inspect both the pixels and the linked semantics evidence.
Judge the surface against the same eight questions:

1. **Purpose:** Is the current state and the next useful action apparent without
   reconstructing meaning from loose labels and values?
2. **Hierarchy:** Do title, context, content, status, primary action, and
   secondary/destructive actions have distinct, repeatable emphasis?
3. **Composition:** Are related facts bounded and grouped; are unrelated jobs
   separated; is density appropriate to the task?
4. **Design language:** Does the surface reuse Whip typography, spacing, shape,
   color, icon, row/card, editor, inspector, and dialog roles rather than invent
   a local dialect?
5. **Action grammar:** Are commit, cancel, navigation, overflow, selection,
   warning, and destruction placed and styled consistently with their meaning?
6. **State truth:** Are empty, populated, selected, disabled, busy, success,
   warning, error, paused, completed, and archived states visible without color
   alone or misleading controls?
7. **Reachability:** Are controls legible, named, at least 48 dp where
   interactive, ordered logically, scrollable when needed, and compatible with
   keyboard, screen reader, narrow width, and enlarged text?
8. **Copy:** Is language concise, concrete, domain-consistent, and explicit
   about consequences where a choice changes or destroys data?

Review in this order so shared causes are visible before leaf fixes: shared
shell and controls; Tasks; Habits; Goals; Tracks; Gym; Settings; Organization.
Within each family compare pages, then inspectors/editors, then menus and
confirmation/error states.

## Record and close findings

Every confirmed issue receives a durable `FND-*` record before implementation,
including severity, evidence surface IDs, expected behavior, shared cause, and
affected users. Resolve a shared component when it safely fixes several
surfaces; otherwise make the smallest truthful leaf change. Do not declare a
surface passed because its test executed.

After each coherent fix, run the exact affected tests and recapture its family.
After production source is frozen, run a fresh complete catalog capture and
review every card again. Completion requires exact evidence accounting, no
unresolved P0/P1 or unaddressed confirmed P2, documented P3 decisions, passing
affected checks, and a final signed in-place phone deployment. Play Store-only
candidate tests are outside this development review and run only for a Play
Store release.
