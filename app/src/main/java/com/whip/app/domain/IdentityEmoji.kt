package com.whip.app.domain

import java.util.Locale

const val DEFAULT_TASK_EMOJI = "✅"
const val DEFAULT_HABIT_EMOJI = "🔁"
const val DEFAULT_GOAL_EMOJI = "🎯"
const val DEFAULT_TRACK_EMOJI = "📋"

/**
 * One ranked concept in Whip's shared identity-emoji library. [searchTerms]
 * includes ordinary task language so the picker can find an emoji from a
 * user's intent instead of requiring them to recognize every glyph.
 */
data class IdentityEmojiPreset(
    val emoji: String,
    val label: String,
    val searchTerms: String,
)

/** A reusable identity choice owned and named by the user. */
data class CustomIdentityEmoji(
    val emoji: String,
    val name: String,
)

/**
 * Common-first across planning, home life, health, fitness, learning, money,
 * relationships, and structured tracking. Keep this a single ranked list:
 * position is part of the picker contract and is covered by tests.
 */
val IDENTITY_EMOJI_PRESETS: List<IdentityEmojiPreset> = listOf(
    IdentityEmojiPreset("✅", "General Task", "done complete check off todo"),
    IdentityEmojiPreset("📅", "Plan & Schedule", "calendar date today agenda appointment"),
    IdentityEmojiPreset("🎯", "Goal", "target objective outcome milestone"),
    IdentityEmojiPreset("📝", "Notes & Writing", "write document draft journal note"),
    IdentityEmojiPreset("⏰", "Reminder", "alarm wake deadline due time"),
    IdentityEmojiPreset("💼", "Work", "job career office business shift"),
    IdentityEmojiPreset("🏠", "Home", "house personal household"),
    IdentityEmojiPreset("❤️", "Health", "wellness heart wellbeing"),
    IdentityEmojiPreset("💪", "Fitness", "fitness exercise workout gym train training active strength muscle bicep weights lift lifting"),
    IdentityEmojiPreset("📚", "Reading", "book books library pages"),
    IdentityEmojiPreset("🛒", "Groceries", "grocery supermarket food shop"),
    IdentityEmojiPreset("🧹", "Cleaning", "clean tidy sweep chores room"),
    IdentityEmojiPreset("🍳", "Cooking", "cook breakfast kitchen recipe"),
    IdentityEmojiPreset("😴", "Sleep", "bed bedtime rest nap"),
    IdentityEmojiPreset("💧", "Hydration", "water drink fluids"),
    IdentityEmojiPreset("🚶", "Walking", "walk steps stroll"),
    IdentityEmojiPreset("🏃", "Running", "run jog cardio race"),
    IdentityEmojiPreset("🧘", "Mindfulness", "meditate meditation calm breathe breathing yoga"),
    IdentityEmojiPreset("🎓", "Study", "school course homework class learn education"),
    IdentityEmojiPreset("💰", "Budget & Saving", "money cash save savings finance budget"),
    IdentityEmojiPreset("👨‍👩‍👧", "Family", "parents children spouse partner relatives"),
    IdentityEmojiPreset("📞", "Calls", "call phone contact voicemail"),
    IdentityEmojiPreset("📧", "Email", "mail inbox reply message"),
    IdentityEmojiPreset("💊", "Medication", "medicine vitamin supplement prescription"),
    IdentityEmojiPreset("🥗", "Nutrition", "diet healthy meal vegetables calories"),
    IdentityEmojiPreset("🧺", "Laundry", "wash clothes fold clothing"),
    IdentityEmojiPreset("🧽", "Dishes", "dishwasher sink kitchen cleanup"),
    IdentityEmojiPreset("🗑️", "Trash & Recycling", "garbage bin compost recycling"),
    IdentityEmojiPreset("🛏️", "Make the Bed", "bedroom sheets linens"),
    IdentityEmojiPreset("🚿", "Shower & Self-Care", "bathe bathroom hygiene self care"),
    IdentityEmojiPreset("🪥", "Dental Care", "brush teeth floss oral hygiene"),
    IdentityEmojiPreset("🏥", "Medical Appointment", "doctor hospital clinic therapy appointment"),
    IdentityEmojiPreset("🦷", "Dentist", "dental teeth orthodontist appointment"),
    IdentityEmojiPreset("🧠", "Focus & Mental Health", "concentrate deep work therapy mind"),
    IdentityEmojiPreset("🌅", "Recovery & Sobriety", "sober sobriety recovery abstinence reset streak"),
    IdentityEmojiPreset("💻", "Computer Work", "laptop desktop online admin"),
    IdentityEmojiPreset("🤝", "Meetings", "meet collaborate networking one on one"),
    IdentityEmojiPreset("📊", "Reports & Analytics", "spreadsheet metrics review chart data"),
    IdentityEmojiPreset("📋", "Checklist & Tracking", "list log record entries inventory track"),
    IdentityEmojiPreset("🗂️", "Organize Files", "folders paperwork filing documents"),
    IdentityEmojiPreset("📌", "Priority", "important pin remember"),
    IdentityEmojiPreset("🔁", "Routine & Repeat", "recurring repeat cadence habit daily weekly"),
    IdentityEmojiPreset("⏱️", "Focus Timer", "timer pomodoro duration estimate"),
    IdentityEmojiPreset("🚗", "Driving & Commute", "car commute vehicle school run"),
    IdentityEmojiPreset("🚲", "Cycling", "bike bicycle ride cardio"),
    IdentityEmojiPreset("🚌", "Public Transit", "bus train subway commute"),
    IdentityEmojiPreset("✈️", "Travel", "flight vacation holiday airport"),
    IdentityEmojiPreset("🧳", "Packing", "pack luggage suitcase trip"),
    IdentityEmojiPreset("🗺️", "Trip Planning", "route map itinerary directions"),
    IdentityEmojiPreset("💳", "Bills & Payments", "pay credit card subscription invoice"),
    IdentityEmojiPreset("🏦", "Banking", "bank account transfer deposit"),
    IdentityEmojiPreset("📈", "Investing & Progress", "investment stocks growth improve trend"),
    IdentityEmojiPreset("🧾", "Taxes & Receipts", "tax expense receipt bookkeeping"),
    IdentityEmojiPreset("🛍️", "Shopping", "buy purchase store clothes"),
    IdentityEmojiPreset("📦", "Errands & Deliveries", "package pickup post office return ship"),
    IdentityEmojiPreset("🍽️", "Meal Planning", "dinner lunch meals menu dine"),
    IdentityEmojiPreset("☕", "Morning Routine", "coffee morning caffeine routine"),
    IdentityEmojiPreset("🍎", "Healthy Snack", "fruit snack food nutrition"),
    IdentityEmojiPreset("🐕", "Pet Care", "dog cat animal feed vet walk"),
    IdentityEmojiPreset("🌱", "Plants & Gardening", "garden plant lawn yard grow"),
    IdentityEmojiPreset("🔧", "Repairs", "fix repair tools broken"),
    IdentityEmojiPreset("🛠️", "DIY Project", "build renovate craft project"),
    IdentityEmojiPreset("🧰", "Maintenance", "maintain service inspection home car"),
    IdentityEmojiPreset("🏊", "Swimming", "swim pool laps cardio"),
    IdentityEmojiPreset("⚽", "Team Sports", "sport soccer football practice game"),
    IdentityEmojiPreset("🧗", "Climbing", "climb boulder hike adventure"),
    IdentityEmojiPreset("🤸", "Mobility & Flexibility", "stretch stretching mobility flexibility gymnastics"),
    IdentityEmojiPreset("⚖️", "Weight & Measurements", "weigh scale body measurement balance"),
    IdentityEmojiPreset("🩺", "Checkup", "blood pressure health screening medical"),
    IdentityEmojiPreset("🧴", "Skincare", "skin lotion sunscreen care"),
    IdentityEmojiPreset("✂️", "Grooming", "hair haircut nails trim salon"),
    IdentityEmojiPreset("🧑‍💻", "Coding", "code program software development computer"),
    IdentityEmojiPreset("🎨", "Art & Design", "draw paint creative design"),
    IdentityEmojiPreset("🎵", "Music Practice", "instrument sing singing piano guitar"),
    IdentityEmojiPreset("📷", "Photography", "camera photo photos video"),
    IdentityEmojiPreset("✍️", "Journaling", "journal diary reflection write"),
    IdentityEmojiPreset("🎮", "Gaming", "game play console hobby"),
    IdentityEmojiPreset("🎬", "Movies & Shows", "movie film television tv watch media"),
    IdentityEmojiPreset("🎧", "Podcasts & Audio", "podcast audiobook listen audio"),
    IdentityEmojiPreset("♟️", "Chess", "chess opening openings tactics rating practice"),
    IdentityEmojiPreset("🗣️", "Language Practice", "language speak speaking vocabulary conversation"),
    IdentityEmojiPreset("🧪", "Research & Experiments", "science research experiment lab test"),
    IdentityEmojiPreset("🧩", "Puzzles & Problem Solving", "puzzle solve logic challenge"),
    IdentityEmojiPreset("🏆", "Achievement", "win award success competition"),
    IdentityEmojiPreset("🙏", "Prayer & Spirituality", "pray religion church spiritual gratitude"),
    IdentityEmojiPreset("🤲", "Volunteering & Giving", "volunteer charity donate help service"),
    IdentityEmojiPreset("👶", "Childcare", "baby child kids parenting daycare"),
    IdentityEmojiPreset("🧓", "Eldercare", "senior parent caregiver care"),
    IdentityEmojiPreset("🫂", "Friends & Social", "friend social connection relationship visit"),
    IdentityEmojiPreset("🎉", "Events & Celebrations", "party event celebrate occasion"),
    IdentityEmojiPreset("🎂", "Birthdays", "birthday anniversary cake"),
    IdentityEmojiPreset("🎁", "Gifts", "gift present holiday shopping"),
    IdentityEmojiPreset("🔒", "Security & Passwords", "secure password privacy lock backup"),
    IdentityEmojiPreset("📱", "Screen Time & Phone", "mobile app social media digital detox"),
    IdentityEmojiPreset("💡", "Ideas & Brainstorming", "idea brainstorm think inspiration"),
    IdentityEmojiPreset("🚀", "Launch & Big Project", "launch ship release startup project"),
    IdentityEmojiPreset("🌍", "Community & Environment", "community climate environment civic local"),
    IdentityEmojiPreset("🌿", "Outdoors & Nature", "nature outside hike trail camping"),
    IdentityEmojiPreset("🔥", "Momentum & Streak", "streak consistency momentum challenge"),
    IdentityEmojiPreset("✨", "Personal Growth", "growth improve aspiration transformation"),
)

