package com.muyeon.domain.model.member

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LessonOwnerModeTest {
    @Test fun `주체 모드 — 학원만 학원, 나머지는 개인`() {
        assertEquals(LessonOwnerMode.ACADEMY, LessonOwnerMode.of("academy"))
        assertEquals(LessonOwnerMode.PERSONAL, LessonOwnerMode.of("TEACHER"))
        assertEquals(LessonOwnerMode.PERSONAL, LessonOwnerMode.of("DANCER"))
        assertEquals(LessonOwnerMode.PERSONAL, LessonOwnerMode.of(null))
    }

    @Test fun `보낼 칸·소속 API·레슨 설정`() {
        assertEquals("asTeacherId", LessonOwnerMode.ACADEMY.bylineField)
        assertEquals("atAcademyId", LessonOwnerMode.PERSONAL.bylineField)
        assertEquals("/academy-teachers/mine", LessonOwnerMode.ACADEMY.affiliationPath)
        assertEquals("/academy-teachers/invites", LessonOwnerMode.PERSONAL.affiliationPath)
        assertFalse(LessonOwnerMode.ACADEMY.hasLessonSettings)
        assertTrue(LessonOwnerMode.PERSONAL.hasLessonSettings)
    }

    @Test fun `체험·시간표 잠금`() {
        assertTrue(LessonOwnerMode.ACADEMY.experienceVisible(hasByline = false, isEdit = false, isExperience = false))
        assertFalse(LessonOwnerMode.PERSONAL.experienceVisible(hasByline = false, isEdit = false, isExperience = false))
        assertTrue(LessonOwnerMode.PERSONAL.experienceVisible(hasByline = true, isEdit = false, isExperience = false))
        assertTrue(LessonOwnerMode.PERSONAL.experienceVisible(hasByline = false, isEdit = true, isExperience = true))
        assertTrue(LessonOwnerMode.PERSONAL.slotsReadOnly(ownerAcademyId = 3))
        assertFalse(LessonOwnerMode.ACADEMY.slotsReadOnly(ownerAcademyId = 3))
        assertFalse(LessonOwnerMode.PERSONAL.slotsReadOnly(ownerAcademyId = null))
    }

    @Test fun `문구 표 — 수업과 레슨`() {
        val a = LessonProviderCopy.of("ACADEMY")
        val t = LessonProviderCopy.of("TEACHER")
        assertEquals("새 수업 등록", a.createButton)
        assertEquals("새 레슨 개설", t.createButton)
        assertEquals("수업 수정", a.editTitle)
        assertEquals("레슨 개설", t.createTitle)
        assertEquals("수업명", a.nameField)
        assertEquals("김강사 강사", a.partyLabel(LessonOwnerMode.ACADEMY, "김강사"))
        assertEquals("루체 발레", t.partyLabel(LessonOwnerMode.PERSONAL, "루체 발레"))
        assertEquals("학원", LessonProviderCopy.roleLabel("ACADEMY"))
        assertEquals("강사", LessonProviderCopy.roleLabel("TEACHER"))
        assertEquals("무용수", LessonProviderCopy.roleLabel("DANCER"))
        assertEquals("", LessonProviderCopy.roleLabel(""))
    }

    @Test fun `성과 문구 — 무용수 제공자도 맞는 말`() {
        assertEquals("학원 성과", PerformanceCopy.of("ACADEMY").screenTitle)
        assertEquals("강사 성과", PerformanceCopy.of("TEACHER").screenTitle)
        assertEquals("무용수 성과", PerformanceCopy.of("DANCER").screenTitle)
        assertEquals("회원들이 무용수님을 찾고 있어요", PerformanceCopy.of("DANCER").headline)
        assertEquals("회원들이 강사님을 찾고 있어요", PerformanceCopy.of("").headline)
        assertTrue(PerformanceCopy.of("academy").isAcademy)
    }
}
