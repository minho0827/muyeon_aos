package com.muyeon.app.ui.chat

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 대화 종료(locked·lockReason·reRequest) 파싱·문구·목록 pill. */
class ChatRoomLockTest {
    @Test fun `구버전 서버 응답은 잠기지 않은 방`() {
        val s = ChatRoomSummary.from(JSONObject("""{"roomId":1,"unreadCount":0}"""))
        assertFalse(s.locked)
        assertNull(s.lockReason)
        val d = ChatRoomDetail.from(JSONObject("""{"roomId":1}"""))
        assertFalse(d.locked)
        assertNull(d.lockReason)
    }

    @Test fun `locked lockReason 파싱`() {
        val s = ChatRoomSummary.from(JSONObject("""{"roomId":1,"locked":true,"lockReason":"CANCELED"}"""))
        assertTrue(s.locked)
        assertEquals("CANCELED", s.lockReason)
        val d = ChatRoomDetail.from(JSONObject("""{"roomId":1,"locked":true,"lockReason":"MATCHED_OTHER"}"""))
        assertTrue(d.locked)
        assertEquals("MATCHED_OTHER", d.lockReason)
    }

    @Test fun `reRequest 파싱 — 없거나 null 이면 null`() {
        assertNull(ChatRoomDetail.from(JSONObject("""{"roomId":1}""")).reRequest)
        assertNull(ChatRoomDetail.from(JSONObject("""{"roomId":1,"locked":true,"reRequest":null}""")).reRequest)
        assertNull(ChatRoomDetail.from(JSONObject("""{"roomId":1,"locked":true,"reRequest":{}}""")).reRequest)
        assertNull(ChatRoomDetail.from(JSONObject("""{"roomId":1,"locked":true,"reRequest":{"teacherId":0,"quoteId":3}}""")).reRequest)
    }

    @Test fun `reRequest 파싱 — teacherId categoryId quoteId`() {
        val d = ChatRoomDetail.from(JSONObject(
            """{"roomId":1,"locked":true,"lockReason":"EXPIRED","reRequest":{"teacherId":42,"categoryId":"ballet","quoteId":113}}""",
        ))
        assertEquals(ChatReRequest(teacherId = 42, categoryId = "ballet", quoteId = 113), d.reRequest)
        val noCat = ChatRoomDetail.from(JSONObject(
            """{"roomId":1,"locked":true,"reRequest":{"teacherId":42,"categoryId":null,"quoteId":113}}""",
        )).reRequest
        assertEquals(42, noCat?.teacherId)
        assertNull(noCat?.categoryId)
    }

    @Test fun `목록 pill — 대화 종료가 견적 마감보다 우선`() {
        assertEquals("대화 종료", ChatRoomSummary.from(JSONObject("""{"roomId":1,"locked":true,"quoteStatus":"EXPIRED"}""")).statusPill)
        assertEquals("견적 마감", ChatRoomSummary.from(JSONObject("""{"roomId":1,"quoteStatus":"EXPIRED"}""")).statusPill)
        assertNull(ChatRoomSummary.from(JSONObject("""{"roomId":1,"quoteStatus":"OPEN"}""")).statusPill)
    }

    @Test fun `안내 띠 문구`() {
        assertEquals("레슨 요청이 취소되어 대화가 종료됐어요.", ChatRoomDetail.lockNotice("CANCELED"))
        assertEquals("다른 강사님으로 레슨이 정해져 대화가 종료됐어요.", ChatRoomDetail.lockNotice("MATCHED_OTHER"))
        assertEquals("레슨 요청 기간이 끝나 대화가 종료됐어요.", ChatRoomDetail.lockNotice("EXPIRED"))
        assertEquals("대화가 종료된 채팅방이에요.", ChatRoomDetail.lockNotice(null))
    }
}
