package com.muyeon.app.ui.region

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muyeon.app.R
import com.muyeon.app.common_components.dialog.ContentAlignment
import com.muyeon.app.common_components.dialog.CustomDialog
import com.muyeon.app.ui.components.ChipOption
import com.muyeon.app.ui.components.ChipRow
import com.muyeon.app.ui.components.EmptyView
import com.muyeon.app.ui.components.MainTopBar
import com.muyeon.app.ui.region.components.AddRegionRow
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.Dimensions
import com.muyeon.domain.model.region.ActiveRegion
import com.muyeon.domain.usecase.region.AddRegionOptionsUseCase
import org.koin.androidx.compose.koinViewModel

/**
 * ★ 2026-11-08 (b4) 지역 추가 — 웹 /addRegion 그대로: '지역 선택' · 시·도 칩(수도권 먼저) · 'OO 전체(시·도 단위로 등록)' · 수도권이면 시군구 줄.
 *   누르면 관심 지역에 넣고 목록으로 돌아간다(5개면 팝업).
 */
@Composable
fun AddRegionScreen(onBack: () -> Unit, onAdded: () -> Unit, viewModel: AddRegionViewModel = koinViewModel()) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.events.collect { onAdded() } }
    Column(Modifier.fillMaxSize().background(AppColors.Surface).navigationBarsPadding()) {
        MainTopBar(title = stringResource(R.string.add_region_title), primary = false, onBack = onBack)
        when {
            s.isLoading -> Unit
            s.errorMessage != null -> EmptyView(stringResource(R.string.common_list_failed), content = s.errorMessage,
                buttonText = stringResource(R.string.common_retry), onButton = viewModel::load)
            else -> {
                Text(stringResource(R.string.add_region_heading), style = AppTypography.Title18, color = AppColors.TextHead,
                    modifier = Modifier.padding(horizontal = Dimensions.gutter, vertical = Dimensions.spacingL))
                ChipRow(s.groups.map { ChipOption(it.first, it.first) }, s.sido, viewModel::selectSido)
                val allName = stringResource(R.string.add_region_sido_all, s.sido)
                LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(top = Dimensions.spacingM)) {
                    item(key = "all") {
                        AddRegionRow(allName, stringResource(R.string.add_region_sido_unit), tag = "addRegion.all") {
                            viewModel.add(AddRegionOptionsUseCase.sidoAll(s.items, allName))
                        }
                    }
                    if (s.isMetro) items(s.items, key = { it.code }) { r ->
                        AddRegionRow(r.sigungu, r.sido, tag = "addRegion.${r.code}") { viewModel.add(ActiveRegion(ActiveRegion.SIGUNGU, r.code, r.name)) }
                    }
                }
            }
        }
    }
    CustomDialog(
        title = stringResource(R.string.regions_max_notice), content = null, leftButtonText = null, rightButtonText = stringResource(R.string.common_confirm),
        buttonCount = 1, alignment = ContentAlignment.Middle, onDismiss = viewModel::dismissMax, onRightButtonClick = viewModel::dismissMax, showPopup = s.maxReached,
    )
}
