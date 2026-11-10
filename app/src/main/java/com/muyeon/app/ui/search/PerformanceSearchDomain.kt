package com.muyeon.app.ui.search

import androidx.compose.runtime.Composable
import com.muyeon.app.R
import com.muyeon.app.navigation.Screen
import com.muyeon.app.ui.performance.components.PerformanceRow
import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.model.performance.PerformanceCategory
import com.muyeon.domain.model.performance.PerformanceSummary
import com.muyeon.domain.model.search.PerformanceSearchParams
import com.muyeon.domain.model.search.SearchFilter
import com.muyeon.domain.model.search.SearchPage
import com.muyeon.domain.model.search.SearchQueryText
import com.muyeon.domain.usecase.performance.CountPerformanceSearchUseCase
import com.muyeon.domain.usecase.performance.SearchPerformancesUseCase

/** 공연 검색 — `GET /performances/search`. 칩: [공연 상태▾][장르▾][지역▾] · 정렬: 추천·시작·종료 임박·최근 등록. */
class PerformanceSearchDomain(
    private val searchUseCase: SearchPerformancesUseCase,
    private val countUseCase: CountPerformanceSearchUseCase,
) : SearchDomain<PerformanceSummary> {
    override val key = SearchDomainKeys.PERFORMANCE
    override val titleRes = R.string.search_tab_performance
    override val showCountRes = R.string.search_show_performance
    override val filterSections = SECTIONS
    override val sortOptions = SORTS
    // ★ 11-10 [공연 상태▾]를 맨 앞에(공연 예정·공연 중·공연 종료 — 필터 시트의 상태 칸과 같다).
    override val quickChips = QUICK_CHIPS

    override suspend fun search(query: String, filter: SearchFilter, sort: String, cursor: String?): ApiResult<SearchPage<PerformanceSummary>> =
        searchUseCase(params(query, filter, sort), cursor)

    override suspend fun count(query: String, filter: SearchFilter): Int? =
        (countUseCase(params(query, filter, defaultSort)) as? ApiResult.Success)?.data

    override fun idOf(item: PerformanceSummary): Any = item.id
    override fun open(item: PerformanceSummary): String = Screen.PerformanceDetail.build(item.id)

    @Composable
    override fun Row(item: PerformanceSummary, onClick: () -> Unit) {
        // 검색 결과는 찜 하트 없이(목록과 같은 줄).
        PerformanceRow(item = item, onClick = onClick)
    }

    companion object {
        val STATUS = FilterSection(
            FilterKeys.STATUS, R.string.search_section_status,
            listOf(
                FilterOption("UPCOMING", R.string.search_status_upcoming),
                FilterOption("ONGOING", R.string.search_status_ongoing),
                FilterOption("ENDED", R.string.search_status_ended),
            ),
        )
        val GENRE = FilterSection(FilterKeys.GENRE, R.string.search_section_genre, PerformanceCategory.GENRES.map { FilterOption(it) })
        val REGION = FilterSection(FilterKeys.REGION, R.string.search_section_region, SearchRegions.options)
        val SECTIONS = listOf(STATUS, GENRE, REGION)
        val QUICK_CHIPS = listOf(FilterKeys.STATUS, FilterKeys.GENRE, FilterKeys.REGION)

        val SORTS = listOf(
            SortOption("RECOMMENDED", R.string.search_sort_recommended),
            SortOption("START", R.string.search_sort_start),
            SortOption("ENDING", R.string.search_sort_ending),
            SortOption("RECENT", R.string.search_sort_recent),
        )

        /** 화면 조건 → 서버 조건(값은 칩 순서로 콤마). */
        fun params(query: String, filter: SearchFilter, sort: String): PerformanceSearchParams = PerformanceSearchParams(
            q = SearchQueryText.normalize(query),
            statuses = filter.csv(STATUS.key, STATUS.order),
            genres = filter.csv(GENRE.key, GENRE.order),
            regions = filter.csv(REGION.key, REGION.order),
            sort = sort.takeIf { s -> SORTS.any { it.key == s } } ?: SORTS.first().key,
        )
    }
}
