package com.muyeon.app.ui.lesson

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors
import com.muyeon.app.ui.common.MuyeonDialog
import com.muyeon.app.ui.common.MuyeonDialogAction
import com.muyeon.app.ui.common.MuyeonDialogKind
import com.muyeon.app.webview.ActiveRole
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

/** iOS 시스템 색 — 예약 화면을 iOS `LessonBookingSheet` 와 같은 색으로 그린다(2026-10-24 1:1 맞춤). */
private object IosGray {
    val gray = Color(0xFF8E8E93)    // .systemGray
    val gray2 = Color(0xFFAEAEB2)   // .systemGray2
    val gray3 = Color(0xFFC7C7CC)   // .systemGray3
    val gray4 = Color(0xFFD1D1D6)   // .systemGray4
    val gray5 = Color(0xFFE5E5EA)   // .systemGray5
    val gray6 = Color(0xFFF2F2F7)   // .systemGray6
    val separator = Color(0xFFC6C6C8) // Divider()
    val red = Color(0xFFFF3B30)     // .red
    val grouped2 = Color.White      // .secondarySystemGroupedBackground(라이트)
}

/** 달력 칸 하나 — iOS `LessonBookingDay`. */
private data class BookingDay(
    val day: Int,
    val ymd: String,
    val isCurrentMonth: Boolean,
    val isToday: Boolean,
    val isSunday: Boolean,
    val hasSlot: Boolean,   // 마감 포함 회차 있음(회색 표시)
    val hasOpen: Boolean,   // 예약 가능한 회차 있음(주황 점)
    val isPast: Boolean,
)

/** 무용수 유형 예약 차단 문구 — iOS `RoleGate.dancerLessonCustomerGate` 와 같은 문구. */
private const val DANCER_GATE_MESSAGE = "무용수 유형에서는 레슨을 요청하거나 예약할 수 없어요. 일반 유형으로 전환하면 계속할 수 있어요."

/**
 * 레슨 예약 — iOS `LessonBookingSheet.swift` 와 1:1(2026-10-24 다시 맞춤).
 *  달력에서 날짜 → 시간 칩(마감 포함) → 인원 칩 → 수업료 안내 → [레슨 예약하기] → 예약 확인(예약금) 시트에서
 *  취소·환불 규정 확인 후 확정·결제. rescheduleReservationId 가 있으면 **변경 모드**(원자적 리스케줄).
 *  회차는 보이는 달만 읽는다(iOS 와 같다). 예약금은 예약 화면 위 결제 창(LessonPaymentActivity)에서 낸다.
 */
