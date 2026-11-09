package com.muyeon.domain.repository

import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.common.CursorPage
import com.muyeon.domain.model.performance.PerformanceDetail
import com.muyeon.domain.model.performance.PerformanceSummary
import com.muyeon.domain.model.search.PerformanceSearchParams
import com.muyeon.domain.model.search.SearchPage

interface PerformanceRepository {
    /** @param genre null = 전체 */
    suspend fun page(genre: String?, subGenre: String?, tag: String?, cursor: String?): ApiResult<CursorPage<PerformanceSummary>>
    suspend fun detail(id: Long): ApiResult<PerformanceDetail>
    /** 검색 — `GET /performances/search`. */
    suspend fun search(params: PerformanceSearchParams, cursor: String?): ApiResult<SearchPage<PerformanceSummary>>
    /** 같은 조건 개수 — `GET /performances/search/count`. */
    suspend fun searchCount(params: PerformanceSearchParams): ApiResult<Int>
}
