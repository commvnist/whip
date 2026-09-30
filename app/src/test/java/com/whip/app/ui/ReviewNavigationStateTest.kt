package com.whip.app.ui

import androidx.compose.runtime.mutableStateOf
import com.whip.app.core.ReviewSection
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class ReviewNavigationStateTest {
    private fun navigation() = ReviewNavigationState(
        mutableStateOf(AppDestination.Home),
        mutableStateOf(TaskDestination.All),
        mutableStateOf(null), mutableStateOf(null),
        mutableStateOf(null), mutableStateOf(null),
        mutableStateOf(null), mutableStateOf(null), mutableStateOf(null),
    )

    @Test fun habitOutcomeRoutesToItsExactHistoricalDate() {
        val navigation = navigation()
        val date = LocalDate.of(2026, 9, 21)
        navigation.open(ReviewOutcome(ReviewSection.Habits, 7L, "7", "Weekly habit", date, 1.0), TaskUiState())
        assertEquals(AppDestination.Habits, navigation.destination.value)
        assertEquals(7L, navigation.habit.value)
        assertEquals(date.toEpochDay(), navigation.habitEpochDay.value)
    }

    @Test fun workoutOutcomeKeepsTheExactSessionRequest() {
        val navigation = navigation()
        navigation.open(ReviewOutcome(ReviewSection.Gym, 9L, "9", "Earlier workout", LocalDate.of(2026, 9, 21), 1.0), TaskUiState())
        assertEquals(AppDestination.Gym, navigation.destination.value)
        assertEquals(SearchDomain.Workout, navigation.gymDomain.value)
        assertEquals(9L, navigation.gymId.value)
    }

    @Test fun completedTaskOutcomeKeepsItsOccurrenceKey() {
        val navigation = navigation()
        navigation.open(ReviewOutcome(ReviewSection.Tasks, 8L, "8:20600", "Recurring task", LocalDate.of(2026, 9, 21), 1.0), TaskUiState())
        assertEquals(AppDestination.Tasks, navigation.destination.value)
        assertEquals(TaskDestination.Completed, navigation.taskDestination.value)
        assertEquals("8:20600", navigation.completedTask.value)
    }
}
