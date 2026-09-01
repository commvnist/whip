package com.whip.app.data

import androidx.room.withTransaction
import com.whip.app.core.WhipClock
import com.whip.app.core.WhipIdGenerator
import com.whip.app.domain.BuiltInUnits
import com.whip.app.domain.Contribution
import com.whip.app.domain.EstimatedOneRepMaxFormula
import com.whip.app.domain.HabitLogStatus
import com.whip.app.domain.GoalStatus
import com.whip.app.domain.LinkBackfillPreview
import com.whip.app.domain.LinkKind
import com.whip.app.domain.LinkRule
import com.whip.app.domain.LinkRuleDraft
import com.whip.app.domain.LinkSourceMetric
import com.whip.app.domain.LinkSourceType
import com.whip.app.domain.LinkValueMode
import com.whip.app.domain.MetricEntryStatus
import com.whip.app.domain.MetricSourceType
import com.whip.app.domain.TargetComparison
import com.whip.app.domain.TriggerOccurrence
import com.whip.app.domain.TriggerOutcome
import com.whip.app.domain.TriggerRule
import com.whip.app.domain.TriggerRuleDraft
import com.whip.app.domain.TriggerTargetType
import com.whip.app.domain.TriggerAction
import com.whip.app.domain.TriggerFieldMapping
import com.whip.app.domain.TriggerSourceProperty
import com.whip.app.domain.TrackAggregation
import com.whip.app.domain.TrackCondition
import com.whip.app.domain.TrackConditionMode
import com.whip.app.domain.TrackConditionOperator
import com.whip.app.domain.TrackEntryProjection
import com.whip.app.domain.TrackFieldType
import com.whip.app.domain.TrackProjection
import com.whip.app.domain.TrackValueDraft
import com.whip.app.domain.TRACK_ENTRY_DATE_CONDITION_UUID
import com.whip.app.domain.matchingEntries
import com.whip.app.domain.UnitDimension
import com.whip.app.domain.UnitDefinition
import com.whip.app.domain.estimatedOneRepMax
import com.whip.app.domain.normalizeTrackScaleValue
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import org.json.JSONObject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine

interface LinkRepository {
    val rules: Flow<List<LinkRule>>
    val contributions: Flow<List<Contribution>>
    val triggerRules: Flow<List<TriggerRule>>
    val triggerOccurrences: Flow<List<TriggerOccurrence>>

    suspend fun createRule(draft: LinkRuleDraft, commitBackfill: Boolean = false): Long
    suspend fun updateRule(id: Long, draft: LinkRuleDraft)
    suspend fun deleteRule(id: Long)
    suspend fun setRuleEnabled(id: Long, enabled: Boolean)
    suspend fun previewBackfill(draft: LinkRuleDraft): LinkBackfillPreview
    suspend fun rebuildRule(id: Long)
    suspend fun rebuildAll()
    suspend fun rebuildSources(sourceTypes: Set<LinkSourceType>)
    suspend fun setContributionExcluded(id: Long, excluded: Boolean)
    suspend fun setContributionOverride(id: Long, canonicalValue: Double?)

    suspend fun createTrigger(draft: TriggerRuleDraft): Long
    suspend fun updateTrigger(id: Long, draft: TriggerRuleDraft)
    suspend fun deleteTrigger(id: Long)
    suspend fun dismissTriggerOccurrence(id: Long)
    suspend fun remindTriggerOccurrence(id: Long, at: Instant)
    suspend fun trackPromptDraft(id: Long): com.whip.app.domain.TrackEntryDraft
    suspend fun fulfillTrackPrompt(id: Long, draft: com.whip.app.domain.TrackEntryDraft): Long
}