@Composable
fun LessonBookingScreen(
    api: LessonBookingApi,
    productId: Int,
    rescheduleReservationId: Int? = null,
    onClose: () -> Unit,
    onDone: () -> Unit,
    /** 예약금 결제가 끝났을 때(결제 창에서 승인). 호출부가 예약 화면을 닫고 내 예약으로 보낸다(iOS 와 같은 결말). */
    onPaid: (reservationId: Int) -> Unit = { onDone() },
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isReschedule = rescheduleReservationId != null

    // 보이는 달(그달 1일, 한국 시간).
    var monthAnchor by remember { mutableStateOf(kstCalendar().apply { set(Calendar.DAY_OF_MONTH, 1) }.timeInMillis) }
    var slots by remember { mutableStateOf<List<LessonSlot>>(emptyList()) }
    var selectedDate by remember { mutableStateOf<String?>(null) }
    var selected by remember { mutableStateOf<LessonSlot?>(null) }
    var headcount by remember { mutableIntStateOf(1) }
    var busy by remember { mutableStateOf(false) }
    // 결제 설정 — 금액은 서버 값만 쓴다. null 이면 금액을 모르는 상태라 진행을 막는다.
    var product by remember { mutableStateOf<LessonBookingProduct?>(null) }
    var showDeposit by remember { mutableStateOf(false) }
    var policyAgreed by remember { mutableStateOf(false) }
    var showRefundPolicy by remember { mutableStateOf(false) }
    var doneMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var showGate by remember { mutableStateOf(false) }
    // 지금 적용 중인 환불 규정(관리자 관리) — 예약 순간 이 규정이 예약에 찍힌다.
    var refundPolicy by remember { mutableStateOf(LessonRefundPolicyRepo.current()) }
    LaunchedEffect(Unit) { refundPolicy = LessonRefundPolicyRepo.get() }
    // 결제 창을 연 대기 예약 번호 — 결제 완료 후 내 예약으로 보낼 때 쓴다.
    var pendingReservationId by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(monthAnchor) {
        val (from, to) = monthRange(monthAnchor)
        slots = api.availableSlots(productId, from, to).getOrNull() ?: emptyList()
        // 금액은 회차가 아니라 상품에 있다(iOS·웹과 동일).
        if (product == null) api.product(productId).onSuccess { product = it }
    }

    val today = kstYmd.format(Date())
    val openDates = remember(slots) { slots.filter { it.isOpen }.map { it.date }.toSet() }
    val allSlotDates = remember(slots) { slots.map { it.date }.toSet() }   // OPEN+FULL — 마감도 회색으로 노출
    val days = remember(monthAnchor, slots) { monthDays(monthAnchor, today, allSlotDates, openDates) }
    val slotsForSelected = selectedDate?.let { d -> slots.filter { it.date == d }.sortedBy { it.startTime } } ?: emptyList()
    val notBookable = !isReschedule && product?.bookable == false

    val total = product?.totalPrice(headcount) ?: 0
    val deposit = product?.depositFor(headcount) ?: 0
    val remaining = product?.remainingFor(headcount) ?: 0

    fun moveMonth(delta: Int) {
        monthAnchor = kstCalendar().apply { timeInMillis = monthAnchor; add(Calendar.MONTH, delta) }.timeInMillis
        selectedDate = null; selected = null; slots = emptyList()
    }

    fun openConfirm() {
        policyAgreed = false
        showDeposit = true
        // 금액을 못 받아 왔으면 여는 김에 다시 시도(iOS ensureProduct).
        if (product == null) scope.launch { api.product(productId).onSuccess { product = it } }
    }

    fun reload() {
        scope.launch {
            val (from, to) = monthRange(monthAnchor)
            slots = api.availableSlots(productId, from, to).getOrNull() ?: slots
        }
    }

    // 예약금 결제 창(토스) — iOS LessonPaymentWebView. 결제됐으면 예약 화면을 닫고 내 예약으로,
    //  취소·실패면 달력에 남아 다시 고를 수 있게 회차를 다시 읽는다(미결제 자리는 서버가 15분 뒤 회수).
    val paymentLauncher = rememberLauncherForActivityResult(LessonPaymentActivity.Contract()) { wasPaid ->
        val rid = pendingReservationId
        pendingReservationId = null
        if (wasPaid && rid != null) onPaid(rid) else reload()
    }

    fun confirmReserve() {
        val s = selected ?: return
        busy = true
        scope.launch {
            if (rescheduleReservationId != null) {
                api.reschedule(rescheduleReservationId, s.id, headcount)
                    .onSuccess {
                        showDeposit = false
                        doneMessage = "일정 변경 요청을 보냈어요. 강사 또는 학원이 승인하면 확정돼요."
                    }
                    .onFailure { showDeposit = false; errorMessage = it.message }
            } else {
                api.reserve(s.id, headcount)
                    .onSuccess { res ->
                        showDeposit = false
                        // 예약금 상품은 아직 확정이 아니다. 정원만 잡아둔 상태이고
                        //  결제해야 예약이 된다(미결제 시 서버가 15분 뒤 회수). 결제는 예약 화면 위 결제 창에서(iOS 와 같은 흐름).
                        if (res.needsPayment) {
                            selected = null
                            pendingReservationId = res.id
                            paymentLauncher.launch(res.id)
                        } else {
                            // 결제 없이 확정 — '결제된 게 없다'와 '현장에서 낸다'를 함께 말한다(iOS 와 같은 문구).
                            doneMessage = if (total > 0)
                                "예약이 확정됐어요.\n앱에서 결제된 금액은 없어요.\n레슨비 ${total.won()}원은 방문 시 현장에서 결제해 주세요."
                            else "예약이 확정됐어요."
                            reload()
                            selected = null
                        }
                    }
                    .onFailure { showDeposit = false; errorMessage = it.message }
            }
            busy = false
        }
    }

    Column(Modifier.fillMaxSize().background(MuyeonColors.surface).statusBarsPadding()) {
        // 손잡이 — iOS 시트와 같은 모양.
        Box(
            Modifier.align(Alignment.CenterHorizontally).padding(top = 10.dp, bottom = 6.dp)
                .size(width = 40.dp, height = 4.dp).clip(RoundedCornerShape(2.dp)).background(IosGray.gray4),
        )
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 12.dp)) {
            MonthHeader(monthTitle(monthAnchor), onClose = onClose, onPrev = { moveMonth(-1) }, onNext = { moveMonth(1) })
            WeekdayRow()
            CalendarGrid(days, selectedDate) { d ->
                selectedDate = if (selectedDate == d.ymd) null else d.ymd
                selected = null
            }
            HorizontalDivider(Modifier.padding(vertical = 12.dp), thickness = 0.5.dp, color = IosGray.separator)

            when {
                selectedDate == null -> Hint("예약할 날짜를 선택해 주세요")
                slotsForSelected.isEmpty() -> Hint("이 날은 예약 가능한 시간이 없어요")
                else -> {
                    if (slotsForSelected.none { it.isOpen }) Hint("이 날은 예약이 모두 마감됐어요")
                    TimeSlots(slotsForSelected, selected) { s ->
                        if (!s.isOpen) notice = "이 시간은 예약이 마감됐어요."
                        else { selected = s; headcount = 1 }
                    }
                    selected?.let { s ->
                        HeadcountRow(maxOf(1, s.remaining), headcount) { headcount = it }
                        PaymentSummary(product, headcount, total, deposit, remaining)
                    }
                }
            }
        }

        // 하단 버튼 — iOS bottomBar.
        val enabled = selected != null && !busy && !notBookable
        Box(
            Modifier.fillMaxWidth()
                .shadow(8.dp, spotColor = Color.Black.copy(alpha = 0.06f), ambientColor = Color.Black.copy(alpha = 0.06f))
                .background(MuyeonColors.surface)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp).padding(top = 8.dp, bottom = 20.dp),
        ) {
            Text(
                when {
                    notBookable -> "지금은 예약할 수 없는 레슨이에요"
                    selected == null -> "시간을 선택해 주세요"
                    isReschedule -> "레슨 변경하기"
                    else -> "레슨 예약하기"
                },
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 19.sp,
                color = Color.White, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(if (selected == null || notBookable) IosGray.gray3 else MuyeonColors.primary)
                    .clickable(enabled = enabled) {
                        // 무용수 유형은 레슨 예약 불가(변경 모드는 기존 예약이라 제외) — 일반 전환 후 이어서.
                        if (!isReschedule && ActiveRole.isDancer(context)) showGate = true else openConfirm()
                    }
                    .padding(vertical = 15.dp),
            )
        }
    }

    if (showDeposit) {
        DepositSheet(
            product = product,
            total = total, deposit = deposit, remaining = remaining,
            refundPolicy = refundPolicy,
            policyAgreed = policyAgreed,
            onTogglePolicy = { policyAgreed = it },
            onOpenPolicy = { showRefundPolicy = true },
            busy = busy,
            onBack = { showDeposit = false },
            onConfirm = { confirmReserve() },
        )
    }

    if (showRefundPolicy) {
        // 판매자(강사·학원) 취소 정책은 보이지 않는다 — 환불 규정은 무용연이 정한다(2026-09-24).
        LessonRefundPolicySheet(policy = refundPolicy, onDismiss = { showRefundPolicy = false })
    }

    doneMessage?.let { msg ->
        MuyeonDialog(
            title = "예약 완료", message = msg,
            actions = listOf(MuyeonDialogAction("확인", MuyeonDialogKind.PRIMARY) { doneMessage = null; onDone() }),
            onDismiss = { doneMessage = null; onDone() },
        )
    }
    errorMessage?.let { msg ->
        MuyeonDialog(
            title = "예약 실패", message = msg,
            actions = listOf(MuyeonDialogAction("확인", MuyeonDialogKind.PRIMARY) { errorMessage = null }),
            onDismiss = { errorMessage = null },
        )
    }
    notice?.let { msg ->
        MuyeonDialog(
            title = "안내", message = msg,
            actions = listOf(MuyeonDialogAction("확인", MuyeonDialogKind.PRIMARY) { notice = null }),
            onDismiss = { notice = null },
        )
    }
    if (showGate) {
        // iOS roleGateAlert — '일반 유형으로 전환하고 계속' 이면 서버에 저장하고 예약 확인을 이어서 연다.
        MuyeonDialog(
            title = "회원유형 안내", message = DANCER_GATE_MESSAGE,
            actions = listOf(
                MuyeonDialogAction("${ActiveRole.label("GENERAL")} 유형으로 전환하고 계속", MuyeonDialogKind.PRIMARY) {
                    showGate = false
                    scope.launch {
                        if (ActiveRole.saveOnServer(context, "GENERAL")) {
                            ActiveRole.store(context, "GENERAL")
                            openConfirm()
                        } else errorMessage = "회원유형을 바꾸지 못했어요. 잠시 후 다시 시도해 주세요."
                    }
                },
                MuyeonDialogAction("취소", MuyeonDialogKind.CANCEL) { showGate = false },
            ),
            onDismiss = { showGate = false },
        )
    }
}

