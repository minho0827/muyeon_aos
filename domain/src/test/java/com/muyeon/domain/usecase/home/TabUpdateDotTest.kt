package com.muyeon.domain.usecase.home

import com.muyeon.domain.model.home.TabUpdateDot
import com.muyeon.domain.model.home.TabUpdateDot.Decision
import com.muyeon.domain.model.home.TabUpdates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** 하단 [공연] 칸 새 글 점 규칙. */
class TabUpdateDotTest {
    private val t1 = "2026-11-09T05:00:00.000Z"
    private val t2 = "2026-11-09T06:00:00.000Z"

    @Test fun `셋 중 가장 늦은 시각, 다 없으면 null`() {
        assertEquals(t2, TabUpdates(t1, null, t2).latest)
        assertEquals(t2, TabUpdates(t2, t1, null).latest)
        assertNull(TabUpdates(null, null, null).latest)
        assertNull(TabUpdates("이상한 값", null, null).latest)
        // 형식이 달라도(초 소수점 없음·+09:00) 시각으로 비교한다.
        assertEquals("2026-11-09T16:00:00+09:00", TabUpdates(t1, "2026-11-09T16:00:00+09:00", null).latest)
    }

    @Test fun `실패하거나 최신 시각이 없으면 점 없음, 저장 안 함`() {
        assertEquals(Decision(false, null), TabUpdateDot.decide(null, t1, onTab = false))
        assertEquals(Decision(false, null), TabUpdateDot.decide(null, null, onTab = false))
    }

    @Test fun `처음 켰으면 지금 것을 저장하고 점 없음`() {
        assertEquals(Decision(false, t1), TabUpdateDot.decide(t1, null, onTab = false))
        assertEquals(Decision(false, t1), TabUpdateDot.decide(t1, "깨진 값", onTab = false))
    }

    @Test fun `본 뒤 새 글이 있으면 점`() {
        assertEquals(Decision(true, null), TabUpdateDot.decide(t2, t1, onTab = false))
    }

    @Test fun `새 글이 없으면 점 없음`() {
        assertEquals(Decision(false, null), TabUpdateDot.decide(t1, t1, onTab = false))
        assertEquals(Decision(false, null), TabUpdateDot.decide(t1, t2, onTab = false))
    }

    @Test fun `칸을 보고 있으면 점 대신 본 것으로 저장`() {
        assertEquals(Decision(false, t2), TabUpdateDot.decide(t2, t1, onTab = true))
    }
}