class RoomLinkRepository(
    private val database: WhipDatabase,
    private val measurementRepository: MeasurementRepository,
    private val clock: WhipClock,
    private val ids: WhipIdGenerator,
) : LinkRepository {
    private val dao = database.linkDao()
    private val trackAutomationSchema = combine(
        database.trackDao().observeFields(),
        database.trackDao().observeOptions(),
    ) { fields, options -> TrackAutomationSchema(fields, options) }
    private val triggerAutomationDetails = combine(
        dao.observeTriggerConditions(),
        dao.observeTriggerConditionChoices(),
        dao.observeTriggerMappings(),
    ) { conditions, choices, mappings -> TriggerAutomationDetails(conditions, choices, mappings) }

    override val rules = combine(dao.observeRules(), dao.observeRuleConditions(), dao.observeLinkConditionChoices(), trackAutomationSchema) { list, conditions, choices, schema ->
        val fieldUuidById = schema.fields.associate { it.id to it.uuid }
        val optionUuidById = schema.options.associate { it.id to it.uuid }
        list.map { rule ->
            rule.toDomain(conditions.filter { it.linkRuleId == rule.id }.mapNotNull { condition ->
                condition.toDomain(fieldUuidById, choices.filter { it.conditionId == condition.id }.mapNotNull { optionUuidById[it.optionId] }.toSet())
            })
        }
    }
    override val contributions = dao.observeContributions().map { list -> list.map(ContributionEntity::toDomain) }
    override val triggerRules = combine(
        dao.observeTriggerRules(),
        triggerAutomationDetails,
        trackAutomationSchema,
    ) { list, details, schema ->
        val fieldUuidById = schema.fields.associate { it.id to it.uuid }
        val optionUuidById = schema.options.associate { it.id to it.uuid }
        list.map { rule ->
            rule.toDomain(
                details.conditions.filter { it.triggerRuleId == rule.id }.mapNotNull { condition ->
                    condition.toDomain(fieldUuidById, details.choices.filter { it.conditionId == condition.id }.mapNotNull { optionUuidById[it.optionId] }.toSet())
                },
                details.mappings.filter { it.triggerRuleId == rule.id }.map { it.toDomain(optionUuidById) },
            )
        }
    }
    override val triggerOccurrences = dao.observeTriggerOccurrences().map { list -> list.map(TriggerOccurrenceEntity::toDomain) }

    override suspend fun createRule(draft: LinkRuleDraft, commitBackfill: Boolean): Long {
        validateRule(draft)
        val now = clock.now().toEpochMilli()
        val entity = draft.toEntity(ids.nextId(), now).copy(
            retroactiveFromEpochDay = draft.retroactiveFrom?.toEpochDay()
                .takeIf { commitBackfill },
            enabled = false,
        )
        return database.withTransaction {
            val id = dao.insertRule(entity)
            syncRuleConditions(id, draft.conditions)
            id
        }
    }

    override suspend fun updateRule(id: Long, draft: LinkRuleDraft) {
        validateRule(draft, excludingRuleId = id)
        val current = dao.getRule(id) ?: error("Link no longer exists")
        database.withTransaction {
            dao.updateRule(
                draft.toEntity(current.uuid, current.createdAtMillis).copy(
                    id = current.id,
                    retroactiveFromEpochDay = draft.retroactiveFrom?.toEpochDay(),
                    enabled = false,
                    updatedAtMillis = clock.now().toEpochMilli(),
                ),
            )
            syncRuleConditions(id, draft.conditions)
        }
        rebuildRule(id)
    }

    override suspend fun deleteRule(id: Long) = database.withTransaction {
        // Generated measurements are now ordinary historical Goal records. Deleting dormant
        // compatibility metadata must not retract them.
        dao.deleteRule(id)
    }

    override suspend fun setRuleEnabled(id: Long, enabled: Boolean) {
        val current = dao.getRule(id) ?: return
        dao.updateRule(current.copy(enabled = false, updatedAtMillis = clock.now().toEpochMilli()))
    }

    override suspend fun previewBackfill(draft: LinkRuleDraft): LinkBackfillPreview {
        validateRule(draft, allowTrackGoalAggregationChange = true)
        val temporary = draft.toEntity("preview", clock.now().toEpochMilli()).copy(
            id = -1,
            retroactiveFromEpochDay = draft.retroactiveFrom?.toEpochDay() ?: Long.MIN_VALUE,
        )
        val target = database.goalDao().getGoal(draft.targetGoalId) ?: error("Goal no longer exists")
        val rawTrackProjection = draft.sourceEntityId?.takeIf { draft.sourceType == LinkSourceType.Track }?.let { trackProjection(it) }
        val allInHistory = rawTrackProjection?.entries.orEmpty().filter { entry ->
            draft.retroactiveFrom == null || !entry.entry.entryDate.isBefore(draft.retroactiveFrom)
        }
        val matchingTrackEntries = rawTrackProjection?.matchingEntries(draft.conditions, draft.conditionMode).orEmpty()
            .filter { entry -> draft.retroactiveFrom == null || !entry.entry.entryDate.isBefore(draft.retroactiveFrom) }
        val untransformed = if (draft.sourceType == LinkSourceType.Track) {
            trackEvents(temporary, draft.conditions, draft.conditionMode)
                .filter { draft.retroactiveFrom == null || !it.date.isBefore(draft.retroactiveFrom) }
        } else sourceEvents(temporary)
        val events = untransformed.filter { eligibleValue(temporary, it, target) != null || temporary.kind == LinkKind.Context.name }
        val values = events.mapNotNull { eligibleValue(temporary, it, target) }
        val scanned = rawTrackProjection?.let { allInHistory.size } ?: untransformed.size
        val conditionSkipped = if (rawTrackProjection == null) 0 else (allInHistory.size - matchingTrackEntries.size).coerceAtLeast(0)
        val blankSource = if (rawTrackProjection == null) 0 else (matchingTrackEntries.size - untransformed.size).coerceAtLeast(0)
        val transformSkipped = (untransformed.size - events.size).coerceAtLeast(0)
        return LinkBackfillPreview(
            scannedEventCount = scanned,
            eligibleEventCount = events.size,
            skippedEventCount = (scanned - events.size).coerceAtLeast(0),
            skippedReasons = buildMap {
                if (conditionSkipped > 0) put("Did not match conditions", conditionSkipped)
                if (blankSource > 0) put("Blank source Field", blankSource)
                if (transformSkipped > 0) put("Could not convert or transform", transformSkipped)
            },
            contributionCount = events.size,
            totalCanonicalValue = values.sum(),
            firstDate = events.minOfOrNull(SourceEvent::date),
            lastDate = events.maxOfOrNull(SourceEvent::date),
            unitExplanation = if (draft.valueMode == LinkValueMode.FixedValue) {
                "Each eligible event uses ${draft.fixedValue} ${target.unitId}."
            } else "Source values are converted to ${target.unitId} before the multiplier and offset.",
            targetImpact = "${events.size} auditable contribution${if (events.size == 1) "" else "s"} would be created.",
        )
    }

    // Automation execution is retired. These compatibility entry points remain while older
    // ViewModels and deletion code are removed, but must never mutate preserved history.
    override suspend fun rebuildRule(id: Long) = Unit

    override suspend fun rebuildAll() = Unit

    override suspend fun rebuildSources(sourceTypes: Set<LinkSourceType>) = Unit

    override suspend fun setContributionExcluded(id: Long, excluded: Boolean) {
        val now = clock.now().toEpochMilli()
        dao.setContributionExcluded(id, excluded, now)
        val contribution = dao.observeContributionsSnapshot().firstOrNull { it.id == id } ?: return
        rebuildRule(contribution.linkRuleId)
    }

    override suspend fun setContributionOverride(id: Long, canonicalValue: Double?) {
        val now = clock.now().toEpochMilli()
        dao.setContributionOverride(id, canonicalValue, now)
        val contribution = dao.observeContributionsSnapshot().firstOrNull { it.id == id } ?: return
        rebuildRule(contribution.linkRuleId)
    }

    override suspend fun createTrigger(draft: TriggerRuleDraft): Long {
        validateTrigger(draft)
        val now = clock.now().toEpochMilli()
        val id = database.withTransaction {
            dao.insertTriggerRule(
                draft.toEntity(ids.nextId(), now).copy(enabled = false, notificationEnabled = false),
            ).also { syncTriggerDetails(it, draft) }
        }
        rebuildAll()
        return id
    }

    override suspend fun updateTrigger(id: Long, draft: TriggerRuleDraft) {
        validateTrigger(draft, id)
        val current = dao.getTriggerRule(id) ?: error("Trigger no longer exists")
        database.withTransaction {
            dao.updateTriggerRule(
                draft.toEntity(current.uuid, current.createdAtMillis).copy(
                    id = id,
                    enabled = false,
                    notificationEnabled = false,
                    updatedAtMillis = clock.now().toEpochMilli(),
                ),
            )
            syncTriggerDetails(id, draft)
        }
        rebuildAll()
    }

    override suspend fun deleteTrigger(id: Long) {
        database.withTransaction {
            dao.getTriggerRule(id) ?: return@withTransaction
            dao.getTriggerOccurrences(id).forEach { occurrence ->
                database.trackDao().clearSourceOccurrence(occurrence.id)
            }
            dao.deleteTriggerRule(id)
        }
    }

    override suspend fun dismissTriggerOccurrence(id: Long) =
        dao.dismissTriggerOccurrence(id, clock.now().toEpochMilli())

    override suspend fun remindTriggerOccurrence(id: Long, at: Instant) {
        require(at.isAfter(clock.now())) { "Choose a future reminder time" }
        check(dao.remindTriggerOccurrence(id, at.toEpochMilli()) == 1) { "Pending prompt no longer exists" }
    }

    override suspend fun trackPromptDraft(id: Long): com.whip.app.domain.TrackEntryDraft = database.withTransaction {
        val occurrence = dao.getTriggerOccurrence(id) ?: error("Pending prompt no longer exists")
        require(occurrence.dismissedAtMillis == null && occurrence.fulfilledEntryId == null) { "This prompt is no longer pending" }
        val rule = dao.getTriggerRule(occurrence.triggerRuleId) ?: error("Capture Automation no longer exists")
        require(rule.action == TriggerAction.PromptTrackEntry.name && rule.targetType == TriggerTargetType.Track.name) {
            "This prompt does not create a Track Entry"
        }
        val projection = trackProjection(rule.targetEntityId) ?: error("Target Track no longer exists")
        val snapshot = TriggerSnapshot.parse(occurrence.sourceSnapshot)
        val optionUuidById = projection.options.associate { it.id to it.uuid }
        val mappings = dao.getTriggerMappings(rule.id).map { it.toDomain(optionUuidById) }
        val values = mappings.mapNotNull { mapping ->
            val field = projection.fields.firstOrNull { it.id == mapping.targetFieldId } ?: return@mapNotNull null
            val value = mapping.valueFor(field, projection, snapshot, ::resolveUnit) ?: return@mapNotNull null
            field.uuid to value
        }.toMap()
        com.whip.app.domain.TrackEntryDraft(
            entryDate = snapshot.eventDate ?: clock.today(),
            values = values,
            sourceOccurrenceId = occurrence.id,
            sourceExplanation = snapshot.explanation,
        )
    }

    override suspend fun fulfillTrackPrompt(id: Long, draft: com.whip.app.domain.TrackEntryDraft): Long = database.withTransaction {
        val occurrence = dao.getTriggerOccurrence(id) ?: error("Pending prompt no longer exists")
        require(occurrence.fulfilledEntryId == null) { "This prompt has already been fulfilled" }
        val rule = dao.getTriggerRule(occurrence.triggerRuleId) ?: error("Capture Automation no longer exists")
        require(rule.action == TriggerAction.PromptTrackEntry.name && rule.targetType == TriggerTargetType.Track.name) {
            "This prompt does not create a Track Entry"
        }
        val entryId = RoomTrackRepository(database, clock, ids).addEntry(
            rule.targetEntityId,
            draft.copy(
                sourceOccurrenceId = occurrence.id,
                sourceExplanation = draft.sourceExplanation.ifBlank { TriggerSnapshot.parse(occurrence.sourceSnapshot).explanation },
            ),
        )
        check(dao.fulfillTriggerOccurrence(id, entryId, clock.now().toEpochMilli()) == 1)
        entryId
    }

    private suspend fun rebuildRuleLocked(id: Long) {
        val rule = dao.getRule(id) ?: return
        val existing = dao.getContributions(id).associateBy(ContributionEntity::sourceEventId)
        if (!rule.enabled) {
            existing.values.forEach { removeContribution(it) }
            updateLinkedMilestone(rule)
            return
        }
        val target = database.goalDao().getGoal(rule.targetGoalId) ?: return
        val events = sourceEvents(rule)
        val retained = mutableSetOf<String>()
        events.forEach { event ->
            val transformed = eligibleValue(rule, event, target)
            if (rule.kind == LinkKind.Contribution.name && transformed == null && rule.targetMilestoneId == null) return@forEach
            retained += event.id
            val old = existing[event.id]
            val uuid = old?.uuid ?: ids.nextId()
            val excluded = old?.excluded ?: false
            val override = old?.overrideValue
            val effectiveCanonical = override ?: transformed
            val currentMetricEntryId = old?.metricEntryId?.takeIf {
                database.measurementDao().getEntry(it) != null
            }
            val metricEntryId = if (
                rule.kind == LinkKind.Contribution.name &&
                rule.targetMilestoneId == null &&
                !excluded &&
                effectiveCanonical != null
            ) {
                val targetUnit = resolveUnit(target.unitId) ?: error("The linked goal unit no longer exists")
                measurementRepository.record(
                    metricId = target.metricId,
                    value = targetUnit.fromCanonical(effectiveCanonical),
                    unitId = target.unitId,
                    timestamp = event.timestamp,
                    localDate = event.date,
                    zoneId = clock.zoneId(),
                    sourceType = event.metricSourceType,
                    sourceId = uuid,
                    note = "Linked: ${event.explanation}",
                    existingEntryId = currentMetricEntryId,
                )
            } else {
                old?.metricEntryId?.let { measurementRepository.deleteEntry(it) }
                null
            }
            dao.upsertContribution(
                ContributionEntity(
                    id = old?.id ?: 0,
                    uuid = uuid,
                    linkRuleId = rule.id,
                    sourceEventId = event.id,
                    sourceType = event.type.name,
                    sourceEntityId = event.entityId,
                    targetGoalId = target.id,
                    metricEntryId = metricEntryId,
                    canonicalValue = transformed,
                    localEpochDay = event.date.toEpochDay(),
                    timestampMillis = event.timestamp.toEpochMilli(),
                    excluded = excluded,
                    overrideValue = override,
                    explanation = event.explanation,
                    createdAtMillis = old?.createdAtMillis ?: clock.now().toEpochMilli(),
                    updatedAtMillis = clock.now().toEpochMilli(),
                ),
            )
        }
        existing.values.filter { it.sourceEventId !in retained }.forEach { removeContribution(it) }
        updateLinkedMilestone(rule)
    }

    private suspend fun removeContribution(entity: ContributionEntity) {
        entity.metricEntryId?.let { measurementRepository.deleteEntry(it) }
        dao.deleteContribution(entity.id)
    }

    private suspend fun updateLinkedMilestone(rule: LinkRuleEntity) {
        val milestoneId = rule.targetMilestoneId ?: return
        val milestone = database.goalDao().getMilestone(milestoneId) ?: return
        val goal = database.goalDao().getGoal(milestone.goalId) ?: return
        if (goal.archived || goal.status != GoalStatus.Active.name) return
        val completed = dao.getContributions(rule.id).any { !it.excluded }
        if (milestone.completed == completed) return
        val now = clock.now().toEpochMilli()
        database.goalDao().updateMilestone(
            milestone.copy(
                completed = completed,
                completedAtMillis = now.takeIf { completed },
                updatedAtMillis = now,
            ),
        )
    }

    private suspend fun validateRule(
        draft: LinkRuleDraft,
        excludingRuleId: Long? = null,
        allowTrackGoalAggregationChange: Boolean = false,
    ) {
        require(draft.name.isNotBlank()) { "Link name is required" }
        require(draft.multiplier.isFinite() && draft.offset.isFinite()) { "Enter a finite transformation" }
        if (draft.valueMode == LinkValueMode.FixedValue) requireNotNull(draft.fixedValue) { "Enter a fixed value" }
        val target = database.goalDao().getGoal(draft.targetGoalId) ?: error("Goal no longer exists")
        require(target.type != "ElapsedSince") { "Elapsed-time Goals advance from time and cannot accept Progress Automations" }
        draft.targetMilestoneId?.let { milestoneId ->
            require(database.goalDao().getMilestone(milestoneId)?.goalId == target.id) { "Milestone does not belong to this goal" }
        }
        if (draft.kind == LinkKind.Contribution && draft.targetMilestoneId == null && draft.valueMode == LinkValueMode.SourceValue) {
            val sourceDimension = sourceDimension(draft.sourceType, draft.sourceEntityId, draft.sourceMetricId, draft.sourceMetric, draft.sourceFieldId)
            require(sourceDimension == UnitDimension.valueOf(target.dimension)) {
                "The source uses $sourceDimension but the goal uses ${target.dimension}"
            }
        }
        if (draft.sourceType == LinkSourceType.Metric) {
            val metricId = requireNotNull(draft.sourceMetricId) { "Choose a source metric" }
            require(metricId != target.metricId) { "A goal cannot contribute to itself" }
            val edges = dao.getRules().filter { it.id != excludingRuleId && it.enabled && it.sourceType == LinkSourceType.Metric.name }
                .mapNotNull { link ->
                    val goal = database.goalDao().getGoal(link.targetGoalId) ?: return@mapNotNull null
                    link.sourceMetricId?.let { it to goal.metricId }
                } + (metricId to target.metricId)
            require(!hasDirectedCycle(edges)) { "This link would create a circular goal dependency" }
        }
        if (draft.sourceType == LinkSourceType.Track) {
            val track = database.trackDao().getTrack(requireNotNull(draft.sourceEntityId) { "Choose a Track" })
                ?: error("Track no longer exists")
            require(!track.archived) { "Restore the Track before creating an Automation" }
            val aggregation = requireNotNull(draft.trackAggregation) { "Choose how Entries change the Goal" }
            val numericAggregation = aggregation in setOf(TrackAggregation.Sum, TrackAggregation.Average, TrackAggregation.Latest, TrackAggregation.Minimum, TrackAggregation.Maximum)
            val field = draft.sourceFieldId?.let { database.trackDao().getField(it) }
            if (numericAggregation) {
                require(field?.trackId == track.id && field.type in setOf(TrackFieldType.Number.name, TrackFieldType.Scale.name)) {
                    "Choose a Number or Scale Field from ${track.name}"
                }
            }
            require(draft.conditions.all { condition ->
                condition.fieldUuid == TRACK_ENTRY_DATE_CONDITION_UUID || database.trackDao().getFieldByUuid(condition.fieldUuid)?.trackId == track.id
            }) {
                "Every condition must use a Field from ${track.name}"
            }
            val requiredGoalAggregation = when (aggregation) {
                TrackAggregation.Sum -> com.whip.app.domain.GoalAggregation.Sum
                TrackAggregation.Average -> com.whip.app.domain.GoalAggregation.Average
                TrackAggregation.Latest -> com.whip.app.domain.GoalAggregation.Latest
                TrackAggregation.Minimum -> com.whip.app.domain.GoalAggregation.Minimum
                TrackAggregation.Maximum -> com.whip.app.domain.GoalAggregation.Maximum
                TrackAggregation.CountEntries, TrackAggregation.CountMatchingEntries, TrackAggregation.FixedAmount -> null
            }
            val aggregationCompatible = when (aggregation) {
                TrackAggregation.CountEntries, TrackAggregation.CountMatchingEntries -> target.aggregation in setOf(
                    com.whip.app.domain.GoalAggregation.Sum.name,
                    com.whip.app.domain.GoalAggregation.CompletionCount.name,
                )
                TrackAggregation.FixedAmount -> target.aggregation == com.whip.app.domain.GoalAggregation.Sum.name
                TrackAggregation.Latest -> target.aggregation in setOf(
                    com.whip.app.domain.GoalAggregation.Latest.name,
                    com.whip.app.domain.GoalAggregation.TimeInRange.name,
                )
                else -> requiredGoalAggregation == null || target.aggregation == requiredGoalAggregation.name
            }
            require(aggregationCompatible || draft.targetMilestoneId != null || allowTrackGoalAggregationChange) {
                "${aggregation.name} requires a Goal whose calculation is ${requiredGoalAggregation?.name}. Edit the Goal calculation or choose another measure."
            }
        }
    }

    private suspend fun sourceDimension(type: LinkSourceType, entityId: Long?, metricId: String?, metric: LinkSourceMetric, sourceFieldId: Long? = null): UnitDimension = when (type) {
        LinkSourceType.Habit -> UnitDimension.valueOf(database.habitDao().getHabit(requireNotNull(entityId))?.dimension ?: error("Habit no longer exists"))
        LinkSourceType.Task, LinkSourceType.Subtask -> UnitDimension.Count
        LinkSourceType.Workout -> when (metric) {
            LinkSourceMetric.Duration -> UnitDimension.Duration
            LinkSourceMetric.Volume -> UnitDimension.Custom
            else -> UnitDimension.Count
        }
        LinkSourceType.Exercise -> when (metric) {
            LinkSourceMetric.EstimatedOneRepMax, LinkSourceMetric.MaxWeight -> UnitDimension.Mass
            LinkSourceMetric.Distance -> UnitDimension.Distance
            LinkSourceMetric.Repetitions, LinkSourceMetric.Count -> UnitDimension.Count
            LinkSourceMetric.Duration -> UnitDimension.Duration
            LinkSourceMetric.Volume -> UnitDimension.Custom
            else -> UnitDimension.Unitless
        }
        LinkSourceType.Metric -> UnitDimension.valueOf(database.measurementDao().getMetric(requireNotNull(metricId))?.dimension ?: error("Metric no longer exists"))
        LinkSourceType.Track -> if (metric == LinkSourceMetric.EntryCount) {
            UnitDimension.Count
        } else {
            val field = database.trackDao().getField(requireNotNull(sourceFieldId))
                ?: error("Track Field no longer exists")
            field.dimension?.let(UnitDimension::valueOf) ?: UnitDimension.Unitless
        }
    }

    private suspend fun eligibleValue(rule: LinkRuleEntity, event: SourceEvent, target: GoalEntity): Double? {
        if (rule.valueMode == LinkValueMode.FixedValue.name) {
            val unit = resolveUnit(target.unitId) ?: return null
            return rule.fixedValue?.let(unit::toCanonical)?.times(rule.multiplier)?.plus(rule.offset)
        }
        return event.canonicalValue?.times(rule.multiplier)?.plus(rule.offset)
    }

    private suspend fun sourceEvents(rule: LinkRuleEntity): List<SourceEvent> {
        val all = if (LinkSourceType.valueOf(rule.sourceType) == LinkSourceType.Track) {
            trackEvents(rule)
        } else sourceEvents(
            LinkSourceType.valueOf(rule.sourceType),
            rule.sourceEntityId,
            rule.sourceMetricId,
            rule.sourceItemId,
            LinkSourceMetric.valueOf(rule.sourceMetric),
        )
        val earliestEpochDay = rule.retroactiveFromEpochDay
        return all.filter { event ->
            if (earliestEpochDay != null) event.date.toEpochDay() >= earliestEpochDay
            else event.timestamp.toEpochMilli() >= rule.createdAtMillis
        }
    }

    private suspend fun resolveUnit(id: String): UnitDefinition? = BuiltInUnits.get(id)
        ?: database.measurementDao().getUnit(id)?.let { unit ->
            UnitDefinition(
                id = unit.id,
                name = unit.name,
                symbol = unit.symbol,
                dimension = UnitDimension.valueOf(unit.dimension),
                toCanonicalFactor = unit.toCanonicalFactor,
                toCanonicalOffset = unit.toCanonicalOffset,
                custom = unit.custom,
                createdAtMillis = unit.createdAtMillis,
                updatedAtMillis = unit.updatedAtMillis,
            )
        }

    private suspend fun sourceEvents(
        type: LinkSourceType,
        entityId: Long?,
        metricId: String?,
        itemId: Long?,
        metric: LinkSourceMetric,
    ): List<SourceEvent> = when (type) {
        LinkSourceType.Habit -> habitEvents(requireNotNull(entityId), metric)
        LinkSourceType.Task -> taskEvents(requireNotNull(entityId))
        LinkSourceType.Subtask -> subtaskEvents(requireNotNull(entityId), itemId)
        LinkSourceType.Workout -> workoutEvents(metric)
        LinkSourceType.Exercise -> exerciseEvents(requireNotNull(entityId), metric)
        LinkSourceType.Metric -> metricEvents(requireNotNull(metricId))
        LinkSourceType.Track -> emptyList()
    }

    private suspend fun habitEvents(habitId: Long, metric: LinkSourceMetric): List<SourceEvent> {
        val habit = database.habitDao().getHabit(habitId) ?: return emptyList()
        habit.sourceMetricId?.let { sourceMetricId ->
            val sourceEvents = metricEvents(sourceMetricId)
            if (metric != LinkSourceMetric.Success) {
                return sourceEvents.map { event ->
                    event.copy(
                        id = "habit-source:${habit.uuid}:${event.id}",
                        type = LinkSourceType.Habit,
                        entityId = habitId,
                        explanation = "${habit.name}: ${event.explanation}",
                    )
                }
            }
            val unit = resolveUnit(habit.unitId)
            val targetMin = habit.targetMin?.let { unit?.toCanonical(it) ?: it }
            val targetMax = habit.targetMax?.let { unit?.toCanonical(it) ?: it }
            return sourceEvents.groupBy(SourceEvent::date).mapNotNull { (day, events) ->
                val entered = events.sumOf { it.canonicalValue ?: 0.0 }
                val success = when (TargetComparison.valueOf(habit.comparison)) {
                    TargetComparison.AtLeast -> targetMin?.let { entered >= it }
                    TargetComparison.AtMost -> (targetMax ?: targetMin)?.let { entered <= it }
                    TargetComparison.Exactly -> targetMin?.let { entered == it }
                    TargetComparison.WithinRange -> targetMin?.let { min -> targetMax?.let { max -> entered in min..max } }
                    TargetComparison.None -> events.isNotEmpty()
                } == true
                if (!success) return@mapNotNull null
                val latest = events.maxBy(SourceEvent::timestamp)
                SourceEvent(
                    id = "habit-source:${habit.uuid}:day:$day:success",
                    type = LinkSourceType.Habit,
                    entityId = habitId,
                    canonicalValue = 1.0,
                    dimension = UnitDimension.Count,
                    date = day,
                    timestamp = latest.timestamp,
                    explanation = "${habit.name} reached its target from ${latest.explanation}",
                    metricSourceType = latest.metricSourceType,
                )
            }
        }
        val logs = database.habitDao().getAllLogs().filter { it.habitId == habitId }
        if (metric == LinkSourceMetric.Success) {
            return logs.groupBy(HabitLogEntity::localEpochDay).mapNotNull { (day, dayLogs) ->
                val recorded = dayLogs.filter { it.status in setOf(HabitLogStatus.Recorded.name, HabitLogStatus.Success.name, HabitLogStatus.Failed.name) }
                val entered = recorded.sumOf { it.value ?: 0.0 }
                val latest = dayLogs.maxByOrNull(HabitLogEntity::timestampMillis)
                val success = when {
                    recorded.any { it.status == HabitLogStatus.Success.name } -> true
                    habit.trackingMode == "CheckOff" -> recorded.any { (it.value ?: 0.0) > 0.0 }
                    else -> when (TargetComparison.valueOf(habit.comparison)) {
                        TargetComparison.AtLeast -> habit.targetMin?.let { entered >= it }
                        TargetComparison.AtMost -> (habit.targetMax ?: habit.targetMin)?.let { entered <= it }
                        TargetComparison.Exactly -> habit.targetMin?.let { entered == it }
                        TargetComparison.WithinRange -> habit.targetMin?.let { min -> habit.targetMax?.let { max -> entered in min..max } }
                        TargetComparison.None -> recorded.isNotEmpty()
                    } == true
                }
                if (!success || latest == null) null else SourceEvent(
                    "habit:${habit.uuid}:day:$day:success", LinkSourceType.Habit, habitId, 1.0,
                    UnitDimension.Count, LocalDate.ofEpochDay(day), Instant.ofEpochMilli(latest.timestampMillis),
                    "${habit.name} succeeded on ${LocalDate.ofEpochDay(day)}", MetricSourceType.Habit,
                )
            }
        }
        return logs.filter { it.status in setOf(HabitLogStatus.Recorded.name, HabitLogStatus.Success.name) && it.canonicalValue != null }
            .map { log -> SourceEvent(
                "habit-log:${log.uuid}", LinkSourceType.Habit, habitId, log.canonicalValue,
                UnitDimension.valueOf(habit.dimension), LocalDate.ofEpochDay(log.localEpochDay),
                Instant.ofEpochMilli(log.timestampMillis), "${habit.name}: ${log.value} ${log.enteredUnitId.orEmpty()}", MetricSourceType.Habit,
            ) }
    }

    private suspend fun taskEvents(taskId: Long): List<SourceEvent> {
        val task = database.taskDao().getTask(taskId) ?: return emptyList()
        val events = mutableListOf<SourceEvent>()
        task.completedAtMillis?.let { completed ->
            val instant = Instant.ofEpochMilli(completed)
            events += SourceEvent("task:$taskId:single", LinkSourceType.Task, taskId, 1.0, UnitDimension.Count,
                instant.atZone(clock.zoneId()).toLocalDate(), instant, "Completed task: ${task.title}", MetricSourceType.Task)
        }
        database.taskDao().getOccurrences(taskId).filter { it.state == "Completed" && it.completedAtMillis != null }.forEach { occurrence ->
            events += SourceEvent("task:$taskId:${occurrence.originalEpochDay}", LinkSourceType.Task, taskId, 1.0, UnitDimension.Count,
                LocalDate.ofEpochDay(occurrence.originalEpochDay), Instant.ofEpochMilli(requireNotNull(occurrence.completedAtMillis)),
                "Completed task: ${task.title} (${LocalDate.ofEpochDay(occurrence.originalEpochDay)})", MetricSourceType.Task)
        }
        return events
    }

    private suspend fun subtaskEvents(taskId: Long, itemId: Long?): List<SourceEvent> {
        val task = database.taskDao().getTask(taskId) ?: return emptyList()
        val steps = database.taskDao().getAllSteps().filter { it.taskId == taskId }.associateBy { it.id }
        return database.taskDao().getAllStepStates().filter { it.taskId == taskId && it.completed && it.completedAtMillis != null && (itemId == null || it.stepId == itemId) }
            .map { state ->
                val title = state.titleSnapshot.ifBlank { steps[state.stepId]?.title.orEmpty() }
                val instant = Instant.ofEpochMilli(requireNotNull(state.completedAtMillis))
                SourceEvent("subtask:$taskId:${state.occurrenceKey}:${state.stepId}", LinkSourceType.Subtask, taskId, 1.0,
                    UnitDimension.Count, instant.atZone(clock.zoneId()).toLocalDate(), instant,
                    "Completed subtask $title in ${task.title}", MetricSourceType.Task)
            }
    }

    private suspend fun workoutEvents(metric: LinkSourceMetric): List<SourceEvent> {
        val gym = database.gymDao()
        val sessions = gym.getAllSessions().filter { it.state == "Finished" && !it.archived }
        val workoutExercises = gym.getAllWorkoutExercises()
        val sets = gym.getAllWorkoutSets()
        val exercises = gym.getAllExercises().associateBy(ExerciseEntity::id)
        return sessions.mapNotNull { session ->
            val sessionExercises = workoutExercises.filter { it.sessionId == session.id }
            val sessionSets = sets.filter { set -> sessionExercises.any { it.id == set.workoutExerciseId } && set.completed && set.deletedAtMillis == null }
            val valueAndDimension = when (metric) {
                LinkSourceMetric.Count, LinkSourceMetric.Completion -> 1.0 to UnitDimension.Count
                LinkSourceMetric.Duration -> ((session.endedAtMillis ?: session.startedAtMillis) - session.startedAtMillis).coerceAtLeast(0).div(1_000.0) to UnitDimension.Duration
                LinkSourceMetric.Volume -> sessionSets.sumOf { set -> rawSetVolume(set, sessionExercises, exercises) } to UnitDimension.Custom
                else -> return@mapNotNull null
            }
            SourceEvent("workout:${session.uuid}:${metric.name}", LinkSourceType.Workout, session.id, valueAndDimension.first,
                valueAndDimension.second, LocalDate.ofEpochDay(session.localEpochDay), Instant.ofEpochMilli(session.endedAtMillis ?: session.startedAtMillis),
                "Workout ${session.name.ifBlank { LocalDate.ofEpochDay(session.localEpochDay).toString() }}: ${metric.name}", MetricSourceType.Workout)
        }
    }

    private suspend fun exerciseEvents(exerciseId: Long, metric: LinkSourceMetric): List<SourceEvent> {
        val gym = database.gymDao()
        val exercise = gym.getExercise(exerciseId) ?: return emptyList()
        val sessions = gym.getAllSessions().filter { it.state == "Finished" && !it.archived }.associateBy(WorkoutSessionEntity::id)
        val workoutExercises = gym.getAllWorkoutExercises().filter { it.exerciseId == exerciseId }
        val sets = gym.getAllWorkoutSets()
        return workoutExercises.mapNotNull { workoutExercise ->
            val session = sessions[workoutExercise.sessionId] ?: return@mapNotNull null
            val eligible = sets.filter { it.workoutExerciseId == workoutExercise.id && it.completed && it.deletedAtMillis == null }
            val valueDimension = when (metric) {
                LinkSourceMetric.EstimatedOneRepMax -> eligible.mapNotNull { rawEstimatedOneRepMax(it, exercise) }.maxOrNull()?.let { it to UnitDimension.Mass }
                LinkSourceMetric.MaxWeight -> eligible.mapNotNull(WorkoutSetEntity::canonicalWeightKg).maxOrNull()?.let { it to UnitDimension.Mass }
                LinkSourceMetric.Distance -> eligible.sumOf { it.canonicalDistanceMetres ?: 0.0 } to UnitDimension.Distance
                LinkSourceMetric.Repetitions, LinkSourceMetric.Count -> eligible.sumOf { it.repetitions ?: 0 }.toDouble() to UnitDimension.Count
                LinkSourceMetric.Duration -> eligible.sumOf { it.durationSeconds ?: 0L }.toDouble() to UnitDimension.Duration
                LinkSourceMetric.Volume -> eligible.sumOf { rawSetVolume(it, listOf(workoutExercise), mapOf(exerciseId to exercise)) } to UnitDimension.Custom
                else -> null
            } ?: return@mapNotNull null
            SourceEvent("exercise:${exercise.uuid}:workout:${session.uuid}:${metric.name}", LinkSourceType.Exercise, exerciseId,
                valueDimension.first, valueDimension.second, LocalDate.ofEpochDay(session.localEpochDay),
                Instant.ofEpochMilli(session.endedAtMillis ?: session.startedAtMillis), "${exercise.name}: ${metric.name}", MetricSourceType.Exercise)
        }
    }

    private suspend fun metricEvents(metricId: String): List<SourceEvent> {
        val metric = database.measurementDao().getMetric(metricId) ?: return emptyList()
        return database.measurementDao().getAllEntries().filter { it.metricId == metricId && it.status == MetricEntryStatus.Recorded.name && it.canonicalValue != null }
            .map { entry -> SourceEvent("metric-entry:${entry.id}", LinkSourceType.Metric, null, entry.canonicalValue,
                UnitDimension.valueOf(metric.dimension), LocalDate.ofEpochDay(entry.localEpochDay), Instant.ofEpochMilli(entry.timestampMillis),
                "${metric.name} measurement", MetricSourceType.valueOf(entry.sourceType)) }
    }

    private suspend fun trackEvents(
        rule: LinkRuleEntity,
        conditionOverride: List<TrackCondition>? = null,
        conditionModeOverride: TrackConditionMode? = null,
    ): List<SourceEvent> {
        val projection = trackProjection(requireNotNull(rule.sourceEntityId)) ?: return emptyList()
        val fieldUuidById = projection.fields.associate { it.id to it.uuid }
        val optionUuidById = projection.options.associate { it.id to it.uuid }
        val conditions = conditionOverride ?: run {
            val conditionRows = dao.getRuleConditions(rule.id)
            val choiceRows = if (conditionRows.isEmpty()) emptyList() else dao.getLinkConditionChoices(conditionRows.map { it.id })
            conditionRows.mapNotNull { condition ->
                condition.toDomain(fieldUuidById, choiceRows.filter { it.conditionId == condition.id }.mapNotNull { optionUuidById[it.optionId] }.toSet())
            }
        }
        val matching = projection.matchingEntries(conditions, conditionModeOverride ?: TrackConditionMode.valueOf(rule.conditionMode))
        val sourceField = rule.sourceFieldId?.let { id -> projection.fields.firstOrNull { it.id == id } }
        return matching.mapNotNull { entry ->
            val valueAndDimension = when (LinkSourceMetric.valueOf(rule.sourceMetric)) {
                LinkSourceMetric.EntryCount -> 1.0 to UnitDimension.Count
                LinkSourceMetric.FieldValue -> when (sourceField?.type) {
                    TrackFieldType.Number -> entry.value(sourceField.id)?.canonicalNumber?.let { it to requireNotNull(sourceField.dimension) }
                    TrackFieldType.Scale -> entry.value(sourceField.id)?.scaleValue?.let { it to UnitDimension.Unitless }
                    else -> null
                }
                else -> null
            } ?: return@mapNotNull null
            SourceEvent(
                id = "track:${projection.track.uuid}:entry:${entry.entry.uuid}:field:${sourceField?.uuid ?: "count"}",
                type = LinkSourceType.Track,
                entityId = projection.track.id,
                canonicalValue = valueAndDimension.first,
                dimension = valueAndDimension.second,
                date = entry.entry.entryDate,
                timestamp = Instant.ofEpochMilli(entry.entry.createdAtMillis),
                explanation = buildString {
                    append(projection.track.name)
                    append(": ")
                    append(projection.primaryText(entry))
                    sourceField?.let { append(" · ${it.name} ${valueAndDimension.first}") }
                },
                metricSourceType = MetricSourceType.Track,
            )
        }
    }

    private suspend fun trackProjection(trackId: Long): TrackProjection? {
        val track = database.trackDao().getTrack(trackId)?.toDomain() ?: return null
        val fields = database.trackDao().getFields(trackId).map(TrackFieldEntity::toDomain)
        val options = if (fields.isEmpty()) emptyList() else database.trackDao().getOptionsForFields(fields.map { it.id }).map(TrackChoiceOptionEntity::toDomain)
        val entries = database.trackDao().getEntries(trackId)
        val values = if (entries.isEmpty()) emptyMap() else database.trackDao().getValuesForEntries(entries.map { it.id }).groupBy { it.entryId }
        return TrackProjection(track, fields, options, entries.map { entry ->
            TrackEntryProjection(entry.toDomain(), values[entry.id].orEmpty().associate { it.fieldId to it.toDomain() })
        })
    }

    private suspend fun rebuildTriggersLocked(sourceTypeNames: Set<String>? = null) {
        dao.getTriggerRules().filter { sourceTypeNames == null || it.sourceType in sourceTypeNames }.forEach { rule ->
            val targetAcceptsEvents = if (rule.targetType == TriggerTargetType.Track.name) {
                database.trackDao().getTrack(rule.targetEntityId)?.archived == false
            } else {
                true
            }
            val events = (if (rule.enabled && targetAcceptsEvents) triggerSourceEvents(rule) else emptyList())
                .filter { it.timestamp.toEpochMilli() >= rule.createdAtMillis }
            val retained = events.mapTo(mutableSetOf(), SourceEvent::id)
            val existing = dao.getTriggerOccurrences(rule.id).associateBy(TriggerOccurrenceEntity::sourceEventId)
            events.forEach { event ->
                if (event.id !in existing) {
                    dao.upsertTriggerOccurrence(
                        TriggerOccurrenceEntity(
                            triggerRuleId = rule.id,
                            sourceEventId = event.id,
                            availableAtMillis = quietHoursAdjusted(event.timestamp.plusSeconds(rule.delayMinutes.toLong() * 60), rule).toEpochMilli(),
                            deliveredAtMillis = null,
                            dismissedAtMillis = null,
                            remindAtMillis = null,
                            fulfilledEntryId = null,
                            sourceSnapshot = TriggerSnapshot.from(event, TriggerOutcome.valueOf(rule.outcome)).encode(),
                        ),
                    )
                }
            }
            val automaticallyLogsTargetHabit = rule.targetType == TriggerTargetType.Habit.name &&
                rule.action == TriggerAction.CheckOffHabit.name
            if (automaticallyLogsTargetHabit) {
                syncGeneratedHabitLogs(rule, events)
            } else {
                removeGeneratedHabitLogs(rule, emptySet())
            }
            existing.values.filter { it.sourceEventId !in retained && it.fulfilledEntryId == null }.forEach { stale ->
                database.openHelper.writableDatabase.execSQL("DELETE FROM trigger_occurrences WHERE id = ?", arrayOf(stale.id))
            }
        }
    }

    private suspend fun triggerSourceEvents(rule: TriggerRuleEntity): List<SourceEvent> {
        val type = LinkSourceType.valueOf(rule.sourceType)
        val outcome = TriggerOutcome.valueOf(rule.outcome)
        if (type == LinkSourceType.Track) {
            val projection = trackProjection(rule.sourceEntityId) ?: return emptyList()
            val fieldUuidById = projection.fields.associate { it.id to it.uuid }
            val optionUuidById = projection.options.associate { it.id to it.uuid }
            val conditionRows = dao.getTriggerConditions(rule.id)
            val choiceRows = if (conditionRows.isEmpty()) emptyList() else dao.getTriggerConditionChoices(conditionRows.map { it.id })
            val conditions = conditionRows.mapNotNull { condition ->
                condition.toDomain(fieldUuidById, choiceRows.filter { it.conditionId == condition.id }.mapNotNull { optionUuidById[it.optionId] }.toSet())
            }
            return projection.matchingEntries(conditions, TrackConditionMode.valueOf(rule.conditionMode)).map { entry ->
                SourceEvent(
                    "track:${projection.track.uuid}:entry:${entry.entry.uuid}",
                    LinkSourceType.Track,
                    projection.track.id,
                    1.0,
                    UnitDimension.Count,
                    entry.entry.entryDate,
                    Instant.ofEpochMilli(entry.entry.createdAtMillis),
                    "${projection.track.name}: ${projection.primaryText(entry)}",
                    MetricSourceType.Track,
                )
            }
        }
        if (type == LinkSourceType.Habit && outcome != TriggerOutcome.Completed) {
            val habit = database.habitDao().getHabit(rule.sourceEntityId) ?: return emptyList()
            if (outcome == TriggerOutcome.Skipped) {
                return database.habitDao().getSkips(habit.id).map { skip ->
                    SourceEvent(
                        "habit-skip:${skip.uuid}",
                        type,
                        habit.id,
                        null,
                        UnitDimension.Count,
                        LocalDate.ofEpochDay(skip.localEpochDay),
                        Instant.ofEpochMilli(skip.skippedAtMillis),
                        "${habit.name} skipped",
                        MetricSourceType.Habit,
                    )
                }
            }
            val acceptedStatuses = when (outcome) {
                TriggerOutcome.Recorded -> setOf(HabitLogStatus.Recorded.name, HabitLogStatus.Success.name, HabitLogStatus.Failed.name)
                TriggerOutcome.Failed -> setOf(HabitLogStatus.Failed.name)
                TriggerOutcome.Skipped, TriggerOutcome.Completed -> emptySet()
            }
            return database.habitDao().getAllLogs().filter { it.habitId == habit.id && it.status in acceptedStatuses }.map { log ->
                SourceEvent("habit-log:${log.uuid}:${outcome.name}", type, habit.id, log.canonicalValue,
                    if (outcome == TriggerOutcome.Recorded) UnitDimension.valueOf(habit.dimension) else UnitDimension.Count,
                    LocalDate.ofEpochDay(log.localEpochDay), Instant.ofEpochMilli(log.timestampMillis),
                    "${habit.name} ${outcome.name.lowercase()}", MetricSourceType.Habit)
            }
        }
        if (type == LinkSourceType.Task && outcome == TriggerOutcome.Skipped) {
            val task = database.taskDao().getTask(rule.sourceEntityId) ?: return emptyList()
            return database.taskDao().getOccurrences(task.id).filter { it.state == "Skipped" }.map { occurrence ->
                val timestamp = Instant.ofEpochMilli(task.updatedAtMillis)
                SourceEvent("task:${task.id}:${occurrence.originalEpochDay}:skipped", type, task.id, null, UnitDimension.Count,
                    LocalDate.ofEpochDay(occurrence.originalEpochDay), timestamp, "Skipped task: ${task.title}", MetricSourceType.Task)
            }
        }
        if (type == LinkSourceType.Subtask) {
            return subtaskEvents(rule.sourceEntityId, rule.sourceItemId)
        }
        val metric = when (type) {
            LinkSourceType.Habit -> LinkSourceMetric.Success
            LinkSourceType.Task, LinkSourceType.Subtask, LinkSourceType.Workout -> LinkSourceMetric.Completion
            LinkSourceType.Exercise -> LinkSourceMetric.Count
            LinkSourceType.Metric -> LinkSourceMetric.NumericValue
            LinkSourceType.Track -> LinkSourceMetric.EntryCount
        }
        return sourceEvents(type, rule.sourceEntityId, null, rule.sourceItemId, metric)
    }

    private suspend fun syncGeneratedHabitLogs(rule: TriggerRuleEntity, events: List<SourceEvent>) {
        val habit = database.habitDao().getHabit(rule.targetEntityId) ?: return
        val prefix = "trigger:${rule.uuid}:"
        val retained = events.mapTo(mutableSetOf()) { prefix + it.id }
        events.forEach { event ->
            val sourceId = prefix + event.id
            if (database.habitDao().getLogBySource(event.metricSourceType.name, sourceId) == null) {
                val logUuid = ids.nextId()
                val entryId = measurementRepository.record(
                    habit.metricId, 1.0, habit.unitId, timestamp = event.timestamp, localDate = event.date,
                    zoneId = clock.zoneId(),
                    sourceType = event.metricSourceType, sourceId = sourceId, note = "Automatically logged by ${rule.name}",
                )
                val now = clock.now().toEpochMilli()
                database.habitDao().insertLog(
                    HabitLogEntity(uuid = logUuid, habitId = habit.id, value = 1.0, canonicalValue = BuiltInUnits.get(habit.unitId)?.toCanonical(1.0) ?: 1.0,
                        enteredUnitId = habit.unitId, status = HabitLogStatus.Success.name, timestampMillis = event.timestamp.toEpochMilli(),
                        localEpochDay = event.date.toEpochDay(), zoneId = clock.zoneId().id,
                        offsetSeconds = clock.zoneId().rules.getOffset(event.timestamp).totalSeconds, note = "Automatically logged by ${rule.name}",
                        sourceType = event.metricSourceType.name, sourceId = sourceId, metricEntryId = entryId, createdAtMillis = now, updatedAtMillis = now),
                )
            }
        }
        removeGeneratedHabitLogs(rule, retained)
    }

    private suspend fun removeGeneratedHabitLogs(rule: TriggerRuleEntity, retained: Set<String>) {
        val prefix = "trigger:${rule.uuid}:%"
        database.habitDao().getLogsBySourcePrefix(prefix).filter { it.sourceId !in retained }.forEach { log ->
            log.metricEntryId?.let { measurementRepository.deleteEntry(it) }
            database.habitDao().deleteLog(log.id)
        }
    }

    private suspend fun validateTrigger(draft: TriggerRuleDraft, excludingId: Long? = null) {
        require(draft.name.isNotBlank()) { "Trigger name is required" }
        require(draft.delayMinutes >= 0) { "Delay cannot be negative" }
        when (draft.sourceType) {
            LinkSourceType.Habit -> {
                require(database.habitDao().getHabit(draft.sourceEntityId) != null) { "Source Habit no longer exists" }
                require(draft.sourceItemId == null) { "Habit triggers cannot reference a Subtask" }
            }
            LinkSourceType.Task -> {
                require(database.taskDao().getTask(draft.sourceEntityId) != null) { "Source Task no longer exists" }
                require(draft.sourceItemId == null) { "Task triggers cannot reference a Subtask" }
                require(draft.outcome in setOf(TriggerOutcome.Completed, TriggerOutcome.Skipped)) {
                    "Tasks can trigger automation only when completed or skipped"
                }
            }
            LinkSourceType.Subtask -> {
                require(database.taskDao().getTask(draft.sourceEntityId) != null) { "Source Task no longer exists" }
                val stepId = requireNotNull(draft.sourceItemId) { "Choose a Subtask" }
                val step = database.taskDao().getAllSteps().firstOrNull { it.id == stepId }
                require(step?.taskId == draft.sourceEntityId && !step.archived) { "The selected Subtask no longer belongs to this Task" }
                require(draft.outcome == TriggerOutcome.Completed) { "Subtasks can trigger automation only when completed" }
            }
            LinkSourceType.Track -> {
                require(database.trackDao().getTrack(draft.sourceEntityId) != null) { "Source Track no longer exists" }
                require(draft.outcome == TriggerOutcome.Completed) { "Track Automations run when an Entry matches" }
                require(draft.sourceItemId == null) { "Track triggers cannot reference a Subtask" }
            }
            LinkSourceType.Workout -> {
                require(draft.sourceEntityId == 0L) { "Workout Automations use all completed Workouts" }
                require(draft.outcome == TriggerOutcome.Completed) { "Workouts can trigger automation only when completed" }
            }
            LinkSourceType.Exercise, LinkSourceType.Metric -> error("This source is available for Goal progress, not follow-up Automations")
        }
        val sourceNodeType = if (draft.sourceType == LinkSourceType.Subtask) TriggerTargetType.Task.name else draft.sourceType.name
        require(!(sourceNodeType == draft.targetType.name && draft.sourceEntityId == draft.targetEntityId)) { "A trigger cannot target itself" }
        require(
            when (draft.action) {
                TriggerAction.PromptTask -> draft.targetType == TriggerTargetType.Task
                TriggerAction.PromptHabit, TriggerAction.CheckOffHabit -> draft.targetType == TriggerTargetType.Habit
                TriggerAction.PromptTrackEntry -> draft.targetType == TriggerTargetType.Track
            },
        ) { "The selected consequence does not match its target type" }
        when (draft.targetType) {
            TriggerTargetType.Task -> require(database.taskDao().getTask(draft.targetEntityId) != null) { "Target Task no longer exists" }
            TriggerTargetType.Habit -> require(database.habitDao().getHabit(draft.targetEntityId) != null) { "Target Habit no longer exists" }
            TriggerTargetType.Track -> Unit
        }
        if (draft.action == TriggerAction.CheckOffHabit) {
            val habit = database.habitDao().getHabit(draft.targetEntityId) ?: error("Habit no longer exists")
            require(habit.trackingMode == com.whip.app.domain.HabitTrackingMode.CheckOff.name) {
                "Automatic Check Off is available only for Check Off Habits"
            }
        }
        if (draft.targetType == TriggerTargetType.Track) {
            val track = database.trackDao().getTrack(draft.targetEntityId) ?: error("Track no longer exists")
            require(!track.archived) { "Restore ${track.name} before creating a Capture Automation" }
            val fields = database.trackDao().getFields(track.id)
            require(draft.mappings.map(TriggerFieldMapping::targetFieldId).distinct().size == draft.mappings.size) {
                "Each target Field can have only one prefill mapping"
            }
            require(draft.mappings.all { mapping -> fields.any { it.id == mapping.targetFieldId } }) {
                "Every prefill mapping must target a Field in ${track.name}"
            }
            draft.mappings.forEach { mapping ->
                val field = fields.first { it.id == mapping.targetFieldId }
                val type = TrackFieldType.valueOf(field.type)
                val allowed = when (type) {
                    TrackFieldType.ShortText, TrackFieldType.LongText -> TriggerSourceProperty.entries.toSet()
                    TrackFieldType.Number, TrackFieldType.Scale -> setOf(TriggerSourceProperty.NumericValue, TriggerSourceProperty.Constant)
                    TrackFieldType.SingleChoice -> setOf(TriggerSourceProperty.Outcome, TriggerSourceProperty.Unit, TriggerSourceProperty.Constant)
                    TrackFieldType.Date -> setOf(TriggerSourceProperty.EventDate, TriggerSourceProperty.Constant)
                    TrackFieldType.YesNo -> setOf(TriggerSourceProperty.Outcome, TriggerSourceProperty.Constant)
                }
                require(mapping.sourceProperty in allowed) { "${mapping.sourceProperty.name} cannot prefill ${field.name}" }
                if (mapping.sourceProperty == TriggerSourceProperty.NumericValue && type == TrackFieldType.Number) {
                    val metric = when (draft.sourceType) {
                        LinkSourceType.Habit, LinkSourceType.Metric -> LinkSourceMetric.NumericValue
                        LinkSourceType.Task, LinkSourceType.Subtask -> LinkSourceMetric.Completion
                        LinkSourceType.Workout, LinkSourceType.Exercise, LinkSourceType.Track -> LinkSourceMetric.Count
                    }
                    val sourceDimension = sourceDimension(draft.sourceType, draft.sourceEntityId, null, metric)
                    require(field.dimension == sourceDimension.name) {
                        "${field.name} uses ${field.dimension}, but the source number uses ${sourceDimension.name}. Choose another Field or mapping."
                    }
                }
                if (mapping.sourceProperty == TriggerSourceProperty.Constant) {
                    val constant = requireNotNull(mapping.constantValue) { "Enter the constant for ${field.name}" }
                    require(!constant.isBlankFor(type)) { "Enter the constant for ${field.name}" }
                    require(constant.enteredNumber?.isFinite() != false) { "${field.name}'s constant must be finite" }
                    if (type == TrackFieldType.Scale) require(
                        constant.scaleValue?.let { value ->
                            normalizeTrackScaleValue(
                                value,
                                requireNotNull(field.scaleMin),
                                requireNotNull(field.scaleMax),
                                field.scaleStep,
                            )
                        } != null,
                    ) { "${field.name}'s constant must use a selectable Scale increment" }
                }
            }
        }
        if (draft.sourceType == LinkSourceType.Track) {
            val track = database.trackDao().getTrack(draft.sourceEntityId) ?: error("Source Track no longer exists")
            require(draft.conditions.all { condition ->
                condition.fieldUuid == TRACK_ENTRY_DATE_CONDITION_UUID || database.trackDao().getFieldByUuid(condition.fieldUuid)?.trackId == track.id
            }) {
                "Every condition must use a Field from ${track.name}"
            }
        } else {
            require(draft.conditions.isEmpty()) { "Conditions are available for Track Entry sources" }
        }
        fun sourceNode(type: String, entityId: Long): String =
            "${if (type == LinkSourceType.Subtask.name) TriggerTargetType.Task.name else type}:$entityId"
        val edges = dao.getTriggerRules().filter { it.id != excludingId && it.enabled }.map {
            sourceNode(it.sourceType, it.sourceEntityId) to "${it.targetType}:${it.targetEntityId}"
        } + (sourceNode(draft.sourceType.name, draft.sourceEntityId) to "${draft.targetType.name}:${draft.targetEntityId}")
        require(!hasDirectedCycle(edges)) { "This trigger would create a cycle" }
    }

    private suspend fun syncRuleConditions(ruleId: Long, conditions: List<TrackCondition>) {
        dao.deleteRuleConditions(ruleId)
        conditions.forEachIndexed { position, condition ->
            val field = condition.fieldUuid.takeUnless { it == TRACK_ENTRY_DATE_CONDITION_UUID }
                ?.let { database.trackDao().getFieldByUuid(it) }
            if (condition.fieldUuid != TRACK_ENTRY_DATE_CONDITION_UUID) requireNotNull(field) { "Condition Field no longer exists" }
            val conditionId = dao.insertRuleCondition(condition.toLinkEntity(ruleId, field?.id, position))
            condition.choiceOptionUuids.forEach { uuid ->
                val option = database.trackDao().getOptionByUuid(uuid) ?: error("Condition choice no longer exists")
                require(option.fieldId == field?.id) { "Condition choice does not belong to ${field?.name ?: "Entry Date"}" }
                dao.insertLinkConditionChoice(LinkConditionChoiceEntity(conditionId, option.id))
            }
        }
    }

    private suspend fun syncTriggerDetails(ruleId: Long, draft: TriggerRuleDraft) {
        dao.deleteTriggerConditions(ruleId)
        dao.deleteTriggerMappings(ruleId)
        draft.conditions.forEachIndexed { position, condition ->
            val field = condition.fieldUuid.takeUnless { it == TRACK_ENTRY_DATE_CONDITION_UUID }
                ?.let { database.trackDao().getFieldByUuid(it) }
            if (condition.fieldUuid != TRACK_ENTRY_DATE_CONDITION_UUID) requireNotNull(field) { "Condition Field no longer exists" }
            val conditionId = dao.insertTriggerCondition(condition.toTriggerEntity(ruleId, field?.id, position))
            condition.choiceOptionUuids.forEach { uuid ->
                val option = database.trackDao().getOptionByUuid(uuid) ?: error("Condition choice no longer exists")
                require(option.fieldId == field?.id) { "Condition choice does not belong to ${field?.name ?: "Entry Date"}" }
                dao.insertTriggerConditionChoice(TriggerConditionChoiceEntity(conditionId, option.id))
            }
        }
        draft.mappings.forEach { mapping ->
            val option = mapping.constantValue?.choiceOptionUuid?.let { uuid ->
                database.trackDao().getOptionByUuid(uuid) ?: error("Constant choice no longer exists")
            }
            if (option != null) require(option.fieldId == mapping.targetFieldId) { "Constant choice does not belong to the mapped Field" }
            dao.insertTriggerMapping(mapping.toEntity(ruleId, option?.id))
        }
    }

    private fun quietHoursAdjusted(instant: Instant, rule: TriggerRuleEntity): Instant {
        val start = rule.quietStartMinutes ?: return instant
        val end = rule.quietEndMinutes ?: return instant
        val zone = clock.zoneId()
        val zoned = instant.atZone(zone)
        val minute = zoned.hour * 60 + zoned.minute
        val quiet = if (start <= end) minute in start until end else minute >= start || minute < end
        if (!quiet) return instant
        val date = if (start <= end || minute >= start) zoned.toLocalDate() else zoned.toLocalDate()
        var release = date.atStartOfDay(zone).plusMinutes(end.toLong())
        if (!release.isAfter(zoned)) release = release.plusDays(1)
        return release.toInstant()
    }
}

