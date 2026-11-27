package com.muyeon.app.auth

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceSyncPolicyTest {
    private val base = DeviceFingerprint(token = "tok", allowed = true, appVersion = "1.2.0", userId = "42")

    @Test fun loggedOut_neverSends_evenForced() {
        assertFalse(DeviceSyncPolicy.shouldSend(null, base, force = true))
        assertFalse(DeviceSyncPolicy.shouldSend(base.copy(userId = ""), null, force = true))
    }

    @Test fun unusableToken_skips() {
        assertFalse(DeviceSyncPolicy.shouldSend(base.copy(token = "unknown"), null, force = true))
        assertFalse(DeviceSyncPolicy.shouldSend(base.copy(token = ""), null, force = false))
    }

    @Test fun firstSend_whenNothingRemembered() =
        assertTrue(DeviceSyncPolicy.shouldSend(base, null, force = false))

    @Test fun identical_skips() =
        assertFalse(DeviceSyncPolicy.shouldSend(base, base.copy(), force = false))

    @Test fun force_sendsEvenIfIdentical() =
        assertTrue(DeviceSyncPolicy.shouldSend(base, base, force = true))

    @Test fun anyFieldChange_sends() {
        assertTrue(DeviceSyncPolicy.shouldSend(base.copy(token = "tok2"), base, false))
        assertTrue(DeviceSyncPolicy.shouldSend(base.copy(allowed = false), base, false))
        assertTrue(DeviceSyncPolicy.shouldSend(base.copy(appVersion = "1.3.0"), base, false))
        assertTrue(DeviceSyncPolicy.shouldSend(base.copy(userId = "7"), base, false))
    }
}
