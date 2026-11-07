package com.muyeon.app.ui.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.muyeon.app.R
import com.muyeon.app.ui.components.ChipOption
import com.muyeon.app.ui.components.ChipRow
import com.muyeon.app.ui.components.ViewModeToggle
import com.muyeon.app.ui.home.HomeClassicUiState
import com.muyeon.app.ui.home.HomeDomainLabels
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography

/** 클래식 홈 머리(그리드 위) — "내 지역 신규" · 영역 칩 · 세부 칩 · 보기 전환. */
@Composable
fun HomeClassicHead(state: HomeClassicUiState, onDomain: (String) -> Unit, onSub: (String) -> Unit, onGrid: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(top = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.home_classic_heading), style = AppTypography.Title20, color = AppColors.TextHead, modifier = Modifier.padding(start = 20.dp))
        ChipRow(state.domains.map { ChipOption(it, HomeDomainLabels.chip(it)?.let { r -> stringResource(r) } ?: it) }, state.domain, onDomain)
        if (state.subs.isNotEmpty()) {
            TextTabRow(state.subs.map { ChipOption(it, if (it == "ALL") stringResource(R.string.common_all) else HomeDomainLabels.tag(it)?.let { r -> stringResource(r) } ?: it) }, state.sub, onSub)
        }
        ViewModeToggle(state.grid, onGrid, Modifier.padding(start = 20.dp, top = 12.dp, bottom = 4.dp))
    }
}
