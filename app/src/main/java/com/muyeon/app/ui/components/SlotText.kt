package com.muyeon.app.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow

/**
 * ★ 2026-11-08 카드 글 줄 — 값이 없어도 자리는 남긴다(투명). 같은 줄의 카드들 높이가 데이터마다 달라지지 않게(사용자 요청).
 *   [lines] 줄 수만큼 자리를 잡는다(minLines = maxLines).
 */
@Composable
fun SlotText(text: String?, style: TextStyle, color: Color, modifier: Modifier = Modifier, lines: Int = 1) {
    val empty = text.isNullOrBlank()
    Text(
        if (empty) " " else text!!, style = style, color = color, maxLines = lines, minLines = lines, overflow = TextOverflow.Ellipsis,
        modifier = modifier.then(if (empty) Modifier.alpha(0f) else Modifier),
    )
}
