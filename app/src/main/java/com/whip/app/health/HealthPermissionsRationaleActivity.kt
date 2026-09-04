package com.whip.app.health

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.whip.app.R
import com.whip.app.WhipApplication
import com.whip.app.ui.WhipButton
import com.whip.app.ui.WhipContentWidth
import com.whip.app.ui.WhipGroupedInformationCard
import com.whip.app.ui.WhipNoticeCard
import com.whip.app.ui.WhipNoticeTone
import com.whip.app.ui.WhipPageContentPadding
import com.whip.app.ui.WhipPageHeader
import com.whip.app.ui.WhipSpacing
import com.whip.app.ui.ExternalWhipActivityHost

class HealthPermissionsRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as WhipApplication
        setContent {
            ExternalWhipActivityHost(
                activity = this@HealthPermissionsRationaleActivity,
                app = app,
                title = stringResource(R.string.health_rationale_page_title),
            ) {
                HealthPermissionsRationaleContent(onClose = ::finish)
            }
        }
    }
}

@Composable
internal fun HealthPermissionsRationaleContent(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val pageTitle = stringResource(R.string.health_rationale_page_title)
    Column(modifier.fillMaxSize().testTag("health-rationale-surface")) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.CenterHorizontally)
                    .widthIn(max = WhipContentWidth.readable)
                    .fillMaxWidth()
                    .testTag("health-rationale-list"),
                contentPadding = WhipPageContentPadding,
                verticalArrangement = Arrangement.spacedBy(WhipSpacing.standard),
            ) {
                item {
                    WhipPageHeader(
                        title = pageTitle,
                        supportingText = stringResource(R.string.health_rationale_page_supporting),
                    )
                }
                item {
                    WhipNoticeCard(
                        title = stringResource(R.string.health_rationale_read_only_title),
                        message = stringResource(R.string.health_rationale_read_only_message),
                        tone = WhipNoticeTone.Success,
                        modifier = Modifier.testTag("health-rationale-read-only"),
                    )
                }
                item {
                    HealthRationaleSection(
                        title = stringResource(R.string.health_rationale_storage_title),
                        message = stringResource(R.string.health_rationale_storage_message),
                        modifier = Modifier.testTag("health-rationale-storage"),
                    )
                }
                item {
                    HealthRationaleSection(
                        title = stringResource(R.string.health_rationale_privacy_title),
                        message = stringResource(R.string.health_rationale_privacy_message),
                        modifier = Modifier.testTag("health-rationale-privacy"),
                    )
                }
                item {
                    WhipNoticeCard(
                        title = stringResource(R.string.health_rationale_sync_title),
                        message = stringResource(R.string.health_rationale_sync_message),
                        tone = WhipNoticeTone.Informative,
                        modifier = Modifier.testTag("health-rationale-sync-retention"),
                    )
                }
                item {
                    Text(
                        stringResource(R.string.health_rationale_settings_path),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            HorizontalDivider()
            Box(
                modifier = Modifier.fillMaxWidth().padding(
                    horizontal = WhipSpacing.screenCompact,
                    vertical = WhipSpacing.compact,
                ),
                contentAlignment = Alignment.Center,
            ) {
                WhipButton(
                    onClick = onClose,
                    modifier = Modifier
                        .widthIn(max = WhipContentWidth.compactDialog)
                        .fillMaxWidth()
                        .testTag("health-rationale-close"),
                ) {
                    Text(stringResource(R.string.health_rationale_close))
                }
            }
    }
}

@Composable
private fun HealthRationaleSection(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
) {
    WhipGroupedInformationCard(modifier) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
            modifier = Modifier.semantics { heading() },
        )
        Text(message, style = MaterialTheme.typography.bodyLarge)
    }
}