private val defaultIdentityEmojis = IDENTITY_EMOJI_PRESETS.mapTo(hashSetOf(), IdentityEmojiPreset::emoji)

fun String.isDefaultIdentityEmoji(): Boolean = trim() in defaultIdentityEmojis

/**
 * Preserves user order while enforcing the boundary between Whip's immutable
 * defaults and named, user-owned choices.
 */
fun normalizeCustomIdentityEmojis(values: Iterable<CustomIdentityEmoji>): List<CustomIdentityEmoji> = values
    .map { choice -> choice.copy(emoji = choice.emoji.trim(), name = choice.name.trim()) }
    .filter { choice ->
        choice.name.isNotBlank() && choice.emoji.isIdentityEmoji() && !choice.emoji.isDefaultIdentityEmoji()
    }
    .distinctBy(CustomIdentityEmoji::emoji)
    .distinctBy { choice -> choice.name.lowercase(Locale.ROOT) }

private val legacyIdentityEmoji = mapOf(
    "✓" to "✅",
    "○" to "⭕",
    "◆" to "🔹",
    "◎" to "🎯",
    "⚑" to "🚩",
    "✚" to "💊",
    "◉" to "💧",
    "▤" to "📋",
    "★" to "⭐",
    "⏱" to "⏱️",
    "◈" to "⚖️",
    "$" to "💰",
    "↗" to "📈",
    "◫" to "🗂️",
    "✦" to "✨",
    "♟" to "♟️",
)

