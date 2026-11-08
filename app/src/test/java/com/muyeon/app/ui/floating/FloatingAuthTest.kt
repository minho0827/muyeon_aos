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

    /** 채팅 떠 있는 버튼은 위 바 종이비행기로 옮겨 없앴다(cf3de46) — 어느 탭에서도 다시 뜨지 않고, 다른 떠 있는 것 숨김 상태는 탭을 바꿔도 남는다. */
    @Test fun chatFloatIsGoneOnEveryRouteButModalStateIsPreserved() {
        FloatingState.updateRoute("/home")
        FloatingState.updateHideChatFloat(true)
        FloatingState.updateFloatingHidden(true)
        FloatingState.updateRoute("/community")
        assertFalse(FloatingState.isChatFloatRoute)
        assertFalse(FloatingState.hideChatFloat)
        assertTrue(FloatingState.floatingHidden)
        FloatingState.updateFloatingHidden(false)
        FloatingState.updateRoute("/login")
        assertFalse(FloatingState.isChatFloatRoute)
    }
}
