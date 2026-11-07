package com.muyeon.app.ui.performance.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.muyeon.app.R
import com.muyeon.app.ui.components.ChipOption
import com.muyeon.app.ui.components.ChipRow
import com.muyeon.app.ui.performance.PerformanceLabels
import com.muyeon.app.ui.performance.PerformanceListViewModel
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.Dimensions
import com.muyeon.domain.model.performance.PerformanceDetailFilter
import androidx.compose.foundation.background

/** 공연 칩 두 줄 — 1줄 장르, 2줄 세부 분류·태그(줄 사이 8). 웹 ListPage filters. */
@Composable
fun PerformanceFilterChips(
    genres: List<String>,
    genre: String,
    details: List<PerformanceDetailFilter>,
    detail: PerformanceDetailFilter,
    onGenre: (String) -> Unit,
    onDetail: (PerformanceDetailFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    val all = stringResource(R.string.common_all)
    Column(
        modifier.fillMaxWidth().background(AppColors.Surface).padding(vertical = Dimensions.spacingL),
        verticalArrangement = Arrangement.spacedBy(Dimensions.spacingS),
    ) {
        ChipRow(
            options = genres.map { ChipOption(it, if (it == PerformanceListViewModel.ALL) all else it) },
            selected = genre,
            onSelect = onGenre,
        )
        val detailOptions = details.map { d ->
            val text = when (d) {
                PerformanceDetailFilter.All -> all
                is PerformanceDetailFilter.Sub -> PerformanceLabels.code(d.code)?.let { stringResource(it) } ?: d.code
                is PerformanceDetailFilter.Tag -> PerformanceLabels.code(d.code)?.let { stringResource(it) } ?: d.code
            }
            ChipOption(d.key, text)
        }
        ChipRow(
            options = detailOptions,
            selected = detail.key,
            onSelect = { key -> onDetail(details.first { it.key == key }) },
        )
    }
}
