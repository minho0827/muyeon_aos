package com.muyeon.app.ui.space

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Remove
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.FormTextField
import com.muyeon.app.ui.common.MarketDatePickerDialog
import com.muyeon.app.ui.common.MarketDates
import com.muyeon.app.ui.lesson.LessonPaymentActivity
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * 공간 '예약하기' 시트(2026-11-03 결제 예약) — iOS `SpaceReserveSheet` 와 같은 순서.
 *  세부공간 → 달력(오늘 이전·휴무 비활성) → GET availability → 시작 시간 칩(30분, 막힌 구간 비활성) →
 *  이용 시간(minHours 부터 0.5 단위) → 인원 −/＋ → 요청사항 → 금액 미리보기 → 환불 규정 동의 → [결제하기].
 *  [결제하기] = POST /spaces/:id/reservation(결제 대기 10분) → 결제 창(LessonPaymentActivity)에서 웹 /spacePay/{id}.
 *  결제 창이 결제 완료로 닫히면 onPaid(시트 닫고 내 예약), 아니면 잡아 둔 자리를 풀고 가능 시간을 다시 읽는다.
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
    var agree by remember { mutableStateOf(false) }
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
    val canSubmit = !submitting && date != null && slotOk && agree

    ModalBottomSheet(onDismissRequest = { onFinish(null) }, sheetState = sheetState, containerColor = Color.White) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().height(SpaceDesign.headerHeight).padding(horizontal = SpaceDesign.gutter),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "예약하기", fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp,
                    lineHeight = 22.sp, color = SpaceDesign.ink900, modifier = Modifier.weight(1f),
                )
                Icon(Icons.Filled.Close, "닫기", tint = SpaceDesign.ink900, modifier = Modifier.size(16.dp).clickable { onFinish(null) })
            }

            Column(
                Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())
                    .padding(horizontal = SpaceDesign.gutter).padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                if (options.isNotEmpty()) {
                    FieldGroup("세부공간") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            options.forEach { o ->
                                RadioRow("${o.label ?: "세부공간"} · ${o.priceText}", optionId == o.id) { optionId = o.id }
                            }
                        }
                    }
                }

                FieldGroup("날짜") {
                    BoxField(date ?: "날짜를 선택해 주세요", placeholder = date == null) { showDate = true }
                }

                if (date != null) {
                    when {
                        availLoading -> Box(Modifier.fillMaxWidth().height(48.dp), Alignment.Center) {
                            CircularProgressIndicator(color = SpaceDesign.primary, modifier = Modifier.size(24.dp))
                        }
                        availError != null -> Notice(availError!!)
                        a != null && !a.open -> Notice("이날은 예약할 수 없어요. 다른 날짜를 골라 주세요.")
                        a != null -> {
                            FieldGroup("시작 시간") {
                                val starts = (a.start..(a.end - (minHours * 60).toInt()) step 30).toList()
                                if (starts.isEmpty()) Notice("예약할 수 있는 시간이 없어요.")
                                ChipFlow(starts.map { t -> Triple(SpaceAvailability.hhmm(t), startMin == t, a.fits(t, minHours)) }) { i ->
                                    val t = starts[i]
                                    startMin = t
                                    if (!a.fits(t, hours)) hours = minHours
                                }
                            }
                            FieldGroup("이용 시간") {
                                val s0 = startMin
                                val choices = generateSequence(minHours) { it + 0.5 }.takeWhile { it <= 24.0 && (s0 ?: a.start) + it * 60 <= a.end }.toList()
                                ChipFlow(choices.map { h -> Triple(hourLabel(h), hours == h, s0 == null || a.fits(s0, h)) }) { i -> hours = choices[i] }
                                if (s0 == null) Caption("시작 시간을 먼저 골라 주세요.")
                            }
                        }
                    }
                }

                FieldGroup("인원") {
                    val maxHc = space.capacityMax ?: 100
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        StepButton(Icons.Filled.Remove, headcount > 1) { headcount -= 1 }
                        Text("${headcount}명", fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = SpaceDesign.ink900)
                        StepButton(Icons.Filled.Add, headcount < maxHc) { headcount += 1 }
                        space.capacityMax?.let { Caption("최대 ${it}명") }
                    }
                    if (space.baseHeadcount != null && (space.extraPersonFee ?: 0) > 0) {
                        Caption("기본 ${space.baseHeadcount}명, 초과 1인당 시간당 ${SpaceDesign.won(space.extraPersonFee ?: 0)}원")
                    }
                }

                FieldGroup("요청사항") { FormTextField(message, { message = it }, "호스트에게 전할 말을 적어 주세요(선택)", 500, minLines = 3) }

                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SpaceDesign.ink100)
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("예상 금액", fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = SpaceDesign.ink900)
                        Text(
                            SpaceDesign.won(estimate), fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 24.sp,
                            color = SpaceDesign.primaryText, textAlign = TextAlign.End, modifier = Modifier.weight(1f),
                        )
                        Text("원", fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = SpaceDesign.primaryText)
                    }
                    Caption(
                        "시간당 ${SpaceDesign.won(unitPrice)}원 × ${hourLabel(hours)}" +
                            (if (extraFee > 0) " + 초과 인원 ${SpaceDesign.won(extraFee.roundToInt())}원" else "") +
                            " · 최종 금액은 결제 화면에서 확인해요.",
                    )
                }

                val rules = (space.refundPolicy ?: emptyList()).filter { !it.label.isNullOrEmpty() }
                    .ifEmpty { DEFAULT_REFUND }
                FieldGroup("환불 규정") {
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).border(1.dp, SpaceDesign.ink200, RoundedCornerShape(12.dp)).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        rules.forEach { r ->
                            Row {
                                Text(r.label ?: "-", fontFamily = customFontFamily, fontSize = 14.sp, color = SpaceDesign.ink600, modifier = Modifier.weight(1f))
                                Text(r.value ?: "-", fontFamily = customFontFamily, fontSize = 14.sp, color = SpaceDesign.ink900)
                            }
                        }
                    }
                }
                Row(
                    Modifier.fillMaxWidth().clickable { agree = !agree },
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        Modifier.size(22.dp).clip(RoundedCornerShape(6.dp))
                            .background(if (agree) SpaceDesign.primary else Color.White)
                            .border(1.dp, if (agree) SpaceDesign.primary else SpaceDesign.ink300, RoundedCornerShape(6.dp)),
                        Alignment.Center,
                    ) { if (agree) Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(16.dp)) }
                    Text("유의사항과 환불 규정을 확인했고 동의해요.", fontFamily = customFontFamily, fontSize = 14.sp, color = SpaceDesign.ink900)
                }

                error?.let { Text(it, fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp, color = Color(0xFFE64747)) }
                Spacer(Modifier.height(8.dp))
            }

            Column(Modifier.fillMaxWidth().background(Color.White).navigationBarsPadding()) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(SpaceDesign.ink200))
                Text(
                    if (submitting) "예약 중…" else "결제하기",
                    fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 20.sp,
                    color = if (canSubmit) Color.White else SpaceDesign.ink100, textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = SpaceDesign.gutter).padding(top = 12.dp, bottom = 20.dp)
                        .fillMaxWidth().height(52.dp).clip(RoundedCornerShape(8.dp))
                        .background(if (canSubmit) SpaceDesign.primary else SpaceDesign.ink300)
                        .clickable(enabled = canSubmit) {
                            if (!api.isLoggedIn) { error = "로그인 후 이용할 수 있어요."; return@clickable }
                            submitting = true; error = null
                            scope.launch {
                                api.reserve(
                                    space.id,
                                    SpaceReservationRequest(optionId, date!!, SpaceAvailability.hhmm(startMin!!), hours, headcount, message),
                                ).onSuccess { (rid, _) ->
                                    onReserved()
                                    pendingId = rid
                                    payLauncher.launch(LessonPaymentActivity.PaymentPage("/spacePay/$rid", "대관료 결제"))
                                }.onFailure { error = it.message ?: "예약하지 못했어요. 잠시 후 다시 시도해 주세요." }
                                submitting = false
                            }
                        }
                        .wrapContentHeight(Alignment.CenterVertically),
                )
            }
        }
    }

    if (showDate) {
        MarketDatePickerDialog(
            initial = date,
            allow = { it >= today && space.isOpenOn(it) },
            onPick = { date = it },
            onDismiss = { showDate = false },
        )
    }
}

