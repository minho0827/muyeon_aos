package com.muyeon.app.ui.dancer

import com.muyeon.domain.model.dancer.DancerMyQuote
import com.muyeon.domain.model.dancer.DancerProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** ★ 2026-11-16 무용수 상세 레슨 버튼 규칙 — 강사 상세와 같다. */
class DancerLessonCtaTest {
    private fun profile(provider: Boolean, room: Long? = null, quote: DancerMyQuote? = null, limited: Boolean = false) = DancerProfile(
        id = 5, name = "무용수", images = emptyList(), major = null, gender = null, height = null, weight = null, region = null,
        oneLiner = null, intro = null, birth = null, education = null, career = null, companyCareer = null, awards = null,
        performances = null, videoUrl = null, availableDays = emptyList(), availableTimeSlots = emptyList(), hasVideo = false,
        self = false, limited = limited, lessonProvider = provider, myQuote = quote, chatRoomId = room,
    )

    @Test fun `레슨 안 받는 무용수·내 프로필이면 버튼 없음`() {
        assertNull(profile(provider = false).lessonCta(isSelf = false))
        assertNull(profile(provider = true).lessonCta(isSelf = true))
    }

    @Test fun `소개만 받은 응답이어도 레슨 문의는 보인다`() {
        assertEquals(DancerLessonCta(null, null), profile(provider = true, limited = true).lessonCta(isSelf = false))
    }

    @Test fun `답이 온 요청만 받은 견적 보기, 방이 있으면 문의하기`() {
        assertEquals(DancerLessonCta(9, null), profile(true, room = 9, quote = DancerMyQuote(3, hasResponse = false)).lessonCta(false))
        assertEquals(DancerLessonCta(null, 3), profile(true, quote = DancerMyQuote(3, hasResponse = true)).lessonCta(false))
        assertEquals(DancerLessonCta(null, null), profile(true, room = 0).lessonCta(false))
    }
}
