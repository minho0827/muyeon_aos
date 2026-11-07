package com.muyeon.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.CategoryColors

/** 카드 아래 복숭아색 태그 — 웹 components/muyeon/CardTags.js(#fdeee9 / #d97a5a · 11 semibold · 최대 [max]개). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CardTags(tags: List<String>, modifier: Modifier = Modifier, max: Int = 3) {
    val list = tags.filter { it.isNotBlank() }.take(max)
    if (list.isEmpty()) return
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        list.forEach { t ->
            Text(
                t, style = AppTypography.Pill11, color = CategoryColors.CardTag.fg, maxLines = 1,
                modifier = Modifier.background(CategoryColors.CardTag.bg, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }
    }
}
