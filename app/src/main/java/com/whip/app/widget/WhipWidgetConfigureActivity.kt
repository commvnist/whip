package com.whip.app.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.whip.app.WhipApplication
import com.whip.app.domain.AreaScope
import com.whip.app.ui.AreaSelectionDropdown
import com.whip.app.ui.theme.WhipTheme

class WhipWidgetConfigureActivity : ComponentActivity() {
    private var widgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(Activity.RESULT_CANCELED)
        widgetId = intent?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        val app = application as WhipApplication
        setContent {
            val areas by app.areaRepository.areas.collectAsStateWithLifecycle(initialValue = emptyList())
            var selectedAreaId by rememberSaveable { mutableStateOf<String?>(null) }
            WhipTheme(dynamicColor = false) {
                Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text("Configure Whip widget", style = MaterialTheme.typography.headlineSmall)
                    Text("Choose which area this widget summarizes. New tasks and habits opened from it inherit the same view.")
                    AreaSelectionDropdown(
                        areas = areas.filterNot { it.archived },
                        selectedAreaId = selectedAreaId,
                        selectedAreaName = if (selectedAreaId == null) "All areas" else "",
                        onSelect = { id, _ -> selectedAreaId = id },
                        nullLabel = "All areas",
                    )
                    Text("Choose “All areas” by leaving the widget unscoped.", style = MaterialTheme.typography.bodySmall)
                    Button(onClick = {
                        WhipWidgetProvider.saveScope(this@WhipWidgetConfigureActivity, widgetId, selectedAreaId?.let { AreaScope.One(it) } ?: AreaScope.All)
                        WhipWidgetProvider.update(this@WhipWidgetConfigureActivity, widgetId)
                        setResult(Activity.RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
                        finish()
                    }, modifier = Modifier.fillMaxWidth()) { Text("Add widget") }
                    OutlinedButton(onClick = ::finish, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
                }
            }
        }
    }
}
