package com.muyeon.domain.model.member

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LessonProvidersTest {
    @After fun tearDown() {
        LessonProviders.onChanged = null
        LessonProviders.reset()
    }

    @Test fun `기본값 — 강사·학원만 제공자, 무용수·일반·팀은 아님`() {
        assertEquals(setOf("TEACHER", "ACADEMY"), LessonProviders.types)
        assertTrue(LessonProviders.canProvide("TEACHER"))
        assertTrue(LessonProviders.canProvide("academy"))
        assertFalse(LessonProviders.canProvide("DANCER"))
        assertFalse(LessonProviders.canProvide("GENERAL"))
        assertFalse(LessonProviders.canProvide("TEAM"))
        assertFalse(LessonProviders.canProvide(null))
        assertFalse(DancerLessonProviderAddon.enabled)
    }

    @Test fun `서버가 무용수를 넣으면 켜지고, 빼면 꺼진다`() {
        LessonProviders.update(listOf("TEACHER", "ACADEMY", "DANCER"))
        assertTrue(LessonProviders.canProvide("DANCER"))
        assertTrue(DancerLessonProviderAddon.enabled)
        LessonProviders.update(listOf("TEACHER", "ACADEMY"))
        assertFalse(LessonProviders.canProvide("DANCER"))
        assertFalse(DancerLessonProviderAddon.enabled)
    }

    @Test fun `값이 없거나 비면 기본값으로 돌아간다`() {
        LessonProviders.update(listOf("DANCER"))
        LessonProviders.update(null)
        assertEquals(LessonProviders.DEFAULT, LessonProviders.types)
        LessonProviders.update(listOf("DANCER"))
        LessonProviders.update(listOf(" ", ""))
        assertEquals(LessonProviders.DEFAULT, LessonProviders.types)
    }

    @Test fun `앞뒤 공백·소문자도 맞춘다`() {
        assertEquals(setOf("TEACHER", "DANCER"), LessonProviders.update(listOf(" teacher", "Dancer ")))
        assertTrue(LessonProviders.canProvide("dancer"))
        assertFalse(LessonProviders.canProvide("ACADEMY"))
    }

    @Test fun `첨부 종류 — 학원만 학원 정보, 강사·무용수는 개인 이력서`() {
        assertEquals("ACADEMY", LessonProviders.attachmentType("ACADEMY"))
        assertEquals("TEACHER", LessonProviders.attachmentType("TEACHER"))
        assertEquals("TEACHER", LessonProviders.attachmentType("DANCER"))
        assertEquals("TEACHER", LessonProviders.attachmentType(null))
    }

    @Test fun `이력서 레슨 항목 — 강사는 늘, 무용수는 켜졌을 때만, 학원은 없음`() {
        assertTrue(LessonProviders.showsLessonFields("TEACHER"))
        assertFalse(LessonProviders.showsLessonFields("DANCER"))
        assertFalse(LessonProviders.showsLessonFields("ACADEMY"))
        LessonProviders.update(listOf("TEACHER", "ACADEMY", "DANCER"))
        assertTrue(LessonProviders.showsLessonFields("DANCER"))
    }

    @Test fun `바뀔 때만 저장 콜백을 부른다`() {
        val calls = mutableListOf<Set<String>>()
        LessonProviders.onChanged = { calls += it }
        LessonProviders.update(listOf("TEACHER", "ACADEMY"))
        LessonProviders.update(listOf("TEACHER", "ACADEMY", "DANCER"))
        LessonProviders.update(listOf("TEACHER", "ACADEMY", "DANCER"))
        LessonProviders.update(null)
        assertEquals(listOf(setOf("TEACHER", "ACADEMY", "DANCER"), LessonProviders.DEFAULT), calls)
    }
}
