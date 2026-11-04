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
import com.muyeon.app.ui.common.*
import com.muyeon.app.ui.common.market.*
import com.muyeon.app.ui.space.SpaceDesign
import com.muyeon.app.ui.space.SpaceReservationRow
import kotlinx.coroutines.launch

internal fun wonOrDash(v: Int?) = v?.let { "${SpaceDesign.won(it)}원" } ?: "-"

/** 예약 상태 배지 — 확정은 주황 채움, 결제 대기·요청은 연주황, 나머지는 회색. */
@Composable
fun ReservationStatusBadge(r: SpaceReservationRow, label: String = r.statusLabel) {
    val (fg, bg) = when {
        r.awaitingHost -> MuyeonColors.primary to SpaceDesign.primaryTint
        r.status == "PAID" || r.status == "CONFIRMED" -> Color.White to MuyeonColors.primary
        r.status == "PENDING_PAYMENT" || r.status == "REQUESTED" -> MuyeonColors.primary to SpaceDesign.primaryTint
        else -> MuyeonColors.textSub to MuyeonList.ink100
    }
    MarketStatusBadge(label, RESERVATION_BADGE_WIDTH, fg = fg, bg = bg)
}

/** 예약 상태 배지 폭(가장 긴 "결제 시간 초과" 기준) — 취소·결제로 상태가 바뀌어도 배지·옆 이름이 밀리지 않게. */
val RESERVATION_BADGE_WIDTH = 92.dp

/** 예약 카드 틀 — 썸네일 · 상태(· 예약자) · 공간 · 일시 · 인원·금액 · 아래 칸(footer, 쓰는 곳이 고른다). */
@Composable
fun ReservationCardFrame(
    r: SpaceReservationRow,
    bookerName: String?,
    onClick: () -> Unit,
    statusLabel: String = r.statusLabel,
    footer: @Composable ColumnScope.() -> Unit = {},
) {
    Column(Modifier.fillMaxWidth().clip(MuyeonList.cardShape).background(Color.White).clickable(onClick = onClick).padding(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MarketThumb(r.spaceImage, Modifier.size(72.dp).clip(RoundedCornerShape(10.dp)))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ReservationStatusBadge(r, statusLabel)
                    bookerName?.let { Text(it, fontFamily = customFontFamily, fontSize = 13.sp, color = MuyeonColors.textSub, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                }
                Text(r.spaceName, fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = MuyeonColors.textHead, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(r.whenText, fontFamily = customFontFamily, fontSize = 13.sp, color = MuyeonColors.textSub, style = TabularNums, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    listOfNotNull(r.headcount?.let { "${it}명" }, wonOrDash(r.amount)).joinToString(" · "),
                    fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MuyeonColors.textHead,
                    style = TabularNums, maxLines = 1,
                )
            }
        }
        footer()
    }
}

/** 카드 아래 버튼 줄. */
@Composable
fun ReservationActions(content: @Composable RowScope.() -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), content = content)
}

/** 예약 카드 자리(불러오는 중) — 썸네일 72 + 글 4줄(배지 줄·공간·일시·금액), ReservationCardFrame 과 같은 높이. */
@Composable
fun ReservationCardSkeleton() {
    Column(Modifier.fillMaxWidth().clip(MuyeonList.cardShape).background(Color.White).padding(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(72.dp).skeleton(10))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(Modifier.width(RESERVATION_BADGE_WIDTH)) { SkeletonLine(12) }
                SkeletonLine(15, 0.7f)
                SkeletonLine(13, 0.6f)
                SkeletonLine(13, 0.4f)
            }
        }
    }
}

/** 예약 목록 자리 N장. */
@Composable
fun ReservationListSkeleton(count: Int = 5) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(count) { ReservationCardSkeleton() }
    }
}

/** 응답 기한 줄 — "{기한}까지 호스트 응답"(예약자) / "{기한}까지 수락해 주세요"(호스트). 카드 아래 칸에 쓴다. */
@Composable
fun ReservationDeadlineLine(text: String) {
    Text(
        text, fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = MuyeonColors.primary,
        style = TabularNums, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp),
    )
}
