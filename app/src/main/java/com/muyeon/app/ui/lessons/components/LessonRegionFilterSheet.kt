package com.muyeon.app.ui.lessons.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.muyeon.app.R
import com.muyeon.app.ui.components.ChipOption
import com.muyeon.app.ui.components.ChipRow
import com.muyeon.app.ui.components.SheetAction
import com.muyeon.app.ui.components.SheetButton
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.Dimensions

/** 지역(시·도) 필터 시트 — 웹 LessonTab data-id="lesson-filter": 제목 "필터" · "지역" 칩 줄 · [초기화][적용]. 고르는 즉시 반영. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonRegionFilterSheet(options: List<String>, selected: String?, onSelect: (String?) -> Unit, onDismiss: () -> Unit) {
    val all = stringResource(R.string.common_all)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = AppColors.Surface, dragHandle = null) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(vertical = Dimensions.gutter), verticalArrangement = Arrangement.spacedBy(Dimensions.spacingL)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = Dimensions.gutter), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.lesson_filter), style = AppTypography.Title18, color = AppColors.TextHead, modifier = Modifier.weight(1f))
                Icon(painterResource(R.drawable.ic_web_close), stringResource(R.string.common_close), tint = AppColors.TextHead, modifier = Modifier.size(Dimensions.iconL).clickable(onClick = onDismiss))
            }
            Text(stringResource(R.string.lesson_filter_region), style = AppTypography.Body14SemiBold, color = AppColors.TextBody, modifier = Modifier.padding(horizontal = Dimensions.gutter))
            ChipRow(
                options = listOf(ChipOption("", all)) + options.map { ChipOption(it, it) },
                selected = selected.orEmpty(),
                onSelect = { onSelect(it.ifEmpty { null }) },
                contentPadding = PaddingValues(horizontal = Dimensions.gutter),
            )
            Row(Modifier.fillMaxWidth().padding(horizontal = Dimensions.gutter), horizontalArrangement = Arrangement.spacedBy(Dimensions.spacingS)) {
                SheetButton(SheetAction(stringResource(R.string.lesson_filter_reset), false) { onSelect(null) }, Modifier.weight(1f))
                SheetButton(SheetAction(stringResource(R.string.lesson_filter_apply), true, onDismiss), Modifier.weight(1f))
            }
        }
    }
}
