package com.muyeon.app.ui.chat.dealbar

import com.muyeon.app.ui.chat.dealbar.DealAction.ACADEMY_INFO
import com.muyeon.app.ui.chat.dealbar.DealAction.ACCEPT
import com.muyeon.app.ui.chat.dealbar.DealAction.CALL
import com.muyeon.app.ui.chat.dealbar.DealAction.CHECK_PROPOSAL
import com.muyeon.app.ui.chat.dealbar.DealAction.COMPLETE_LESSON
import com.muyeon.app.ui.chat.dealbar.DealAction.HIRE
import com.muyeon.app.ui.chat.dealbar.DealAction.REVIEW
import com.muyeon.app.ui.chat.dealbar.DealAction.SCHEDULE
import com.muyeon.app.ui.chat.dealbar.DealAction.VIEW_LESSON
import com.muyeon.app.ui.chat.dealbar.DealAction.VIEW_MY_QUOTE
import com.muyeon.app.ui.chat.dealbar.DealAction.VIEW_QUOTES
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** ★ 2026-11-14 상단 고정 바 규칙 — iOS ChatDealBarResolverTests 와 같은 경우. */
class ChatDealBarResolverTest {
    private fun input(
        pair: String? = "MEMBER_TEACHER", role: String? = "customer", step: String? = "RESPONDED",
        lessonId: Int? = 7, canAccept: Boolean = true, waiting: Boolean = false, awaitingMe: Boolean = false,
        revealed: Boolean = false, locked: Boolean = false, cycles: Int = 1, started: Boolean = true,
    ) = DealBarInput(
        pair = pair, myRole = role, locked = locked, step = step, title = "발레 레슨", priceText = "회당 50,000원",
        scheduleText = "11/26(목) 오후 3:00", lessonId = lessonId, canAccept = canAccept, proposalWaiting = waiting,
        proposalAwaitingMe = awaitingMe, contactRevealed = revealed, cyclesCount = cycles, hasQuote = true,
        lessonStarted = started,
    )

    private fun actions(i: DealBarInput) = ChatDealBarResolver.resolve(i)?.actions

    @Test fun `회원 - 제안 도착이면 받은 제안 보기와 채택하기`() {
        assertEquals(listOf(VIEW_QUOTES, ACCEPT), actions(input()))
        assertEquals(listOf(VIEW_QUOTES), actions(input(canAccept = false)))
    }

    @Test fun `회원 - 채택 후 약속잡기, 대기 제안이 있으면 약속 확인하기, 공개면 전화`() {
        assertEquals(listOf(SCHEDULE, VIEW_QUOTES, CALL), actions(input(step = "ACCEPTED", revealed = true)))
        assertEquals(listOf(CHECK_PROPOSAL, VIEW_QUOTES), actions(input(step = "ACCEPTED", awaitingMe = true)))
    }

    @Test fun `회원 - 확정 후 일정 보기, 완료 후 후기 보내기가 먼저`() {
        assertEquals(listOf(VIEW_LESSON, CALL), actions(input(step = "SCHEDULED", revealed = true)))
        val done = ChatDealBarResolver.resolve(input(step = "DONE"))!!
        assertEquals(listOf(REVIEW, VIEW_LESSON), done.actions)
        assertTrue(done.statusEmphasis)
        assertEquals("레슨 완료", done.status)
    }

    @Test fun `강사 - 견적 보기 → 약속잡기 → 일정 보기·레슨 완료 → 일정 보기`() {
        assertEquals(listOf(VIEW_MY_QUOTE), actions(input(role = "pro")))
        assertEquals(listOf(SCHEDULE), actions(input(role = "pro", step = "ACCEPTED")))
        assertEquals(listOf(VIEW_LESSON, COMPLETE_LESSON, CALL), actions(input(role = "pro", step = "SCHEDULED", revealed = true)))
        assertEquals(listOf(VIEW_LESSON), actions(input(role = "pro", step = "DONE")))
        assertNull("레슨 id 없는 확정은 숨김", actions(input(role = "pro", step = "SCHEDULED", lessonId = null)))
    }

    @Test fun `학원 - 함께하기, 강사는 학원 정보, 함께한 뒤 전화만`() {
        assertEquals(listOf(VIEW_QUOTES, HIRE), actions(input(pair = "ACADEMY_TEACHER")))
        assertEquals(listOf(ACADEMY_INFO), actions(input(pair = "ACADEMY_TEACHER", role = "pro")))
        assertEquals(listOf(CALL), actions(input(pair = "ACADEMY_TEACHER", step = "ACCEPTED", revealed = true)))
        assertNull(actions(input(pair = "ACADEMY_TEACHER", step = "ACCEPTED")))
    }

    @Test fun `약속잡기 방`() {
        assertEquals(listOf(SCHEDULE), actions(input(pair = "PROPOSAL", role = "pro", step = "ACCEPTED")))
        assertEquals(listOf(CHECK_PROPOSAL), actions(input(pair = "PROPOSAL", step = "ACCEPTED", awaitingMe = true)))
        assertNull(actions(input(pair = "PROPOSAL", step = "ACCEPTED")))
        assertEquals(listOf(REVIEW, VIEW_LESSON), actions(input(pair = "PROPOSAL", step = "DONE")))
    }

    @Test fun `공간·양도·채용·잠긴 방은 바를 숨긴다`() {
        listOf("SPACE", "TRANSFER", "RECRUIT", "DIRECT").forEach { assertNull(it, ChatDealBarResolver.resolve(input(pair = it))) }
        assertNull(ChatDealBarResolver.resolve(input(locked = true)))
    }

    @Test fun `예전 서버(pair 없음)는 견적이 있으면 회원-강사로 본다`() {
        assertEquals(listOf(VIEW_QUOTES, ACCEPT), actions(input(pair = null)))
    }

    @Test fun `버튼은 최대 3개, 여러 레슨이면 개수를 넘긴다`() {
        val m = ChatDealBarResolver.resolve(input(step = "ACCEPTED", revealed = true, cycles = 3))!!
        assertTrue(m.actions.size <= 3)
        assertEquals(3, m.cyclesCount)
    }

    @Test fun `강사 - 레슨 시작 전에는 레슨 완료가 없다`() {
        assertEquals(listOf(VIEW_LESSON, CALL), actions(input(role = "pro", step = "SCHEDULED", revealed = true, started = false)))
    }
}
