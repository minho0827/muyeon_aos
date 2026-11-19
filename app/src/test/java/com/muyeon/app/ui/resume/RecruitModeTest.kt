package com.muyeon.app.ui.resume

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecruitModeTest {
    @Test fun academyAndTeamOpenTeacherProfileInRecruitMode() {
        assertTrue(RecruitMode.of(false, "ACADEMY"))
        assertTrue(RecruitMode.of(false, "TEAM"))
        assertTrue(RecruitMode.of(false, "academy"))
    }

    @Test fun lessonCustomersStayInLessonMode() {
        assertFalse(RecruitMode.of(false, "GENERAL"))
        assertFalse(RecruitMode.of(false, "TEACHER"))
        assertFalse(RecruitMode.of(false, "DANCER"))
        assertFalse(RecruitMode.of(false, null))
    }

    @Test fun explicitRecruitAlwaysWins() {
        assertTrue(RecruitMode.of(true, "GENERAL"))
    }
}
