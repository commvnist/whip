package com.whip.app.data

import androidx.room.withTransaction
import com.whip.app.core.WhipClock
import com.whip.app.core.WhipIdGenerator
import com.whip.app.domain.Area
import java.util.Locale
import kotlin.math.sign
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface AreaRepository {
    val areas: Flow<List<Area>>

    suspend fun ensureDefaultArea(): String
    suspend fun create(name: String, colorArgb: Long? = null): String
    suspend fun rename(id: String, name: String)
    suspend fun merge(sourceId: String, targetId: String)
    suspend fun moveAssignments(sourceId: String?, targetId: String)
    suspend fun deletePermanently(id: String, replacementAreaId: String? = null)
    suspend fun setColor(id: String, colorArgb: Long?)
    suspend fun setArchived(id: String, archived: Boolean)
    suspend fun move(id: String, direction: Int)
    suspend fun resolve(areaId: String?, requestedName: String = ""): AreaSelection
}

data class AreaSelection(val id: String?, val name: String)

class RoomAreaRepository(
    private val database: WhipDatabase,
    private val clock: WhipClock,
    private val ids: WhipIdGenerator,
) : AreaRepository {
    private val dao = database.measurementDao()

    override val areas: Flow<List<Area>> = dao.observeAreas().map { rows -> rows.map(AreaEntity::toArea) }

    override suspend fun ensureDefaultArea(): String = database.withTransaction {
        val area = ensureActiveArea()
        dao.reassignAllTaskAreas(null, area.id, area.name)
        dao.reassignAllHabitAreas(null, area.id, area.name)
        dao.reassignAllGoalAreas(null, area.id, area.name)
        database.trackDao().reassignAllAreas(null, area.id, area.name, clock.now().toEpochMilli())
        area.id
    }

    override suspend fun create(name: String, colorArgb: Long?): String = database.withTransaction {
        val displayName = normalizeAreaName(name)
        val key = areaNameKey(displayName)
        dao.getAreaByNameKey(key)?.let { existing ->
            if (existing.archived) {
                dao.updateArea(existing.copy(archived = false, updatedAtMillis = clock.now().toEpochMilli()))
            }
            return@withTransaction existing.id
        }
        val now = clock.now().toEpochMilli()
        val id = ids.nextId()
        dao.upsertArea(
            AreaEntity(
                id = id,
                name = displayName,
                nameKey = key,
                colorArgb = colorArgb,
                position = dao.nextAreaPosition(),
                archived = false,
                createdAtMillis = now,
                updatedAtMillis = now,
            ),
        )
        id
    }

    override suspend fun rename(id: String, name: String) = database.withTransaction {
        val current = requireNotNull(dao.getArea(id)) { "Area no longer exists" }
        val displayName = normalizeAreaName(name)
        val key = areaNameKey(displayName)
        val target = dao.getAreaByNameKey(key)
        if (target != null && target.id != id) {
            error("An Area named “${target.name}” already exists. Use Merge instead.")
        }
        dao.updateArea(
            current.copy(name = displayName, nameKey = key, updatedAtMillis = clock.now().toEpochMilli()),
        )
        updateDisplayNames(id, displayName)
    }

    override suspend fun merge(sourceId: String, targetId: String) = database.withTransaction {
        require(sourceId != targetId) { "Choose two different Areas" }
        val source = requireNotNull(dao.getArea(sourceId)) { "Source Area no longer exists" }
        val target = requireNotNull(dao.getArea(targetId)) { "Destination Area no longer exists" }
        require(!target.archived) { "Choose an active destination Area" }
        mergeRows(source, target)
    }

    override suspend fun moveAssignments(sourceId: String?, targetId: String) = database.withTransaction {
        require(sourceId != targetId) { "Choose a different destination" }
        sourceId?.let { requireNotNull(dao.getArea(it)) { "Source Area no longer exists" } }
        val target = requireNotNull(dao.getArea(targetId)) { "Destination Area no longer exists" }
        require(!target.archived) { "Choose an active destination Area" }
        val targetName = target.name
        dao.reassignAllTaskAreas(sourceId, targetId, targetName)
        dao.reassignAllHabitAreas(sourceId, targetId, targetName)
        dao.reassignAllGoalAreas(sourceId, targetId, targetName)
        database.trackDao().reassignAllAreas(sourceId, targetId, targetName, clock.now().toEpochMilli())
        Unit
    }

    override suspend fun deletePermanently(id: String, replacementAreaId: String?) = database.withTransaction {
        val area = requireNotNull(dao.getArea(id)) { "Area no longer exists" }
        val active = dao.observeAreasSnapshot().filterNot(AreaEntity::archived)
        if (!area.archived) {
            require(active.any { it.id != id }) { "Create another Area before deleting ${area.name}." }
        }
        val assignmentCount = dao.countAreaAssignments(id) + database.trackDao().countAreaAssignments(id)
        val replacement = replacementAreaId?.let { targetId ->
            require(targetId != id) { "Choose a different destination" }
            requireNotNull(dao.getArea(targetId)) { "Destination Area no longer exists" }
                .also { require(!it.archived) { "Choose an active destination Area" } }
        }
        require(assignmentCount == 0 || replacement != null) {
            "Choose another Area for ${area.name}'s items before deleting it."
        }
        replacement?.let { target ->
            dao.reassignAllTaskAreas(id, target.id, target.name)
            dao.reassignAllHabitAreas(id, target.id, target.name)
            dao.reassignAllGoalAreas(id, target.id, target.name)
            database.trackDao().reassignAllAreas(id, target.id, target.name, clock.now().toEpochMilli())
        }
        check(dao.deleteArea(id) == 1) { "Area could not be deleted" }
    }

    override suspend fun setColor(id: String, colorArgb: Long?) {
        val current = requireNotNull(dao.getArea(id)) { "Area no longer exists" }
        dao.updateArea(current.copy(colorArgb = colorArgb, updatedAtMillis = clock.now().toEpochMilli()))
    }

    override suspend fun setArchived(id: String, archived: Boolean) = database.withTransaction {
        val current = requireNotNull(dao.getArea(id)) { "Area no longer exists" }
        if (archived && !current.archived) {
            val active = dao.observeAreasSnapshot().count { !it.archived }
            require(active > 1) { "Create another Area before archiving ${current.name}." }
        }
        dao.updateArea(current.copy(archived = archived, updatedAtMillis = clock.now().toEpochMilli()))
    }

    override suspend fun move(id: String, direction: Int) = database.withTransaction {
        val ordered = dao.observeAreasSnapshot().toMutableList()
        val from = ordered.indexOfFirst { it.id == id }
        if (from < 0 || direction == 0) return@withTransaction
        val to = (from + direction.sign).coerceIn(0, ordered.lastIndex)
        if (from != to) java.util.Collections.swap(ordered, from, to)
        val now = clock.now().toEpochMilli()
        ordered.forEachIndexed { index, area ->
            if (area.position != index) dao.updateArea(area.copy(position = index, updatedAtMillis = now))
        }
    }

    override suspend fun resolve(areaId: String?, requestedName: String): AreaSelection = database.withTransaction {
        if (!areaId.isNullOrBlank()) {
            val area = requireNotNull(dao.getArea(areaId)) { "Selected Area no longer exists" }
            return@withTransaction AreaSelection(area.id, area.name)
        }
        if (requestedName.isBlank()) {
            val area = ensureActiveArea()
            return@withTransaction AreaSelection(area.id, area.name)
        }
        val id = create(requestedName)
        val area = requireNotNull(dao.getArea(id))
        AreaSelection(area.id, area.name)
    }

    private suspend fun mergeRows(source: AreaEntity, target: AreaEntity) {
        dao.moveTaskAreaReferences(source.id, target.id, target.name)
        dao.moveHabitAreaReferences(source.id, target.id, target.name)
        dao.moveGoalAreaReferences(source.id, target.id, target.name)
        database.trackDao().moveAreaReferences(source.id, target.id, target.name, clock.now().toEpochMilli())
        dao.deleteArea(source.id)
    }

    private suspend fun updateDisplayNames(id: String, name: String) {
        dao.updateTaskAreaNames(id, name)
        dao.updateHabitAreaNames(id, name)
        dao.updateGoalAreaNames(id, name)
        database.trackDao().updateAreaNames(id, name, clock.now().toEpochMilli())
    }

    private suspend fun ensureActiveArea(): AreaEntity {
        dao.observeAreasSnapshot().firstOrNull { !it.archived }?.let { return it }
        val now = clock.now().toEpochMilli()
        dao.getAreaByNameKey(areaNameKey(DEFAULT_AREA_NAME))?.let { existing ->
            val restored = existing.copy(archived = false, updatedAtMillis = now)
            dao.updateArea(restored)
            return restored
        }
        val id = DEFAULT_AREA_ID.takeIf { dao.getArea(it) == null } ?: ids.nextId()
        val area = AreaEntity(
            id = id,
            name = DEFAULT_AREA_NAME,
            nameKey = areaNameKey(DEFAULT_AREA_NAME),
            colorArgb = null,
            position = dao.nextAreaPosition(),
            archived = false,
            createdAtMillis = now,
            updatedAtMillis = now,
        )
        dao.upsertArea(area)
        return area
    }
}

internal const val DEFAULT_AREA_NAME = "Main"
internal const val DEFAULT_AREA_ID = "whip-default-main"

internal fun normalizeAreaName(value: String): String = value.trim().replace(Regex("\\s+"), " ")
    .also {
        require(it.isNotBlank()) { "Area name is required" }
        require(it.length <= 40) { "Area names can be at most 40 characters" }
    }

internal fun areaNameKey(value: String): String = normalizeAreaName(value).lowercase(Locale.ROOT)

internal fun AreaEntity.toArea() = Area(
    id = id,
    name = name,
    colorArgb = colorArgb,
    position = position,
    archived = archived,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis,
)
