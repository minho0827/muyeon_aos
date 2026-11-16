package com.muyeon.app.ui.chat.dealbar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors

/**
 * ★ 2026-11-16 채팅방 상단 대타 카드 — 공고 제목 · 날짜·시간·금액 · 상태 칩 · 내 역할 버튼.
 *  무엇을 보여 줄지는 [SubContextResolver] 가 정하고, 여기는 그리기만 한다(레슨 바 ChatDealBar 와 같은 모양).
 *  대타 건이 여러 개면 대표 한 건만 보이고 '진행 중 N건'을 누르면 나머지가 펼쳐진다.
 */
@Composable
fun SubContextCard(model: SubCardModel, onAction: (SubAction, SubCardItem) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().background(Color.White).testTag("subcard")) {
        SubItemBlock(model.primary, onAction) {
            if (model.count >= 2) SubCountChip(model.count, expanded) { expanded = !expanded }
        }
        if (expanded) model.others.forEach { other ->
            HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MuyeonColors.border, thickness = 1.dp)
            SubItemBlock(other, onAction) {}
        }
        HorizontalDivider(color = MuyeonColors.border, thickness = 1.dp)
    }
}

@Composable
private fun SubItemBlock(item: SubCardItem, onAction: (SubAction, SubCardItem) -> Unit, trailing: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SubStatusChip(item.status, item.statusEmphasis)
            Spacer(Modifier.width(6.dp))
            Text(
                item.title, color = MuyeonColors.textHead, maxLines = 1, overflow = TextOverflow.Ellipsis,
                fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 18.sp,
                modifier = Modifier.weight(1f, fill = false),
            )
            Spacer(Modifier.weight(1f))
            trailing()
        }
        item.detail?.let {
            Text(
                it, color = MuyeonColors.textSub, maxLines = 1, overflow = TextOverflow.Ellipsis,
                fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 16.sp,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 3.dp),
            )
        }
        if (item.actions.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item.actions.forEach { a ->
                    SubActionButton(a, primary = a == SubAction.CONFIRM, onClick = { onAction(a, item) }, modifier = Modifier.weight(1f))
                }
            }
        } else {
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun SubStatusChip(text: String, emphasis: Boolean) {
    Text(
        text,
        color = if (emphasis) MuyeonColors.primary else MuyeonColors.textSub,
        fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp, lineHeight = 15.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (emphasis) MuyeonColors.primary.copy(alpha = 0.12f) else Color(0xFFF0F0F2))
            .padding(horizontal = 8.dp, vertical = 3.dp)
            .testTag("subcard.status"),
    )
}

@Composable
private fun SubCountChip(count: Int, expanded: Boolean, onClick: () -> Unit) {
    Text(
        if (expanded) "접기" else "진행 중 ${count}건",
        color = MuyeonColors.primary,
        fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp, lineHeight = 15.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MuyeonColors.tileHeld)
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 4.dp)
            .testTag("subcard.count"),
    )
}

/** 레슨 바 버튼(DealBarActionButton)과 같은 캡슐. [primary] = 주황 채움. */
@Composable
private fun SubActionButton(action: SubAction, primary: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(10.dp)
    val fg = if (primary) Color.White else MuyeonColors.textHead
    Row(
        modifier
            .height(40.dp)
            .clip(shape)
            .background(if (primary) MuyeonColors.primary else Color.White)
            .then(if (primary) Modifier else Modifier.border(1.dp, MuyeonColors.border, shape))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp)
            .testTag("subcard.${action.name}"),
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

private fun iconOf(a: SubAction): ImageVector = when (a) {
    SubAction.VIEW_APPLICATION, SubAction.VIEW_POSTING -> Icons.Filled.Description
    SubAction.CONFIRM -> Icons.Filled.HowToReg
    SubAction.WITHDRAW -> Icons.Filled.Close
    SubAction.CANCEL_CONFIRMED -> Icons.Filled.EventBusy
}

/** 대타 카드 아래 한 번 보이는 안내(방마다 처음 한 번, [SubChatNotice]). */
@Composable
fun SubChatNoticeLine() {
    Text(
        SubChatNotice.TEXT,
        color = MuyeonColors.textSub,
        fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp,
        modifier = Modifier.fillMaxWidth().background(MuyeonColors.groupedBg)
            .padding(horizontal = 16.dp, vertical = 8.dp).testTag("subcard.notice"),
    )
}

/** 방마다 처음 한 번만 — 안내를 이미 본 방은 다시 띄우지 않는다. */
object SubChatNotice {
    const val TEXT = "근무 조건은 채팅에서 확인하고, 확정은 위 버튼으로 해요."
    private const val PREFS = "chat_sub_notice"

    fun consume(ctx: android.content.Context, roomId: Int): Boolean {
        val p = ctx.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)
        val key = "room_$roomId"
        if (p.getBoolean(key, false)) return false
        p.edit().putBoolean(key, true).apply()
        return true
    }
}
