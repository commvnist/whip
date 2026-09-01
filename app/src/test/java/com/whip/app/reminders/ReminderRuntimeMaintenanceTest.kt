package com.whip.app.reminders

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderRuntimeMaintenanceTest {
    @Test
    fun missingVersionCancelsAndAwaitsEachDomainBeforeRecordingVersion() = runBlocking {
        val events = mutableListOf<String>()
        val store = FakeVersionStore()
        val maintenance = maintenance(store, events, currentVersion = 3)

        assertTrue(maintenance.upgradeDeliveryClaimsIfRequired())

        assertEquals(listOf("cancel", "tasks", "habits", "goals", "write:3"), events)
        assertEquals(3, store.version)
    }

    @Test
    fun olderVersionUpgradesButCurrentAndFutureVersionsAreIdempotent() = runBlocking {
        val oldStore = FakeVersionStore(version = 1)
        val oldEvents = mutableListOf<String>()
        assertTrue(maintenance(oldStore, oldEvents, currentVersion = 3).upgradeDeliveryClaimsIfRequired())
        assertEquals(3, oldStore.version)

        listOf(3, 4).forEach { existingVersion ->
            val store = FakeVersionStore(version = existingVersion)
            val events = mutableListOf<String>()
            assertFalse(maintenance(store, events, currentVersion = 3).upgradeDeliveryClaimsIfRequired())
            assertTrue(events.isEmpty())
            assertEquals(existingVersion, store.version)
        }
    }

    @Test
    fun failedDomainStopsLaterDomainsAndDoesNotRecordVersion() = runBlocking {
        val store = FakeVersionStore()
        val events = mutableListOf<String>()
        var habitFails = true
        val maintenance = maintenance(
            store = store,
            events = events,
            currentVersion = 3,
            syncHabits = {
                events += "habits"
                if (habitFails) error("habit sync failed")
            },
        )

        assertFailure { maintenance.upgradeDeliveryClaimsIfRequired() }
        assertEquals(listOf("cancel", "tasks", "habits"), events)
        assertEquals(-1, store.version)

        habitFails = false
        assertTrue(maintenance.upgradeDeliveryClaimsIfRequired())
        assertEquals(
            listOf(
                "cancel", "tasks", "habits",
                "cancel", "tasks", "habits", "goals", "write:3",
            ),
            events,
        )
        assertEquals(3, store.version)
    }

    @Test
    fun failedNotificationCancellationDoesNotTouchQueuesOrRecordVersion() = runBlocking {
        val store = FakeVersionStore()
        val events = mutableListOf<String>()
        val maintenance = ReminderRuntimeMaintenance(
            versionStore = store,
            currentClaimVersion = 3,
            cancelVisibleLegacyReminders = {
                events += "cancel"
                error("platform failure")
            },
            syncTaskReminders = { events += "tasks" },
            syncHabitReminders = { events += "habits" },
            syncGoalReminders = { events += "goals" },
            refreshWidgets = { events += "widgets" },
        )

        assertFailure { maintenance.upgradeDeliveryClaimsIfRequired() }
        assertEquals(listOf("cancel"), events)
        assertEquals(-1, store.version)
    }

    @Test
    fun failedDurableWriteLeavesUpgradePendingAfterEverySyncSucceeded() = runBlocking {
        val store = FakeVersionStore(allowWrite = false)
        val events = mutableListOf<String>()

        assertFailure { maintenance(store, events, currentVersion = 3).upgradeDeliveryClaimsIfRequired() }

        assertEquals(listOf("cancel", "tasks", "habits", "goals", "write:3"), events)
        assertEquals(-1, store.version)
    }

    @Test
    fun deviceTimeZoneChangeOnlyInvalidatesFollowDeviceSchedules() = runBlocking {
        val fixedEvents = mutableListOf<String>()
        val fixed = maintenance(FakeVersionStore(3), fixedEvents, currentVersion = 3)
        val fixedPlan = fixed.handleSystemTimeInvalidation(
            action = ACTION_DEVICE_TIME_ZONE_CHANGED,
            followsDeviceTimeZone = false,
        )
        assertEquals(ReminderTimeInvalidationPlan(false, false), fixedPlan)
        assertTrue(fixedEvents.isEmpty())

        val followEvents = mutableListOf<String>()
        val follow = maintenance(FakeVersionStore(3), followEvents, currentVersion = 3)
        val followPlan = follow.handleSystemTimeInvalidation(
            action = ACTION_DEVICE_TIME_ZONE_CHANGED,
            followsDeviceTimeZone = true,
        )
        assertEquals(ReminderTimeInvalidationPlan(true, true), followPlan)
        assertEquals(listOf("tasks", "habits", "goals", "widgets"), followEvents)
    }

    @Test
    fun wallClockChangeInvalidatesFixedAndFollowDeviceSchedules() = runBlocking {
        listOf(false, true).forEach { followsDevice ->
            val events = mutableListOf<String>()
            val plan = maintenance(FakeVersionStore(3), events, currentVersion = 3)
                .handleSystemTimeInvalidation(ACTION_DEVICE_TIME_CHANGED, followsDevice)

            assertEquals(ReminderTimeInvalidationPlan(true, true), plan)
            assertEquals(listOf("tasks", "habits", "goals", "widgets"), events)
        }
    }

    @Test
    fun dateChangeRefreshesEveryWidgetButOnlyResyncsFollowDeviceReminders() = runBlocking {
        val fixedEvents = mutableListOf<String>()
        val fixedPlan = maintenance(FakeVersionStore(3), fixedEvents, currentVersion = 3)
            .handleSystemTimeInvalidation(ACTION_DEVICE_DATE_CHANGED, false)
        assertEquals(ReminderTimeInvalidationPlan(false, true), fixedPlan)
        assertEquals(listOf("widgets"), fixedEvents)

        val followEvents = mutableListOf<String>()
        val followPlan = maintenance(FakeVersionStore(3), followEvents, currentVersion = 3)
            .handleSystemTimeInvalidation(ACTION_DEVICE_DATE_CHANGED, true)
        assertEquals(ReminderTimeInvalidationPlan(true, true), followPlan)
        assertEquals(listOf("tasks", "habits", "goals", "widgets"), followEvents)
    }

    @Test
    fun startupUpgradeAndTimeInvalidationCannotInterleave() = runBlocking {
        val events = mutableListOf<String>()
        val taskSyncStarted = CompletableDeferred<Unit>()
        val releaseTaskSync = CompletableDeferred<Unit>()
        val store = FakeVersionStore().onWrite { version -> events += "write:$version" }
        val maintenance = ReminderRuntimeMaintenance(
            versionStore = store,
            currentClaimVersion = 3,
            cancelVisibleLegacyReminders = { events += "cancel" },
            syncTaskReminders = {
                events += "tasks"
                if (!taskSyncStarted.isCompleted) {
                    taskSyncStarted.complete(Unit)
                    releaseTaskSync.await()
                }
            },
            syncHabitReminders = { events += "habits" },
            syncGoalReminders = { events += "goals" },
            refreshWidgets = { events += "widgets" },
        )

        val startup = async { maintenance.upgradeDeliveryClaimsIfRequired() }
        taskSyncStarted.await()
        val timeChange = async {
            maintenance.handleSystemTimeInvalidation(ACTION_DEVICE_TIME_CHANGED, false)
        }
        yield()
        assertEquals(listOf("cancel", "tasks"), events)

        releaseTaskSync.complete(Unit)
        assertTrue(startup.await())
        assertEquals(ReminderTimeInvalidationPlan(true, true), timeChange.await())
        assertEquals(
            listOf(
                "cancel", "tasks", "habits", "goals", "write:3",
                "tasks", "habits", "goals", "widgets",
            ),
            events,
        )
    }

    @Test
    fun unknownBroadcastActionIsIgnored() = runBlocking {
        val events = mutableListOf<String>()
        val plan = maintenance(FakeVersionStore(3), events, currentVersion = 3)
            .handleSystemTimeInvalidation("example.invalid", true)

        assertEquals(ReminderTimeInvalidationPlan(false, false), plan)
        assertTrue(events.isEmpty())
    }

    private fun maintenance(
        store: FakeVersionStore,
        events: MutableList<String>,
        currentVersion: Int,
        syncHabits: suspend () -> Unit = { events += "habits" },
    ) = ReminderRuntimeMaintenance(
        versionStore = store.onWrite { version -> events += "write:$version" },
        currentClaimVersion = currentVersion,
        cancelVisibleLegacyReminders = { events += "cancel" },
        syncTaskReminders = { events += "tasks" },
        syncHabitReminders = syncHabits,
        syncGoalReminders = { events += "goals" },
        refreshWidgets = { events += "widgets" },
    )

    private suspend fun assertFailure(block: suspend () -> Unit) {
        var failed = false
        try {
            block()
        } catch (_: Throwable) {
            failed = true
        }
        assertTrue("Expected operation to fail", failed)
    }

    private class FakeVersionStore(
        var version: Int = -1,
        private val allowWrite: Boolean = true,
    ) : ReminderClaimVersionStore {
        private var writeObserver: (Int) -> Unit = {}

        override fun read(): Int = version

        override fun write(version: Int): Boolean {
            writeObserver(version)
            if (allowWrite) this.version = version
            return allowWrite
        }

        fun onWrite(observer: (Int) -> Unit): FakeVersionStore = apply {
            writeObserver = observer
        }
    }
}