/** 1,234 형태 천단위 구분. */
private fun Int.won(): String = "%,d".format(this)

// ── 달력 ────────────────────────────────────────────────

@Composable
private fun MonthHeader(title: String, onClose: () -> Unit, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            "닫기", fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp,
            color = IosGray.gray, modifier = Modifier.clickable(onClick = onClose),
        )
        Spacer(Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            Icon(Icons.Filled.ChevronLeft, "이전 달", tint = MuyeonColors.textHead, modifier = Modifier.size(22.dp).clickable(onClick = onPrev))
            Text(title, fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = MuyeonColors.textHead)
            Icon(Icons.Filled.ChevronRight, "다음 달", tint = MuyeonColors.textHead, modifier = Modifier.size(22.dp).clickable(onClick = onNext))
        }
        Spacer(Modifier.weight(1f))
        Spacer(Modifier.width(34.dp))
    }
}

@Composable
private fun WeekdayRow() {
    Row(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
        listOf("일", "월", "화", "수", "목", "금", "토").forEachIndexed { i, s ->
            Text(
                s, fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp,
                color = if (i == 0) IosGray.red else IosGray.gray, textAlign = TextAlign.Center, modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun CalendarGrid(days: List<BookingDay>, selectedDate: String?, onPick: (BookingDay) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        days.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { d -> DayCell(d, d.ymd == selectedDate, Modifier.weight(1f), onPick) }
            }
        }
    }
}

@Composable
private fun DayCell(d: BookingDay, selected: Boolean, modifier: Modifier, onPick: (BookingDay) -> Unit) {
    val selectable = d.isCurrentMonth && !d.isPast && d.hasSlot
    val textColor = when {
        selected -> Color.White
        !d.isCurrentMonth || d.isPast -> IosGray.gray3
        !selectable -> IosGray.gray2
        d.hasSlot && !d.hasOpen -> IosGray.gray2   // 전부 마감 → 회색
        d.isToday -> MuyeonColors.primary
        d.isSunday -> IosGray.red
        else -> MuyeonColors.textHead
    }
    // 점: 예약가능=주황, 전부마감=회색, 없음=투명.
    val dot = when {
        !d.hasSlot -> Color.Transparent
        selected -> Color.White
        d.hasOpen -> MuyeonColors.primary
        else -> IosGray.gray3
    }
    Column(
        modifier.height(40.dp).clickable(
            enabled = selectable, interactionSource = remember { MutableInteractionSource() }, indication = null,
        ) { onPick(d) },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
    ) {
        Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
            if (selected) Box(Modifier.matchParentSize().clip(CircleShape).background(MuyeonColors.primary))
            else if (d.isToday && d.isCurrentMonth) Box(Modifier.matchParentSize().clip(CircleShape).background(MuyeonColors.primary.copy(alpha = 0.15f)))
            Text(
                "${d.day}", fontFamily = customFontFamily, fontSize = 14.sp,
                fontWeight = if (selected || d.isToday) FontWeight.Bold else FontWeight.Normal, color = textColor,
            )
        }
        Box(Modifier.size(4.dp).clip(CircleShape).background(dot))
    }
}