/** 환불 규정이 비었을 때 서버 기본값과 같은 안내(이용 2일 전 100% / 전날 50% / 당일 불가). */
internal val DEFAULT_REFUND = listOf(
    SpaceRefundRule("이용 2일 전", "총 금액의 100% 환불"),
    SpaceRefundRule("이용 전날", "총 금액의 50% 환불"),
    SpaceRefundRule("이용 당일", "환불 불가"),
)

internal fun hourLabel(value: Double): String {
    val whole = value.toInt()
    return if (value % 1.0 != 0.0) (if (whole > 0) "${whole}시간 30분" else "30분") else "${whole}시간"
}

// MARK: 폼 조각 — 라벨 + 테두리 박스(h48, r8, #EAEAEA)

@Composable
internal fun FieldGroup(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 17.sp, color = SpaceDesign.ink900)
        content()
    }
}

@Composable
private fun Caption(text: String) {
    Text(text, fontFamily = customFontFamily, fontSize = 12.sp, lineHeight = 16.sp, color = SpaceDesign.ink600)
}

@Composable
private fun Notice(text: String) {
    Text(
        text, fontFamily = customFontFamily, fontSize = 14.sp, color = SpaceDesign.ink700,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(SpaceDesign.primaryTint).padding(14.dp),
    )
}

