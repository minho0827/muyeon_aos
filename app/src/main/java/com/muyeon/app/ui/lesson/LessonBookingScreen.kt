package com.muyeon.app.ui.lesson

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors
import com.muyeon.app.ui.quote.QuoteDialog
import com.muyeon.app.ui.quote.QuoteNavBar
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

/**
 * 레슨 예약 — iOS `LessonBookingSheet.swift` 이식.
 *  회차(슬롯) 선택 + 인원 → 예약. reservationId 가 있으면 **변경 모드**(원자적 리스케줄).
 */
@Composable
fun LessonBookingScreen(
    api: LessonBookingApi,
    productId: Int,
    rescheduleReservationId: Int? = null,
    onClose: () -> Unit,
    onDone: () -> Unit,
    /** 예약금 결제가 필요할 때(PENDING_PAYMENT). 호출부가 웹 결제 화면으로 보낸다. */
    onNeedPayment: (reservationId: Int) -> Unit = { onDone() },
) {
    var slots by remember { mutableStateOf<List<LessonSlot>>(emptyList()) }
    var selected by remember { mutableStateOf<LessonSlot?>(null) }
    var headcount by remember { mutableIntStateOf(1) }
    var loading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    // 결제 설정 — 금액은 서버 값만 쓴다. null 이면 금액을 모르는 상태라 진행을 막는다.
    var product by remember { mutableStateOf<LessonBookingProduct?>(null) }
    // 취소·환불 규정 확인. iOS 와 동일하게 결제 전 필수 게이트.
    var policyAgreed by remember { mutableStateOf(false) }
    var showRefundPolicy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(productId) {
        val (from, to) = bookingRange()
        api.availableSlots(productId, from, to).onSuccess { slots = it }
        // 금액은 회차가 아니라 상품에 있다(iOS·웹과 동일).
        api.product(productId).onSuccess { product = it }
        loading = false
    }

    val isReschedule = rescheduleReservationId != null
    val byDate = remember(slots) { slots.filter { it.isOpen }.groupBy { it.date }.toSortedMap() }

    Column(Modifier.fillMaxSize().background(MuyeonColors.surface)) {
        QuoteNavBar(title = if (isReschedule) "예약 변경" else "레슨 예약", onBack = onClose)

        if (loading) {
            Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                CircularProgressIndicator(color = MuyeonColors.primary)
            }
            return@Column
        }

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            if (byDate.isEmpty()) {
                Text(
                    "예약 가능한 회차가 없어요.\n강사가 시간을 열면 여기에 보여요.",
                    fontFamily = customFontFamily, fontSize = 14.sp, lineHeight = 20.sp,
                    color = MuyeonColors.textSub, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                )
            }
            byDate.forEach { (date, list) ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        date,
                        fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                        lineHeight = 18.sp, color = MuyeonColors.textHead,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        list.forEach { s ->
                            val on = selected?.id == s.id
                            Column(
                                Modifier.clip(RoundedCornerShape(10.dp))
                                    .background(if (on) MuyeonColors.primary else Color(0xFFF2F2F7))
                                    .clickable {
                                        selected = s
                                        // 남은 자리보다 많은 인원이 선택돼 있으면 줄인다.
                                        if (headcount > s.remaining) headcount = s.remaining.coerceAtLeast(1)
                                    }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Text(
                                    s.timeLabel,
                                    fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                                    lineHeight = 17.sp, color = if (on) Color.White else MuyeonColors.textHead,
                                )
                                Text(
                                    "${s.remaining}자리",
                                    fontFamily = customFontFamily, fontSize = 11.sp, lineHeight = 13.sp,
                                    color = if (on) Color.White.copy(alpha = 0.9f) else MuyeonColors.textSub,
                                )
                            }
                        }
                    }
                }
            }

            // 인원 — 남은 자리 상한
            selected?.let { s ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "인원",
                        fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                        lineHeight = 18.sp, color = MuyeonColors.textHead,
                    )
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                            .border(1.dp, MuyeonColors.border, RoundedCornerShape(10.dp)).padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Filled.Remove, "감소", tint = MuyeonColors.textSub,
                            modifier = Modifier.size(24.dp).clickable(enabled = headcount > 1) { headcount -= 1 },
                        )
                        Text(
                            "${headcount}명",
                            fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                            lineHeight = 18.sp, color = MuyeonColors.textHead, textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(
                            Icons.Filled.Add, "증가", tint = MuyeonColors.textSub,
                            modifier = Modifier.size(24.dp).clickable(enabled = headcount < s.remaining) { headcount += 1 },
                        )
                    }
                    Text(
                        "남은 자리 ${s.remaining}명",
                        fontFamily = customFontFamily, fontSize = 12.sp, lineHeight = 14.sp, color = MuyeonColors.textSub,
                    )
                }
            }
            // 결제 예정 금액 + 취소·환불 규정 — iOS LessonBookingSheet 의 예약금 시트와 같은 내용.
            BookingPaymentBlock(
                product = product,
                headcount = headcount,
                onOpenPolicy = { showRefundPolicy = true },
                policyAgreed = policyAgreed,
                onTogglePolicy = { policyAgreed = it },
            )

            Spacer(Modifier.height(8.dp))
        }

        // ★ 규정 확인 없이는, 그리고 금액을 모르는 상태(product == null)에서는 진행하지 않는다.
        //   금액을 모른 채 넘기면 화면(0원)과 실제 청구액이 갈라진다.
        val canSubmit = selected != null && !busy && product != null && policyAgreed
        Text(
            if (busy) "처리 중…" else if (isReschedule) "예약 변경하기" else "예약하기",
            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp,
            lineHeight = 19.sp, color = Color.White, textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(horizontal = 20.dp).padding(top = 8.dp, bottom = 12.dp)
                .fillMaxWidth().clip(RoundedCornerShape(12.dp))
                .background(if (canSubmit) MuyeonColors.primary else Color.Gray.copy(alpha = 0.4f))
                .clickable(enabled = canSubmit) {
                    val s = selected ?: return@clickable
                    busy = true
                    scope.launch {
                        if (rescheduleReservationId != null) {
                            api.reschedule(rescheduleReservationId, s.id, headcount)
                                .onSuccess { onDone() }
                                .onFailure { errorMessage = it.message }
                        } else {
                            api.reserve(s.id, headcount)
                                .onSuccess { res ->
                                    // 예약금 상품은 아직 확정이 아니다. 정원만 잡아둔 상태이고
                                    //  결제해야 예약이 된다(미결제 시 서버가 15분 뒤 회수).
                                    //  결제창은 네이티브가 아니라 웹으로 넘긴다 — iOS 와 같은 경로.
                                    if (res.needsPayment) onNeedPayment(res.id) else onDone()
                                }
                                .onFailure { errorMessage = it.message }
                        }
                        busy = false
                    }
                }
                .padding(vertical = 16.dp),
        )
    }

    if (showRefundPolicy) {
        LessonRefundPolicySheet(
            sellerPolicy = product?.cancelPolicy,
            onDismiss = { showRefundPolicy = false },
        )
    }

    errorMessage?.let { msg ->
        QuoteDialog("알림", msg, "확인", onConfirm = { errorMessage = null }, onDismiss = { errorMessage = null })
    }
}

