package com.muyeon.app.ui.chat.dealbar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.ui.common.MuyeonColors
import com.muyeon.app.theme.customFontFamily

/**
 * ★ 2026-11-14 상단 고정 바의 버튼 한 개(당근 '약속잡기' 캡슐, iOS DealBarActionButton 1:1).
 *  [primary] = 내 차례의 첫 버튼 — 주황 채움. 나머지는 흰 바탕 + 테두리.
 */
@Composable
fun DealBarActionButton(action: DealAction, primary: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(10.dp)
    val bg = if (primary) MuyeonColors.primary else Color.White
    val fg = if (primary) Color.White else MuyeonColors.textHead
    Row(
        modifier
            .height(40.dp)
            .clip(shape)
            .background(bg)
            .then(if (primary) Modifier else Modifier.border(1.dp, MuyeonColors.border, shape))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp)
            .testTag("dealbar.${action.name}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
    ) {
        Icon(iconOf(action), null, tint = fg, modifier = Modifier.size(17.dp))
        Text(
            action.label, color = fg, maxLines = 1,
            fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 17.sp,
        )
    }
}

private fun iconOf(a: DealAction): ImageVector = when (a) {
    DealAction.VIEW_QUOTES, DealAction.VIEW_MY_QUOTE -> Icons.Filled.Description
    DealAction.ACCEPT, DealAction.HIRE -> Icons.Filled.HowToReg
    DealAction.SCHEDULE -> Icons.Filled.CalendarMonth
    DealAction.CHECK_PROPOSAL -> Icons.Filled.EventAvailable
    DealAction.VIEW_LESSON -> Icons.Filled.EventAvailable
    DealAction.COMPLETE_LESSON -> Icons.Filled.CheckCircle
    DealAction.REVIEW -> Icons.Filled.EditNote
    DealAction.CALL -> Icons.Filled.Call
    DealAction.ACADEMY_INFO -> Icons.Filled.School
}
