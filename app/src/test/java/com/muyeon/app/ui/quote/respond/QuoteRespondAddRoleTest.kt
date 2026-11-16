package com.muyeon.app.ui.quote.respond

import com.muyeon.domain.model.member.LessonProviders
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** ★ 2026-11-16 견적 응답 '유형 추가' 선택지 — 레슨 제공 유형표에서 만든다(강사·학원 고정 대신). */
class QuoteRespondAddRoleTest {
    @After fun reset() = LessonProviders.reset()

    @Test fun `기본은 강사(바로) · 학원(서류)`() {
        assertEquals(
            listOf(QuoteRespondRoles.AddRoleOption("TEACHER", true), QuoteRespondRoles.AddRoleOption("ACADEMY", false)),
            QuoteRespondRoles.addRoleOptions(),
        )
        assertTrue(QuoteRespondRoles.addsDirectly("TEACHER"))
        assertFalse(QuoteRespondRoles.addsDirectly("ACADEMY"))
    }

    @Test fun `명부에 무용수가 있으면 무용수도 바로 추가`() {
        LessonProviders.update(listOf("TEACHER", "ACADEMY", "DANCER"))
        assertEquals(listOf("TEACHER", "ACADEMY", "DANCER"), QuoteRespondRoles.addRoleOptions().map { it.type })
        assertTrue(QuoteRespondRoles.addRoleOptions().last().direct)
    }
}