@Composable
private fun Hint(text: String) {
    Text(
        text, fontFamily = customFontFamily, fontSize = 14.sp, color = IosGray.gray, textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
    )
}

// ── 시간·인원·수업료 ─────────────────────────────────────

/** iOS FlowChips(LazyVGrid .adaptive(minimum:)) — 칸 너비로 열 수를 정하고, 칩은 칸 가운데에 제 크기로. */
@Composable
private fun <T> FlowChips(
    items: List<T>,
    minimumWidth: Dp,
    cellAlignment: Alignment = Alignment.Center,
    content: @Composable (T) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val cols = maxOf(1, ((maxWidth + 8.dp) / (minimumWidth + 8.dp)).toInt())
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items.chunked(cols).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { item -> Box(Modifier.weight(1f), contentAlignment = cellAlignment) { content(item) } }
                    repeat(cols - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun TimeSlots(list: List<LessonSlot>, selected: LessonSlot?, onTap: (LessonSlot) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle("시간 선택")
        // 시간 칩은 제목 아래 왼쪽에 붙인다(칸 가운데면 첫 칩이 떠 보였다 — 10-24 사용자 확인).
        FlowChips(list, 112.dp, cellAlignment = Alignment.CenterStart) { s ->
            val on = selected?.id == s.id
            val full = !s.isOpen
            Column(
                Modifier.clip(RoundedCornerShape(10.dp))
                    .background(if (on) MuyeonColors.primary else if (full) IosGray.gray5 else IosGray.grouped2)
                    .clickable { onTap(s) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    s.timeLabel, fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    color = if (on) Color.White else if (full) IosGray.gray2 else MuyeonColors.textHead,
                )
                Text(
                    if (full) "마감" else "잔여: ${s.remaining}명", fontFamily = customFontFamily, fontSize = 11.sp, maxLines = 1,
                    color = if (on) Color.White.copy(alpha = 0.9f) else if (full) IosGray.gray else IosGray.gray,
                )
            }
        }
    }
}

@Composable
private fun HeadcountRow(maxN: Int, headcount: Int, onPick: (Int) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle("예약 인원")
        FlowChips((1..maxN).toList(), 64.dp) { n ->
            val on = headcount == n
            Box(
                Modifier.size(52.dp).clip(CircleShape)
                    .background(if (on) MuyeonColors.primary else Color.Transparent)
                    .border(1.dp, if (on) Color.Transparent else IosGray.gray4, CircleShape)
                    .clickable { onPick(n) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "${n}명", fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp,
                    color = if (on) Color.White else MuyeonColors.textHead,
                )
            }
        }
    }
}

