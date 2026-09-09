package com.whip.app

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.core.WhipClock
import com.whip.app.core.WhipIdGenerator
import com.whip.app.data.RoomAreaRepository
import com.whip.app.data.RoomTrackRepository
import com.whip.app.data.WhipDatabase
import com.whip.app.domain.TrackChoiceOptionDraft
import com.whip.app.domain.TrackDraft
import com.whip.app.domain.TrackEntryDraft
import com.whip.app.domain.TrackFieldDraft
import com.whip.app.domain.TrackFieldType
import com.whip.app.domain.TrackProjection
import com.whip.app.domain.TrackValueDraft
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TrackProjectionConsistencyTest {
    private lateinit var database: WhipDatabase
    private lateinit var tracks: RoomTrackRepository

    @Before fun setUp() = runBlocking {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), WhipDatabase::class.java)
            .addCallback(WhipDatabase.integrityGuardCallback)
            .build()
        val sequence = AtomicInteger()
        val ids = object : WhipIdGenerator { override fun nextId() = "projection-${sequence.incrementAndGet()}" }
        RoomAreaRepository(database, TestClock, ids).ensureDefaultArea()
        tracks = RoomTrackRepository(database, TestClock, ids)
    }

    @After fun tearDown() = database.close()

    @Test fun definitionEmissionsKeepTrackFieldsAndChoicesInTheSameRevision() = runBlocking {
        val updates = Channel<List<TrackProjection>>(Channel.UNLIMITED)
        val observer = launch { tracks.projections.collect { updates.send(it) } }
        try {
            assertEquals(emptyList<TrackProjection>(), withTimeout(10_000) { updates.receive() })
            val id = tracks.create(definition(0))
            updates.awaitCoherent { it.singleOrNull()?.track?.id == id }
            val writer = launch {
                for (revision in 1..8) {
                    val current = requireNotNull(tracks.projection(id))
                    tracks.update(id, definition(revision, current), requireNotNull(tracks.definitionBoundary(id)))
                }
            }
            updates.awaitCoherent { it.singleOrNull()?.track?.name == "Reading 8" }
            writer.join()
        } finally {
            observer.cancelAndJoin()
            updates.close()
        }
    }

    @Test fun entryEmissionsAlwaysIncludeTheirCommittedRequiredValuesAndChoices() = runBlocking {
        val id = tracks.create(definition(0))
        val definition = requireNotNull(tracks.projection(id))
        val title = definition.primaryField
        val genre = definition.fields.single { it.type == TrackFieldType.SingleChoice }
        val history = definition.optionsFor(genre.id).first()
        val updates = Channel<List<TrackProjection>>(Channel.UNLIMITED)
        val observer = launch { tracks.projections.collect { updates.send(it) } }
        try {
            updates.awaitCoherent { it.singleOrNull()?.track?.id == id }
            for (index in 1..8) {
                val entryId = tracks.addEntry(id, TrackEntryDraft(TestClock.today(), mapOf(
                    title.uuid to TrackValueDraft(textValue = "Book $index"),
                    genre.uuid to TrackValueDraft(choiceOptionUuid = history.uuid),
                )))
                val projection = updates.awaitCoherent { rows ->
                    rows.singleOrNull()?.entries?.any { it.entry.id == entryId } == true
                }.single()
                assertEquals("Book $index", projection.primaryText(projection.entries.single { it.entry.id == entryId }))
                assertEquals(index, projection.entries.size)
                assertEquals((index downTo 1).map { "Book $it" }, projection.entries.map(projection::primaryText))
            }
        } finally {
            observer.cancelAndJoin()
            updates.close()
        }
    }

    private suspend fun Channel<List<TrackProjection>>.awaitCoherent(
        predicate: (List<TrackProjection>) -> Boolean,
    ): List<TrackProjection> = withTimeout(10_000) {
        var rows: List<TrackProjection>
        do {
            rows = receive()
            rows.forEach { projection ->
                val revision = projection.track.name.substringAfterLast(' ')
                assertEquals("Fields must match the emitted Track revision", listOf("Title $revision", "Genre $revision"),
                    projection.fields.map { it.name })
                val genre = projection.fields.single { it.type == TrackFieldType.SingleChoice }
                assertEquals("Choices must match the emitted definition revision", listOf("History $revision", "Fiction $revision"),
                    projection.optionsFor(genre.id).map { it.label })
                projection.entries.forEach { entry ->
                    assertNotNull("Published Entry ${entry.entry.id} needs its required Title", entry.value(projection.primaryField.id)?.textValue)
                    val choiceId = entry.value(genre.id)?.choiceOptionId
                    assertNotNull("Published Entry ${entry.entry.id} needs its required Genre", choiceId)
                    assertNotNull("Published Choice must resolve in the same projection", projection.options.singleOrNull { it.id == choiceId })
                }
            }
        } while (!predicate(rows))
        rows
    }

    private fun definition(revision: Int, previous: TrackProjection? = null): TrackDraft {
        val title = previous?.fields?.get(0)
        val genre = previous?.fields?.get(1)
        return TrackDraft(name = "Reading $revision", fields = listOf(
            TrackFieldDraft("Title $revision", TrackFieldType.ShortText, required = true, primary = true,
                uuid = title?.uuid, id = title?.id),
            TrackFieldDraft("Genre $revision", TrackFieldType.SingleChoice, required = true,
                uuid = genre?.uuid, id = genre?.id,
                options = listOf("History", "Fiction").mapIndexed { index, label ->
                    val option = genre?.let { previous.optionsFor(it.id)[index] }
                    TrackChoiceOptionDraft("$label $revision", uuid = option?.uuid, id = option?.id)
                }),
        ))
    }

    private object TestClock : WhipClock {
        override fun now(): Instant = Instant.parse("2026-09-09T12:00:00Z")
        override fun zoneId(): ZoneId = ZoneId.of("UTC")
        override fun today(zoneId: ZoneId): LocalDate = LocalDate.of(2026, 9, 9)
    }
}
