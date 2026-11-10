package com.muyeon.app.ui.quote.owner

import com.muyeon.app.ui.quote.QuoteDetailResponse
import com.muyeon.app.ui.quote.SentQuoteItem
import com.muyeon.app.ui.quote.respond.MyResponseBadge
import com.muyeon.app.ui.quote.respond.QuoteRespondDetail
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** ★ 2026-11-10 보낸 제안 고치기·철회 — 새 칸 읽기 · 표시 고르기 · 바뀐 칸만 보내기. */
class QuoteResponseOwnerModelsTest {

    @Test fun `보낸 제안 목록 — 예약금 · 수정 · 철회 칸`() {
        val o = JSONObject("""{"id":3,"quoteId":12,"status":"WITHDRAWN","priceAmount":50000,"priceUnit":"PER_SESSION",
            "paymentMode":"DEPOSIT","depositAmount":10000,"editedAt":"2026-11-10T01:00:00.000Z",
            "withdrawnAt":"2026-11-10T02:30:00.000Z","withdrawReason":"일정이 안 맞아요","quoteStatus":"OPEN"}""")
        val item = SentQuoteItem.from(o)
        assertEquals("DEPOSIT", item.paymentMode)
        assertEquals(10000, item.depositAmount)
        assertEquals("2026-11-10T02:30:00.000Z", item.withdrawnAt)
        assertEquals("일정이 안 맞아요", item.withdrawReason)
        val mark = QuoteResponseMark.of(item.status, item.editedAt, item.withdrawnAt, item.withdrawReason)
        assertTrue(mark is QuoteResponseMark.Withdrawn)
        assertEquals("11.10 11:30", QuoteResponseMark.timeText(item.withdrawnAt))   // 한국 시간
    }

    @Test fun `받은 견적 — 철회 · 수정 표시`() {
        val d = QuoteDetailResponse.from(JSONObject("""{"quote":{"id":12,"status":"OPEN"},"responses":[
            {"id":1,"status":"WITHDRAWN","withdrawnAt":"2026-11-10T02:30:00.000Z","withdrawReason":""},
            {"id":2,"status":"SENT","editedAt":"2026-11-09T15:05:00.000Z"},
            {"id":3,"status":"SENT"}]}"""))
        val marks = d.responses.map { QuoteResponseMark.of(it.status, it.editedAt, it.withdrawnAt, it.withdrawReason) }
        assertNull((marks[0] as QuoteResponseMark.Withdrawn).reason)   // 빈 사유는 없음
        assertEquals("11.10 00:05", QuoteResponseMark.timeText((marks[1] as QuoteResponseMark.Edited).at))
        assertEquals(QuoteResponseMark.None, marks[2])
    }

    @Test fun `견적 응답 화면 — 내가 철회한 제안 · 배지`() {
        val d = QuoteRespondDetail.from(JSONObject("""{"quote":{"id":12,"status":"OPEN"},
            "responses":[{"id":3,"responderId":7,"status":"WITHDRAWN","withdrawnAt":"2026-11-10T02:30:00.000Z","editedAt":null}]}"""), myUserId = 7)!!
        assertEquals("WITHDRAWN", d.myResponse?.status)
        assertEquals("2026-11-10T02:30:00.000Z", d.myResponse?.withdrawnAt)
        assertNull(d.myResponse?.editedAt)
        assertEquals(MyResponseBadge.WITHDRAWN, MyResponseBadge.of("WITHDRAWN"))
    }

    @Test fun `고치기 — 바뀐 칸만 · 안 바뀌면 null`() {
        val sent = QuoteResponseDraft.of(50000, "PER_SESSION", "DEPOSIT", 10000, "안녕하세요")
        assertEquals(10000, sent.depositAmount)
        assertNull(sent.copy(message = " 안녕하세요 ").patchFrom(sent))

        val price = sent.copy(priceAmount = 60000).patchFrom(sent)!!
        assertEquals(60000, price.getInt("priceAmount"))
        assertEquals("PER_SESSION", price.getString("priceUnit"))
        assertFalse(price.has("message"))
        assertFalse(price.has("paymentMode"))

        val noDeposit = sent.copy(depositAmount = null, priceAmount = null).patchFrom(sent)!!
        assertEquals("NONE", noDeposit.getString("paymentMode"))
        assertEquals(0, noDeposit.getInt("depositAmount"))
        assertTrue(noDeposit.isNull("priceAmount"))
        assertTrue(noDeposit.isNull("priceUnit"))
    }

    @Test fun `예약금 없음(NONE)은 null · 고치기·철회는 SENT 이고 열린 요청만`() {
        assertNull(QuoteResponseDraft.of(null, null, "NONE", 0, null).depositAmount)
        assertTrue(canChangeResponse("SENT", true))
        assertFalse(canChangeResponse("SENT", false))
        assertFalse(canChangeResponse("WITHDRAWN", true))
        assertFalse(canChangeResponse("ACCEPTED", true))
    }

    @Test fun `서버가 돌려준 제안 읽기`() {
        val u = QuoteResponseUpdate.from(JSONObject("""{"id":3,"status":"SENT","priceAmount":60000,"editedAt":"2026-11-10T01:00:00.000Z","withdrawnAt":null}"""))
        assertEquals(3, u.id)
        assertEquals(60000, u.priceAmount)
        assertNull(u.withdrawnAt)
    }
}
