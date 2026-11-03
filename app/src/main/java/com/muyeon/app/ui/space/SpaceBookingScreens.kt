package com.muyeon.app.ui.space

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material.icons.outlined.MeetingRoom
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.castingstage.components.StageBottomButton
import com.muyeon.app.ui.common.market.MarketBadge
import com.muyeon.app.ui.common.market.MarketInfoTable
import com.muyeon.app.ui.common.market.MarketThumb
import com.muyeon.app.ui.common.MuyeonColors
import com.muyeon.app.ui.common.MuyeonDialog
import com.muyeon.app.ui.common.MuyeonDialogAction
import com.muyeon.app.ui.common.MuyeonDialogKind
import com.muyeon.app.ui.common.MuyeonEmptyState
import com.muyeon.app.ui.common.MuyeonHeaderIcon
import com.muyeon.app.ui.common.MuyeonList
import com.muyeon.app.ui.common.MuyeonListHeader
import com.muyeon.app.ui.common.MuyeonPillTab
import com.muyeon.app.ui.common.MuyeonPillTabs
import com.muyeon.app.ui.lesson.LessonPaymentActivity
import com.muyeon.app.ui.common.market.MarketRowButton as RowButton
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private fun nowKst(): String = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.KOREA).apply { timeZone = TimeZone.getTimeZone("Asia/Seoul") }.format(Date())

private fun won(v: Int?) = v?.let { "${SpaceDesign.won(it)}원" } ?: "-"

@Composable
private fun statusBadge(r: SpaceReservationRow) {
    val (fg, bg) = when {
        r.status == "PAID" || r.status == "CONFIRMED" -> Color.White to MuyeonColors.primary
        r.status == "PENDING_PAYMENT" || r.status == "REQUESTED" -> MuyeonColors.primary to SpaceDesign.primaryTint
        else -> MuyeonColors.textSub to MuyeonList.ink100
    }
    MarketBadge(r.statusLabel, fg = fg, bg = bg)
}

@Composable
private fun ReservationRowCard(r: SpaceReservationRow, showBooker: Boolean, onClick: () -> Unit, actions: (@Composable RowScope.() -> Unit)? = null) {
    Column(Modifier.fillMaxWidth().clip(MuyeonList.cardShape).background(Color.White).clickable(onClick = onClick).padding(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MarketThumb(r.spaceImage, Modifier.size(72.dp).clip(RoundedCornerShape(10.dp)))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    statusBadge(r)
                    if (showBooker) r.bookerName?.let { Text(it, fontFamily = customFontFamily, fontSize = 13.sp, color = MuyeonColors.textSub) }
                }
                Text(r.spaceName, fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = MuyeonColors.textHead, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(r.whenText, fontFamily = customFontFamily, fontSize = 13.sp, color = MuyeonColors.textSub)
                Text(
                    listOfNotNull(r.headcount?.let { "${it}명" }, won(r.amount)).joinToString(" · "),
                    fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MuyeonColors.textHead,
                )
            }
        }
        if (actions != null) Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), content = actions)
    }
}

/**
 * 내 예약(예약자) — 예정 / 지난 / 취소 탭. 행 → 상세 시트(공간·일시·인원·금액·상태) + [예약 취소]
 *  → cancel-preview 로 "환불 예정 금액 N원(R%)" 확인 → cancel. 결제 대기 행은 [결제하기]로 결제 창을 다시 연다.
 */
