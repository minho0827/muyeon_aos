package com.muyeon.app.ui.resume

import org.junit.Assert.assertEquals
import org.junit.Test

class ResumeModeTest {
    @Test fun `지금 유형으로 이력서 종류 — 무용수만 무용수 이력서`() {
        assertEquals(ResumeMode.DANCER, ResumeMode.forActiveType("dancer"))
        assertEquals(ResumeMode.TEACHER, ResumeMode.forActiveType("TEACHER"))
        assertEquals(ResumeMode.TEACHER, ResumeMode.forActiveType("ACADEMY"))
        assertEquals(ResumeMode.TEACHER, ResumeMode.forActiveType(null))
    }

    @Test fun `링크 as 값이 먼저`() {
        assertEquals("teacher", ResumeMode.forLink("teacher", "DANCER").raw)
        assertEquals("dancer", ResumeMode.forLink("dancer", "TEACHER").raw)
        assertEquals("dancer", ResumeMode.forLink("", "DANCER").raw)
        assertEquals("teacher", ResumeMode.forLink("", "GENERAL").raw)
    }
}
