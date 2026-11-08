package com.muyeon.domain.repository

import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.common.CursorPage
import com.muyeon.domain.model.reservation.CompletedLesson
import com.muyeon.domain.model.reservation.ReservationCard

/** 내가 예약한 내역 — [period] 서버 값(1m·3m·6m, null = 전체). */
interface ReservationRepository {
    suspend fun historyPage(category: String, period: String?, cursor: String?): ApiResult<CursorPage<ReservationCard>>
    suspend fun completedPage(period: String?, cursor: String?): ApiResult<CursorPage<CompletedLesson>>
}
