package com.whip.app

import android.app.Activity
import android.os.Bundle
import android.widget.TextView
import com.whip.app.domain.MissedOccurrencePolicy
import com.whip.app.domain.RecurrenceAnchor
import com.whip.app.domain.RepeatStepPolicy
import com.whip.app.domain.ScheduleKind
import com.whip.app.domain.TaskEffort
import com.whip.app.domain.TaskPriority
import com.whip.app.domain.TaskProgressDisplay
import java.time.LocalDate
import kotlinx.coroutines.runBlocking

/** Benchmark-build-only deterministic fixture loader. It is absent from debug and release APKs. */
class BenchmarkDataActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val status = TextView(this).apply { text = "Seeding benchmark data…"; textSize = 22f; setPadding(48, 48, 48, 48) }
        setContentView(status)
        Thread {
            val mode = intent.getStringExtra("mode") ?: "home"
            runCatching { seed(mode) }
                .onSuccess {
                    writeSeedStatus("ready:$mode")
                    runOnUiThread { status.text = "Seed ready: $mode" }
                }
                .onFailure { error ->
                    writeSeedStatus("failed:$mode:${error.message.orEmpty()}")
                    runOnUiThread { status.text = "Seed failed: ${error.message}" }
                }
        }.start()
    }

    private fun writeSeedStatus(value: String) {
        externalMediaDirs.firstOrNull()?.let { directory ->
            directory.mkdirs()
            directory.resolve(SEED_STATUS_FILE).writeText(value)
        }
    }

    private fun seed(mode: String) {
        val app = application as WhipApplication
        app.settingsRepository.update {
            it.copy(setupCompleted = true, hiddenHomeSections = emptySet(), collapsedHomeSections = emptySet())
        }
        runBlocking { app.database.clearAllTables() }
        val db = app.database.openHelper.writableDatabase
        val now = System.currentTimeMillis()
        val today = LocalDate.now().toEpochDay()
        db.beginTransaction()
        try {
            if (mode == "home") seedDenseHome(db, now, today)
            if (mode == "graphs") seedLargeGraphs(db, now, today)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun seedDenseHome(db: androidx.sqlite.db.SupportSQLiteDatabase, now: Long, today: Long) {
        db.execSQL(
            "$FIVE_DIGIT_SEQUENCE INSERT INTO tasks (uuid,title,notes,scheduleKind,dateEpochDay,recurrenceUnit,recurrenceInterval,weekdaysMask,recurrenceEnd,recurrenceEndEpochDay,recurrenceCount,timeMinutes,reminderEnabled,archived,completedAtMillis,createdAtMillis,updatedAtMillis,showSubtaskProgress,progressDisplay,autoCompleteFromSteps,repeatStepPolicy,pinned,priority,area,tagsCsv,deadlineEpochDay,recurrenceAnchor,reminderOffsetsMinutesCsv,missedOccurrencePolicy,inbox,durationMinutes,effort,manualPosition) " +
                "SELECT 'benchmark-task-'||x,'Benchmark task '||x,'','${ScheduleKind.Once.name}',$today,NULL,1,0,NULL,NULL,NULL,NULL,0,0,NULL,$now,$now,0,'${TaskProgressDisplay.Percent.name}',0,'${RepeatStepPolicy.Reset.name}',0,'${TaskPriority.Medium.name}','Benchmark','dense',NULL,'${RecurrenceAnchor.Schedule.name}','','${MissedOccurrencePolicy.KeepLatest.name}',0,30,'${TaskEffort.Moderate.name}',x FROM seq WHERE x < 10000",
        )
        db.execSQL("INSERT INTO metric_definitions VALUES ('benchmark-habit-metric','Benchmark check-ins','Integer','Count','count',0,1,0,$now,$now)")
        db.execSQL(
            "INSERT INTO habits (id,uuid,metricId,name,notes,area,tagsCsv,icon,trackingMode,dimension,unitId,precision,comparison,targetMin,targetMax,targetPeriod,rollingDays,scheduleType,scheduleInterval,weekdaysMask,flexibleTimesPerWeek,startEpochDay,endType,endEpochDay,endValue,quickIncrement,quickActionsCsv,reminderMinutesCsv,weekdayReminderMinutesCsv,weekStart,timerStartedAtMillis,pinned,position,archived,paused,createdAtMillis,updatedAtMillis,sourceMetricId) VALUES (1,'benchmark-habit','benchmark-habit-metric','Benchmark habit','','Benchmark','dense','✓','Count','Count','count',0,'AtLeast',10000,NULL,'Day',NULL,'Daily',1,0,NULL,$today,'Never',NULL,NULL,1,'','','','MONDAY',NULL,0,0,0,0,$now,$now,NULL)",
        )
        db.execSQL(
            "$FIVE_DIGIT_SEQUENCE INSERT INTO metric_entries (id,metricId,canonicalValue,enteredValue,enteredUnitId,status,timestampMillis,localEpochDay,zoneId,offsetSeconds,sourceType,sourceId,note,createdAtMillis,updatedAtMillis) " +
                "SELECT 'benchmark-habit-entry-'||x,'benchmark-habit-metric',1,1,'count','Recorded',$now+x,$today,'UTC',0,'Habit','benchmark-habit-log-'||x,'',$now,$now FROM seq WHERE x < 10000",
        )
        db.execSQL(
            "$FIVE_DIGIT_SEQUENCE INSERT INTO habit_logs (uuid,habitId,value,canonicalValue,enteredUnitId,status,timestampMillis,localEpochDay,zoneId,offsetSeconds,note,sourceType,sourceId,metricEntryId,createdAtMillis,updatedAtMillis) " +
                "SELECT 'benchmark-habit-log-'||x,1,1,1,'count','Recorded',$now+x,$today,'UTC',0,'','Habit','benchmark-habit-log-'||x,'benchmark-habit-entry-'||x,$now,$now FROM seq WHERE x < 10000",
        )
    }

    private fun seedLargeGraphs(db: androidx.sqlite.db.SupportSQLiteDatabase, now: Long, today: Long) {
        db.execSQL("INSERT INTO metric_definitions VALUES ('benchmark-goal-metric','Benchmark trend','Decimal','Unitless','unitless',1,1,0,$now,$now)")
        db.execSQL("INSERT INTO goals (id,uuid,metricId,name,description,area,tagsCsv,icon,type,dimension,unitId,precision,baseline,targetMin,targetMax,direction,startEpochDay,deadlineEpochDay,aggregation,aggregationPeriod,rollingDays,paceType,consistencyPeriod,consistencyRequiredPeriods,reminderMinutes,status,pinned,position,createdAtMillis,updatedAtMillis) VALUES (1,'benchmark-goal','benchmark-goal-metric','100k point goal','','Benchmark','dense','◎','OpenEndedTrend','Unitless','unitless',1,NULL,NULL,NULL,'Neutral',$today,NULL,'Latest','All',NULL,'None','Week',NULL,NULL,'Active',0,0,$now,$now)")
        db.execSQL(
            "$FIVE_DIGIT_SEQUENCE INSERT INTO metric_entries (id,metricId,canonicalValue,enteredValue,enteredUnitId,status,timestampMillis,localEpochDay,zoneId,offsetSeconds,sourceType,sourceId,note,createdAtMillis,updatedAtMillis) " +
                "SELECT 'benchmark-goal-entry-'||x,'benchmark-goal-metric',x%1000,x%1000,'unitless','Recorded',$now-(x*60000),$today-(x/10),'UTC',0,'Manual',NULL,'',$now,$now FROM seq WHERE x < 100000",
        )
        db.execSQL("INSERT INTO exercises (id,uuid,name,trackingType,notes,equipment,primaryMuscles,secondaryMuscles,weightUnitId,weightIncrement,repetitionIncrement,defaultRestSeconds,defaultGraphMetric,oneRepMaxFormula,barWeightKg,availablePlatesKgCsv,includeInVolume,includeInPersonalRecords,bodyweightLoadPolicy,effectiveBodyweightPercent,showRpe,showRir,showTempo,favorite,position,archived,createdAtMillis,updatedAtMillis,loadInterpretation) VALUES (1,'benchmark-exercise','Benchmark press','WeightReps','','','','','kilogram',2.5,1,120,'EstimatedOneRepMax','Epley',20,'',1,1,'ExternalWeightOnly',100,NULL,NULL,NULL,0,0,0,$now,$now,'TotalSystem')")
        db.execSQL("INSERT INTO workout_sessions (id,uuid,name,notes,startedAtMillis,endedAtMillis,localEpochDay,zoneId,state,keepScreenAwake,restTimerDeadlineMillis,restTimerDurationSeconds,archived,createdAtMillis,updatedAtMillis,sourceRoutineId) VALUES (1,'benchmark-finished','Large history','',$now-3600000,$now,$today,'UTC','Finished',0,NULL,NULL,0,$now,$now,NULL),(2,'benchmark-active','Input latency','',$now,NULL,$today,'UTC','Active',0,NULL,NULL,0,$now,$now,NULL)")
        val placementValues = "'','','','','TotalSystem',NULL,'WeightReps','ExternalWeightOnly',100,'Epley',1,1,'kilogram',1,'',1,'',1,'Single',NULL,''"
        db.execSQL("INSERT INTO workout_exercises (id,uuid,sessionId,exerciseId,position,notes,groupId,createdAtMillis,updatedAtMillis,machineId,machineNameSnapshot,machineLoadTypeSnapshot,machineUnitIdSnapshot,machineLevelLabelSnapshot,loadInterpretationSnapshot,baseLoadKgSnapshot,trackingTypeSnapshot,bodyweightLoadPolicySnapshot,effectiveBodyweightPercentSnapshot,oneRepMaxFormulaSnapshot,includeInVolumeSnapshot,includeInPersonalRecordsSnapshot,exerciseWeightUnitSnapshot,loadMultiplierSnapshot,machineConfigurationGroupSnapshot,machineConfigurationVersionSnapshot,machineConfigurationSnapshot,machinePulleyRatioSnapshot,machineStackModeSnapshot,machineAddOnPlateKgSnapshot,machineMassMappingCsvSnapshot,alternativeExerciseIdsCsvSnapshot) VALUES (1,'benchmark-placement',1,1,0,'',NULL,$now,$now,NULL,$placementValues,''),(2,'benchmark-active-placement',2,1,0,'',NULL,$now,$now,NULL,$placementValues,'')")
        db.execSQL(
            "$FIVE_DIGIT_SEQUENCE INSERT INTO workout_sets (uuid,workoutExerciseId,position,classification,planned,completed,canonicalWeightKg,enteredWeight,enteredWeightUnitId,repetitions,canonicalDistanceMetres,enteredDistance,enteredDistanceUnitId,durationSeconds,bodyweightKg,note,rpe,rir,tempo,restSeconds,completedAtMillis,deletedAtMillis,createdAtMillis,updatedAtMillis,machineLoadValue,unilateral,prescribedCanonicalWeightKg,prescribedEnteredWeight,prescribedWeightUnitId,prescribedRepetitions,prescribedRpe,prescribedRir,prescribedDurationSeconds,prescribedMachineLoadValue,prescribedRepetitionsMax,prescriptionSourceLabel) " +
                "SELECT 'benchmark-set-'||x,1,x,'Working',0,1,50+(x%100),50+(x%100),'kilogram',5,NULL,NULL,NULL,NULL,NULL,'',NULL,NULL,'',NULL,$now,NULL,$now,$now,NULL,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'' FROM seq WHERE x < 100000",
        )
        db.execSQL("INSERT INTO workout_sets (uuid,workoutExerciseId,position,classification,planned,completed,canonicalWeightKg,enteredWeight,enteredWeightUnitId,repetitions,note,tempo,createdAtMillis,updatedAtMillis,unilateral,prescriptionSourceLabel) VALUES ('benchmark-active-set',2,0,'Working',0,0,60,60,'kilogram',5,'','',$now,$now,0,'')")
    }

    private companion object {
        const val SEED_STATUS_FILE = "benchmark-seed-status.txt"
        const val FIVE_DIGIT_SEQUENCE = "WITH digits(n) AS (VALUES(0),(1),(2),(3),(4),(5),(6),(7),(8),(9)), seq(x) AS (SELECT a.n+10*b.n+100*c.n+1000*d.n+10000*e.n FROM digits a CROSS JOIN digits b CROSS JOIN digits c CROSS JOIN digits d CROSS JOIN digits e)"
    }
}
