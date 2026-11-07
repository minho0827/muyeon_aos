package com.muyeon.app.ui.quote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

/** 견적 만료 — 요청일 + 30×24h, KST 달력 일수(서버 D-2/D-1 안내와 같은 계산). */
class QuoteExpiryTest {
    private val kst = ZoneId.of("Asia/Seoul")
    private fun kstMillis(y: Int, m: Int, d: Int, h: Int, min: Int = 0) =
        LocalDateTime.of(y, m, d, h, min).atZone(kst).toInstant().toEpochMilli()

    // 2026-11-10 23:00 KST = 2026-11-10T14:00:00Z
    private val expiresAt = "2026-11-10T14:00:00.000Z"

    @Test fun `만료 23시 KST, 전날 10시 KST 확인 = 1일(내일 만료)`() {
        val now = kstMillis(2026, 11, 9, 10)
        assertEquals(1, QuoteUi.daysUntilExpiry(null, expiresAt, nowMillis = now))
        assertEquals("내일 만료", QuoteUi.expiryLabel(null, expiresAt, nowMillis = now))
    }

    @Test fun `만료 당일 만료 전 = 0일(오늘 만료)`() {
        val now = kstMillis(2026, 11, 10, 9)
        assertEquals(0, QuoteUi.daysUntilExpiry(null, expiresAt, nowMillis = now))
        assertEquals("오늘 만료", QuoteUi.expiryLabel(null, expiresAt, nowMillis = now))
    }

    @Test fun `만료 시각이 지나면 만료됨`() {
        val now = kstMillis(2026, 11, 10, 23, 1)
        assertEquals(0, QuoteUi.daysUntilExpiry(null, expiresAt, nowMillis = now))
        assertEquals("만료됨", QuoteUi.expiryLabel(null, expiresAt, nowMillis = now))
        assertEquals("만료됨", QuoteUi.expiryLabel(null, expiresAt, nowMillis = kstMillis(2026, 11, 12, 8)))
        assertEquals(-2, QuoteUi.daysUntilExpiry(null, expiresAt, nowMillis = kstMillis(2026, 11, 12, 8)))
    }

    @Test fun `2일 이상 남으면 N일 후 만료`() {
        val now = kstMillis(2026, 11, 8, 0, 30)
        assertEquals(2, QuoteUi.daysUntilExpiry(null, expiresAt, nowMillis = now))
        assertEquals("2일 후 만료", QuoteUi.expiryLabel(null, expiresAt, nowMillis = now))
    }

    @Test fun `expiresAt 없으면 createdAt + 30x24h`() {
        // 생성 2026-10-01 09:00 KST(00:00Z) → 만료 2026-10-31 09:00 KST
        val created = "2026-10-01T00:00:00.000Z"
        assertEquals(1, QuoteUi.daysUntilExpiry(created, nowMillis = kstMillis(2026, 10, 30, 23, 59)))
        assertEquals(0, QuoteUi.daysUntilExpiry(created, nowMillis = kstMillis(2026, 10, 31, 0, 0)))
        assertEquals("오늘 만료", QuoteUi.expiryLabel(created, nowMillis = kstMillis(2026, 10, 31, 8, 59)))
        assertEquals("만료됨", QuoteUi.expiryLabel(created, nowMillis = kstMillis(2026, 10, 31, 9, 0)))
    }

    @Test fun `UTC 날짜가 달라도 KST 날짜로 센다`() {
        // 만료 2026-11-10 01:00 KST(= 11-09 16:00Z), 확인 11-09 23:00 KST(= 11-09 14:00Z) → UTC 로는 같은 날이지만 KST 로는 내일
        val exp = "2026-11-09T16:00:00.000Z"
        assertEquals(1, QuoteUi.daysUntilExpiry(null, exp, nowMillis = kstMillis(2026, 11, 9, 23)))
    }

    @Test fun `날짜 없으면 null`() {
        assertNull(QuoteUi.daysUntilExpiry(null, null))
        assertNull(QuoteUi.expiryLabel(null, null))
    }
}