private data class SourceEvent(
    val id: String,
    val type: LinkSourceType,
    val entityId: Long?,
    val canonicalValue: Double?,
    val dimension: UnitDimension,
    val date: LocalDate,
    val timestamp: Instant,
    val explanation: String,
    val metricSourceType: MetricSourceType,
)

private data class TriggerSnapshot(
    val title: String,
    val name: String,
    val notes: String,
    val numericValue: Double?,
    val unit: String,
    val outcome: TriggerOutcome,
    val eventDate: LocalDate?,
    val explanation: String,
) {
    fun encode(): String = JSONObject()
        .put("version", 1)
        .put("title", title)
        .put("name", name)
        .put("notes", notes)
        .put("numericValue", numericValue)
        .put("unit", unit)
        .put("outcome", outcome.name)
        .put("eventDate", eventDate?.toString())
        .put("explanation", explanation)
        .toString()

    companion object {
        fun from(event: SourceEvent, outcome: TriggerOutcome): TriggerSnapshot {
            val readableName = event.explanation
                .removePrefix("Completed task: ")
                .removePrefix("Completed subtask ")
                .substringBefore(" (")
                .substringBefore(" · ")
                .trim()
            return TriggerSnapshot(
                title = readableName,
                name = readableName,
                notes = event.explanation,
                numericValue = event.canonicalValue,
                unit = event.dimension.name,
                outcome = outcome,
                eventDate = event.date,
                explanation = event.explanation,
            )
        }

        fun parse(raw: String): TriggerSnapshot = runCatching {
            val json = JSONObject(raw)
            TriggerSnapshot(
                title = json.optString("title"),
                name = json.optString("name"),
                notes = json.optString("notes"),
                numericValue = json.optDouble("numericValue").takeUnless(Double::isNaN),
                unit = json.optString("unit"),
                outcome = runCatching { TriggerOutcome.valueOf(json.optString("outcome")) }.getOrDefault(TriggerOutcome.Completed),
                eventDate = json.optString("eventDate").takeIf(String::isNotBlank)?.let(LocalDate::parse),
                explanation = json.optString("explanation").ifBlank { raw },
            )
        }.getOrElse {
            TriggerSnapshot(raw, raw, raw, null, "", TriggerOutcome.Completed, null, raw)
        }
    }
}

