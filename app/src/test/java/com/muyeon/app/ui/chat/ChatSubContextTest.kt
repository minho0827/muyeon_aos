package com.muyeon.app.ui.chat

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** ★ 2026-11-16 대타 채팅 — 방 상세 subContexts · 목록 contextKind 파싱과 채용·대타 칩. */
class ChatSubContextTest {

    @Test fun `예전 서버 응답은 대타 건 없음`() {
        val d = ChatRoomDetail.from(JSONObject("""{"roomId":1}"""))
        assertTrue(d.subContexts.isEmpty())
        val s = ChatRoomSummary.from(JSONObject("""{"roomId":1,"kind":"direct"}"""))
        assertFalse(s.isSub)
        assertNull(s.contextLine)
    }

    @Test fun `subContexts 파싱 — 빠진 값은 기본값, 지원 번호 없는 건은 버린다`() {
        val d = ChatRoomDetail.from(JSONObject("""
            {"roomId":3,"subContexts":[
              {"applicationId":11,"postingId":5,"title":"발레 대타","classDate":"2026-11-20","classTime":"19:00~20:00",
               "region":"서울 강남","pay":60000,"postingStatus":"OPEN","applicationStatus":"APPLIED","myRole":"author",
               "canConfirm":true,"canWithdraw":false,"canCancelConfirmed":false,"confirmedAt":null,"resultReason":null},
              {"postingId":6,"title":"번호 없음"},
              {"applicationId":"12","pay":"45000","myRole":"applicant","resultReason":"OTHER_CONFIRMED"}
            ]}
        """.trimIndent()))
        assertEquals(2, d.subContexts.size)
        val a = d.subContexts[0]
        assertEquals(11, a.applicationId)
        assertEquals(5, a.postingId)
        assertEquals("발레 대타", a.title)
        assertEquals(60000, a.pay)
        assertTrue(a.isAuthor)
        assertTrue(a.canConfirm)
        assertFalse(a.canWithdraw)
        assertNull(a.confirmedAt)
        assertNull(a.resultReason)
        val b = d.subContexts[1]
        assertEquals(12, b.applicationId)
        assertEquals(0, b.postingId)
        assertEquals(45000, b.pay)
        assertFalse(b.isAuthor)
        assertFalse(b.canConfirm)
        assertEquals("OTHER_CONFIRMED", b.resultReason)
    }

    @Test fun `목록 contextKind sub — 대타 배지, 공고 제목 줄, 채용·대타 칩`() {
        val s = ChatRoomSummary.from(JSONObject("""{"roomId":2,"kind":"direct","contextKind":"sub","contextTitle":"현대무용 대타"}"""))
        assertTrue(s.isSub)
        assertEquals("대타", s.roleBadge)
        assertEquals("현대무용 대타", s.contextLine)
        assertTrue(ChatRoomFilter.RECRUIT.matches(s))
        assertFalse(ChatRoomFilter.REQUESTED.matches(s))
        assertFalse(ChatRoomFilter.RESPONDED.matches(s))
        assertFalse(ChatRoomFilter.INQUIRY.matches(s))
        assertTrue(ChatRoomFilter.ALL.matches(s))
    }

    @Test fun `pair RECRUIT 방도 채용·대타 칩, 일반 다이렉트 방은 아님`() {
        val recruit = ChatRoomSummary.from(JSONObject("""{"roomId":4,"kind":"direct","pair":"RECRUIT"}"""))
        assertTrue(ChatRoomFilter.RECRUIT.matches(recruit))
        assertFalse(ChatRoomFilter.INQUIRY.matches(recruit))
        assertNull(recruit.contextLine)
        val direct = ChatRoomSummary.from(JSONObject("""{"roomId":5,"kind":"direct"}"""))
        assertFalse(ChatRoomFilter.RECRUIT.matches(direct))
        assertTrue(ChatRoomFilter.INQUIRY.matches(direct))
        assertTrue(ChatRoomFilter.REQUESTED.matches(direct))
    }

    @Test fun `웹 필터 키 recruit`() {
        assertEquals(ChatRoomFilter.RECRUIT, ChatRoomFilter.from("recruit"))
    }

    @Test fun `대타만 걸린 잠긴 방은 대타 문구`() {
        assertEquals("다른 강사님으로 대타가 확정돼 대화가 끝났어요.", ChatRoomDetail.lockNotice("MATCHED_OTHER", subRoom = true))
        assertEquals("다른 강사님으로 레슨이 정해져 대화가 종료됐어요.", ChatRoomDetail.lockNotice("MATCHED_OTHER"))
    }
}
