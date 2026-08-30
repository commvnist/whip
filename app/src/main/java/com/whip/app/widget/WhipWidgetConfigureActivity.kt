package com.whip.app.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.whip.app.R
import com.whip.app.WhipApplication
import com.whip.app.core.AppThemeMode
import com.whip.app.domain.AreaScope
import com.whip.app.domain.Habit
import com.whip.app.domain.matches
import com.whip.app.ui.AreaSelectionDropdown
import com.whip.app.ui.SegmentedChoiceBar
import com.whip.app.ui.WhipButton
import com.whip.app.ui.WhipContentWidth
import com.whip.app.ui.WhipFilterChip
import com.whip.app.ui.WhipFullScreenSurface
import com.whip.app.ui.WhipPageContentPadding
import com.whip.app.ui.WhipPageHeader
import com.whip.app.ui.WhipSection
import com.whip.app.ui.WhipSettingsRow
import com.whip.app.ui.WhipSpacing
import com.whip.app.ui.WhipTextButton
import com.whip.app.ui.WhipTrailingCloseAction
import com.whip.app.ui.theme.WhipTheme
import kotlin.math.abs

private enum class HabitSelectionMode { AllMatching, ChooseHabits }

private val TransparencyOptions = listOf(0, 20, 40, 60, MAX_WIDGET_TRANSPARENCY)

class WhipWidgetConfigureActivity : ComponentActivity() {
    private var widgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(Activity.RESULT_CANCELED)
        widgetId = intent?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        enableEdgeToEdge()

        val manager = AppWidgetManager.getInstance(this)
        val kind = if (
            manager.getAppWidgetInfo(widgetId)?.provider?.className ==
            HabitTrackingWidgetProvider::class.java.name
        ) {
            WidgetKind.HabitTracking
        } else {
            WidgetKind.TaskAgenda
        }
        val existing = WhipWidgetPreferences.load(this, widgetId)
        val app = application as WhipApplication

        setContent {
            val areas by app.areaRepository.areas.collectAsStateWithLifecycle(initialValue = emptyList())
            val habits by app.habitRepository.habits.collectAsStateWithLifecycle(initialValue = emptyList())
            val appSettings by app.settingsRepository.settings.collectAsStateWithLifecycle(
                initialValue = app.settingsRepository.current(),
            )
            val systemDarkTheme = isSystemInDarkTheme()
            val darkTheme = when (appSettings.themeMode) {
                AppThemeMode.System -> systemDarkTheme
                AppThemeMode.Light -> false
                AppThemeMode.Dark -> true
            }
            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }

            var selectedAreaId by rememberSaveable(widgetId) {
                mutableStateOf((existing.areaScope as? AreaScope.One)?.areaId)
            }
            var transparencyPercent by rememberSaveable(widgetId) {
                mutableStateOf(
                    TransparencyOptions.minBy { option ->
                        abs(option - existing.transparencyPercent)
                    },
                )
            }
            var agendaRange by rememberSaveable(widgetId) { mutableStateOf(existing.agendaRange) }
            var showCompletedHabits by rememberSaveable(widgetId) {
                mutableStateOf(existing.showCompletedHabits)
            }
            var habitSelectionMode by rememberSaveable(widgetId, "habit_selection_mode") {
                mutableStateOf(
                    if (existing.selectedHabitIds == null) {
                        HabitSelectionMode.AllMatching
                    } else {
                        HabitSelectionMode.ChooseHabits
                    },
                )
            }
            var selectedHabitIds by rememberSaveable(widgetId, "selected_habits") {
                mutableStateOf(existing.selectedHabitIds.orEmpty().toList())
            }

            val selectedScope = selectedAreaId?.let(AreaScope::One) ?: AreaScope.All
            val selectableHabits = habits
                .filter { habit -> !habit.archived && selectedScope.matches(habit.areaId) }
                .sortedWith(
                    compareByDescending<Habit> { it.pinned }
                        .thenBy { it.position }
                        .thenBy { it.name },
                )
            val selectableHabitIds = selectableHabits.mapTo(mutableSetOf()) { it.id }
            val selectedHabitCount = selectedHabitIds.count { it in selectableHabitIds }
            val selectedArea = areas.firstOrNull { it.id == selectedAreaId }
            val selectedAreaLabel = when {
                selectedAreaId == null -> "All areas"
                selectedArea != null -> selectedArea.name
                else -> "Area unavailable"
            }
            val title = if (kind == WidgetKind.TaskAgenda) "Task Agenda" else "Habit Tracking"
            val canSave = kind == WidgetKind.TaskAgenda ||
                habitSelectionMode == HabitSelectionMode.AllMatching ||
                selectedHabitCount > 0

