package com.muyeon.app.ui.search

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.muyeon.domain.model.search.SearchFilter

/** 칩 하나 — [value] 는 서버 값. 이름은 [labelRes] 가 있으면 문구, 없으면 [label] → [value] 그대로(장르·시·도는 한글 값). */
data class FilterOption(val value: String, @StringRes val labelRes: Int? = null, val label: String? = null)

@Composable
fun FilterOption.text(): String = labelRes?.let { stringResource(it) } ?: label ?: value

/** 필터 섹션 하나(상태·장르·지역·출연료). [single] 이면 하나만 고른다. */
data class FilterSection(
    val key: String,
    @StringRes val titleRes: Int,
    val options: List<FilterOption>,
    val single: Boolean = false,
) {
    val order: List<String> get() = options.map { it.value }
}

/** 정렬 하나 — [key] 는 서버 sort 값. */
data class SortOption(val key: String, @StringRes val labelRes: Int)

/** 필터 섹션 키 — 두 도메인이 같은 이름을 쓴다. */
object FilterKeys {
    const val STATUS = "status"
    const val GENRE = "genre"
    const val REGION = "region"
    const val FEE = "fee"
}

/** 시·도 17개 — 서버 regions·sidos 값 그대로. */
object SearchRegions {
    val SIDOS = listOf(
        "서울", "경기", "인천", "부산", "대구", "광주", "대전", "울산", "세종",
        "강원", "충북", "충남", "전북", "전남", "경북", "경남", "제주",
    )
    val options: List<FilterOption> = SIDOS.map { FilterOption(it) }
}

/** 칩 줄 상태 계산(화면 밖에서 시험한다). */
object FilterChipState {
    /** [필터] 칩 점 — 하나라도 골랐으면. */
    fun dot(filter: SearchFilter): Boolean = !filter.isEmpty

    /** 빠른 칩([장르▾]) 이 켜졌나 — 그 섹션에 고른 값이 있으면. */
    fun active(filter: SearchFilter, section: FilterSection): Boolean = filter.of(section.key).isNotEmpty()

    /** 빠른 칩 이름 — 고른 게 없으면 null(섹션 제목), 하나면 그 값, 여럿이면 (첫 값, 나머지 수). */
    fun picked(filter: SearchFilter, section: FilterSection): Pair<FilterOption, Int>? {
        val set = filter.of(section.key)
        val first = section.options.firstOrNull { it.value in set } ?: return null
        return first to (set.size - 1)
    }
}
