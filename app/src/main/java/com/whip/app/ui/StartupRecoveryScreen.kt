package com.whip.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.whip.app.R
import com.whip.app.startup.StartupBlockReason
import com.whip.app.startup.StartupRecoveryState

@Composable
fun StartupRecoveryScreen(
    state: StartupRecoveryState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 48.dp)
                .semantics { liveRegion = LiveRegionMode.Polite }
                .testTag("startup-recovery-screen"),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start,
        ) {
            when (state) {
                StartupRecoveryState.Checking -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    )
                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = stringResource(R.string.startup_recovery_checking_title),
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .semantics { heading() },
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.startup_recovery_checking_message),
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }

                is StartupRecoveryState.Blocked -> {
                    Icon(
                        imageVector = Icons.Outlined.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                    )
                    Spacer(Modifier.height(20.dp))
                    Text(
                        text = stringResource(
                            if (state.reason == StartupBlockReason.Recovery) {
                                R.string.startup_recovery_blocked_title
                            } else {
                                R.string.startup_initialization_blocked_title
                            },
                        ),
                        modifier = Modifier.semantics { heading() },
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(
                            if (state.reason == StartupBlockReason.Recovery) {
                                R.string.startup_recovery_blocked_message
                            } else {
                                R.string.startup_initialization_blocked_message
                            },
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = stringResource(
                            if (state.reason == StartupBlockReason.Recovery) {
                                R.string.startup_recovery_safe_guidance
                            } else {
                                R.string.startup_initialization_safe_guidance
                            },
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(28.dp))
                    WhipButton(
                        onClick = onRetry,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("startup-recovery-retry"),
                    ) {
                        Text(
                            stringResource(
                                if (state.reason == StartupBlockReason.Recovery) {
                                    R.string.startup_recovery_retry
                                } else {
                                    R.string.action_try_again
                                },
                            ),
                        )
                    }
                }

                StartupRecoveryState.Ready -> Unit
            }
        }
    }
}
