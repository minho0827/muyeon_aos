package com.muyeon.app.ui.space.booking

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.castingstage.components.StageBottomButton
import com.muyeon.app.ui.common.*
import com.muyeon.app.ui.common.market.*
import com.muyeon.app.ui.space.SpaceReservationRow
import com.muyeon.app.ui.space.kstDateTime
import kotlinx.coroutines.launch

/** 호스트 예약 상세 표 — 예약자·이용 일시·인원·결제 금액·결제 시각·요청사항(없으면 "없음"). 전화번호는 보이지 않는다(채팅으로만). */
fun hostBookingRows(r: SpaceReservationRow): List<Pair<String, String>> = buildList {
    add("예약자" to (r.bookerName ?: "-"))
    add("이용 일시" to r.rangeText)
    add("인원" to (r.headcount?.let { "${it}명" } ?: "-"))
    add("결제 금액" to wonOrDash(r.amount))
    add("결제 시각" to (kstDateTime(r.paidAt) ?: "-"))
    add("요청사항" to (r.message?.takeIf { it.isNotBlank() } ?: "없음"))
}

/** 취소 정보 표 — 취소된 예약만(아니면 null). */
fun hostCancelRows(r: SpaceReservationRow): List<Pair<String, String>>? = if (!r.status.startsWith("CANCELED")) null else listOf(
    "취소 시각" to (kstDateTime(r.canceledAt) ?: "-"),
    "취소 사유" to (r.cancelReason?.takeIf { it.isNotBlank() } ?: "-"),
    "환불 금액" to wonOrDash(r.refundAmount),
)

/**
 * 호스트 예약 상세 시트 — 상태·예약번호 → 공간(썸네일·이름·세부공간) → 정보 표 → (취소면) 취소 정보 → 하단 버튼.
 *  하단 버튼은 쓰는 쪽이 고른다: [HostBookingChatActions] 또는 [HostBookingChatCancelActions].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostBookingDetailSheet(r: SpaceReservationRow, onDismiss: () -> Unit, actions: @Composable RowScope.() -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = Color.White) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            HostBookingHeader(r)
            HostBookingSpace(r)
            MarketInfoTable(hostBookingRows(r))
            hostCancelRows(r)?.let { HostBookingCancelInfo(it) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 16.dp), content = actions)
        }
    }
}

/** 상태 배지(폭 고정) + "예약 #id". */
@Composable
private fun HostBookingHeader(r: SpaceReservationRow) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ReservationStatusBadge(r)
        Text("예약 #${r.id}", fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MuyeonColors.textHead, style = TabularNums)
    }
}

/** 공간 썸네일 + 이름 + 세부공간(없으면 빈 줄 — 높이 고정). */
@Composable
private fun HostBookingSpace(r: SpaceReservationRow) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        MarketThumb(r.spaceImage, Modifier.size(56.dp).clip(RoundedCornerShape(10.dp)))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(r.spaceName, fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = MuyeonColors.textHead, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(r.optionLabel.orEmpty(), fontFamily = customFontFamily, fontSize = 13.sp, color = MuyeonColors.textSub, minLines = 1, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun HostBookingCancelInfo(rows: List<Pair<String, String>>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("취소 정보", fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = MuyeonColors.textHead)
        MarketInfoTable(rows)
    }
}

/** 하단 — [예약자에게 채팅]만. */
@Composable
fun RowScope.HostBookingChatActions(chatBusy: Boolean, onChat: () -> Unit) {
    Box(Modifier.weight(1f)) { StageBottomButton("예약자에게 채팅", busy = chatBusy, onClick = onChat) }
}

/** 하단 — [예약자에게 채팅] + [취소(전액 환불)](결제 완료·시작 전). */
@Composable
fun RowScope.HostBookingChatCancelActions(chatBusy: Boolean, onChat: () -> Unit, onCancel: () -> Unit) {
    HostBookingChatActions(chatBusy, onChat)
    Box(
        Modifier.weight(1f).height(56.dp).clip(RoundedCornerShape(16.dp)).background(MuyeonList.ink100).clickable(onClick = onCancel),
        Alignment.Center,
    ) { Text("취소(전액 환불)", fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = MuyeonColors.danger) }
}
