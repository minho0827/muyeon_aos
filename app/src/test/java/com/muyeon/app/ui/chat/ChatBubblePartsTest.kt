package com.muyeon.app.ui.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** PACERA ChatLinks·ChatMessageGrouping·isExternalWebLink 규칙 이식 확인. */
class ChatBubblePartsTest {
    private fun msg(id: Int, sender: Int, at: String, type: String = "TEXT") = ChatMessage(
        id = id, roomId = 1, senderId = sender, type = type, content = "x", imageUrl = null, replyToId = null, editedAt = null,
        deletedAt = null, replyTo = null, createdAt = at, sender = null, surveyDone = null, surveySeq = null, surveySentAt = null,
        surveyRevision = null, surveyRespondedAt = null, surveyUpdatedAt = null, reactions = null,
    )

    @Test fun `같은 사람 같은 분이면 이어진다, 분·사람·시스템이 다르면 끊는다`() {
        assertTrue(ChatGrouping.continuesGroup(msg(1, 7, "2026-11-04T01:00:05Z"), msg(2, 7, "2026-11-04T01:00:55Z")))
        assertFalse(ChatGrouping.continuesGroup(msg(1, 7, "2026-11-04T01:00:05Z"), msg(2, 7, "2026-11-04T01:01:00Z")))
        assertFalse(ChatGrouping.continuesGroup(msg(1, 7, "2026-11-04T01:00:05Z"), msg(2, 8, "2026-11-04T01:00:10Z")))
        assertFalse(ChatGrouping.continuesGroup(msg(1, 7, "2026-11-04T01:00:05Z", "SYSTEM"), msg(2, 7, "2026-11-04T01:00:10Z")))
    }

    @Test fun `첫 메시지와 날짜가 바뀐 메시지 위에만 구분선`() {
        val list = listOf(msg(1, 7, "2026-11-03T10:00:00+09:00"), msg(2, 7, "2026-11-03T11:00:00+09:00"), msg(3, 8, "2026-11-04T09:00:00+09:00"))
        assertNotNull(ChatGrouping.dayDivider(list, 0))
        assertNull(ChatGrouping.dayDivider(list, 1))
        assertNotNull(ChatGrouping.dayDivider(list, 2))
    }

    @Test fun `링크 찾기 - 끝 문장부호 빼고 www 는 https 를 붙인다`() {
        val f = ChatLinks.find("보세요 https://a.com/x. 그리고 www.b.com!")
        assertEquals(listOf("https://a.com/x", "https://www.b.com"), f.map { it.url })
    }

    @Test fun `무용연 링크는 앱 안, 남의 사이트는 확인 시트`() {
        assertTrue(isMuyeonLink("https://muyeon.co.kr/jobs/1"))
        assertFalse(isExternalWebLink("https://www.muyeon.co.kr/spaces/3"))
        assertTrue(isExternalWebLink("https://example.com"))
        assertFalse(isExternalWebLink("tel:0101234"))
    }

    @Test fun `인용 한 줄 - 사진·동영상·빈 글`() {
        assertEquals("사진", chatReplySnippet("IMAGE", ""))
        assertEquals("동영상", chatReplySnippet("VIDEO", null))
        assertEquals("메시지", chatReplySnippet("TEXT", " "))
        assertEquals("a b", chatReplySnippet("TEXT", "a\nb"))
    }
}
