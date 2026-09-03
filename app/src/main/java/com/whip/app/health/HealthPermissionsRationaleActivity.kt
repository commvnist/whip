package com.whip.app.health

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.whip.app.R
import com.whip.app.WhipApplication
import com.whip.app.core.AppThemeMode
import com.whip.app.ui.WhipButton
import com.whip.app.ui.WhipContentWidth
import com.whip.app.ui.WhipFullScreenSurface
import com.whip.app.ui.WhipNoticeCard
import com.whip.app.ui.WhipNoticeTone
import com.whip.app.ui.WhipPageContentPadding
import com.whip.app.ui.WhipPageHeader
import com.whip.app.ui.WhipSpacing
import com.whip.app.ui.StartupRecoveryScreen
import com.whip.app.ui.theme.WhipTheme
import com.whip.app.startup.StartupRecoveryState

class HealthPermissionsRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as WhipApplication
        setContent {
            val startupState by app.startupRecoveryState.collectAsStateWithLifecycle()
            if (startupState != StartupRecoveryState.Ready) {
                WhipTheme(darkTheme = isSystemInDarkTheme(), dynamicColor = false) {
                    StartupRecoveryScreen(
                        state = startupState,
                        onRetry = app::retryStartupRecovery,
                        onEraseAllData = app::beginFreshStartReset,
                        onKeepDataAndClose = ::finishAffinity,
                    )
                }
                return@setContent
            }
            val settings by app.settingsRepository.settings.collectAsStateWithLifecycle(
                initialValue = app.settingsRepository.current(),
            )
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (settings.themeMode) {
                AppThemeMode.System -> systemDark
                AppThemeMode.Light -> false
                AppThemeMode.Dark -> true
            }
            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
            WhipTheme(darkTheme = darkTheme, dynamicColor = settings.dynamicColor) {
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
    WhipFullScreenSurface(title = pageTitle, modifier = modifier) {
        Column(Modifier.fillMaxSize().testTag("health-rationale-surface")) {
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
}

@Composable
private fun HealthRationaleSection(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(WhipSpacing.standard),
            verticalArrangement = Arrangement.spacedBy(WhipSpacing.sibling),
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                modifier = Modifier.semantics { heading() },
            )
            Text(message, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
