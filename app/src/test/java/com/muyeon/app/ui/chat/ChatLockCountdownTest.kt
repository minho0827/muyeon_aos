package com.muyeon.app.ui.chat

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

/** 대화 종료 예정(locksAt) — 파싱 · KST 달력 D-N · 목록 pill · 종료된 대화 구역 나누기. */
class ChatLockCountdownTest {
    private val kst = ZoneId.of("Asia/Seoul")
    private fun kstMillis(y: Int, m: Int, d: Int, h: Int, min: Int = 0) =
        LocalDateTime.of(y, m, d, h, min).atZone(kst).toInstant().toEpochMilli()

    // ── 파싱 ──

    @Test fun `locksAt 없으면 null — 구버전 서버`() {
        assertNull(ChatRoomSummary.from(JSONObject("""{"roomId":1}""")).locksAt)
        assertNull(ChatRoomDetail.from(JSONObject("""{"roomId":1}""")).locksAt)
        assertNull(ChatRoomSummary.from(JSONObject("""{"roomId":1,"locksAt":null}""")).locksAt)
        assertNull(ChatRoomDetail.from(JSONObject("""{"roomId":1,"locksAt":null}""")).locksAt)
    }

    @Test fun `locksAt 파싱 — 목록·상세`() {
        val iso = "2026-11-20T14:59:59.000Z"
        assertEquals(iso, ChatRoomSummary.from(JSONObject("""{"roomId":1,"locksAt":"$iso"}""")).locksAt)
        assertEquals(iso, ChatRoomDetail.from(JSONObject("""{"roomId":1,"locksAt":"$iso"}""")).locksAt)
    }

    @Test fun `locksAt 시각 해석 — 밀리초 유무 모두`() {
        val expected = kstMillis(2026, 11, 20, 23, 59) + 59_000
        assertEquals(expected, ChatLockCountdown.parse("2026-11-20T14:59:59.000Z"))
        assertEquals(expected, ChatLockCountdown.parse("2026-11-20T14:59:59Z"))
        assertNull(ChatLockCountdown.parse("not-a-date"))
        assertNull(ChatLockCountdown.parse(null))
        assertNull(ChatLockCountdown.daysLeft("not-a-date", 0L))
    }

    // ── D-N (KST 달력 일수) ──

    @Test fun `같은 KST 날짜면 0 — 오늘 대화 종료`() {
        // 2026-11-20 23:00 KST = 14:00Z
        val now = kstMillis(2026, 11, 20, 0, 5)
        assertEquals(0, ChatLockCountdown.daysLeft("2026-11-20T14:00:00.000Z", now))
        assertEquals("오늘 대화 종료", ChatLockCountdown.roomLabel(0))
    }

    @Test fun `UTC 로는 전날이지만 KST 로는 다음 날 — KST 날짜 기준`() {
        // locksAt 2026-11-20T15:30Z = 11-21 00:30 KST. now 11-20 23:50 KST(= 14:50Z, UTC 날짜는 둘 다 11-20).
        val now = kstMillis(2026, 11, 20, 23, 50)
        assertEquals(1, ChatLockCountdown.daysLeft("2026-11-20T15:30:00.000Z", now))
        assertEquals("D-1 후 대화 종료", ChatLockCountdown.roomLabel(1))
    }

    @Test fun `KST 자정 경계 — 23시59분과 00시00분`() {
        val locksAt = "2026-11-27T03:00:00.000Z" // 11-27 12:00 KST
        assertEquals(7, ChatLockCountdown.daysLeft(locksAt, kstMillis(2026, 11, 20, 23, 59)))
        assertEquals(6, ChatLockCountdown.daysLeft(locksAt, kstMillis(2026, 11, 21, 0, 0)))
    }

    @Test fun `시간 차이가 24시간 미만이어도 날짜가 바뀌면 1`() {
        // now 11-20 22:00 KST, locksAt 11-21 01:00 KST (3시간 뒤)
        assertEquals(1, ChatLockCountdown.daysLeft("2026-11-20T16:00:00.000Z", kstMillis(2026, 11, 20, 22)))
    }

    @Test fun `이미 지났는데 아직 안 잠김 — 0 으로 본다`() {
        assertEquals(0, ChatLockCountdown.daysLeft("2026-11-18T03:00:00.000Z", kstMillis(2026, 11, 20, 10)))
    }

    @Test fun `경고 기준 7일`() {
        assertTrue(ChatLockCountdown.isWarn(0))
        assertTrue(ChatLockCountdown.isWarn(7))
        assertFalse(ChatLockCountdown.isWarn(8))
        assertEquals("D-12 후 대화 종료", ChatLockCountdown.roomLabel(12))
    }

