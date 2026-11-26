package com.muyeon.app.ui.chat

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** ★ 2026-11-26 상대 평소 응답 시간(opponentResponse) 파싱 · 머리글 부제 한 줄. */
class ChatOpponentResponseTest {
    @Test fun `예전 서버·null·빈 label 은 null`() {
        assertNull(ChatRoomDetail.from(JSONObject("""{"roomId":1}""")).opponentResponse)
        assertNull(ChatRoomDetail.from(JSONObject("""{"roomId":1,"opponentResponse":null}""")).opponentResponse)
        assertNull(ChatRoomDetail.from(JSONObject("""{"roomId":1,"opponentResponse":{"label":" ","minutes":10}}""")).opponentResponse)
    }

    @Test fun `label minutes 파싱`() {
        val r = ChatRoomDetail.from(JSONObject("""{"roomId":1,"opponentResponse":{"label":"보통 10분 이내 응답","minutes":10}}""")).opponentResponse
        assertEquals("보통 10분 이내 응답", r?.label)
        assertEquals(10, r?.minutes)
    }

    @Test fun `부제는 있는 것만 가운뎃점으로 잇는다`() {
        assertEquals("접속 중 · 보통 1시간 이내 응답", chatHeaderSubtitleLine("접속 중", "보통 1시간 이내 응답"))
        assertEquals("3분 전 활동", chatHeaderSubtitleLine("3분 전 활동", null))
        assertEquals("보통 하루 이내 응답", chatHeaderSubtitleLine(null, "보통 하루 이내 응답"))
        assertNull(chatHeaderSubtitleLine(null, ""))
    }
}
