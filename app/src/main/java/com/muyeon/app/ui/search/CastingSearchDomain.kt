package com.muyeon.app.ui.search

import com.muyeon.app.util.runCatchingCancellable

import androidx.compose.runtime.Composable
import com.muyeon.app.R
import com.muyeon.app.ui.castingstage.StageRoutes
import com.muyeon.app.ui.castingstage.components.StageCastingRow
import com.muyeon.app.ui.castingstage.create.CastingCreateOptions
import com.muyeon.app.ui.castingstage.data.CastingStageApi
import com.muyeon.app.ui.castingstage.model.StageCasting
import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.model.search.SearchFilter
import com.muyeon.domain.model.search.SearchPage
import com.muyeon.domain.model.search.SearchQueryText

/** 캐스팅 검색 — `GET /casting/search`. 칩: 장르·지역·출연료 · 정렬: 최근 등록·마감 임박. */
class CastingSearchDomain(private val api: CastingStageApi) : SearchDomain<StageCasting> {
    override val key = SearchDomainKeys.CASTING
    override val titleRes = R.string.search_tab_casting
    override val showCountRes = R.string.search_show_casting
    override val filterSections = SECTIONS
    override val sortOptions = SORTS
    override val quickChips = listOf(FilterKeys.GENRE, FilterKeys.REGION)

    override suspend fun search(query: String, filter: SearchFilter, sort: String, cursor: String?): ApiResult<SearchPage<StageCasting>> =
        runCatchingCancellable { api.search(params(query, filter, sort), cursor) }
            .fold({ ApiResult.Success(it) }, { e -> ApiResult.Error(e, e.message?.takeIf { it.isNotBlank() } ?: com.muyeon.domain.common.UserFacingError.server) })

    override suspend fun count(query: String, filter: SearchFilter): Int? =
        runCatchingCancellable { api.searchCount(params(query, filter, defaultSort)) }.getOrNull()

    override fun idOf(item: StageCasting): Any = item.id
    override fun open(item: StageCasting): String = StageRoutes.SHELL.casting(item.id)

    @Composable
    override fun Row(item: StageCasting, onClick: () -> Unit) = StageCastingRow(item, onClick)

    companion object {
        /** 캐스팅 장르 — 캐스팅 탭 칩과 같은 값(전체 뺌). */
        val GENRE = FilterSection(
            FilterKeys.GENRE, R.string.search_section_genre,
            listOf("발레", "한국무용", "현대무용", "실용무용", "뮤지컬", "행사").map { FilterOption(it) },
        )
        val REGION = FilterSection(FilterKeys.REGION, R.string.search_section_region, SearchRegions.options)
        /** 출연료 — 공고 등록과 같은 값(서버 casting-fee.ts). 서버 feeRange 가 한 값이라 하나만 고른다. */
        val FEE = FilterSection(
            FilterKeys.FEE, R.string.search_section_fee,
            CastingCreateOptions.feeRanges.map { (v, label) -> FilterOption(v, label = label) }, single = true,
        )
        val SECTIONS = listOf(GENRE, REGION, FEE)

        val SORTS = listOf(
            SortOption("RECENT", R.string.search_sort_recent),
            SortOption("DEADLINE", R.string.search_sort_deadline),
        )

        /** 화면 조건 → 쿼리 맵(빈 값은 뺀다). */
        fun params(query: String, filter: SearchFilter, sort: String): Map<String, String> = buildMap {
            SearchQueryText.normalize(query)?.let { put("q", it) }
            filter.csv(GENRE.key, GENRE.order)?.let { put("genres", it) }
            filter.csv(REGION.key, REGION.order)?.let { put("sidos", it) }
            filter.csv(FEE.key, FEE.order)?.let { put("feeRange", it) }
            put("sort", sort.takeIf { s -> SORTS.any { it.key == s } } ?: SORTS.first().key)
        }
    }
}
