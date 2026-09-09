package com.whip.app.ui

import com.whip.app.domain.TrackEntryBoundary
import com.whip.app.domain.TrackEntryEditSnapshot
import java.io.Serializable
import java.time.LocalDate

/** The original deletion authority and review text, independent of authored-value size. */
internal data class TrackEntryDeleteCandidate private constructor(
    val sessionId: Long,
    val openingDataGeneration: Long,
    val boundary: TrackEntryBoundary,
    val displayName: String,
    val trackName: String,
    val entryDate: LocalDate,
    val populatedValueCount: Int,
) : Serializable {
    companion object {
        fun from(snapshot: TrackEntryEditSnapshot, sessionId: Long, openingDataGeneration: Long) =
            TrackEntryDeleteCandidate(
                sessionId = sessionId,
                openingDataGeneration = openingDataGeneration,
                // Deletion validates identity and canonical form/Entry tokens. The arrays
                // support editing/normalization; their complete meaning is already hashed.
                boundary = snapshot.boundary.copy(
                    formBoundary = snapshot.boundary.formBoundary.copy(
                        fieldContracts = emptyList(), choiceContracts = emptyList(), unitContracts = emptyList(),
                    ),
                    enteredUnitContracts = emptyList(),
                ),
                displayName = reviewExcerpt(snapshot.displayName),
                trackName = reviewExcerpt(snapshot.form.track.name),
                entryDate = snapshot.draft.entryDate,
                populatedValueCount = snapshot.populatedValueCount,
            )

        private fun reviewExcerpt(value: String): String =
            if (value.codePointCount(0, value.length) <= 160) value
            else value.substring(0, value.offsetByCodePoints(0, 160)) + "…"
    }
}