@OptIn(ExperimentalMaterial3Api::class)
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

    Column(Modifier.fillMaxSize().background(MuyeonList.pageBg)) {
        MuyeonListHeader("내 공간 예약", onBack = onBack)
        MuyeonPillTabs(
            listOf(MuyeonPillTab("upcoming", "예정"), MuyeonPillTab("past", "지난"), MuyeonPillTab("canceled", "취소")),
            tab, { tab = it }, tagPrefix = "space.bookings.tab",
        )
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                loading -> CircularProgressIndicator(color = MuyeonColors.primary, modifier = Modifier.align(Alignment.Center))
                shown.isEmpty() -> MuyeonEmptyState(error ?: "예약 내역이 없어요.", icon = Icons.Outlined.EventNote, modifier = Modifier.align(Alignment.Center))
                else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(shown, key = { it.id }) { r ->
                        ReservationRowCard(r, showBooker = false, onClick = { open = r }, actions = if (r.status == "PENDING_PAYMENT") ({
                            RowButton("결제하기", Modifier.weight(1f)) { pay.launch(LessonPaymentActivity.PaymentPage("/spacePay/${r.id}", "대관료 결제")) }
                            RowButton("예약 취소", Modifier.weight(1f), danger = true) { askCancel(r) }
                        }) else null)
                    }
                }
            }
        }
    }

    open?.let { r ->
        ModalBottomSheet(onDismissRequest = { open = null }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = Color.White) {
            Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("예약 상세", fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MuyeonColors.textHead)
                MarketInfoTable(
                    buildList {
                        add("공간" to r.spaceName)
                        r.spaceRegion?.let { add("지역" to it) }
                        add("일시" to r.whenText)
                        r.headcount?.let { add("인원" to "${it}명") }
                        add("금액" to won(r.amount))
                        add("상태" to r.statusLabel)
                        r.refundAmount?.takeIf { r.isCanceled }?.let { add("환불 금액" to won(it)) }
                        r.message?.let { add("요청사항" to it) }
                    },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 16.dp)) {
                    Box(Modifier.weight(1f)) { StageBottomButton("공간 보기") { open = null; onOpenSpace(r.spaceId) } }
                    if (r.status == "PAID" || r.status == "PENDING_PAYMENT") {
                        Box(
                            Modifier.weight(1f).height(56.dp).clip(RoundedCornerShape(16.dp)).background(MuyeonList.ink100)
                                .clickable { open = null; askCancel(r) },
                            Alignment.Center,
                        ) { Text("예약 취소", fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = MuyeonColors.danger) }
                    }
                }
            }
        }
    }

    cancelAsk?.let { (r, preview) ->
        val (rate, refund) = preview
        MuyeonDialog(
            title = "예약을 취소할까요?",
            message = if (r.status == "PENDING_PAYMENT") "아직 결제하지 않은 예약이에요." else "환불 예정 금액 ${SpaceDesign.won(refund)}원(${rate}%)",
            actions = listOf(
                MuyeonDialogAction("예약 취소", MuyeonDialogKind.DESTRUCTIVE) {
                    scope.launch {
                        api.cancel(r.id).onSuccess { onMessage("예약을 취소했어요."); onChanged(); tick++ }
                            .onFailure { onMessage(it.message ?: "취소하지 못했어요.") }
                    }
                },
                MuyeonDialogAction("닫기", MuyeonDialogKind.CANCEL),
            ),
            onDismiss = { cancelAsk = null },
        )
    }
}

