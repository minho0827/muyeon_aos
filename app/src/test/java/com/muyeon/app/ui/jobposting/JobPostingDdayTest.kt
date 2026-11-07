package com.muyeon.app.ui.jobposting

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

/** 공고 D-day — 'YYYY-MM-DD' 마감일, KST 달력 일수. 마감일 당일 = 0(오늘 마감). */
class JobPostingDdayTest {
    private val today = LocalDate.of(2026, 11, 7)

    @Test fun `마감일 당일 = 0`() = assertEquals(0, MyPosting.ddayOf("2026-11-07", today))
    @Test fun `마감 전날 = 1`() = assertEquals(1, MyPosting.ddayOf("2026-11-08", today))
    @Test fun `일주일 뒤 = 7`() = assertEquals(7, MyPosting.ddayOf("2026-11-14", today))
    @Test fun `지난 마감일 = 음수`() = assertEquals(-1, MyPosting.ddayOf("2026-11-06", today))
    @Test fun `월 경계`() = assertEquals(24, MyPosting.ddayOf("2026-12-01", today))
    @Test fun `시각이 붙어도 날짜만 본다`() = assertEquals(0, MyPosting.ddayOf("2026-11-07T00:00:00.000Z", today))

    @Test fun `미정·형식 오류는 null`() {
        assertNull(MyPosting.ddayOf(null, today))
        assertNull(MyPosting.ddayOf("-", today))
        assertNull(MyPosting.ddayOf("2026-11", today))
        assertNull(MyPosting.ddayOf("abcd-ef-gh", today))
    }

    private fun posting(status: String, deadline: String?, pending: Boolean = false) = MyPosting(
        kind = "JOB", id = 1, title = null, status = status, genre = null, region = null, fields = null,
        days = null, target = null, deadline = deadline, applicants = null, views = null,
        updatedAt = null, createdAt = null, pendingVerification = pending,
    )

    @Test fun `OPEN 이 아니거나 인증 대기면 dday 없음`() {
        assertNull(posting("CLOSED", "2099-01-01").dday)
        assertNull(posting("OPEN", "2099-01-01", pending = true).dday)
        assertNull(posting("OPEN", "-").dday)
    }
}