/** 1,234 형태 천단위 구분. */
private fun Int.won(): String = "%,d".format(this)

/**
 * 결제 예정 금액 + 취소·환불 규정 — iOS `LessonBookingSheet.depositSheet` 와 같은 내용.
 *  ★ 문구·기준은 iOS·웹(SlotPickerSheet)·약관과 한 벌로 유지한다. 한 곳만 고치면 갈라진다.
 */
@Composable
private fun BookingPaymentBlock(
    product: LessonBookingProduct?,
    headcount: Int,
    onOpenPolicy: () -> Unit,
    policyAgreed: Boolean,
    onTogglePolicy: (Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (product == null) {
            // ★ 금액을 모를 때 '무료' 로 단정하면 표시액과 청구액이 갈라진다.
            Text(
                "결제 금액을 불러오는 중이에요.\n계속 표시되면 창을 닫았다 다시 열어 주세요.",
                fontFamily = customFontFamily, fontSize = 14.sp, lineHeight = 20.sp,
                color = MuyeonColors.textSub,
            )
        } else {
            val total = product.totalPrice(headcount)
            val deposit = product.depositFor(headcount)
            val remaining = product.remainingFor(headcount)

            Text(
                if (deposit > 0) "예약금을 결제하면 예약이 확정돼요." else "결제 없이 바로 예약이 확정돼요.",
                fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp,
                lineHeight = 17.sp, color = MuyeonColors.textHead,
            )
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .border(1.dp, MuyeonColors.border, RoundedCornerShape(14.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AmountRow("총 레슨비", "${total.won()}원")
                AmountRow("지금 결제할 예약금", "${deposit.won()}원")
                AmountRow("현장 결제 예정액", "${remaining.won()}원")
                HorizontalDivider(color = MuyeonColors.border)
                AmountRow("지금 결제", "${deposit.won()}원", emphasize = true)
            }
        }

        Text(
            "예약금은 레슨비에 포함되며, 수업 24시간 전까지 취소하면 전액 환불돼요.",
            fontFamily = customFontFamily, fontSize = 13.sp, lineHeight = 18.sp,
            color = MuyeonColors.textSub,
        )

        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFF2F2F7)).clickable { onOpenPolicy() }
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "취소·환불 규정 전체보기",
                fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                lineHeight = 17.sp, color = MuyeonColors.textHead, modifier = Modifier.weight(1f),
            )
            Text(
                "›", fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp,
                color = MuyeonColors.textSub,
            )
        }

        Row(
            Modifier.fillMaxWidth().clickable { onTogglePolicy(!policyAgreed) },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = policyAgreed, onCheckedChange = onTogglePolicy)
            Text(
                "취소·환불 규정을 확인했습니다.",
                fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp,
                lineHeight = 17.sp, color = MuyeonColors.textHead,
            )
        }
    }
}

