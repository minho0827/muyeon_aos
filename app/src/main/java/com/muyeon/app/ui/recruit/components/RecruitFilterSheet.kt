package com.muyeon.app.ui.recruit.components

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.muyeon.app.R
import com.muyeon.app.ui.components.ChipOption
import com.muyeon.app.ui.components.SheetAction
import com.muyeon.app.ui.components.SheetButton
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography

/** 필터 묶음 한 줄 — [multi] 여러 개(웹 구인·구직) / 아니면 한 개(웹 대타). 빈 선택 = 전체. */
data class FilterGroup(val key: String, val title: String, val options: List<ChipOption>, val multi: Boolean)

/**
 * 채용 탭 필터 시트 — 묶음마다 제목 + 칩(고른 칩은 검정). [초기화] [적용].
 *   웹 JobBoard·SubList 필터 시트와 같은 묶음·값. 고른 값은 묶음 key → 값 집합.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RecruitFilterSheet(groups: List<FilterGroup>, initial: Map<String, Set<String>>, onApply: (Map<String, Set<String>>) -> Unit, onDismiss: () -> Unit) {
    var picked by remember { mutableStateOf(initial) }
    ModalBottomSheet(onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = AppColors.Surface) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).navigationBarsPadding()) {
            Text(stringResource(R.string.recruit_filter), style = AppTypography.Title18, color = AppColors.TextHead, modifier = Modifier.padding(bottom = 8.dp))
            groups.forEach { g ->
                Text(g.title, style = AppTypography.Section16, color = AppColors.TextHead, modifier = Modifier.padding(top = 18.dp, bottom = 10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    g.options.forEach { o ->
                        val cur = picked[g.key].orEmpty()
                        val on = o.value in cur
                        Text(
                            o.text, style = AppTypography.Chip14, color = if (on) AppColors.TextOnPrimary else AppColors.ChipText,
                            modifier = Modifier
                                .background(if (on) AppColors.ChipSelectedBg else AppColors.Surface, RoundedCornerShape(50))
                                .border(1.dp, if (on) AppColors.ChipSelectedBg else AppColors.ChipBorder, RoundedCornerShape(50))
                                .clickable {
                                    val next = when {
                                        on -> cur - o.value
                                        g.multi -> cur + o.value
                                        else -> setOf(o.value)
                                    }
                                    picked = picked + (g.key to next)
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                        )
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SheetButton(SheetAction(stringResource(R.string.recruit_filter_reset), false) { picked = emptyMap() }, Modifier.weight(1f))
                SheetButton(SheetAction(stringResource(R.string.recruit_filter_apply), true) { onApply(picked) }, Modifier.weight(1f))
            }
        }
    }
}