/**
 * 수업료 — 인원을 고르면 가격 기준대로 바로 계산해 보여 준다(iOS paymentSummary, 웹 예약 시트와 같은 문구).
 *  수업료는 무용연이 받지 않고 현장에서 낸다 — 앱에서 내는 건 예약금뿐.
 */
@Composable
private fun PaymentSummary(product: LessonBookingProduct?, headcount: Int, total: Int, deposit: Int, remaining: Int) {
    val p = product ?: return
    if ((p.price ?: 0) <= 0) return
    Column(
        Modifier.fillMaxWidth().padding(top = 10.dp).clip(RoundedCornerShape(10.dp)).background(IosGray.grouped2).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (deposit > 0) {
            SummaryLine("예약금 ${deposit.won()}원 지금 결제")
            SummaryLine("현장 결제 ${remaining.won()}원")
        } else {
            SummaryLine("현장 결제 ${total.won()}원")
        }
        Text(
            LessonOptions.priceFormula(p.price ?: 0, p.priceUnit ?: "PER_PERSON", headcount),
            fontFamily = customFontFamily, fontSize = 13.sp, color = IosGray.gray,
        )
    }
}

@Composable
private fun SummaryLine(text: String) =
    Text(text, fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MuyeonColors.textHead)

@Composable
private fun SectionTitle(text: String) =
    Text(text, fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MuyeonColors.textHead)