@Composable
internal fun RadioRow(text: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(8.dp))
            .border(1.dp, if (selected) SpaceDesign.primary else SpaceDesign.ink200, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(20.dp).clip(CircleShape)
                .border(if (selected) 6.dp else 1.dp, if (selected) SpaceDesign.primary else SpaceDesign.ink300, CircleShape),
        )
        Text(text, fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 18.sp, color = SpaceDesign.ink900)
    }
}

@Composable
private fun BoxField(text: String, placeholder: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(8.dp))
            .border(1.dp, SpaceDesign.ink200, RoundedCornerShape(8.dp)).clickable(onClick = onClick).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text, fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 18.sp,
            color = if (placeholder) SpaceDesign.ink500 else SpaceDesign.ink900, modifier = Modifier.weight(1f),
        )
        Icon(Icons.Filled.ExpandMore, null, tint = SpaceDesign.ink500, modifier = Modifier.size(12.dp))
    }
}

/** 시간 칩 — (글자, 고름, 고를 수 있음). 못 고르는 칩은 흐리게. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipFlow(chips: List<Triple<String, Boolean, Boolean>>, onTap: (Int) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        chips.forEachIndexed { i, (label, on, enabled) ->
            Text(
                label, fontFamily = customFontFamily, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium, fontSize = 14.sp,
                color = if (on) Color.White else SpaceDesign.ink900,
                modifier = Modifier.alpha(if (enabled) 1f else 0.35f).clip(RoundedCornerShape(50))
                    .background(if (on) SpaceDesign.primary else SpaceDesign.ink100)
                    .clickable(enabled = enabled) { onTap(i) }.padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun StepButton(icon: androidx.compose.ui.graphics.vector.ImageVector, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(36.dp).alpha(if (enabled) 1f else 0.35f).clip(CircleShape).border(1.dp, SpaceDesign.ink300, CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        Alignment.Center,
    ) { Icon(icon, null, tint = SpaceDesign.ink900, modifier = Modifier.size(18.dp)) }
}
