package com.muyeon.domain.usecase.performance

import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.common.CursorPage
import com.muyeon.domain.model.performance.PerformanceDetail
import com.muyeon.domain.model.performance.PerformanceDetailFilter
import com.muyeon.domain.model.performance.PerformanceSummary
import com.muyeon.domain.model.search.PerformanceSearchParams
import com.muyeon.domain.model.search.SearchPage
import com.muyeon.domain.repository.PerformanceRepository

class GetPerformancePageUseCase(private val repo: PerformanceRepository) {
    suspend operator fun invoke(genre: String?, detail: PerformanceDetailFilter, cursor: String?): ApiResult<CursorPage<PerformanceSummary>> =
        repo.page(
            genre = genre,
            subGenre = (detail as? PerformanceDetailFilter.Sub)?.code,
            tag = (detail as? PerformanceDetailFilter.Tag)?.code,
            cursor = cursor,
        )
}

class GetPerformanceDetailUseCase(private val repo: PerformanceRepository) {
    suspend operator fun invoke(id: Long): ApiResult<PerformanceDetail> = repo.detail(id)
}

/** 공연 검색 한 페이지. */
class SearchPerformancesUseCase(private val repo: PerformanceRepository) {
    suspend operator fun invoke(params: PerformanceSearchParams, cursor: String?): ApiResult<SearchPage<PerformanceSummary>> = repo.search(params, cursor)
}

/** 공연 검색 개수(필터 시트 'N개 보기'). */
class CountPerformanceSearchUseCase(private val repo: PerformanceRepository) {
    suspend operator fun invoke(params: PerformanceSearchParams): ApiResult<Int> = repo.searchCount(params)
}
