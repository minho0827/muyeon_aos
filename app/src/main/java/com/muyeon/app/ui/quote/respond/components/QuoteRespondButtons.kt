package com.muyeon.app.ui.quote.respond.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.muyeon.app.ui.components.SheetAction
import com.muyeon.app.ui.components.SheetButton
import com.muyeon.app.ui.theme.Dimensions

/**
 * 화면 아래 버튼 묶음 — 웹 .btn-group(btn-md filled primary / outlined). 가로로 나란히, [vertical] 이면 위아래(웹 .flex-col · 간격 8).
 *  버튼 모양은 공용 [SheetButton](높이 48 · 모서리 8) 그대로.
 */
@Composable
fun QuoteRespondButtons(actions: List<SheetAction>, modifier: Modifier = Modifier, vertical: Boolean = false) {
    if (vertical) {
        Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Dimensions.spacingS)) {
            actions.forEach { SheetButton(it, Modifier.fillMaxWidth()) }
        }
    } else {
        Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimensions.spacingS)) {
            actions.forEach { SheetButton(it, Modifier.weight(1f)) }
        }
    }
}
