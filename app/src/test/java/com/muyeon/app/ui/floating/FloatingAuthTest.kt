package com.muyeon.app.ui.floating

import org.junit.Assert.*
import org.junit.Test

class FloatingAuthTest {
    @Test fun loginAndLogoutAreReflectedWithoutRecreatingTheOverlay() {
        var token: String? = null
        val api = FloatingApi { token }
        assertFalse(api.isLoggedIn)
        token = "unit-test-only-token"
        assertTrue(api.isLoggedIn)
        token = null
        assertFalse(api.isLoggedIn)
    }

    @Test fun aDifferentTopLevelTabRestoresTheChatButtonButModalStateIsPreserved() {
        FloatingState.updateRoute("/home")
        FloatingState.updateHideChatFloat(true)
        FloatingState.updateFloatingHidden(true)
        FloatingState.updateRoute("/community")
        assertTrue(FloatingState.isChatFloatRoute)
        assertFalse(FloatingState.hideChatFloat)
        assertTrue(FloatingState.floatingHidden)
        FloatingState.updateFloatingHidden(false)
        FloatingState.updateRoute("/login")
        assertFalse(FloatingState.isChatFloatRoute)
    }
}
