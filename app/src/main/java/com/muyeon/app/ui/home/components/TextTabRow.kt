package com.muyeon.app.ui.home.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.muyeon.app.ui.components.ChipOption
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography

/** 글자 탭 줄 — 웹 .tabs.type-text(14 semibold 회색, 고른 것은 진하게). 클래식 홈 세부 칩. */
@Composable
fun TextTabRow(options: List<ChipOption>, selected: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    LazyRow(modifier, contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(0.dp)) {
        items(options, key = { it.value }) { o ->
            Text(o.text, style = AppTypography.Body14SemiBold, color = if (o.value == selected) AppColors.TextHead else AppColors.TextHint,
                modifier = Modifier.clickable { onSelect(o.value) }.padding(horizontal = 8.dp, vertical = 6.dp))
        }
    }
}