// ── 예약 확인(예약금) 시트 — iOS depositSheet ─────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DepositSheet(
    product: LessonBookingProduct?,
    total: Int,
    deposit: Int,
    remaining: Int,
    refundPolicy: LessonRefundPolicy,
    policyAgreed: Boolean,
    onTogglePolicy: (Boolean) -> Unit,
    onOpenPolicy: () -> Unit,
    busy: Boolean,
    onBack: () -> Unit,
    onConfirm: () -> Unit,
) {
    // 금액·환불 안내가 압축되지 않게 항상 큰 시트로 연다(iOS .presentationDetents([.large])).
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // 확정 게이트 — 규정 확인 + 금액을 아는 상태에서만. 금액을 모르면 결제 단계로 넘기지 않는다.
    val canConfirm = product != null && policyAgreed && !busy
    ModalBottomSheet(onDismissRequest = onBack, sheetState = sheetState, containerColor = Color.White, dragHandle = null) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.92f)) {
            Box(
                Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp, bottom = 12.dp)
                    .size(width = 40.dp, height = 4.dp).clip(RoundedCornerShape(2.dp)).background(IosGray.gray4),
            )
            Text(
                if (deposit > 0) "예약금 결제" else "예약 확인",
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MuyeonColors.textHead,
                maxLines = 1, modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 10.dp),
            )
            Box(Modifier.padding(top = 10.dp).fillMaxWidth().height(3.dp).background(MuyeonColors.primary))
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                if (product == null) {
                    // ★ 금액을 모를 때 0원으로 그리면 표시액과 청구액이 갈라진다 — 사유를 보여 주고 진행을 막는다.
                    Row(Modifier.padding(horizontal = 20.dp).padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Warning, null, tint = MuyeonColors.orange, modifier = Modifier.size(18.dp))
                        Text(
                            "결제 금액을 불러오는 중이에요.\n계속 표시되면 창을 닫았다 다시 열어 주세요.",
                            fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = MuyeonColors.textHead,
                        )
                    }
                } else {
                    Row(
                        Modifier.padding(horizontal = 20.dp).padding(top = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.Info, null, tint = MuyeonColors.primary, modifier = Modifier.size(18.dp))
                        // ★ 예약금 없는 레슨은 '지금 결제 안 함 + 현장 결제'를 분명히 말한다(금액 오해 방지 — iOS 와 같은 문구).
                        Text(
                            when {
                                deposit > 0 -> "예약금을 결제하면 예약이 확정돼요."
                                total > 0 -> "지금은 결제하지 않아요. 레슨비는 수업 당일 현장에서 직접 결제해 주세요."
                                else -> "무료 레슨이라 결제 없이 예약이 확정돼요."
                            },
                            fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = MuyeonColors.textHead,
                        )
                    }
                    Column(
                        Modifier.padding(horizontal = 20.dp).padding(top = 16.dp).fillMaxWidth()
                            .border(1.dp, IosGray.gray5, RoundedCornerShape(14.dp)).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        AmountRow("총 레슨비", "${total.won()}원")
                        if (deposit > 0) {
                            AmountRow("지금 결제할 예약금", "${deposit.won()}원")
                            AmountRow("현장 결제 예정액", "${remaining.won()}원")
                            HorizontalDivider(thickness = 0.5.dp, color = IosGray.separator)
                            AmountRow("지금 결제", "${deposit.won()}원", emphasize = true)
                        } else {
                            // 예약금 없음 — 강조는 '방문 시 현장 결제'. '지금 결제 0원'은 보조로만.
                            AmountRow("지금 결제", "0원")
                            HorizontalDivider(thickness = 0.5.dp, color = IosGray.separator)
                            AmountRow("방문 시 현장 결제", "${total.won()}원", emphasize = true)
                        }
                    }
                }
                Column(Modifier.padding(horizontal = 20.dp).padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        if (deposit > 0) refundPolicy.summary ?: LessonRefundPolicy.FALLBACK.summary.orEmpty()
                        else "예약금이 없는 레슨이에요. 앱에서는 결제되지 않으며, 레슨비는 수업 당일 강사·학원에 직접 결제해요.",
                        fontFamily = customFontFamily, fontSize = 13.sp, color = IosGray.gray,
                    )
                    Row(
                        Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(12.dp)).background(IosGray.gray6)
                            .clickable(onClick = onOpenPolicy).padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(Icons.Outlined.Description, null, tint = MuyeonColors.textHead, modifier = Modifier.size(18.dp))
                        Text(
                            "취소·환불 규정 전체보기", fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                            color = MuyeonColors.textHead, modifier = Modifier.weight(1f),
                        )
                        Icon(Icons.Filled.ChevronRight, null, tint = MuyeonColors.textHead, modifier = Modifier.size(16.dp))
                    }
                }
                Row(
                    Modifier.fillMaxWidth().clickable { onTogglePolicy(!policyAgreed) }.padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "취소·환불 규정을 확인했어요.", fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp,
                        color = MuyeonColors.textHead, modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = policyAgreed, onCheckedChange = onTogglePolicy,
                        // iOS Toggle 모양 — 꺼짐은 옅은 회색 바탕·흰 손잡이, 테두리 없음.
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = MuyeonColors.primary, checkedThumbColor = Color.White, checkedBorderColor = Color.Transparent,
                            uncheckedTrackColor = IosGray.gray5, uncheckedThumbColor = Color.White, uncheckedBorderColor = Color.Transparent,
                        ),
                    )
                }
            }
            Row(
                Modifier.fillMaxWidth()
                    .shadow(8.dp, spotColor = Color.Black.copy(alpha = 0.06f), ambientColor = Color.Black.copy(alpha = 0.06f))
                    .background(Color.White).navigationBarsPadding()
                    .padding(horizontal = 20.dp).padding(top = 10.dp, bottom = 12.dp)
                    // 두 버튼 높이를 같게 — 금액이 들어가 결제 버튼이 두 줄이 되면 '이전'도 같이 커진다(10-24 사용자 확인).
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(14.dp))
                        .border(1.dp, IosGray.gray4, RoundedCornerShape(14.dp)).clickable(onClick = onBack).padding(vertical = 15.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "이전", fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp,
                        color = MuyeonColors.textHead, textAlign = TextAlign.Center,
                    )
                }
                Box(
                    Modifier.weight(1f).fillMaxHeight().alpha(if (canConfirm) 1f else 0.45f).clip(RoundedCornerShape(14.dp))
                        .background(MuyeonColors.primary).clickable(enabled = canConfirm, onClick = onConfirm).padding(vertical = 15.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (deposit > 0) "예약금 ${deposit.won()}원 결제하기" else "예약 확정하기",
                        // 반 칸에 안 들어가면 iOS 처럼 두 줄로(한 줄로 자르면 '결제하기'가 잘렸다).
                        fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 19.sp,
                        color = Color.White, textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun AmountRow(label: String, value: String, emphasize: Boolean = false) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            label, fontFamily = customFontFamily, fontSize = 15.sp,
            fontWeight = if (emphasize) FontWeight.Bold else FontWeight.Normal,
            color = if (emphasize) MuyeonColors.textHead else IosGray.gray, modifier = Modifier.weight(1f),
        )
        Text(
            value, fontFamily = customFontFamily, fontSize = 15.sp,
            fontWeight = if (emphasize) FontWeight.Bold else FontWeight.SemiBold,
            color = if (emphasize) MuyeonColors.primary else MuyeonColors.textHead,
        )
    }
}

