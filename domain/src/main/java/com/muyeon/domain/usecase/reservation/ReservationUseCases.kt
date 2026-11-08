package com.muyeon.domain.usecase.reservation

import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.common.CursorPage
import com.muyeon.domain.model.reservation.MyReservationItem
import com.muyeon.domain.model.reservation.ReservationCategory
import com.muyeon.domain.repository.ReservationRepository

/** 내가 예약한 내역 한 쪽 — 분류가 '레슨 완료'면 완료 레슨 목록, 아니면 예약 목록(웹 MyReservations 와 같은 갈래). */
class GetMyReservationsPageUseCase(private val repo: ReservationRepository) {
    suspend operator fun invoke(category: ReservationCategory, period: String?, cursor: String?): ApiResult<CursorPage<MyReservationItem>> =
        if (category == ReservationCategory.DONE) repo.completedPage(period, cursor).map { CursorPage(it.items, it.nextCursor) }
        else repo.historyPage(category.name, period, cursor).map { CursorPage(it.items, it.nextCursor) }
}
