package com.muyeon.data.repository

import com.muyeon.data.common.safeApiCall
import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.common.CursorPage
import com.muyeon.domain.common.CursorPaging
import com.muyeon.domain.common.UserFacingError
import com.muyeon.domain.common.ErrorKind
import com.muyeon.domain.model.performance.PerformanceDetail
import com.muyeon.domain.model.performance.PerformanceSummary
import com.muyeon.domain.model.search.PerformanceSearchParams
import com.muyeon.domain.model.search.SearchPage
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

    override suspend fun search(params: PerformanceSearchParams, cursor: String?): ApiResult<SearchPage<PerformanceSummary>> =
        safeApiCall(moshi) { api.search(params.toQuery(), cursor, SEARCH_LIMIT) }
            .map { SearchPage(it.items.orEmpty().mapNotNull { d -> d.toSummary() }, it.next, it.total) }

    override suspend fun searchCount(params: PerformanceSearchParams): ApiResult<Int> =
        safeApiCall(moshi) { api.searchCount(params.toQuery()) }.map { it.total ?: 0 }

    private companion object {
        /** 검색은 서버 계약대로 20개씩. */
        const val SEARCH_LIMIT = 20
    }
}
