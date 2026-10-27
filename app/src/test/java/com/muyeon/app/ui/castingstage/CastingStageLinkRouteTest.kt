package com.muyeon.app.ui.castingstage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** 링크 payload → 캐스팅·무대 안쪽 경로(10-27, iOS StageRoute.initialPath 와 같은 규칙). */
class CastingStageLinkRouteTest {
    private fun r(castingId: String? = null, screen: String? = null, issueNo: String? = null, category: String? = null) =
        CastingStageActivity.linkRoute(castingId, screen, issueNo, category)

    @Test fun castingDetail() = assertEquals("casting/12", r(castingId = "12"))
    @Test fun screens() {
        assertEquals("castingList", r(screen = "castingList"))
        assertEquals("issues", r(screen = "issues"))
    }
    @Test fun issueAndCategory() {
        assertEquals("issue/3", r(issueNo = "3"))
        assertEquals("newsList/3/DOMESTIC", r(issueNo = "3", category = "DOMESTIC"))
        assertEquals("issue/3", r(issueNo = "3", category = "NOPE"))
    }
    @Test fun badValuesOpenHome() {
        assertNull(r())
        assertNull(r(castingId = "abc"))
        assertNull(r(castingId = "0"))
        assertNull(r(issueNo = "x"))
    }
}
