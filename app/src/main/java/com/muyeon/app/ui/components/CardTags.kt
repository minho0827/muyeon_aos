package com.muyeon.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.CategoryColors

/**
 * 카드 아래 복숭아색 태그 — 웹 components/muyeon/CardTags.js(#fdeee9 / #d97a5a · 11 semibold · 최대 [max]개).
 *   ★ 2026-11-08 한 줄로만(넘치면 잘림) · 태그가 없어도 한 줄 자리를 남긴다 — 카드 높이를 맞추려고(사용자 요청).
 */
@Composable
fun CardTags(tags: List<String>, modifier: Modifier = Modifier, max: Int = 3) {
    val list = tags.filter { it.isNotBlank() }.take(max)
    Row(modifier.clipToBounds(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        (list.ifEmpty { listOf(" ") }).forEach { t ->
            Text(
                t, style = AppTypography.Pill11, color = CategoryColors.CardTag.fg, maxLines = 1, softWrap = false,
                modifier = Modifier.then(if (list.isEmpty()) Modifier.alpha(0f) else Modifier)
                    .background(CategoryColors.CardTag.bg, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }
    }
}
