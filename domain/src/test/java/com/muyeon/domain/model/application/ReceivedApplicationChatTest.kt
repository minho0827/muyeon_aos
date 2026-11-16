package com.muyeon.domain.model.application

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** ★ 2026-11-16 받은 지원 줄의 [채팅] — 방이 있고 지원 취소·잠금이 아닐 때만. */
class ReceivedApplicationChatTest {
    private fun app(roomId: Int?, status: String = "APPLIED", canceledBy: String? = null, locked: Boolean = false) = ReceivedApplication(
        id = 1, kind = "SUB", postingId = 2, postingTitle = "발레 대타", applicantName = "강사", applicantImage = null,
        status = status, canceledBy = canceledBy, viewedAt = null, createdAt = null, resultReason = null, postingStatus = "OPEN",
        postingConfirmedApplicationId = null, acceptedPay = null, effectivePay = null, viaDispatch = false, applicantCancels90d = null,
        locked = locked, roomId = roomId,
    )

    @Test fun `방 번호가 있으면 채팅 가능`() = assertTrue(app(roomId = 7).canOpenChat)
    @Test fun `방 번호가 없으면 숨김`() = assertFalse(app(roomId = null).canOpenChat)
    @Test fun `지원 취소한 건은 숨김`() = assertFalse(app(roomId = 7, status = "CANCELED", canceledBy = "WITHDRAW").canOpenChat)
    @Test fun `잠긴 지원자는 숨김`() = assertFalse(app(roomId = 7, locked = true).canOpenChat)
}
