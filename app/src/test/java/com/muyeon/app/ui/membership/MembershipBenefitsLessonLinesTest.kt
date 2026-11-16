package com.muyeon.app.ui.membership

import com.muyeon.domain.model.member.LessonProviders
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MembershipBenefitsLessonLinesTest {
    @After fun reset() = LessonProviders.reset()

    @Test fun `레슨 줄은 레슨 제공 유형표로 가린다`() {
        assertTrue(MembershipBenefits.hidesLessonLines("DANCER"))
        assertTrue(MembershipBenefits.hidesLessonLines("TEAM"))
        assertFalse(MembershipBenefits.hidesLessonLines("TEACHER"))
        assertFalse(MembershipBenefits.hidesLessonLines("ACADEMY"))
        assertFalse(MembershipBenefits.hidesLessonLines("GENERAL"))
        LessonProviders.update(listOf("TEACHER", "ACADEMY", "DANCER"))
        assertFalse(MembershipBenefits.hidesLessonLines("DANCER"))
    }
}