private suspend fun TriggerFieldMapping.valueFor(
    field: com.whip.app.domain.TrackField,
    projection: TrackProjection,
    snapshot: TriggerSnapshot,
    resolveUnit: suspend (String) -> UnitDefinition?,
): TrackValueDraft? {
    if (sourceProperty == TriggerSourceProperty.Constant) return constantValue
    val text = when (sourceProperty) {
        TriggerSourceProperty.Title -> snapshot.title
        TriggerSourceProperty.Notes -> snapshot.notes
        TriggerSourceProperty.Name -> snapshot.name
        TriggerSourceProperty.Unit -> snapshot.unit
        TriggerSourceProperty.Outcome -> snapshot.outcome.name.lowercase()
        TriggerSourceProperty.EventDate -> snapshot.eventDate?.toString().orEmpty()
        TriggerSourceProperty.NumericValue -> snapshot.numericValue?.toString().orEmpty()
        TriggerSourceProperty.Constant -> ""
    }
    return when (field.type) {
        TrackFieldType.ShortText, TrackFieldType.LongText -> TrackValueDraft(textValue = text.takeIf(String::isNotBlank))
        TrackFieldType.Number -> snapshot.numericValue?.let { canonical ->
            val unit = field.unitId?.let { id -> resolveUnit(id) }
            TrackValueDraft(enteredNumber = unit?.fromCanonical(canonical) ?: canonical, enteredUnitId = field.unitId)
        }
        TrackFieldType.Scale -> snapshot.numericValue?.let { value ->
            normalizeTrackScaleValue(
                value,
                requireNotNull(field.scaleMin),
                requireNotNull(field.scaleMax),
                field.scaleStep,
            )
        }?.let { TrackValueDraft(scaleValue = it) }
        TrackFieldType.Date -> snapshot.eventDate?.let { TrackValueDraft(dateValue = it) }
        TrackFieldType.YesNo -> when (snapshot.outcome) {
            TriggerOutcome.Completed, TriggerOutcome.Recorded -> TrackValueDraft(booleanValue = true)
            TriggerOutcome.Failed -> TrackValueDraft(booleanValue = false)
            TriggerOutcome.Skipped -> null
        }
        TrackFieldType.SingleChoice -> projection.optionsFor(field.id)
            .firstOrNull { it.label.equals(text, ignoreCase = true) }
            ?.let { TrackValueDraft(choiceOptionUuid = it.uuid) }
    }
}

