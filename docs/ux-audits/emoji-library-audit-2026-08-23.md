# Identity Emoji Library Audit — 2026-08-23

## Outcome

Whip now has one shared, ranked library of exactly 100 identity emojis for Habits, Goals, and Tracks. The canonical inventory is `IDENTITY_EMOJI_PRESETS` in `IdentityEmoji.kt`; keeping the rank and search vocabulary beside each emoji prevents different editors from drifting into different presets.

The picker also has a persistent **My Emojis** section. Every reusable custom emoji has a user-defined name that appears in search and anywhere the choice is selected. Users can add a named choice from any picker, or add, rename, replace, and remove their choices in **Settings → Organization → Custom Emojis**. Custom order is stable across app restarts and backup/restore.

## Audit method

The rank combines:

1. Broad planning utility: general task completion, scheduling, goals, notes, and reminders come first because they can represent the widest range of records.
2. Everyday activity prevalence: work, home, personal care, eating, household work, shopping, caregiving, education, and leisure categories were checked against the U.S. Bureau of Labor Statistics American Time Use Survey.
3. Whip's actual product vocabulary and templates: habits, goals, Tracks, gym, health, sobriety, reading, media, chess, finance, projects, and recurring routines were all checked directly in current source.
4. Long-tail specificity: travel, sports, creative practice, community, hobbies, and specialized tracking follow the broad daily categories.

This is a common-first product ranking, not a claim that one exact worldwide frequency order exists. Within the constraints of a compact preset, broad concepts deliberately precede narrow ones.

## Coverage result

The 100-item library covers these high-level groups:

- Planning and productivity: tasks, schedule, goals, notes, reminders, priorities, routines, focus timers, checklists, files, reports, and projects.
- Work and administration: work, calls, email, meetings, computer work, bills, banking, taxes, shopping, deliveries, and security.
- Home and personal care: groceries, cleaning, cooking, laundry, dishes, trash, bed, shower, dental care, grooming, repairs, and maintenance.
- Health and fitness: general health, sleep, hydration, medication, nutrition, medical and dental appointments, mental health, sobriety, strength, walking, running, cycling, swimming, sports, mobility, and measurements.
- Learning and creativity: reading, study, coding, art, music, photography, journaling, languages, research, and problem solving.
- Relationships and life: family, childcare, eldercare, pets, friends, volunteering, spirituality, celebrations, birthdays, and gifts.
- Track-oriented interests: books, movies and shows, podcasts, chess openings, gaming, travel, gardening, nature, and personal growth.

Search terms include ordinary intent words that may not appear in the visible label. Examples: `brush teeth` finds Dental Care, `sober` finds Recovery & Sobriety, `books` finds Reading, `opening` finds Chess, and `subscription` finds Bills & Payments.

## Product and data rules

- Built-in presets remain immutable, common-first, and visibly identified as read-only in settings.
- User-owned emojis are named records and always render after built-ins under **My Emojis**; search matches either the name or glyph.
- A built-in emoji cannot be added to, renamed through, replaced through, or deleted from the custom collection.
- Invalid text, blank names, two unrelated emojis, and duplicate custom glyphs are discarded during normalization, restore, and repository persistence.
- Joined emojis, skin tones, flags, keycaps, and variation-selector sequences remain supported.
- There is no arbitrary limit on custom emoji count.
- Removing a custom choice does not rewrite existing Habit, Goal, or Track identities that already use it.

## Verification contract

- `IdentityEmojiTest` enforces the exact count, uniqueness, valid emoji sequences, top-rank order, representative coverage, immutable-default separation, and named custom-list normalization.
- `InteractionControlUiTest` covers intent/name search, named custom save, reuse, and removal.
- `SettingsBehaviorUiTest` covers the visible Organization manager's add, rename, and remove flow.
- `AppSettingsPersistenceTest` covers process/repository recreation.
- `BackupRepositoryTest` covers export, data reset, and restore.
- `SettingsCauseEffectContractTest` prevents the new persisted setting from losing named persistence or behavior evidence.
