package com.muyeon.app.ui.chat.dealbar

import com.muyeon.app.ui.chat.ChatSubContext
import com.muyeon.app.ui.chat.dealbar.SubAction.CANCEL_CONFIRMED
import com.muyeon.app.ui.chat.dealbar.SubAction.CONFIRM
import com.muyeon.app.ui.chat.dealbar.SubAction.VIEW_APPLICATION
import com.muyeon.app.ui.chat.dealbar.SubAction.VIEW_POSTING
import com.muyeon.app.ui.chat.dealbar.SubAction.WITHDRAW
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** ★ 2026-11-16 대타 카드 — 상태 칩·역할별 버튼·여러 건·상단 카드 표. */
class SubContextResolverTest {

    private fun ctx(
        appId: Int = 1, role: String = "author", status: String = "APPLIED", posting: String = "OPEN",
        canConfirm: Boolean = false, canWithdraw: Boolean = false, canCancel: Boolean = false,
        confirmedAt: String? = null, reason: String? = null, postingId: Int = 9,
    ) = ChatSubContext(
        applicationId = appId, postingId = postingId, title = "발레 대타", classDate = "2026-11-20", classTime = "19:00~20:00",
        region = null, pay = 60000, postingStatus = posting, applicationStatus = status, myRole = role,
        canConfirm = canConfirm, canWithdraw = canWithdraw, canCancelConfirmed = canCancel,
        confirmedAt = confirmedAt, resultReason = reason,
    )

    @Test fun `대타 건이 없으면 카드 없음`() {
        assertNull(SubContextResolver.resolve(emptyList(), locked = false))
    }

    @Test fun `원장님 확정 전 — 지원서 보기 · 대타 확정, 주황 강조`() {
        val m = SubContextResolver.resolve(listOf(ctx(canConfirm = true)), locked = false)!!
        assertEquals(listOf(VIEW_APPLICATION, CONFIRM), m.primary.actions)
        assertEquals("지원 도착", m.primary.status)
        assertTrue(m.primary.statusEmphasis)
        assertEquals("11/20(금) · 19:00~20:00 · 60,000원", m.primary.detail)
    }

    @Test fun `원장님 확정 뒤 — 지원서 보기 · 확정 취소`() {
        val m = SubContextResolver.resolve(listOf(ctx(status = "ACCEPTED", canCancel = true, confirmedAt = "2026-11-16T01:00:00Z")), false)!!
        assertEquals(listOf(VIEW_APPLICATION, CANCEL_CONFIRMED), m.primary.actions)
        assertEquals("대타 확정", m.primary.status)
    }

    @Test fun `지원자 확정 전 — 공고 보기 · 지원 취소`() {
        val m = SubContextResolver.resolve(listOf(ctx(role = "applicant", canWithdraw = true)), false)!!
        assertEquals(listOf(VIEW_POSTING, WITHDRAW), m.primary.actions)
        assertEquals("지원함", m.primary.status)
        assertFalse(m.primary.statusEmphasis)
    }

    @Test fun `지원자 확정 뒤 — 공고 보기 · 확정 취소(지원 취소는 숨김)`() {
        val m = SubContextResolver.resolve(listOf(ctx(role = "applicant", status = "ACCEPTED", canWithdraw = true, canCancel = true)), false)!!
        assertEquals(listOf(VIEW_POSTING, CANCEL_CONFIRMED), m.primary.actions)
    }

    @Test fun `잠긴 방은 보기 버튼만`() {
        val m = SubContextResolver.resolve(listOf(ctx(canConfirm = true)), locked = true)!!
        assertEquals(listOf(VIEW_APPLICATION), m.primary.actions)
    }

    @Test fun `공고 번호가 없으면 보기 버튼 없음`() {
        val m = SubContextResolver.resolve(listOf(ctx(role = "applicant", canWithdraw = true, postingId = 0)), false)!!
        assertEquals(listOf(WITHDRAW), m.primary.actions)
    }

    @Test fun `끝난 이유가 상태 칩을 정한다`() {
        val cases = mapOf(
            "OTHER_CONFIRMED" to "다른 강사로 확정", "REJECTED" to "미선정", "CLOSED" to "공고 마감",
            "WITHDRAWN" to "지원 취소", "CANCELED" to "확정 취소",
        )
        cases.forEach { (reason, label) -> assertEquals(label, SubContextResolver.status(ctx(role = "applicant", status = "REJECTED", reason = reason))) }
    }

    @Test fun `끝난 이유가 없으면 지원 상태로`() {
        assertEquals("확정 취소", SubContextResolver.status(ctx(status = "CANCELED", confirmedAt = "2026-11-16")))
        assertEquals("지원 취소", SubContextResolver.status(ctx(status = "CANCELED")))
        assertEquals("미선정", SubContextResolver.status(ctx(status = "REJECTED")))
        assertEquals("공고 마감", SubContextResolver.status(ctx(posting = "CLOSED")))
        assertEquals("원장님 확인", SubContextResolver.status(ctx(role = "applicant", status = "REVIEWING")))
    }

    @Test fun `여러 건이면 할 일이 있는 건이 대표, 나머지는 접힌다`() {
        val done = ctx(appId = 1, status = "REJECTED", reason = "OTHER_CONFIRMED")
        val todo = ctx(appId = 2, canConfirm = true)
        val m = SubContextResolver.resolve(listOf(done, todo), false)!!
        assertEquals(2, m.count)
        assertEquals(2, m.primary.context.applicationId)
        assertEquals(listOf(1), m.others.map { it.context.applicationId })
    }

    @Test fun `날짜를 못 읽으면 원문, 값이 다 없으면 상세 줄 없음`() {
        assertEquals("다음 주", SubContextResolver.shortDate("다음 주"))
        assertNull(SubContextResolver.detail(ctx().copy(classDate = null, classTime = null, pay = null)))
    }

    @Test fun `상단 카드 표 — 대타만 있는 방은 대타 카드 하나, 레슨과 함께면 둘 다`() {
        val noLesson = DealBarInput(
            pair = "RECRUIT", myRole = null, locked = false, step = null, title = null, priceText = null, scheduleText = null,
            lessonId = null, canAccept = false, proposalWaiting = false, proposalAwaitingMe = false, contactRevealed = false,
            cyclesCount = 0, hasQuote = false,
        )
        val subOnly = ChatTopCards.resolve(ChatTopInput(noLesson, listOf(ctx(canConfirm = true)), locked = false))
        assertEquals(1, subOnly.size)
        assertTrue(subOnly[0] is ChatTopCard.Sub)

        val lesson = noLesson.copy(pair = "MEMBER_TEACHER", myRole = "customer", step = "RESPONDED", canAccept = true)
        val both = ChatTopCards.resolve(ChatTopInput(lesson, listOf(ctx(canConfirm = true)), locked = false))
        assertEquals(2, both.size)
        assertTrue(both[0] is ChatTopCard.Lesson)
        assertTrue(both[1] is ChatTopCard.Sub)

        assertTrue(ChatTopCards.resolve(ChatTopInput(noLesson, emptyList(), locked = false)).isEmpty())
    }
}
