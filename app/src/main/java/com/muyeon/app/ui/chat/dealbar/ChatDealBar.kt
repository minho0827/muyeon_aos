package com.muyeon.app.ui.chat.dealbar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.ui.common.MuyeonColors
import com.muyeon.app.theme.customFontFamily

/**
 * ★ 2026-11-14 채팅방 상단 고정 바(당근 거래 바, iOS ChatDealBar 1:1).
 *  요약 줄(상태 · 레슨명 · 가격/일정) + 내 역할의 할 일 버튼(최대 3개). 메시지를 아무리 내려도 늘 이 자리에 있다.
 *  무엇을 보여 줄지는 [ChatDealBarResolver] 가 정하고, 여기는 그리기만 한다.
 */
@Composable
fun ChatDealBar(model: DealBarModel, onAction: (DealAction) -> Unit, onOpenCycles: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(Color.White).testTag("dealbar")) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                model.status,
                color = if (model.statusEmphasis) MuyeonColors.primary else MuyeonColors.textSub,
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 17.sp,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                model.title, color = MuyeonColors.textHead, maxLines = 1, overflow = TextOverflow.Ellipsis,
                fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 18.sp,
                modifier = Modifier.weight(1f, fill = false),
            )
            Spacer(Modifier.weight(1f))
            if (model.cyclesCount >= 2) CyclesChip(model.cyclesCount, onOpenCycles)
        }
        model.detail?.let {
            Text(
                it, color = MuyeonColors.textSub, maxLines = 1, overflow = TextOverflow.Ellipsis,
                fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 16.sp,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 3.dp),
            )
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            model.actions.forEachIndexed { idx, a ->
                DealBarActionButton(
                    a, primary = idx == 0 && model.statusEmphasis, onClick = { onAction(a) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        HorizontalDivider(color = MuyeonColors.border, thickness = 1.dp)
    }
}

/** 한 방에 레슨이 여러 건이면 '진행 중 n건' — 누르면 기존 LessonCyclesSheet. */
@Composable
private fun CyclesChip(count: Int, onClick: () -> Unit) {
    Text(
        "진행 중 ${count}건",
        color = MuyeonColors.primary,
        fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp, lineHeight = 15.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MuyeonColors.tileHeld)
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 4.dp)
            .testTag("dealbar.cycles"),
    )
}
