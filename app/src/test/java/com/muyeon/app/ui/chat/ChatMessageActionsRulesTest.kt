package com.muyeon.app.ui.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** PACERA ChatMessageActionsRulesTest 이식 — 무용연 백엔드(글리프·토글 API)에 맞춘 반응·행동 규칙. */
class ChatMessageActionsRulesTest {
    private fun r(e: String, c: Int, mine: Boolean = false) = ChatReaction(e, c, mine)

    @Test fun `처음 누르면 그 반응이 하나 늘고 내 것, 토글 한 번`() {
        val (list, calls) = ChatReactionRules.toggle(emptyList(), "❤️")
        assertEquals(listOf(r("❤️", 1, true)), list)
        assertEquals(listOf("❤️"), calls)
    }

    @Test fun `같은 반응을 다시 누르면 취소 - 그 글리프를 다시 토글`() {
        val (list, calls) = ChatReactionRules.toggle(listOf(r("👍", 2, true)), "👍")
        assertEquals(listOf(r("👍", 1)), list)
        assertEquals(listOf("👍"), calls)
    }

    @Test fun `마지막 하나를 취소하면 칩이 사라진다`() {
        val (list, _) = ChatReactionRules.toggle(listOf(r("😭", 1, true)), "😭")
        assertTrue(list.isEmpty())
    }

    @Test fun `다른 반응이면 교체 - 옛것 해제 후 새것, 화면 순서`() {
        val (list, calls) = ChatReactionRules.toggle(listOf(r("🎉", 1), r("👍", 1, true)), "❤️")
        assertEquals(listOf(r("❤️", 1, true), r("🎉", 1)), list)
        assertEquals(listOf("👍", "❤️"), calls)
    }

    @Test fun `예전에 여러 개 둔 반응도 모두 해제하고 하나만`() {
        val (list, calls) = ChatReactionRules.toggle(listOf(r("😂", 1, true), r("👍", 1, true)), "👌")
        assertEquals(listOf(r("👌", 1, true)), list)
        assertEquals(listOf("😂", "👍", "👌"), calls)
    }

    @Test fun `모르는 예전 글리프는 뒤에 붙는다`() {
        val out = ChatReactionRules.sorted(listOf(r("😮", 1), r("👍", 2), r("❤️", 0)))
        assertEquals(listOf("👍", "😮"), out.map { it.emoji })
    }

    private fun msg(type: String = "TEXT", id: Int = 10, deleted: Boolean = false, content: String = "안녕") = ChatMessage(
        id = id, roomId = 1, senderId = 7, type = type, content = content, imageUrl = null, replyToId = null, editedAt = null,
        deletedAt = if (deleted) "2026-11-04T00:00:00Z" else null, replyTo = null, createdAt = "2026-11-04T00:00:00Z", sender = null,
        surveyDone = null, surveySeq = null, surveySentAt = null, surveyRevision = null, surveyRespondedAt = null, surveyUpdatedAt = null,
        reactions = null,
    )

    @Test fun `시트·반응·편집·삭제·신고 가능 여부`() {
        assertFalse(ChatMessageRules.hasActionSheet(msg(deleted = true)))
        assertFalse(ChatMessageRules.hasActionSheet(msg(type = "SYSTEM")))
        assertTrue(ChatMessageRules.canReact(msg()))
        assertFalse(ChatMessageRules.canReact(msg(id = -1)))
        assertFalse(ChatMessageRules.canCopy(msg(type = "IMAGE")))
        assertTrue(ChatMessageRules.canEdit(msg(), mine = true))
        assertFalse(ChatMessageRules.canEdit(msg(), mine = false))
        assertFalse(ChatMessageRules.canEdit(msg(type = "IMAGE"), mine = true))
        assertTrue(ChatMessageRules.canDelete(msg(type = "IMAGE"), mine = true))
        val a = ChatMessageRules.actions(msg(), mine = false, canBlock = true)
        assertTrue(a.canReport && a.canBlock && !a.canEdit && !a.canDelete)
    }
}
