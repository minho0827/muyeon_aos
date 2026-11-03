package com.muyeon.app.ui.space.reserve

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
import com.muyeon.app.ui.space.*
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * 공간 '예약하기' 시트(2026-11-03 결제 예약) — iOS `SpaceReserveSheet` 와 같은 순서.
 *  세부공간 → 달력(오늘 이전·휴무 비활성) → GET availability → 시작 시간 칩(30분, 막힌 구간 비활성) →
 *  이용 시간(minHours 부터 0.5 단위) → 인원 −/＋ → 요청사항 → 금액 미리보기 → 환불 표 → 동의(서버 bookingConsents, 모두 필수) → [결제하기].
 *  [결제하기] = POST /spaces/:id/reservation(결제 대기 10분) → 결제 창(LessonPaymentActivity)에서 웹 /spacePay/{id}.
 *  결제 창이 결제 완료로 닫히면 onPaid(시트 닫고 내 예약), 아니면 잡아 둔 자리를 풀고 가능 시간을 다시 읽는다.
 *  각 칸은 reserve/ 의 조각, 이 시트는 상태·API 연결과 조립만 한다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpaceReserveSheet(
    api: SpaceApi,
    space: SpaceDetail,
    initialDate: String = "",
    initialOptionId: Int? = null,
    /** 닫힘(문구가 있으면 상위가 띄운다). */
    onFinish: (String?) -> Unit,
    /** 결제 대기 예약을 만들었음 — 결과 키 RESERVATIONS 용. */
    onReserved: () -> Unit = {},
    /** 결제 완료 — 상위가 시트를 닫고 내 예약을 연다. */
    onPaid: () -> Unit = {},
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val today = remember { MarketDates.todayKst() }
    val options = space.hourlyOptions

    var optionId by remember { mutableStateOf(initialOptionId ?: options.firstOrNull()?.id) }
    var date by remember {
        mutableStateOf(initialDate.take(10).takeIf { it.length == 10 && it >= today && space.isOpenOn(it) })
    }
    var avail by remember { mutableStateOf<SpaceAvailability?>(null) }
    var availLoading by remember { mutableStateOf(false) }
    var availError by remember { mutableStateOf<String?>(null) }
    var availTick by remember { mutableIntStateOf(0) }
    var startMin by remember { mutableStateOf<Int?>(null) }
    var hours by remember { mutableDoubleStateOf(max(1.0, (space.hourlyMinHours ?: 1).toDouble())) }
    var headcount by remember { mutableIntStateOf(max(1, space.baseHeadcount ?: 1)) }
    var message by remember { mutableStateOf("") }
    // 동의한 문서(docKey) — 서버가 준 필수 항목을 모두 체크해야 결제.
    var agreed by remember { mutableStateOf<Set<String>>(emptySet()) }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showDate by remember { mutableStateOf(false) }
    var pendingId by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(date, optionId, availTick) {
        val d = date ?: return@LaunchedEffect
        availLoading = true; availError = null; avail = null; startMin = null
        api.availability(space.id, d, optionId)
            .onSuccess { a -> avail = a; if (hours < a.minHours) hours = a.minHours }
            .onFailure { availError = it.message }
        availLoading = false
    }

    val payLauncher = rememberLauncherForActivityResult(LessonPaymentActivity.PathContract()) { paid ->
        val rid = pendingId
        pendingId = null
        if (paid) {
            onPaid()
        } else {
            // 결제를 마치지 않고 닫음 — 아직 결제 대기면 자리를 풀어 같은 시간을 바로 다시 고를 수 있게.
            scope.launch {
                if (rid != null) {
                    val stillPending = api.myReservations().getOrNull()?.firstOrNull { it.id == rid }?.status == "PENDING_PAYMENT"
                    if (stillPending) api.cancel(rid)
                }
                availTick++
            }
        }
    }

    val a = avail
    val minHours = a?.minHours ?: max(1.0, (space.hourlyMinHours ?: 1).toDouble())
    val unitPrice = options.firstOrNull { it.id == optionId }?.price ?: space.pricePerHour ?: 0
    val extraFee = run {
        val base = space.baseHeadcount
        val fee = space.extraPersonFee ?: 0
        if (base != null && fee > 0) max(0, headcount - base) * fee * hours else 0.0
    }
    val estimate = (unitPrice * hours + extraFee).roundToInt()
    val slotOk = a != null && a.open && startMin != null && a.fits(startMin!!, hours)
    val consentsOk = space.bookingConsents.filter { it.required }.all { it.docKey in agreed }
    val canSubmit = !submitting && date != null && slotOk && consentsOk
    val slot = slotState(date, availLoading, availError, a)
    val extraNote = space.baseHeadcount?.takeIf { (space.extraPersonFee ?: 0) > 0 }
        ?.let { "기본 ${it}명, 초과 1인당 시간당 ${SpaceDesign.won(space.extraPersonFee ?: 0)}원" }
    val priceCaption = "시간당 ${SpaceDesign.won(unitPrice)}원 × ${hourLabel(hours)}" +
        (if (extraFee > 0) " + 초과 인원 ${SpaceDesign.won(extraFee.roundToInt())}원" else "") +
        " · 최종 금액은 결제 화면에서 확인해요."

    fun submit() {
        if (!api.isLoggedIn) { error = "로그인 후 이용할 수 있어요."; return }
        submitting = true; error = null
        scope.launch {
            api.reserve(space.id, SpaceReservationRequest(optionId, date!!, SpaceAvailability.hhmm(startMin!!), hours, headcount, message, agreeThirdParty = true))
                .onSuccess { (rid, _) ->
                    onReserved()
                    pendingId = rid
                    payLauncher.launch(LessonPaymentActivity.PaymentPage("/spacePay/$rid", "대관료 결제"))
                }.onFailure { error = it.message ?: "예약하지 못했어요. 잠시 후 다시 시도해 주세요." }
            submitting = false
        }
    }

    val expected = remember(space.id) { expectedAvailability(space.businessHours, space.hourlyMinHours) }

    ModalBottomSheet(onDismissRequest = { onFinish(null) }, sheetState = sheetState, containerColor = Color.White) {
        // 시트 높이 고정(화면의 92%) + 안쪽 스크롤 — 가능 시간이 오거나 고른 값이 바뀌어도 시트가 커졌다 작아지지 않는다.
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.92f)) {
            ReserveSheetTitle { onFinish(null) }
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState())
                    .padding(horizontal = SpaceDesign.gutter).padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                options.takeIf { it.isNotEmpty() }?.let { ReserveOptionPicker(it, optionId) { id -> optionId = id } }
                ReserveDateField(date) { showDate = true }
                // 날짜 전·읽는 중·쉬는 날·오류도 같은 높이(예상 영업시간 칩 배치로 자리 잡기).
                ReserveSlotArea(
                    expected, slot, startMin, hours,
                    onPickStart = { av, t -> startMin = t; if (!av.fits(t, hours)) hours = minHours },
                    onPickHours = { hours = it },
                )
                HeadcountStepper(headcount, space.capacityMax ?: 100, space.capacityMax?.let { "최대 ${it}명" }, extraNote) { headcount = it }
                ReserveMessageField(message) { message = it }
                PriceSummary(estimate, priceCaption)
                ReserveRefundTable(space.refund)
                BookingConsents(space.bookingConsents, agreed) { k -> agreed = if (k in agreed) agreed - k else agreed + k }
                error?.let { ReserveError(it) }
                Spacer(Modifier.height(8.dp))
            }
            ReservePayButton(submitting, canSubmit) { submit() }
        }
    }

    if (showDate) ReserveDatePicker(date, today, space::isOpenOn, onPick = { date = it }, onDismiss = { showDate = false })
}
