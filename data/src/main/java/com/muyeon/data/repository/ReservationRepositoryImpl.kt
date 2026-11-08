package com.muyeon.data.repository

import com.muyeon.data.common.safeApiCall
import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.common.CursorPage
import com.muyeon.domain.common.CursorPaging
import com.muyeon.domain.model.reservation.CompletedLesson
import com.muyeon.domain.model.reservation.ReservationCard
import com.muyeon.domain.repository.ReservationRepository
import com.muyeon.network.api.ReservationApi
import com.squareup.moshi.Moshi

class ReservationRepositoryImpl(private val api: ReservationApi, private val moshi: Moshi) : ReservationRepository {
    override suspend fun historyPage(category: String, period: String?, cursor: String?): ApiResult<CursorPage<ReservationCard>> =
        safeApiCall(moshi) { api.historyPage(category, period, cursor, CursorPaging.DEFAULT_LIMIT) }
            .map { CursorPage(it.items.orEmpty().mapNotNull { d -> d.toDomain() }, it.next) }

    override suspend fun completedPage(period: String?, cursor: String?): ApiResult<CursorPage<CompletedLesson>> =
        safeApiCall(moshi) { api.completedPage(period, cursor, CursorPaging.DEFAULT_LIMIT) }
            .map { CursorPage(it.items.orEmpty().mapNotNull { d -> d.toDomain() }, it.next) }
}
