package com.muyeon.domain.repository

import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.common.CursorPage
import com.muyeon.domain.model.performance.PerformanceDetail
import com.muyeon.domain.model.performance.PerformanceSummary

interface PerformanceRepository {
    /** @param genre null = 전체 */
    suspend fun page(genre: String?, subGenre: String?, tag: String?, cursor: String?): ApiResult<CursorPage<PerformanceSummary>>
    suspend fun detail(id: Long): ApiResult<PerformanceDetail>
}