// ── 날짜 계산(한국 시간) ─────────────────────────────────

/** 보이는 달 1일~말일 — iOS load() 와 같은 조회 구간. */
private fun monthRange(anchor: Long): Pair<String, String> {
    val c = kstCalendar().apply { timeInMillis = anchor; set(Calendar.DAY_OF_MONTH, 1) }
    val from = kstYmd.format(Date(c.timeInMillis))
    c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH))
    return from to kstYmd.format(Date(c.timeInMillis))
}

private fun monthTitle(anchor: Long): String {
    val c = kstCalendar().apply { timeInMillis = anchor }
    return "${c.get(Calendar.YEAR)}년 ${c.get(Calendar.MONTH) + 1}월"
}

/** 월 그리드 42칸(일요일 시작) — iOS days. */
private fun monthDays(anchor: Long, today: String, allSlotDates: Set<String>, openDates: Set<String>): List<BookingDay> {
    val first = kstCalendar().apply { timeInMillis = anchor; set(Calendar.DAY_OF_MONTH, 1) }
    val month = first.get(Calendar.MONTH)
    val leading = first.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY
    val c = (first.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, -leading) }
    return List(42) {
        val ymd = kstYmd.format(Date(c.timeInMillis))
        val d = BookingDay(
            day = c.get(Calendar.DAY_OF_MONTH), ymd = ymd, isCurrentMonth = c.get(Calendar.MONTH) == month,
            isToday = ymd == today, isSunday = c.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY,
            hasSlot = ymd in allSlotDates, hasOpen = ymd in openDates, isPast = ymd < today,
        )
        c.add(Calendar.DAY_OF_MONTH, 1)
        d
    }
}

