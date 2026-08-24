package com.whip.app.ui

import com.whip.app.domain.BodyweightLoadPolicy
import com.whip.app.domain.EstimatedOneRepMaxFormula
import com.whip.app.domain.Exercise
import com.whip.app.domain.ExerciseTrackingType
import com.whip.app.domain.ScheduleKind
import com.whip.app.domain.ScheduledTask
import com.whip.app.domain.TaskPriority
import com.whip.app.domain.Track
import com.whip.app.domain.TrackEntry
import com.whip.app.domain.TrackEntryProjection
import com.whip.app.domain.TrackField
import com.whip.app.domain.TrackFieldType
import com.whip.app.domain.TrackFieldValue
import com.whip.app.domain.TrackProjection
import com.whip.app.domain.WhipTask
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class SortDirectionTest {
    @Test fun taskSortFieldsHonorBothDirectionsAndKeepMissingDatesLast() {
        val tasks = listOf(
            scheduledTask(1, "Zulu", null, TaskPriority.None),
            scheduledTask(2, "Bravo", LocalDate.of(2026, 8, 24), TaskPriority.High),
            scheduledTask(3, "Alpha", LocalDate.of(2026, 8, 23), TaskPriority.Low),
        )

        assertEquals(listOf("Alpha", "Bravo", "Zulu"), tasks.sortedForWorkspace("Title", SortDirection.Ascending).map { it.task.title })
        assertEquals(listOf("Zulu", "Bravo", "Alpha"), tasks.sortedForWorkspace("Title", SortDirection.Descending).map { it.task.title })
        assertEquals(listOf("Alpha", "Bravo", "Zulu"), tasks.sortedForWorkspace("Scheduled Date", SortDirection.Ascending).map { it.task.title })
        assertEquals(listOf("Bravo", "Alpha", "Zulu"), tasks.sortedForWorkspace("Scheduled Date", SortDirection.Descending).map { it.task.title })
        assertEquals(listOf("Zulu", "Alpha", "Bravo"), tasks.sortedForWorkspace("Priority", SortDirection.Ascending).map { it.task.title })
        assertEquals(listOf("Bravo", "Alpha", "Zulu"), tasks.sortedForWorkspace("Priority", SortDirection.Descending).map { it.task.title })
    }

    @Test fun trackBuiltInAndFieldSortsHonorDirectionWhileKeepingBlankValuesLast() {
        val projection = trackProjection()
        val entries = projection.entries
        val score = projection.fields.single { it.name == "Score" }
        val title = projection.fields.single { it.name == "Title" }
        val notes = projection.fields.single { it.name == "Notes" }

        assertEquals(listOf("Title", "Notes", "Score"), projection.sortableFields().map(TrackField::name))
        assertEquals(listOf("Alpha", "Middle", "Zulu"), projection.sortedEntries(entries, TrackSort.Identity, null, SortDirection.Ascending).map(projection::primaryText))
        assertEquals(listOf("Zulu", "Middle", "Alpha"), projection.sortedEntries(entries, TrackSort.Identity, null, SortDirection.Descending).map(projection::primaryText))
        assertEquals(listOf("Zulu", "Alpha", "Middle"), projection.sortedEntries(entries, TrackSort.EntryDate, null, SortDirection.Ascending).map(projection::primaryText))
        assertEquals(listOf("Middle", "Alpha", "Zulu"), projection.sortedEntries(entries, TrackSort.EntryDate, null, SortDirection.Descending).map(projection::primaryText))
        assertEquals(listOf("Alpha", "Middle", "Zulu"), projection.sortedEntries(entries, TrackSort.Identity, title, SortDirection.Ascending).map(projection::primaryText))
        assertEquals(listOf("Zulu", "Middle", "Alpha"), projection.sortedEntries(entries, TrackSort.Identity, title, SortDirection.Descending).map(projection::primaryText))
        assertEquals(listOf("Alpha", "Zulu", "Middle"), projection.sortedEntries(entries, TrackSort.Identity, notes, SortDirection.Ascending).map(projection::primaryText))
        assertEquals(listOf("Zulu", "Alpha", "Middle"), projection.sortedEntries(entries, TrackSort.Identity, notes, SortDirection.Descending).map(projection::primaryText))
        assertEquals(listOf("Zulu", "Alpha", "Middle"), projection.sortedEntries(entries, TrackSort.Identity, score, SortDirection.Ascending).map(projection::primaryText))
        assertEquals(listOf("Alpha", "Zulu", "Middle"), projection.sortedEntries(entries, TrackSort.Identity, score, SortDirection.Descending).map(projection::primaryText))
    }

    @Test fun exerciseLibrarySortsHonorDirectionAndKeepNeverUsedExercisesLast() {
        val exercises = listOf(
            exercise(1, "Zulu", favorite = false),
            exercise(2, "Bravo", favorite = true),
            exercise(3, "Alpha", favorite = false),
        )
        val lastUsed = mapOf(1L to 10L, 2L to 20L)

        assertEquals(listOf("Alpha", "Bravo", "Zulu"), exercises.sortedForLibrary(ExerciseLibrarySort.Name, SortDirection.Ascending).map(Exercise::name))
        assertEquals(listOf("Zulu", "Bravo", "Alpha"), exercises.sortedForLibrary(ExerciseLibrarySort.Name, SortDirection.Descending).map(Exercise::name))
        assertEquals(listOf("Zulu", "Bravo", "Alpha"), exercises.sortedForLibrary(ExerciseLibrarySort.RecentlyUsed, SortDirection.Ascending, lastUsed).map(Exercise::name))
        assertEquals(listOf("Bravo", "Zulu", "Alpha"), exercises.sortedForLibrary(ExerciseLibrarySort.RecentlyUsed, SortDirection.Descending, lastUsed).map(Exercise::name))
        assertEquals("Bravo", exercises.sortedForLibrary(ExerciseLibrarySort.FavoritesFirst, SortDirection.Descending).first().name)
    }

    private fun scheduledTask(id: Long, title: String, date: LocalDate?, priority: TaskPriority) = ScheduledTask(
        task = WhipTask(
            id = id,
            title = title,
            notes = "",
            scheduleKind = if (date == null) ScheduleKind.Anytime else ScheduleKind.Once,
            date = date,
            recurrence = null,
            timeMinutes = null,
            reminderEnabled = false,
            archived = false,
            completedAtMillis = null,
            createdAtMillis = id,
            updatedAtMillis = id,
            priority = priority,
        ),
        originalDate = date,
        scheduledDate = date,
    )

    private fun exercise(id: Long, name: String, favorite: Boolean) = Exercise(
        id = id,
        uuid = "exercise-$id",
        name = name,
        trackingType = ExerciseTrackingType.RepsOnly,
        notes = "",
        equipment = "",
        primaryMuscles = "",
        secondaryMuscles = "",
        weightUnitId = "kilogram",
        weightIncrement = 1.0,
        repetitionIncrement = 1,
        defaultRestSeconds = null,
        defaultGraphMetric = "Repetitions",
        oneRepMaxFormula = EstimatedOneRepMaxFormula.Epley,
        barWeightKg = null,
        availablePlatesKg = emptyList(),
        includeInVolume = false,
        includeInPersonalRecords = false,
        bodyweightLoadPolicy = BodyweightLoadPolicy.ExternalWeightOnly,
        effectiveBodyweightPercent = 100.0,
        showRpe = null,
        showRir = null,
        showTempo = null,
        favorite = favorite,
        position = id.toInt(),
        archived = false,
        createdAtMillis = id,
        updatedAtMillis = id,
    )

    private fun trackProjection(): TrackProjection {
        val title = TrackField(1, "title", 1, "Title", TrackFieldType.ShortText, 0, true, true, false, null, null, 0, null, null, "", "", 1, 1)
        val notes = TrackField(2, "notes", 1, "Notes", TrackFieldType.LongText, 1, false, false, false, null, null, 0, null, null, "", "", 1, 1)
        val score = TrackField(3, "score", 1, "Score", TrackFieldType.Scale, 2, false, false, true, null, null, 0, 1, 5, "", "", 1, 1)
        fun entry(id: Long, name: String, date: LocalDate, notesText: String, rating: Double?) = TrackEntryProjection(
            entry = TrackEntry(id, "entry-$id", 1, date, createdAtMillis = id, updatedAtMillis = id),
            values = buildMap {
                put(title.id, TrackFieldValue(id * 10, "title-$id", id, title.id, textValue = name, createdAtMillis = id, updatedAtMillis = id))
                put(notes.id, TrackFieldValue(id * 10 + 1, "notes-$id", id, notes.id, textValue = notesText, createdAtMillis = id, updatedAtMillis = id))
                rating?.let { put(score.id, TrackFieldValue(id * 10 + 2, "score-$id", id, score.id, scaleValue = it, createdAtMillis = id, updatedAtMillis = id)) }
            },
        )
        return TrackProjection(
            track = Track(1, "track", "Films", "", "🎬", "main", "Main", emptyList(), false, false, 0, 1, 1),
            fields = listOf(title, notes, score),
            options = emptyList(),
            entries = listOf(
                entry(1, "Zulu", LocalDate.of(2026, 8, 21), "Beta notes", 2.0),
                entry(2, "Alpha", LocalDate.of(2026, 8, 22), "alpha notes", 4.0),
                entry(3, "Middle", LocalDate.of(2026, 8, 23), "   ", null),
            ),
        )
    }
}
