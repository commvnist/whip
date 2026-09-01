package com.whip.app

import com.whip.app.data.RoomTrackRepository
import com.whip.app.domain.DeletedTrackEntry
import com.whip.app.domain.TrackEntryDraft

/** Concise adapters for older repository tests that are not exercising mutation ownership. */
internal suspend fun RoomTrackRepository.addEntry(trackId: Long, draft: TrackEntryDraft): Long {
    val preparation = requireNotNull(prepareEntryCreate(trackId))
    return addEntry(preparation.request, draft).entryId
}

internal suspend fun RoomTrackRepository.updateEntry(entryId: Long, draft: TrackEntryDraft) {
    updateEntry(requireNotNull(prepareEntryEdit(entryId)).boundary, draft)
}

internal suspend fun RoomTrackRepository.deleteEntry(entryId: Long): DeletedTrackEntry? =
    prepareEntryEdit(entryId)?.let { deleteEntry(it.boundary).deletedEntry }

internal suspend fun RoomTrackRepository.restoreEntryId(deleted: DeletedTrackEntry): Long =
    restoreEntry(deleted).entryId