private data class TrackAutomationSchema(
    val fields: List<TrackFieldEntity>,
    val options: List<TrackChoiceOptionEntity>,
)

private data class TriggerAutomationDetails(
    val conditions: List<TriggerRuleConditionEntity>,
    val choices: List<TriggerConditionChoiceEntity>,
    val mappings: List<TriggerFieldMappingEntity>,
)

private fun rawSetVolume(set: WorkoutSetEntity, workoutExercises: List<WorkoutExerciseEntity>, exercises: Map<Long, ExerciseEntity>): Double {
    val workoutExercise = workoutExercises.firstOrNull { it.id == set.workoutExerciseId } ?: return 0.0
    val exercise = exercises[workoutExercise.exerciseId] ?: return 0.0
    if (!exercise.includeInVolume || set.classification == "WarmUp") return 0.0
    return (rawEffectiveLoad(set, exercise) ?: return 0.0) * (set.repetitions ?: 1)
}

private fun rawEffectiveLoad(set: WorkoutSetEntity, exercise: ExerciseEntity): Double? {
    val external = set.canonicalWeightKg ?: 0.0
    return when (exercise.trackingType) {
        "WeightReps", "WeightOnly", "WeightDuration" -> set.canonicalWeightKg
        "BodyweightReps" -> when (exercise.bodyweightLoadPolicy) {
            "ExternalWeightOnly" -> set.canonicalWeightKg
            "BodyweightPlusExternal" -> set.bodyweightKg?.plus(external)
            else -> set.bodyweightKg?.times(exercise.effectiveBodyweightPercent / 100.0)?.plus(external)
        }
        "AssistedBodyweightReps" -> set.bodyweightKg?.let { (it * exercise.effectiveBodyweightPercent / 100.0 - external).coerceAtLeast(0.0) }
        else -> null
    }
}