    // ── 목록 pill ──

    @Test fun `목록 pill — 열린 방 7일 이내만`() {
        val now = kstMillis(2026, 11, 20, 10)
        fun room(locksAt: String?, locked: Boolean = false) = ChatRoomSummary.from(
            JSONObject().put("roomId", 1).put("locked", locked).apply { if (locksAt != null) put("locksAt", locksAt) },
        )
        assertEquals("D-7", room("2026-11-27T03:00:00.000Z").lockWarnPill(now))
        assertEquals("D-0", room("2026-11-20T14:00:00.000Z").lockWarnPill(now))
        assertNull(room("2026-11-28T03:00:00.000Z").lockWarnPill(now)) // D-8
        assertNull(room(null).lockWarnPill(now))
        // 잠긴 방은 카운트다운 없음('대화 종료' pill 이 대신)
        assertNull(room("2026-11-21T03:00:00.000Z", locked = true).lockWarnPill(now))
        assertNull(room("2026-11-21T03:00:00.000Z", locked = true).lockDaysLeft(now))
    }

    // ── 종료된 대화 구역 ──

    private fun summary(id: Int, locked: Boolean) =
        ChatRoomSummary.from(JSONObject().put("roomId", id).put("locked", locked))

    @Test fun `구역 나누기 — 순서 유지`() {
        val rooms = listOf(summary(5, false), summary(4, true), summary(3, false), summary(2, true), summary(1, false))
        val s = ChatRoomSections.split(rooms)
        assertEquals(listOf(5, 3, 1), s.open.map { it.roomId })
        assertEquals(listOf(4, 2), s.closed.map { it.roomId })
        assertEquals("종료된 대화 (2)", ChatRoomSections.closedTitle(s.closed.size))
    }

    @Test fun `구역 나누기 — 한쪽만 있거나 비었을 때`() {
        assertEquals(0, ChatRoomSections.split(emptyList()).closed.size)
        assertEquals(0, ChatRoomSections.split(emptyList()).open.size)
        val allOpen = ChatRoomSections.split(listOf(summary(1, false), summary(2, false)))
        assertEquals(listOf(1, 2), allOpen.open.map { it.roomId })
        assertTrue(allOpen.closed.isEmpty())
        val allLocked = ChatRoomSections.split(listOf(summary(1, true)))
        assertTrue(allLocked.open.isEmpty())
        assertEquals(listOf(1), allLocked.closed.map { it.roomId })
    }

    // ── 반응 ──

    @Test fun `잠긴 방 — 시트에서 반응 숨김, 내 메시지 삭제는 그대로`() {
        val m = ChatMessage(
            id = 10, roomId = 1, senderId = 7, type = "TEXT", content = "안녕", imageUrl = null, replyToId = null, editedAt = null,
            deletedAt = null, replyTo = null, createdAt = "2026-11-04T00:00:00Z", sender = null,
            surveyDone = null, surveySeq = null, surveySentAt = null, surveyRevision = null, surveyRespondedAt = null, surveyUpdatedAt = null,
            reactions = null,
        )
        val open = ChatMessageRules.actions(m, mine = true, canBlock = false)
        assertTrue(open.canReact)
        val locked = ChatMessageRules.actions(m, mine = true, canBlock = false, locked = true)
        assertFalse(locked.canReact)
        assertTrue(locked.canDelete)
    }

    @Test fun `REST 거절 코드 — ROOM_LOCKED 판정`() {
        assertEquals("ROOM_LOCKED", ChatApi.errorCode("""{"code":"ROOM_LOCKED","message":"레슨 요청이 마감되어 대화할 수 없어요."}"""))
        assertEquals("ROOM_LOCKED", ChatApi.errorCode("""{"statusCode":403,"data":{"code":"ROOM_LOCKED"}}"""))
        assertNull(ChatApi.errorCode("""{"statusCode":403,"message":"Forbidden"}"""))
        assertNull(ChatApi.errorCode("<html>"))
        assertTrue(ChatApi.isRoomLocked(com.muyeon.app.ui.common.ApiMessageError("x", 403, "ROOM_LOCKED")))
        assertFalse(ChatApi.isRoomLocked(com.muyeon.app.ui.common.ApiMessageError("x", 403, null)))
        assertFalse(ChatApi.isRoomLocked(IllegalStateException("x")))
        assertFalse(ChatApi.isRoomLocked(null))
    }
}
