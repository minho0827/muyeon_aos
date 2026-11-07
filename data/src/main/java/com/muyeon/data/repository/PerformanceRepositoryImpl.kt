package com.muyeon.data.repository

import com.muyeon.data.common.safeApiCall
import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.common.CursorPage
import com.muyeon.domain.common.CursorPaging
import com.muyeon.domain.common.UserFacingError
import com.muyeon.domain.common.ErrorKind
import com.muyeon.domain.model.performance.PerformanceDetail
import com.muyeon.domain.model.performance.PerformanceSummary
import com.muyeon.domain.repository.PerformanceRepository
import com.muyeon.network.api.PerformanceApi
import com.squareup.moshi.Moshi

class PerformanceRepositoryImpl(
    private val api: PerformanceApi,
    private val moshi: Moshi,
) : PerformanceRepository {
    override suspend fun page(genre: String?, subGenre: String?, tag: String?, cursor: String?): ApiResult<CursorPage<PerformanceSummary>> =
        safeApiCall(moshi) { api.page(genre, subGenre, tag, cursor, CursorPaging.DEFAULT_LIMIT) }
            .map { CursorPage(it.items.orEmpty().mapNotNull { d -> d.toSummary() }, it.next) }

    override suspend fun detail(id: Long): ApiResult<PerformanceDetail> =
        when (val r = safeApiCall(moshi) { api.detail(id) }) {
            is ApiResult.Error -> r
            is ApiResult.Success -> r.data.toDetail()?.let { ApiResult.Success(it) }
                ?: ApiResult.Error(IllegalStateException("no id"), UserFacingError.server, kind = ErrorKind.SERVER)
        }
}
