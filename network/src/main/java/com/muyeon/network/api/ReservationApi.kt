package com.muyeon.network.api

import com.muyeon.network.dto.common.CursorPageDto
import com.muyeon.network.dto.reservation.CompletedLessonDto
import com.muyeon.network.dto.reservation.ReservationCardDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/** 출처: lesson-booking.controller.ts(history/page) · lessons.controller.ts(completed-lessons/page). */
interface ReservationApi {
    @GET(ApiEndpoints.Reservations.HISTORY_PAGE)
    suspend fun historyPage(
        @Query("category") category: String, @Query("period") period: String?,
        @Query("cursor") cursor: String?, @Query("limit") limit: Int,
    ): Response<CursorPageDto<ReservationCardDto>>

    @GET(ApiEndpoints.Reservations.COMPLETED_PAGE)
    suspend fun completedPage(
        @Query("period") period: String?, @Query("cursor") cursor: String?, @Query("limit") limit: Int,
    ): Response<CursorPageDto<CompletedLessonDto>>
}