private fun rawEstimatedOneRepMax(set: WorkoutSetEntity, exercise: ExerciseEntity): Double? {
    if (!exercise.includeInPersonalRecords || set.classification == "WarmUp") return null
    return estimatedOneRepMax(rawEffectiveLoad(set, exercise) ?: return null, set.repetitions ?: return null,
        EstimatedOneRepMaxFormula.valueOf(exercise.oneRepMaxFormula))
}

private fun hasDirectedCycle(edges: List<Pair<String, String>>): Boolean {
    val graph = edges.groupBy({ it.first }, { it.second })
    val visiting = mutableSetOf<String>()
    val visited = mutableSetOf<String>()
    fun visit(node: String): Boolean {
        if (node in visiting) return true
        if (!visited.add(node)) return false
        visiting += node
        val cycle = graph[node].orEmpty().any(::visit)
        visiting -= node
        return cycle
    }
    return graph.keys.any(::visit)
}

private fun LinkRuleDraft.toEntity(uuid: String, createdAtMillis: Long) = LinkRuleEntity(
    uuid = uuid, name = name.trim(), kind = kind.name, sourceType = sourceType.name,
    sourceEntityId = sourceEntityId, sourceMetricId = sourceMetricId, sourceItemId = sourceItemId,
    sourceMetric = sourceMetric.name, targetGoalId = targetGoalId, targetMilestoneId = targetMilestoneId,
    valueMode = valueMode.name, fixedValue = fixedValue, multiplier = multiplier, offset = offset,
    retroactiveFromEpochDay = retroactiveFrom?.toEpochDay(), enabled = enabled,
    createdAtMillis = createdAtMillis, updatedAtMillis = createdAtMillis,
    trackAggregation = trackAggregation?.name,
    sourceFieldId = sourceFieldId,
    conditionMode = conditionMode.name,
)

