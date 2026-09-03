package com.whip.app.startup

import org.junit.Assert.assertEquals
import org.junit.Test

class FreshStartRetryPolicyTest {
    @Test fun epochCheckFailureNeverAuthorizesDestructiveReset() {
        assertEquals(
            FreshStartRetryAction.ReevaluateEpoch,
            StartupRecoveryState.FreshStartCheckBlocked("inspection failed").freshStartRetryAction(),
        )
    }

    @Test fun onlyPostConfirmationResetFailureAuthorizesResetRetry() {
        assertEquals(
            FreshStartRetryAction.ResumeConfirmedReset,
            StartupRecoveryState.FreshStartBlocked("reset failed").freshStartRetryAction(),
        )
        assertEquals(
            FreshStartRetryAction.RetryStartupRecovery,
            StartupRecoveryState.FreshStartRequired.freshStartRetryAction(),
        )
    }
}