/** 호스트 — 내 공간 목록([수정][삭제]). */
@Composable
fun SpaceMineScreen(
    api: SpaceApi,
    reloadTick: Int,
    onBack: () -> Unit,
    onOpen: (Int) -> Unit,
    onEdit: (Int) -> Unit,
    onCreate: () -> Unit,
    onReservations: () -> Unit,
    onChanged: () -> Unit,
    onMessage: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var items by remember { mutableStateOf<List<SpaceListItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var deleteTarget by remember { mutableStateOf<SpaceListItem?>(null) }

    LaunchedEffect(reloadTick) {
        if (!api.isLoggedIn) { error = "로그인 후 이용할 수 있어요."; loading = false; return@LaunchedEffect }
        api.mySpaces().onSuccess { items = it; error = null }.onFailure { error = it.message }
        loading = false
    }

    Column(Modifier.fillMaxSize().background(MuyeonList.pageBg)) {
        MuyeonListHeader("내 공간", onBack = onBack) {
            Text(
                "예약 관리", fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = MuyeonColors.primary,
                modifier = Modifier.clickable(onClick = onReservations).padding(horizontal = 8.dp, vertical = 10.dp),
            )
            MuyeonHeaderIcon(Icons.Filled.Add, "공간 등록", onClick = onCreate)
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                loading -> CircularProgressIndicator(color = MuyeonColors.primary, modifier = Modifier.align(Alignment.Center))
                items.isEmpty() -> MuyeonEmptyState(
                    error ?: "아직 등록한 공간이 없어요.", icon = Icons.Outlined.MeetingRoom,
                    actionTitle = if (error == null) "공간 등록하기" else null, onAction = if (error == null) onCreate else null,
                    modifier = Modifier.align(Alignment.Center),
                )
                else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(items, key = { it.id }) { s ->
                        Column(Modifier.fillMaxWidth().clip(MuyeonList.cardShape).background(Color.White)) {
                            SpaceListCard(s, liked = null, onClick = { onOpen(s.id) }, onHeart = {})
                            Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                RowButton("수정", Modifier.weight(1f)) { onEdit(s.id) }
                                RowButton("삭제", Modifier.weight(1f), danger = true) { deleteTarget = s }
                            }
                        }
                    }
                }
            }
        }
    }

    deleteTarget?.let { t ->
        MuyeonDialog(
            title = "공간을 삭제할까요?",
            message = "결제된 예약이 있으면 삭제할 수 없어요.",
            actions = listOf(
                MuyeonDialogAction("삭제", MuyeonDialogKind.DESTRUCTIVE) {
                    scope.launch {
                        api.remove(t.id).onSuccess { items = items.filter { it.id != t.id }; onChanged() }
                            .onFailure { onMessage(it.message ?: "삭제하지 못했어요.") }
                    }
                },
                MuyeonDialogAction("취소", MuyeonDialogKind.CANCEL),
            ),
            onDismiss = { deleteTarget = null },
        )
    }
}

/** 호스트 — 예약 관리(날짜순, 상태 배지, 결제 완료 행 [취소(전액 환불)]). */
@Composable
fun SpaceHostReservationsScreen(
    api: SpaceApi,
    onBack: () -> Unit,
    onChanged: () -> Unit,
    onMessage: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var rows by remember { mutableStateOf<List<SpaceReservationRow>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var tick by remember { mutableIntStateOf(0) }
    var cancelTarget by remember { mutableStateOf<SpaceReservationRow?>(null) }

    LaunchedEffect(tick) {
        if (!api.isLoggedIn) { error = "로그인 후 이용할 수 있어요."; loading = false; return@LaunchedEffect }
        api.hostReservations().onSuccess { list -> rows = list.sortedBy { "${it.date} ${it.startTime ?: ""}" }; error = null }
            .onFailure { error = it.message }
        loading = false
    }

    Column(Modifier.fillMaxSize().background(MuyeonList.pageBg)) {
        MuyeonListHeader("공간 예약 관리", onBack = onBack)
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                loading -> CircularProgressIndicator(color = MuyeonColors.primary, modifier = Modifier.align(Alignment.Center))
                rows.isEmpty() -> MuyeonEmptyState(error ?: "들어온 예약이 없어요.", icon = Icons.Outlined.EventNote, modifier = Modifier.align(Alignment.Center))
                else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(rows, key = { it.id }) { r ->
                        ReservationRowCard(r, showBooker = true, onClick = {}, actions = if (r.status == "PAID") ({
                            RowButton("취소(전액 환불)", Modifier.weight(1f), danger = true) { cancelTarget = r }
                        }) else null)
                    }
                }
            }
        }
    }

    cancelTarget?.let { r ->
        MuyeonDialog(
            title = "이 예약을 취소할까요?",
            message = "${r.bookerName ?: "예약자"}님 · ${r.whenText}\n결제 금액 ${won(r.amount)}을 전액 환불해요.",
            actions = listOf(
                MuyeonDialogAction("취소(전액 환불)", MuyeonDialogKind.DESTRUCTIVE) {
                    scope.launch {
                        api.hostCancel(r.id).onSuccess { onMessage("예약을 취소하고 전액 환불했어요."); onChanged(); tick++ }
                            .onFailure { onMessage(it.message ?: "취소하지 못했어요.") }
                    }
                },
                MuyeonDialogAction("닫기", MuyeonDialogKind.CANCEL),
            ),
            onDismiss = { cancelTarget = null },
        )
    }
}
