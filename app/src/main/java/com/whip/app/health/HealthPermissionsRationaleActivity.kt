package com.whip.app.health

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.whip.app.ui.WhipButton
import com.whip.app.ui.theme.WhipTheme

class HealthPermissionsRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WhipTheme {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text("Health Connect and Whip", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        "Whip only reads the health categories you select. Imported values are stored " +
                            "locally in Whip's measurement ledger so they can contribute to goals and " +
                            "insights. Whip does not sell or upload this data, and it never writes to " +
                            "Health Connect.",
                    )
                    Text("You can revoke access in Health Connect or turn syncing off in Whip Settings at any time.")
                    WhipButton(onClick = ::finish) { Text("Close") }
                }
            }
        }
    }
}