@Composable
private fun AmountRow(label: String, value: String, emphasize: Boolean = false) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            fontFamily = customFontFamily,
            fontWeight = if (emphasize) FontWeight.Bold else FontWeight.Normal,
            fontSize = 15.sp, lineHeight = 18.sp,
            color = if (emphasize) MuyeonColors.textHead else MuyeonColors.textSub,
            modifier = Modifier.weight(1f),
        )
        Text(
            value,
            fontFamily = customFontFamily,
            fontWeight = if (emphasize) FontWeight.Bold else FontWeight.SemiBold,
            fontSize = 15.sp, lineHeight = 18.sp,
            color = if (emphasize) MuyeonColors.primary else MuyeonColors.textHead,
        )
    }
}

/**
 * 취소·환불 규정 전문 — iOS `LessonRefundPolicySheet` 이식.
 *  판매자 추가 안내(cancelPolicy)는 공통 기준보다 소비자에게 유리할 때만 적용된다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonRefundPolicySheet(sellerPolicy: String?, onDismiss: () -> Unit) {
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
                    "취소 시점에 따라 아래 기준으로 예약금이 환불됩니다.",
                    fontFamily = customFontFamily, fontSize = 14.sp, lineHeight = 20.sp,
                    color = MuyeonColors.textSub,
                )
            }

            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0xFFF2F2F7)),
            ) {
                PolicyRow("수업 24시간 전까지", "예약금 전액 환불", accent = true)
                HorizontalDivider(color = MuyeonColors.border, modifier = Modifier.padding(start = 16.dp))
                PolicyRow("수업 24시간 이내", "예약금 환불 불가")
                HorizontalDivider(color = MuyeonColors.border, modifier = Modifier.padding(start = 16.dp))
                PolicyRow("수업 시작 후·노쇼", "예약금 환불 불가")
                HorizontalDivider(color = MuyeonColors.border, modifier = Modifier.padding(start = 16.dp))
                PolicyRow("강사 취소·수업 미제공", "예약금 전액 환불", accent = true)
            }

            if (!sellerPolicy.isNullOrBlank()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "판매자 추가 안내",
                        fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                        lineHeight = 19.sp, color = MuyeonColors.textHead,
                    )
                    Text(
                        sellerPolicy,
                        fontFamily = customFontFamily, fontSize = 14.sp, lineHeight = 20.sp,
                        color = MuyeonColors.textHead,
                    )
                    Text(
                        "공통 기준보다 소비자에게 유리한 경우 적용됩니다.",
                        fontFamily = customFontFamily, fontSize = 12.sp, lineHeight = 16.sp,
                        color = MuyeonColors.textSub,
                    )
                }
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

/** 예약 가능 조회 구간 — 오늘부터 8주. */
private fun bookingRange(): Pair<String, String> {
    val cal = kstCalendar()
    val from = kstYmd.format(Date(cal.timeInMillis))
    cal.add(Calendar.DAY_OF_MONTH, 56)
    return from to kstYmd.format(Date(cal.timeInMillis))
}

/**
 * 예약 취소 사유 시트 — iOS `LessonCancelReasonSheet.swift`.
 *  ⚠️ 사유 코드는 서버·admin 과 3레포 계약이라 값을 바꾸면 관리자 통계가 깨진다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonCancelReasonSheet(onDismiss: () -> Unit, onConfirm: (LessonCancelReason) -> Unit) {
    var selected by remember { mutableStateOf<LessonCancelReason?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Text(
                "취소 사유를 선택해주세요.",
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
            Text(
                "선택 완료",
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                lineHeight = 19.sp, color = Color.White, textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(if (selected != null) MuyeonColors.primary else Color.Gray.copy(alpha = 0.4f))
                    .clickable(enabled = selected != null) { selected?.let(onConfirm) }
                    .padding(vertical = 15.dp),
            )
        }
    }
}
