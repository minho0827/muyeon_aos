package com.muyeon.app.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.muyeon.app.R
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography

/** 정렬 시트 — 한 줄씩, 고른 줄은 연한 주황 바탕 + 주황 굵은 글자 + 체크. 누르면 바로 고르고 닫힌다. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortSheet(options: List<SortOption>, selected: String, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = Color.White) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 12.dp)) {
            Text(stringResource(R.string.search_sort), style = AppTypography.Title18, color = AppColors.TextHead, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
            options.forEach { o ->
                val on = o.key == selected
                Row(
                    Modifier.fillMaxWidth().height(52.dp)
                        .background(if (on) AppColors.PrimarySoft else Color.Transparent)
                        .clickable { onSelect(o.key) }
                        .semantics { this.selected = on }
                        .padding(horizontal = 20.dp)
                        .testTag("search.sort.${o.key}"),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(o.labelRes),
                        style = AppTypography.Body15.copy(fontWeight = if (on) FontWeight.Bold else FontWeight.Normal),
                        color = if (on) AppColors.Primary else AppColors.TextHead,
                        modifier = Modifier.weight(1f),
                    )
                    if (on) Icon(Icons.Filled.Check, null, tint = AppColors.Primary, modifier = Modifier.size(20.dp))
                    else Spacer(Modifier.size(20.dp))
                }
            }
        }
    }
}
