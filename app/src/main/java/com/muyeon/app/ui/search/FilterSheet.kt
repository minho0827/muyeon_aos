package com.muyeon.app.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.muyeon.app.R
import com.muyeon.app.ui.common.FormChipWrap
import com.muyeon.app.ui.common.FormField
import com.muyeon.app.ui.common.market.SheetActions
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import androidx.compose.ui.unit.dp
import androidx.annotation.StringRes
import com.muyeon.domain.model.search.SearchFilter

/**
 * 필터 시트 — 섹션마다 제목 + 줄바꿈 칩(여러 개 고름). 아래 [초기화] [N개 보기].
 *   시트 안 선택은 임시값 — [N개 보기]를 눌러야 [onApply]. 닫으면 버린다. [초기화]는 이 시트 섹션만 비운다.
 *   [sections] 하나만 주면 빠른 칩([장르▾])용 작은 시트.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSheet(
    @StringRes titleRes: Int,
    sections: List<FilterSection>,
    initial: SearchFilter,
    count: Int?,
    @StringRes showCountRes: Int,
    onDraft: (SearchFilter) -> Unit,
    onApply: (SearchFilter) -> Unit,
    onDismiss: () -> Unit,
) {
    var draft by remember { mutableStateOf(initial) }
    LaunchedEffect(draft) { onDraft(draft) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = Color.White) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding()) {
            Text(stringResource(titleRes), style = AppTypography.Title18, color = AppColors.TextHead, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
            Column(
                Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                sections.forEach { s -> FilterSectionChips(s, draft) { v -> draft = draft.toggle(s.key, v, s.single) } }
                Spacer(Modifier.height(4.dp))
            }
            SheetActions(
                onReset = { draft = draft.cleared(sections.map { it.key }) },
                onApply = { onApply(draft) },
                applyLabel = count?.let { stringResource(showCountRes, it) } ?: stringResource(R.string.search_apply),
            )
        }
    }
}

/** 섹션 하나 — 제목 + 칩(줄바꿈). */
@Composable
fun FilterSectionChips(section: FilterSection, selection: SearchFilter, onTap: (String) -> Unit) {
    val on = selection.of(section.key)
    FormField(stringResource(section.titleRes)) {
        FormChipWrap(section.options.map { it.value to it.text() }, { it in on }, onTap)
    }
}