private fun LinkRuleEntity.toDomain(conditions: List<TrackCondition> = emptyList()) = LinkRule(
    id = id,
    uuid = uuid,
    name = name,
    kind = LinkKind.valueOf(kind),
    sourceType = LinkSourceType.valueOf(sourceType),
    sourceEntityId = sourceEntityId,
    sourceMetricId = sourceMetricId,
    sourceItemId = sourceItemId,
    sourceMetric = LinkSourceMetric.valueOf(sourceMetric),
    targetGoalId = targetGoalId,
    targetMilestoneId = targetMilestoneId,
    valueMode = LinkValueMode.valueOf(valueMode),
    fixedValue = fixedValue,
    multiplier = multiplier,
    offset = offset,
    retroactiveFrom = retroactiveFromEpochDay?.let(LocalDate::ofEpochDay),
    enabled = enabled,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis,
    trackAggregation = trackAggregation?.let(TrackAggregation::valueOf),
    sourceFieldId = sourceFieldId,
    conditionMode = TrackConditionMode.valueOf(conditionMode),
    conditions = conditions,
)

private fun ContributionEntity.toDomain() = Contribution(id, uuid, linkRuleId, sourceEventId, LinkSourceType.valueOf(sourceType),
    sourceEntityId, targetGoalId, metricEntryId, canonicalValue, LocalDate.ofEpochDay(localEpochDay),
    Instant.ofEpochMilli(timestampMillis), excluded, overrideValue, explanation, createdAtMillis, updatedAtMillis)

