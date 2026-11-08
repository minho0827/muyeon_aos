package com.muyeon.app.ui.quote.respond

import com.muyeon.app.navigation.NativeRoutes
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 견적 응답 — 서버 응답 변환(내가 보낸 견적 고르기 · 공개 상세) · 경로 표. */
class QuoteRespondModelsTest {
    private val detail = """
        {"quote":{"id":12,"categoryId":"ballet","region":"서울 강남구","status":"OPEN","expired":true,"guestRequest":true,"answers":[]},
         "responses":[{"id":3,"responderId":7,"status":"SENT","priceAmount":50000,"priceUnit":"PER_SESSION","paymentMode":"DEPOSIT","depositAmount":10000,"chatRoomId":0,"message":"안녕하세요"},
                      {"id":4,"responderId":9,"status":"ACCEPTED","chatRoomId":55}]}
    """.trimIndent()

    @Test fun `내가 보낸 견적은 responderId 로 고른다`() {
        val d = QuoteRespondDetail.from(JSONObject(detail), myUserId = 9)!!
        assertEquals(4, d.myResponse?.id)
        assertEquals(55, d.myResponse?.chatRoomId)
        assertTrue(d.expired)
        assertTrue(d.quote.guestRequest)
    }

    @Test fun `내 견적이 없으면 null · 방 0 은 없음`() {
        assertNull(QuoteRespondDetail.from(JSONObject(detail), myUserId = 1)!!.myResponse)
        val mine = QuoteRespondDetail.from(JSONObject(detail), myUserId = 7)!!.myResponse!!
        assertNull(mine.chatRoomId)
        assertEquals(10000, mine.depositAmount)
    }

    @Test fun `quote 가 없으면 삭제로 본다`() {
        assertNull(QuoteRespondDetail.from(JSONObject("{}"), 1))
        assertNull(PublicQuoteDetail.from(JSONObject("{\"quote\":null}")))
    }

    @Test fun `공개 상세 — 사진 수 · 받은 견적 수 · 마감`() {
        val p = PublicQuoteDetail.from(JSONObject("""{"quote":{"id":5,"status":"OPEN","expired":false,"photoCount":2},"responseCount":3}"""))!!
        assertEquals(2, p.photoCount); assertEquals(3, p.responseCount); assertFalse(p.closed)
        assertTrue(PublicQuoteDetail.from(JSONObject("""{"quote":{"id":5,"status":"MATCHED"}}"""))!!.closed)
    }

    @Test fun `경로 — availableQuotes 숫자는 견적 응답, 목록은 그대로`() {
        assertEquals(NativeRoutes.Target.Action("openQuoteRespond", mapOf("quoteId" to "12")), NativeRoutes.resolve("/availableQuotes/12"))
        assertEquals(NativeRoutes.Target.Action("openQuoteRespond", mapOf("quoteId" to "12")), NativeRoutes.resolve("/availableQuotes/12?sent=1"))
        assertEquals(NativeRoutes.Target.Action("openQuoteRespond", mapOf("quoteId" to "12", "compose" to "1")), NativeRoutes.resolve("/availableQuotes/12?compose=1"))
        assertEquals(MyResponseBadge.REJECTED, MyResponseBadge.of("REJECTED"))
    }
}
