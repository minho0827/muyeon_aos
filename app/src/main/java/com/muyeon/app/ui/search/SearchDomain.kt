package com.muyeon.app.ui.search

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.model.search.SearchFilter
import com.muyeon.domain.model.search.SearchPage

/**
 * ★ 2026-11-09 검색 도메인(전략) — 공연·캐스팅이 각자 하나씩 구현한다.
 *   화면(SearchScreen·SearchResultPane·시트)은 도메인을 받아 그리기만 하고 도메인마다 갈라 처리하지 않는다.
 */
interface SearchDomain<T : Any> {
    /** 탭·ViewModel 키("performance" · "casting"). */
    val key: String
    @get:StringRes val titleRes: Int
    /** 시트 아래 주황 버튼 "%d개 … 보기". */
    @get:StringRes val showCountRes: Int
    val filterSections: List<FilterSection>
    val sortOptions: List<SortOption>
    val defaultSort: String get() = sortOptions.first().key
    /** 칩 줄 앞 빠른 칩([장르▾][지역▾]) — 섹션 키. 누르면 그 섹션만 든 시트. */
    val quickChips: List<String>

    suspend fun search(query: String, filter: SearchFilter, sort: String, cursor: String?): ApiResult<SearchPage<T>>
    /** 같은 조건 개수. 못 받으면 null. */
    suspend fun count(query: String, filter: SearchFilter): Int?

    fun idOf(item: T): Any
    /** 누르면 열 화면 경로(바깥 NavHost). */
    fun open(item: T): String

    /** 결과 한 줄. */
    @Composable
    fun Row(item: T, onClick: () -> Unit)
}

fun SearchDomain<*>.section(key: String): FilterSection? = filterSections.firstOrNull { it.key == key }

/** 검색 화면 탭 키 — 위 바 검색창이 처음 고를 탭을 넘긴다. */
object SearchDomainKeys {
    const val PERFORMANCE = "performance"
    const val CASTING = "casting"
}
