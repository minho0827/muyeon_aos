package com.muyeon.app.ui.region

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muyeon.app.R
import com.muyeon.app.common_components.dialog.ContentAlignment
import com.muyeon.app.common_components.dialog.CustomDialog
import com.muyeon.app.ui.components.MainTopBar
import com.muyeon.app.ui.components.PrimaryBottomButton
import com.muyeon.app.ui.region.components.RegionCard
import com.muyeon.app.ui.region.components.RoundOutlinedButton
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.Dimensions
import org.koin.androidx.compose.koinViewModel

/**
 * ★ 2026-11-08 (b4) 관심 지역 — 웹 /myRegions 그대로: [관심 지역  (지역 추가)(지역 삭제)] · 카드(이름 · 단위 · ★ 대표) · 빈 안내 · 최대 5개 안내 · 아래 [확인](→ MY).
 *   [지역 삭제] 를 누르면 카드 왼쪽에 지우기 단추가 나온다(대표는 지울 수 없다 — 팝업). 별을 누르면 대표 지역(홈·목록 지역).
 */
@Composable
fun MyRegionsScreen(onBack: () -> Unit, onAdd: () -> Unit, onDone: () -> Unit, viewModel: MyRegionsViewModel = koinViewModel()) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }
    Column(Modifier.fillMaxSize().background(AppColors.Surface).navigationBarsPadding()) {
        MainTopBar(title = stringResource(R.string.regions_title), primary = false, onBack = onBack)
        LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
            item {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = Dimensions.gutter, vertical = Dimensions.spacingL),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimensions.spacingS),
                ) {
                    Text(stringResource(R.string.regions_title), style = AppTypography.Title18, color = AppColors.TextHead, modifier = Modifier.weight(1f))
                    RoundOutlinedButton(stringResource(R.string.regions_add), tag = "regions.add") { if (viewModel.canOpenAdd()) onAdd() }
                    RoundOutlinedButton(stringResource(if (s.editMode) R.string.regions_delete_done else R.string.regions_delete), on = s.editMode, tag = "regions.edit", onClick = viewModel::toggleEdit)
                }
            }
            items(s.saved.regions, key = { it.code }) { r ->
                RegionCard(
                    region = r, representative = r.code == s.saved.repCode, editMode = s.editMode,
                    onRemove = { viewModel.remove(r.code) }, onRepresent = { viewModel.setRepresentative(r.code) },
                )
            }
            if (s.saved.regions.isEmpty()) item { Notice(stringResource(R.string.regions_empty)) }
            item { Notice(stringResource(R.string.regions_max_notice)) }
        }
        PrimaryBottomButton(stringResource(R.string.common_confirm), onClick = onDone, atScreenBottom = false)
    }
    s.message?.let { m ->
        CustomDialog(
            title = stringResource(m), content = null, leftButtonText = null, rightButtonText = stringResource(R.string.common_confirm),
            buttonCount = 1, alignment = ContentAlignment.Middle, onDismiss = viewModel::dismissMessage, onRightButtonClick = viewModel::dismissMessage, showPopup = true,
        )
    }
}

/** 웹 .notice-14 — 왼쪽 정렬(좁은 화면에서 줄마다 가운데로 몰리지 않게, 웹 10-25). */
@Composable
private fun Notice(text: String) {
    Text(text, style = AppTypography.Body14, color = AppColors.TextSub, modifier = Modifier.fillMaxWidth().padding(horizontal = Dimensions.gutter, vertical = Dimensions.spacingS))
}