/**
 * Returns a valid identity emoji while converting the pre-release symbol set
 * and rejecting arbitrary text. This is deliberately independent of UI so
 * repositories, restores, migrations, and editors share the same invariant.
 */
fun String.normalizedIdentityEmoji(defaultEmoji: String): String {
    val trimmed = trim()
    val migrated = legacyIdentityEmoji[trimmed] ?: trimmed
    return migrated.takeIf(String::isIdentityEmoji) ?: defaultEmoji
}

/**
 * Whip stores one visible emoji sequence, not an arbitrary label. The accepted
 * shape covers variation selectors, skin tones, flags, keycaps, and ZWJ emoji
 * while rejecting prose and two unrelated emoji pasted together.
 */
fun String.isIdentityEmoji(): Boolean {
    if (isBlank()) return false
    val trimmed = trim()
    if (trimmed in legacyIdentityEmoji) return false
    val codePoints = trimmed.codePoints().toArray()
    if (codePoints.isEmpty() || codePoints.size > 16) return false
    val hasKeycap = 0x20E3 in codePoints
    if (codePoints.any { !it.isEmojiSequenceCodePoint(hasKeycap) }) return false

    val bases = codePoints.filter { codePoint ->
        codePoint.isEmojiBase() ||
            (hasKeycap && (codePoint in '0'.code..'9'.code || codePoint == '#'.code || codePoint == '*'.code))
    }
    if (bases.isEmpty()) return false
    val regionalCount = bases.count { it in 0x1F1E6..0x1F1FF }
    return when {
        0x200D in codePoints -> bases.size >= 2
        hasKeycap -> bases.size == 1
        regionalCount > 0 -> regionalCount == 2 && regionalCount == bases.size
        else -> bases.size == 1
    }
}

private fun Int.isEmojiSequenceCodePoint(hasKeycap: Boolean): Boolean =
    isEmojiBase() ||
        this == 0x200D || // zero-width joiner
        this == 0xFE0F || // emoji variation selector
        this == 0x20E3 || // combining enclosing keycap
        this in 0x1F3FB..0x1F3FF || // skin-tone modifiers
        this in 0xE0020..0xE007F || // subdivision flag tags
        (hasKeycap && (this in '0'.code..'9'.code || this == '#'.code || this == '*'.code))

private fun Int.isEmojiBase(): Boolean = when {
    this in 0x1F000..0x1F3FA -> true
    this in 0x1F400..0x1FAFF -> true
    this in 0x2300..0x23FF -> true
    this in 0x2600..0x27BF -> true
    this in 0x1F1E6..0x1F1FF -> true
    this in setOf(0x00A9, 0x00AE, 0x203C, 0x2049, 0x2122, 0x2139, 0x2B50, 0x2B55, 0x3030, 0x303D, 0x3297, 0x3299) -> true
    else -> false
}