            WhipTheme(darkTheme = darkTheme, dynamicColor = appSettings.dynamicColor) {
                WhipFullScreenSurface(title = "$title widget configuration") {
                    Column(Modifier.fillMaxSize()) {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .align(Alignment.CenterHorizontally)
                                .widthIn(max = WhipContentWidth.compactDialog)
                                .fillMaxWidth(),
                            contentPadding = WhipPageContentPadding,
                            verticalArrangement = Arrangement.spacedBy(WhipSpacing.screenExpanded),
                        ) {
                            item {
                                WhipPageHeader(
                                    title = "$title Widget",
                                    modifier = Modifier.testTag("widget-config-header"),
                                    supportingText = if (kind == WidgetKind.TaskAgenda) {
                                        "Choose which Tasks appear and how the widget looks."
                                    } else {
                                        "Choose which Habits appear and how the widget looks."
                                    },
                                    actions = {
                                        WhipTrailingCloseAction(
                                            label = "Cancel widget configuration",
                                            onClick = ::finish,
                                            modifier = Modifier.testTag("widget-config-cancel"),
                                        )
                                    },
                                )
                            }

                            item {
                                WhipSection(
                                    title = "Preview",
                                    modifier = Modifier.testTag("widget-config-preview-section"),
                                ) {
                                    WidgetConfigurationPreview(
                                        kind = kind,
                                        transparencyPercent = transparencyPercent,
                                        agendaRange = agendaRange,
                                        areaLabel = selectedAreaLabel,
                                        selectedHabitCount = selectedHabitCount,
                                        systemDarkTheme = systemDarkTheme,
                                    )
                                }
                            }

                            item {
                                WhipSection(
                                    title = "Content",
                                    supportingText = if (kind == WidgetKind.TaskAgenda) {
                                        "Choose the Area and agenda window shown on your Home screen."
                                    } else {
                                        "Choose the Area and whether completed Habits remain visible."
                                    },
                                    modifier = Modifier.testTag("widget-config-content-section"),
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(WhipSpacing.compact)) {
                                        Column(verticalArrangement = Arrangement.spacedBy(WhipSpacing.micro)) {
                                            Text("Area", style = MaterialTheme.typography.labelLarge)
                                            Text(
                                                if (kind == WidgetKind.TaskAgenda) {
                                                    "Only overdue and scheduled Tasks in this Area appear."
                                                } else {
                                                    "Only scheduled Habits in this Area can appear."
                                                },
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                        AreaSelectionDropdown(
                                            areas = areas.filter { !it.archived || it.id == selectedAreaId },
                                            selectedAreaId = selectedAreaId,
                                            selectedAreaName = if (
                                                selectedAreaId != null && selectedArea == null
                                            ) {
                                                "Area unavailable"
                                            } else {
                                                ""
                                            },
                                            onSelect = { id, _ -> selectedAreaId = id },
                                            modifier = Modifier
                                                .heightIn(min = 48.dp)
                                                .testTag("widget-config-area"),
                                            nullLabel = "All areas",
                                            allowNullSelection = true,
                                        )

                                        if (kind == WidgetKind.TaskAgenda) {
                                            Column(verticalArrangement = Arrangement.spacedBy(WhipSpacing.sibling)) {
                                                Column(verticalArrangement = Arrangement.spacedBy(WhipSpacing.micro)) {
                                                    Text("Agenda window", style = MaterialTheme.typography.labelLarge)
                                                    Text(
                                                        "Overdue Tasks are always included.",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    )
                                                }
                                                SegmentedChoiceBar(
                                                    selected = agendaRange,
                                                    choices = AgendaRange.entries,
                                                    onSelect = { agendaRange = it },
                                                    label = {
                                                        when (it) {
                                                            AgendaRange.Today -> "Today"
                                                            AgendaRange.SevenDays -> "7 Days"
                                                            AgendaRange.ThirtyDays -> "30 Days"
                                                        }
                                                    },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    testTagPrefix = "widget-config-range",
                                                )
                                            }
                                        } else {
                                            WhipSettingsRow(
                                                title = "Keep completed Habits visible",
                                                supportingText = "Shows today’s completed Habits so you can review or undo them.",
                                                checked = showCompletedHabits,
                                                onCheckedChange = { showCompletedHabits = it },
                                                modifier = Modifier.testTag("widget-config-show-completed"),
                                            )
                                        }
                                    }
                                }
                            }

                            if (kind == WidgetKind.HabitTracking) {
                                item {
                                    WhipSection(
                                        title = "Habits",
                                        supportingText = "Choose which active Habits can appear when they are scheduled.",
                                        modifier = Modifier.testTag("widget-config-habits-section"),
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(WhipSpacing.compact)) {
                                            SegmentedChoiceBar(
                                                selected = habitSelectionMode,
                                                choices = HabitSelectionMode.entries,
                                                onSelect = { selected ->
                                                    habitSelectionMode = selected
                                                    if (
                                                        selected == HabitSelectionMode.ChooseHabits &&
                                                        selectedHabitIds.isEmpty()
                                                    ) {
                                                        selectedHabitIds = selectableHabits.map { it.id }
                                                    }
                                                },
                                                label = {
                                                    when (it) {
                                                        HabitSelectionMode.AllMatching -> "All matching"
                                                        HabitSelectionMode.ChooseHabits -> "Choose Habits"
                                                    }
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                testTagPrefix = "widget-config-habit-mode",
                                            )

                                            if (habitSelectionMode == HabitSelectionMode.AllMatching) {
                                                Text(
                                                    "Active Habits are included automatically when they are scheduled.",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            } else {
                                                HabitSelectionList(
                                                    habits = selectableHabits,
                                                    selectedHabitIds = selectedHabitIds.toSet(),
                                                    onSelectionChange = { selectedHabitIds = it.toList() },
                                                )
                                                if (selectedHabitCount == 0) {
                                                    Text(
                                                        "Choose at least one Habit to save this widget.",
                                                        color = MaterialTheme.colorScheme.error,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                    )
                                                } else {
                                                    Text(
                                                        "$selectedHabitCount selected · Expand checklist Habits directly on the widget.",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            item {
                                WhipSection(
                                    title = "Appearance",
                                    supportingText = "Adjust how much of your wallpaper shows through.",
                                    modifier = Modifier.testTag("widget-config-appearance-section"),
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(WhipSpacing.compact)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text("Card transparency", style = MaterialTheme.typography.bodyLarge)
                                            Surface(
                                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                                shape = MaterialTheme.shapes.extraSmall,
                                            ) {
                                                Text(
                                                    if (transparencyPercent == 0) "Solid" else "$transparencyPercent%",
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                    style = MaterialTheme.typography.labelLarge,
                                                )
                                            }
                                        }
                                        FlowRow(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("widget-config-transparency"),
                                            horizontalArrangement = Arrangement.spacedBy(WhipSpacing.sibling),
                                            verticalArrangement = Arrangement.spacedBy(WhipSpacing.sibling),
                                        ) {
                                            TransparencyOptions.forEach { option ->
                                                WhipFilterChip(
                                                    selected = transparencyPercent == option,
                                                    onClick = { transparencyPercent = option },
                                                    label = {
                                                        Text(
                                                            when (option) {
                                                                0 -> "Solid"
                                                                else -> "$option%"
                                                            },
                                                        )
                                                    },
                                                    modifier = Modifier.testTag("widget-config-transparency-$option"),
                                                )
                                            }
                                        }
                                        Surface(
                                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                                            shape = MaterialTheme.shapes.medium,
                                        ) {
                                            Text(
                                                "Only the outer card changes. Headers and rows keep a contrast layer so text and controls stay readable.",
                                                modifier = Modifier.padding(WhipSpacing.compact),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                        ) {
                            WhipButton(
                                onClick = {
                                    val scope = selectedAreaId?.let(AreaScope::One) ?: AreaScope.All
                                    WhipWidgetPreferences.save(
                                        this@WhipWidgetConfigureActivity,
                                        widgetId,
                                        WidgetPreferences(
                                            areaScope = scope,
                                            transparencyPercent = transparencyPercent,
                                            agendaRange = agendaRange,
                                            showCompletedHabits = showCompletedHabits,
                                            selectedHabitIds = if (
                                                habitSelectionMode == HabitSelectionMode.AllMatching
                                            ) {
                                                null
                                            } else {
                                                selectedHabitIds.toSet()
                                            },
                                            expandedHabitIds = if (
                                                habitSelectionMode == HabitSelectionMode.AllMatching
                                            ) {
                                                existing.expandedHabitIds
                                            } else {
                                                existing.expandedHabitIds.intersect(selectedHabitIds.toSet())
                                            },
                                            expandedTaskKeys = existing.expandedTaskKeys,
                                        ),
                                    )
                                    WhipWidgetProvider.update(this@WhipWidgetConfigureActivity, widgetId)
                                    setResult(
                                        Activity.RESULT_OK,
                                        Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId),
                                    )
                                    finish()
                                },
                                enabled = canSave,
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .widthIn(max = WhipContentWidth.compactDialog)
                                    .fillMaxWidth()
                                    .padding(
                                        horizontal = WhipSpacing.screenCompact,
                                        vertical = WhipSpacing.compact,
                                    )
                                    .testTag("widget-config-save"),
                            ) {
                                Text("Save Widget")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HabitSelectionList(
    habits: List<Habit>,
    selectedHabitIds: Set<Long>,
    onSelectionChange: (Set<Long>) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(WhipSpacing.sibling)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WhipTextButton(
                onClick = { onSelectionChange(habits.mapTo(mutableSetOf()) { it.id }) },
                modifier = Modifier.testTag("widget-config-habits-select-all"),
            ) {
                Text("Select All")
            }
            WhipTextButton(
                onClick = { onSelectionChange(emptySet()) },
                modifier = Modifier.testTag("widget-config-habits-clear"),
            ) {
                Text("Clear")
            }
        }
        if (habits.isEmpty()) {
            Text(
                "No active Habits match this Area.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            habits.forEach { habit ->
                val checked = habit.id in selectedHabitIds
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .toggleable(
                                value = checked,
                                role = Role.Checkbox,
                                onValueChange = { selected ->
                                    onSelectionChange(
                                        if (selected) {
                                            selectedHabitIds + habit.id
                                        } else {
                                            selectedHabitIds - habit.id
                                        },
                                    )
                                },
                            )
                            .padding(horizontal = WhipSpacing.standard, vertical = WhipSpacing.sibling)
                            .testTag("widget-config-habit-${habit.id}"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(WhipSpacing.compact),
                    ) {
                        Checkbox(checked = checked, onCheckedChange = null)
                        Text(
                            "${habit.icon} ${habit.name}",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetConfigurationPreview(
    kind: WidgetKind,
    transparencyPercent: Int,
    agendaRange: AgendaRange,
    areaLabel: String,
    selectedHabitCount: Int,
    systemDarkTheme: Boolean,
) {
    val wallpaper = if (systemDarkTheme) {
        listOf(Color(0xFF27231F), Color(0xFF1E2A23), Color(0xFF2B242A))
    } else {
        listOf(Color(0xFFD8D3C8), Color(0xFFB8C4B4), Color(0xFFD6BFA8))
    }
    // The launcher selects these day/night resources for the installed widget.
    // Reading the same resources here keeps the preview from becoming a third
    // independently maintained copy of the widget palette.
    val widgetSurface = colorResource(R.color.widget_surface)
    val contentSurface = colorResource(R.color.widget_content_surface)
    val rowSurface = colorResource(R.color.widget_row_surface)
    val primaryText = colorResource(R.color.widget_primary_text)
    val secondaryText = colorResource(R.color.widget_secondary_text)
    val action = colorResource(R.color.widget_action)
    val onAction = colorResource(R.color.widget_on_action)
    val outline = colorResource(R.color.widget_outline)
    val title = if (kind == WidgetKind.TaskAgenda) "Task Agenda" else "Habit Tracking"
    val subtitle = if (kind == WidgetKind.TaskAgenda) {
        "${agendaRange.title} · $areaLabel"
    } else {
        "Today · $areaLabel"
    }
    val transparencyDescription = if (transparencyPercent == 0) {
        "solid card"
    } else {
        "$transparencyPercent percent transparent"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 150.dp)
            .clip(MaterialTheme.shapes.extraLarge)
            .background(Brush.linearGradient(wallpaper))
            .padding(WhipSpacing.compact)
            .clearAndSetSemantics {
                contentDescription = "$title preview, $subtitle, $transparencyDescription"
            }
            .testTag("widget-config-preview"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(
                    widgetSurface.copy(
                        alpha = 1f - transparencyPercent.coerceIn(
                            0,
                            MAX_WIDGET_TRANSPARENCY,
                        ) / 100f,
                    ),
                )
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(contentSurface.copy(alpha = 0.95f))
                    .padding(start = 8.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_whip_mark),
                    contentDescription = null,
                    modifier = Modifier
                        .size(32.dp)
                        .padding(4.dp),
                )
                Column(
                    Modifier
                        .weight(1f)
                        .padding(start = 6.dp),
                ) {
                    Text(
                        title,
                        color = primaryText,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                    )
                    Text(
                        subtitle,
                        color = secondaryText,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                    )
                }
                Box(
                    modifier = Modifier.size(48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(action),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Outlined.Add,
                            contentDescription = null,
                            tint = onAction,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(rowSurface.copy(alpha = 0.95f)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp, end = 4.dp, top = 7.dp, bottom = 7.dp),
                ) {
                    Text(
                        if (kind == WidgetKind.TaskAgenda) "📋 Prepare weekly review" else "🌱 Morning routine",
                        color = primaryText,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                    )
                    Text(
                        if (kind == WidgetKind.TaskAgenda) {
                            "Today · 2 subtasks left"
                        } else if (selectedHabitCount > 0) {
                            "0 of 3 items"
                        } else {
                            "Due today"
                        },
                        color = secondaryText,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                    )
                }
                Box(
                    modifier = Modifier.size(48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.CheckBoxOutlineBlank,
                        contentDescription = null,
                        tint = outline,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Box(
                    modifier = Modifier.size(48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.ExpandMore,
                        contentDescription = null,
                        tint = secondaryText,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }
    }
}
