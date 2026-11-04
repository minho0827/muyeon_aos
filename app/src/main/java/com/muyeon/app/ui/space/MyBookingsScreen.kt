package com.muyeon.app.ui.space

import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.material.icons.outlined.EventNote
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
import com.muyeon.app.ui.lesson.LessonPaymentActivity
import com.muyeon.app.ui.space.booking.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.launch

private fun nowKst(): String = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.KOREA).apply { timeZone = TimeZone.getTimeZone("Asia/Seoul") }.format(Date())

/**
 * 내 예약(예약자) — 예정 / 지난 / 취소 탭. 행 → 상세 시트(공간·일시·인원·금액·상태) + [예약 취소]
 *  → cancel-preview 로 "환불 예정 금액 N원(R%)" 확인 → cancel. 결제 대기 행은 [결제하기]로 결제 창을 다시 연다.
 */
@Composable
fun SpaceMyBookingsScreen(
    api: SpaceApi,
    onBack: () -> Unit,
    onOpenSpace: (Int) -> Unit,
    onChanged: () -> Unit,
    onMessage: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var rows by remember { mutableStateOf<List<SpaceReservationRow>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var tab by remember { mutableStateOf("upcoming") }
    var tick by remember { mutableIntStateOf(0) }
    var open by remember { mutableStateOf<SpaceReservationRow?>(null) }
    var cancelAsk by remember { mutableStateOf<Pair<SpaceReservationRow, Pair<Int, Int>>?>(null) }
    var busy by remember { mutableStateOf(false) }

    LaunchedEffect(tick) {
        if (!api.isLoggedIn) { error = "로그인 후 이용할 수 있어요."; loading = false; return@LaunchedEffect }
        api.myReservations().onSuccess { rows = it; error = null }.onFailure { error = it.message }
        loading = false
    }
    val pay = rememberLauncherForActivityResult(LessonPaymentActivity.PathContract()) { paid -> if (paid) onChanged(); tick++ }

    val now = remember(rows) { nowKst() }
    val shown = when (tab) {
        "upcoming" -> rows.filter { !it.isCanceled && !it.isPast(now) }.sortedBy { "${it.date} ${it.startTime}" }
        "past" -> rows.filter { !it.isCanceled && it.isPast(now) }
        else -> rows.filter { it.isCanceled }
    }

    fun askCancel(r: SpaceReservationRow) {
        if (busy) return
        busy = true
        scope.launch {
            if (r.status == "PENDING_PAYMENT") cancelAsk = r to (0 to 0)
            else api.cancelPreview(r.id).onSuccess { cancelAsk = r to it }.onFailure { onMessage(it.message ?: "환불 금액을 확인하지 못했어요.") }
            busy = false
        }
    }
    fun pay(r: SpaceReservationRow) = pay.launch(LessonPaymentActivity.PaymentPage("/spacePay/${r.id}", "대관료 결제"))

    Column(Modifier.fillMaxSize().background(MuyeonList.pageBg)) {
        MuyeonListHeader("내 공간 예약", onBack = onBack)
        MuyeonPillTabs(
            listOf(MuyeonPillTab("upcoming", "예정"), MuyeonPillTab("past", "지난"), MuyeonPillTab("canceled", "취소")),
            tab, { tab = it }, tagPrefix = "space.bookings.tab", fixedWeight = true,
        )
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                loading -> ReservationListSkeleton()  // 실제 카드 높이의 자리(스피너가 커지며 바뀌지 않게)
                shown.isEmpty() -> MuyeonEmptyState(error ?: "예약 내역이 없어요.", icon = Icons.Outlined.EventNote, modifier = Modifier.align(Alignment.Center))
                else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(shown, key = { it.id }) { r ->
                        // 결제 대기만 [결제하기][예약 취소] 줄이 붙는다.
                        when {
                            r.status == "PENDING_PAYMENT" -> MyPendingBookingRow(r, onClick = { open = r }, onPay = { pay(r) }, onCancel = { askCancel(r) })
                            r.awaitingHost -> MyAwaitingBookingRow(r) { open = r }
                            else -> MyBookingRow(r) { open = r }
                        }
                    }
                }
            }
        }
    }

    open?.let { r ->
        val cancellable = r.status == "PAID" || r.status == "PENDING_PAYMENT"
        BookingDetailSheet(
            r,
            onOpenSpace = { open = null; onOpenSpace(r.spaceId) },
            onCancel = if (cancellable) ({ open = null; askCancel(r) }) else null,
            onDismiss = { open = null },
        )
    }

    cancelAsk?.let { (r, preview) ->
        CancelConfirmDialog(
            r, preview.first, preview.second,
            onConfirm = {
                scope.launch {
                    api.cancel(r.id).onSuccess { onMessage("예약을 취소했어요."); onChanged(); tick++ }
                        .onFailure { onMessage(it.message ?: "취소하지 못했어요.") }
                }
            },
            onDismiss = { cancelAsk = null },
        )
    }
}