private fun TriggerRuleDraft.toEntity(uuid: String, createdAtMillis: Long) = TriggerRuleEntity(
    uuid = uuid, name = name.trim(), sourceType = sourceType.name, sourceEntityId = sourceEntityId,
    sourceItemId = sourceItemId,
    outcome = outcome.name, targetType = targetType.name, targetEntityId = targetEntityId,
    delayMinutes = delayMinutes, quietStartMinutes = quietStartMinutes, quietEndMinutes = quietEndMinutes,
    action = action.name, notificationEnabled = notificationEnabled, conditionMode = conditionMode.name,
    enabled = enabled, createdAtMillis = createdAtMillis, updatedAtMillis = createdAtMillis,
)

private fun TriggerRuleEntity.toDomain(
    conditions: List<TrackCondition> = emptyList(),
    mappings: List<TriggerFieldMapping> = emptyList(),
) = TriggerRule(
    id = id,
    uuid = uuid,
    name = name,
    sourceType = LinkSourceType.valueOf(sourceType),
    sourceEntityId = sourceEntityId,
    sourceItemId = sourceItemId,
    outcome = TriggerOutcome.valueOf(outcome),
    targetType = TriggerTargetType.valueOf(targetType),
    targetEntityId = targetEntityId,
    delayMinutes = delayMinutes,
    quietStartMinutes = quietStartMinutes,
    quietEndMinutes = quietEndMinutes,
    action = TriggerAction.valueOf(action),
    notificationEnabled = notificationEnabled,
    enabled = enabled,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis,
    conditionMode = TrackConditionMode.valueOf(conditionMode),
    conditions = conditions,
    mappings = mappings,
)

private fun TriggerOccurrenceEntity.toDomain() = TriggerOccurrence(id, triggerRuleId, sourceEventId,
    Instant.ofEpochMilli(availableAtMillis), deliveredAtMillis?.let(Instant::ofEpochMilli), dismissedAtMillis?.let(Instant::ofEpochMilli),
    remindAtMillis?.let(Instant::ofEpochMilli), fulfilledEntryId, sourceSnapshot)

private fun TrackCondition.toLinkEntity(ruleId: Long, fieldId: Long?, position: Int) = LinkRuleConditionEntity(
    linkRuleId = ruleId,
    fieldId = fieldId,
    entryDate = fieldUuid == TRACK_ENTRY_DATE_CONDITION_UUID,
    operator = operator.name,
    position = position,
    textValue = textValue,
    numberValue = numberValue,
    secondNumberValue = secondNumberValue,
    dateEpochDay = dateValue?.toEpochDay(),
    secondDateEpochDay = secondDateValue?.toEpochDay(),
)

private fun TrackCondition.toTriggerEntity(ruleId: Long, fieldId: Long?, position: Int) = TriggerRuleConditionEntity(
    triggerRuleId = ruleId,
    fieldId = fieldId,
    entryDate = fieldUuid == TRACK_ENTRY_DATE_CONDITION_UUID,
    operator = operator.name,
    position = position,
    textValue = textValue,
    numberValue = numberValue,
    secondNumberValue = secondNumberValue,
    dateEpochDay = dateValue?.toEpochDay(),
    secondDateEpochDay = secondDateValue?.toEpochDay(),
)

private fun LinkRuleConditionEntity.toDomain(fieldUuidById: Map<Long, String>, choiceOptionUuids: Set<String>): TrackCondition? =
    (if (entryDate) TRACK_ENTRY_DATE_CONDITION_UUID else fieldId?.let(fieldUuidById::get))
        ?.let { fieldUuid -> conditionDomain(fieldUuid, operator, textValue, numberValue, secondNumberValue, choiceOptionUuids, dateEpochDay, secondDateEpochDay) }

private fun TriggerRuleConditionEntity.toDomain(fieldUuidById: Map<Long, String>, choiceOptionUuids: Set<String>): TrackCondition? =
    (if (entryDate) TRACK_ENTRY_DATE_CONDITION_UUID else fieldId?.let(fieldUuidById::get))
        ?.let { fieldUuid -> conditionDomain(fieldUuid, operator, textValue, numberValue, secondNumberValue, choiceOptionUuids, dateEpochDay, secondDateEpochDay) }

private fun conditionDomain(
    fieldUuid: String,
    operator: String,
    textValue: String?,
    numberValue: Double?,
    secondNumberValue: Double?,
    choiceOptionUuids: Set<String>,
    dateEpochDay: Long?,
    secondDateEpochDay: Long?,
) = TrackCondition(
    fieldUuid = fieldUuid,
    operator = TrackConditionOperator.valueOf(operator),
    textValue = textValue,
    numberValue = numberValue,
    secondNumberValue = secondNumberValue,
    choiceOptionUuids = choiceOptionUuids,
    dateValue = dateEpochDay?.let(LocalDate::ofEpochDay),
    secondDateValue = secondDateEpochDay?.let(LocalDate::ofEpochDay),
)

private fun TriggerFieldMapping.toEntity(ruleId: Long, constantChoiceOptionId: Long?) = TriggerFieldMappingEntity(
    triggerRuleId = ruleId,
    targetFieldId = targetFieldId,
    sourceProperty = sourceProperty.name,
    constantText = constantValue?.textValue,
    constantNumber = constantValue?.enteredNumber,
    constantUnitId = constantValue?.enteredUnitId,
    constantDateEpochDay = constantValue?.dateValue?.toEpochDay(),
    constantBoolean = constantValue?.booleanValue,
    constantChoiceOptionId = constantChoiceOptionId,
    constantScale = constantValue?.scaleValue,
)

private fun TriggerFieldMappingEntity.toDomain(optionUuidById: Map<Long, String>) = TriggerFieldMapping(
    targetFieldId = targetFieldId,
    sourceProperty = TriggerSourceProperty.valueOf(sourceProperty),
    constantValue = TrackValueDraft(
        textValue = constantText,
        enteredNumber = constantNumber,
        enteredUnitId = constantUnitId,
        dateValue = constantDateEpochDay?.let(LocalDate::ofEpochDay),
        booleanValue = constantBoolean,
        choiceOptionUuid = constantChoiceOptionId?.let(optionUuidById::get),
        scaleValue = constantScale,
    ).takeIf { sourceProperty == TriggerSourceProperty.Constant.name },
)
