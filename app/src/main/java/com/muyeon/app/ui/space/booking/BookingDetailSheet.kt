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
import kotlinx.coroutines.launch

/** 예약 상세 표 줄 — 없는 값은 뺀다. */
fun bookingInfoRows(r: SpaceReservationRow): List<Pair<String, String>> = buildList {
    add("공간" to r.spaceName)
    r.spaceRegion?.let { add("지역" to it) }
    add("일시" to r.whenText)
    r.headcount?.let { add("인원" to "${it}명") }
    add("금액" to wonOrDash(r.amount))
    add("상태" to r.bookerStatusLabel)
    r.respondByText?.takeIf { r.awaitingHost }?.let { add("응답 기한" to "${it}까지 호스트 응답") }
    r.refundAmount?.takeIf { r.isCanceled }?.let { add("환불 금액" to wonOrDash(it)) }
    r.message?.let { add("요청사항" to it) }
}

/** 예약 상세 시트 — 표 + [공간 보기] (+ 취소할 수 있으면 [예약 취소]). onCancel 은 취소 가능할 때만 넘긴다. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingDetailSheet(r: SpaceReservationRow, onOpenSpace: () -> Unit, onCancel: (() -> Unit)?, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = Color.White) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("예약 상세", fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MuyeonColors.textHead)
            MarketInfoTable(bookingInfoRows(r))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 16.dp)) {
                Box(Modifier.weight(1f)) { StageBottomButton("공간 보기", onClick = onOpenSpace) }
                onCancel?.let { CancelBookingButton(it) }
            }
        }
    }
}

@Composable
private fun RowScope.CancelBookingButton(onClick: () -> Unit) {
    Box(
        Modifier.weight(1f).height(56.dp).clip(RoundedCornerShape(16.dp)).background(MuyeonList.ink100).clickable(onClick = onClick),
        Alignment.Center,
    ) { Text("예약 취소", fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = MuyeonColors.danger) }
}
