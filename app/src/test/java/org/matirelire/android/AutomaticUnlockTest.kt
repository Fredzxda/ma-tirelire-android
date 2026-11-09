package org.matirelire.android

import org.junit.Assert.*
import org.junit.Test

class AutomaticUnlockTest {
    @Test fun cancellationAndRotationDoNotRequestAgainUntilNextLock() {
        val policy = AutomaticUnlock()
        assertTrue(policy.shouldRequest(true, false, false, false))
        repeat(3) { assertFalse(policy.shouldRequest(true, false, false, false)) }
        policy.onLock()
        assertTrue(policy.shouldRequest(true, false, false, false))
    }

    @Test fun explicitLockWaitsForManualRetryOrNextBackgroundReturn() {
        val policy = AutomaticUnlock()
        policy.onLock(allowAutomatic = false)
        assertFalse(policy.shouldRequest(true, false, false, false))
        policy.onLock()
        assertTrue(policy.shouldRequest(true, false, false, false))
    }

    @Test fun unavailableOrBusySessionsDoNotConsumeTheAttempt() {
        val policy = AutomaticUnlock()
        assertFalse(policy.shouldRequest(false, false, false, false))
        assertFalse(policy.shouldRequest(true, true, false, false))
        assertFalse(policy.shouldRequest(true, false, true, false))
        assertFalse(policy.shouldRequest(true, false, false, true))
        assertTrue(policy.shouldRequest(true, false, false, false))
    }
}