/**
 * 취소·환불 규정 전문 — iOS `LessonRefundPolicySheet` 이식.
 *  판매자 추가 안내(cancelPolicy)는 공통 기준보다 소비자에게 유리할 때만 적용된다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonRefundPolicySheet(policy: LessonRefundPolicy, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "예약금은 레슨비에 포함돼요",
                    fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp,
                    lineHeight = 22.sp, color = MuyeonColors.textHead,
                )
                Text(
                    "취소 시점에 따라 아래 기준으로 예약금이 환불돼요.",
                    fontFamily = customFontFamily, fontSize = 14.sp, lineHeight = 20.sp,
                    color = MuyeonColors.textSub,
                )
            }

            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0xFFF2F2F7)),
            ) {
                // 규정 줄은 서버가 만든다(관리자가 숫자를 바꾸면 문구도 바뀐다).
                policy.lines.forEachIndexed { idx, line ->
                    if (idx > 0) HorizontalDivider(color = MuyeonColors.border, modifier = Modifier.padding(start = 16.dp))
                    PolicyRow(line.title, line.detail, accent = line.accent)
                }
            }

            policy.extraNotice?.takeIf { it.isNotBlank() }?.let { extra ->
                Text(
                    extra,
                    fontFamily = customFontFamily, fontSize = 14.sp, lineHeight = 20.sp,
                    color = MuyeonColors.textHead,
                )
            }

            Text(
                "환불 금액과 처리 상태는 예약 상세에서 확인할 수 있어요.",
                fontFamily = customFontFamily, fontSize = 13.sp, lineHeight = 18.sp,
                color = MuyeonColors.textSub,
            )
        }
    }
}

@Composable
private fun PolicyRow(title: String, detail: String, accent: Boolean = false) {
    Column(
        Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            title,
            fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
            lineHeight = 17.sp, color = MuyeonColors.textHead,
        )
        Text(
            detail,
            fontFamily = customFontFamily, fontSize = 14.sp, lineHeight = 17.sp,
            color = if (accent) MuyeonColors.primary else MuyeonColors.textSub,
        )
    }
}

/**
 * 예약 취소 사유 시트 — iOS `LessonCancelReasonSheet.swift`.
 *  ⚠️ 사유 코드는 서버·admin 과 3레포 계약이라 값을 바꾸면 관리자 통계가 깨진다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonCancelReasonSheet(onDismiss: () -> Unit, onConfirm: (LessonCancelReason, String?) -> Unit) {
    var selected by remember { mutableStateOf<LessonCancelReason?>(null) }
    var otherText by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Text(
                "취소 사유를 선택해 주세요.",
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 19.sp,
                lineHeight = 23.sp, color = MuyeonColors.textHead,
                modifier = Modifier.padding(horizontal = 20.dp).padding(top = 4.dp, bottom = 18.dp),
            )
            LessonCancelReason.entries.forEach { r ->
                val on = selected == r
                Row(
                    Modifier.fillMaxWidth().clickable { selected = r }.padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        Modifier.size(18.dp).clip(RoundedCornerShape(50))
                            .border(if (on) 6.dp else 1.5.dp, if (on) MuyeonColors.primary else MuyeonColors.border, RoundedCornerShape(50)),
                    )
                    Text(
                        r.label,
                        fontFamily = customFontFamily, fontSize = 15.sp, lineHeight = 18.sp, color = MuyeonColors.textHead,
                    )
                }
            }
            // '기타' 는 자유 입력을 함께 받아 서버에 reasonDetail 로 넘긴다(iOS 와 동일).
            if (selected == LessonCancelReason.OTHER) {
                OutlinedTextField(
                    value = otherText,
                    onValueChange = { otherText = it },
                    placeholder = {
                        Text(
                            "사유를 적어 주세요 (선택)",
                            fontFamily = customFontFamily, fontSize = 14.sp, color = MuyeonColors.chevron,
                        )
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }
            Text(
                "선택 완료",
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                lineHeight = 19.sp, color = Color.White, textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(if (selected != null) MuyeonColors.primary else Color.Gray.copy(alpha = 0.4f))
                    .clickable(enabled = selected != null) {
                        selected?.let { r ->
                            val detail = if (r == LessonCancelReason.OTHER) otherText.trim().ifEmpty { null } else null
                            onConfirm(r, detail)
                        }
                    }
                    .padding(vertical = 15.dp),
            )
        }
    }
}
