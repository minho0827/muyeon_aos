package com.muyeon.app.ui.lesson

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.LocalParking
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors
import com.muyeon.app.ui.quote.QuoteDialog
import com.muyeon.app.ui.quote.QuoteNavBar
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.util.Locale

/**
 * 레슨 예약 상세 — iOS `LessonReservationDetailSheet.swift` 1:1.
 *  예약금 안내·예약자·레슨 위치·주차·유의사항·취소정책·상세정보 + 하단 취소/변경.
 *
 * ⚠️ 환불액은 **서버가 계산한 값(expectedRefundAmount)** 을 그대로 쓴다.
 *   앱에서 위약금을 다시 계산하면 규정이 바뀔 때 서버와 갈린다(iOS 주석과 동일 규칙).
 */
@Composable
fun LessonReservationDetailScreen(
    api: LessonBookingApi,
    reservationId: Int,
    onClose: () -> Unit,
    onChange: (Int, Int) -> Unit,   // (productId, reservationId) — 원자적 리스케줄
    onCanceled: (Int) -> Unit,      // 취소 완료 → 웹에 즉시 반영
) {
    var detail by remember { mutableStateOf<LessonReservationDetail?>(null) }
    var loading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var doneMessage by remember { mutableStateOf<String?>(null) }
    var didCancel by remember { mutableStateOf(false) }
    var showReasonSheet by remember { mutableStateOf(false) }
    var pendingReason by remember { mutableStateOf<LessonCancelReason?>(null) }
    var pendingReasonDetail by remember { mutableStateOf<String?>(null) }
    var showFinalConfirmation by remember { mutableStateOf(false) }
    var disputeReason by remember { mutableStateOf("") }
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    suspend fun load() {
        loading = true
        api.reservationDetail(reservationId).onSuccess { detail = it }
        loading = false
    }

    LaunchedEffect(reservationId) { load() }

    Column(Modifier.fillMaxSize().background(MuyeonColors.groupedBg)) {
        QuoteNavBar(title = "방문 예정", onBack = onClose)

        val d = detail
        when {
            loading -> Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                CircularProgressIndicator(color = MuyeonColors.primary)
            }
            d == null -> Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                Text(
                    "예약 정보를 불러오지 못했어요",
                    fontFamily = customFontFamily, fontSize = 14.sp, color = MuyeonColors.textSub,
                )
            }
            else -> {
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState())
                        .padding(horizontal = 14.dp).padding(top = 10.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    HeaderSection(d)
                    PaymentSection(d)
                    VisitorSection(d)
                    LocationSection(d.lesson)
                    if (d.lesson.parkingInfo != null || d.lesson.valetInfo != null) ParkingSection(d.lesson)
                    d.lesson.notice?.takeIf { it.isNotEmpty() }?.let { NoticeSection(it) }
                    CancelPolicySection(d)
                    DetailInfoSection(d.lesson)
                    // 수업 완료 처리 후 5일 이내에만 접수할 수 있다(서버 규정).
                    if (d.status in listOf("ATTENDED", "NOSHOW") && d.disputeStatus == null) {
                        Card {
                            SectionHeader(Icons.Outlined.Forum, "수업 처리 이의 신청")
                            OutlinedTextField(
                                value = disputeReason,
                                onValueChange = { disputeReason = it },
                                placeholder = {
                                    Text(
                                        "확인이 필요한 내용을 적어 주세요",
                                        fontFamily = customFontFamily, fontSize = 14.sp, color = MuyeonColors.chevron,
                                    )
                                },
                                modifier = Modifier.fillMaxWidth().heightIn(min = 90.dp),
                            )
                            Text(
                                "수업 완료 처리 후 5일 이내 앱에서 접수할 수 있어요. 이후 법정 권리는 고객센터로 문의할 수 있습니다.",
                                fontFamily = customFontFamily, fontSize = 12.sp, lineHeight = 17.sp,
                                color = MuyeonColors.textSub,
                            )
                            val canSubmit = disputeReason.trim().isNotEmpty()
                            Text(
                                "이의 신청",
                                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                                lineHeight = 18.sp, color = Color.White, textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                    .background(if (canSubmit) MuyeonColors.primary else MuyeonColors.tileLocked)
                                    .clickable(enabled = canSubmit) {
                                        val reason = disputeReason.trim()
                                        disputeReason = ""
                                        scope.launch {
                                            api.openDispute(reservationId, reason)
                                                .onSuccess {
                                                    doneMessage = "이의 신청이 접수되었습니다. 확인 전까지 정산이 보류됩니다."
                                                    load()
                                                }
                                                .onFailure { errorMessage = it.message }
                                        }
                                    }
                                    .padding(vertical = 12.dp),
                            )
                        }
                    }
                }

                if (d.status == "CONFIRMED") {
                    Row(
                        Modifier.fillMaxWidth().background(MuyeonColors.surface)
                            .padding(horizontal = 16.dp).padding(top = 8.dp, bottom = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            "레슨 취소",
                            fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp,
                            lineHeight = 19.sp, color = MuyeonColors.textHead, textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                                .border(1.dp, MuyeonColors.border, RoundedCornerShape(12.dp))
                                .clickable { showReasonSheet = true }
                                .padding(vertical = 15.dp),
                        )
                        Text(
                            "변경",
                            fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp,
                            lineHeight = 19.sp, color = Color.White, textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                                .background(MuyeonColors.primary)
                                .clickable {
                                    val pid = d.lesson.id
                                    if (pid != null) onChange(pid, reservationId) else onClose()
                                }
                                .padding(vertical = 15.dp),
                        )
                    }
                }
            }
        }
    }

    // 취소 확정 직전 사유 선택 — 사유는 서버 저장 후 admin 에서 조회한다.
    if (showReasonSheet) {
        LessonCancelReasonSheet(
            onDismiss = { showReasonSheet = false },
            onConfirm = { reason, reasonDetail ->
                showReasonSheet = false
                pendingReason = reason
                pendingReasonDetail = reasonDetail
                showFinalConfirmation = true
            },
        )
    }

    if (showFinalConfirmation) {
        QuoteDialogTwoActions(
            title = "레슨을 취소하시겠어요?",
            message = finalConfirmationMessage(detail),
            confirmText = "취소할게요",
            dismissText = "아니요",
            onConfirm = {
                showFinalConfirmation = false
                val reason = pendingReason
                val rd = pendingReasonDetail
                pendingReason = null
                pendingReasonDetail = null
                if (reason != null) {
                    scope.launch {
                        api.cancel(reservationId, reason.code, rd)
                            .onSuccess { didCancel = true; doneMessage = "레슨이 취소되었습니다." }
                            .onFailure { errorMessage = it.message }
                    }
                }
            },
            onDismiss = {
                showFinalConfirmation = false
                pendingReason = null
                pendingReasonDetail = null
            },
        )
    }

    doneMessage?.let { msg ->
        QuoteDialog(
            "처리가 완료되었습니다.", msg, "확인",
            onConfirm = {
                doneMessage = null
                if (didCancel) onCanceled(reservationId)
            },
            onDismiss = {
                doneMessage = null
                if (didCancel) onCanceled(reservationId)
            },
        )
    }
    errorMessage?.let { msg ->
        QuoteDialog("오류", msg, "확인", onConfirm = { errorMessage = null }, onDismiss = { errorMessage = null })
    }
}

/**
 * 취소 확인 문구 — 환불액은 서버가 적용 정책에 따라 계산한다. 앱에서 재계산하지 않는다.
 *  구버전 서버가 예상값을 안 주면 확정값으로 물러선다(iOS finalConfirmationMessage 와 동일).
 */
private fun finalConfirmationMessage(d: LessonReservationDetail?): String {
    val base = "취소 후에는 되돌릴 수 없습니다."
    if (d == null) return base
    val payment = d.paymentAmount ?: d.deposit
    if (payment <= 0) return base
    val refund = d.expectedRefundAmount
        ?: d.refundAmount
        ?: maxOf(0, payment - (d.cancelFee ?: 0))
    if (refund == 0) return "$base\n수업이 시작되어 환불되지 않습니다."
    val fee = d.expectedCancelFee ?: d.cancelFee ?: maxOf(0, payment - refund)
    if (fee == 0) return "$base\n결제한 예약금 ${won(refund)}원이 전액 환불됩니다."
    return "$base\n취소 수수료 ${won(fee)}원을 제외한 ${won(refund)}원이 환불됩니다."
}

private fun won(v: Int) = String.format(Locale.KOREA, "%,d", v)

// MARK: 섹션

@Composable
private fun HeaderSection(d: LessonReservationDetail) = Card {
    Text(
        d.lesson.title,
        fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 22.sp,
        lineHeight = 27.sp, color = MuyeonColors.textHead,
    )
    d.lesson.genre?.let {
        Text(it, fontFamily = customFontFamily, fontSize = 14.sp, lineHeight = 17.sp, color = MuyeonColors.textSub)
    }
    Text(
        d.dateLine,
        fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp,
        lineHeight = 19.sp, color = MuyeonColors.primary,
    )
    if (d.freeCancelDays > 0) FreeCancelBanner(d.freeCancelDays)
}

@Composable
private fun FreeCancelBanner(days: Int) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
            .background(MuyeonColors.green.copy(alpha = 0.1f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Schedule, null, tint = MuyeonColors.green, modifier = Modifier.size(14.dp))
        Text(
            "무료 취소·변경 기간이 ${days}일 남았어요",
            fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp,
            lineHeight = 16.sp, color = MuyeonColors.green,
        )
    }
}

@Composable
private fun PaymentSection(d: LessonReservationDetail) = Card {
    SectionHeader(Icons.Filled.CreditCard, "결제 정보")
    Kv("총 레슨비", "${won(d.totalPrice ?: 0)}원")
    Kv("결제한 예약금", "${won(d.paymentAmount ?: d.deposit)}원")
    Kv("현장 결제 예정액", "${won(d.remainingAmount ?: 0)}원")
    Text(
        "예약금은 별도 수수료가 아니라 전체 레슨비에 포함됩니다.",
        fontFamily = customFontFamily, fontSize = 12.sp, lineHeight = 17.sp, color = MuyeonColors.textSub,
    )
}

@Composable
private fun VisitorSection(d: LessonReservationDetail) = Card {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        SectionHeader(Icons.Filled.Person, "예약자 정보")
        Text(
            "(${d.headcount}명)",
            fontFamily = customFontFamily, fontSize = 13.sp, lineHeight = 16.sp, color = MuyeonColors.textSub,
        )
    }
    Kv("이름", d.member.name)
    d.member.phone?.let { Kv("연락처", it) }
}

@Composable
private fun LocationSection(p: LessonResPlace) {
    val ctx = LocalContext.current
    Card {
        SectionHeader(Icons.Filled.Map, "레슨 위치")
        p.address?.let { addr ->
            Text(addr, fontFamily = customFontFamily, fontSize = 14.sp, lineHeight = 20.sp, color = MuyeonColors.textHead)
            Text(
                "길찾기",
                fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                lineHeight = 17.sp, color = MuyeonColors.primary, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                    .border(1.dp, MuyeonColors.border, RoundedCornerShape(10.dp))
                    .clickable {
                        val q = URLEncoder.encode(p.address ?: p.place ?: p.title, "UTF-8")
                        runCatching {
                            ctx.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://map.naver.com/v5/search/$q")),
                            )
                        }
                    }
                    .padding(vertical = 11.dp),
            )
        }
    }
}

@Composable
private fun ParkingSection(p: LessonResPlace) = Card {
    SectionHeader(Icons.Outlined.LocalParking, "주차 및 발렛 안내")
    p.parkingInfo?.takeIf { it.isNotEmpty() }?.let { LabeledText("주차 정보", it) }
    p.valetInfo?.takeIf { it.isNotEmpty() }?.let { LabeledText("발렛 정보", it) }
}

@Composable
private fun NoticeSection(notice: String) = Card {
    SectionHeader(Icons.Filled.Info, "유의사항")
    Text(notice, fontFamily = customFontFamily, fontSize = 14.sp, lineHeight = 20.sp, color = MuyeonColors.textHead)
}

@Composable
private fun CancelPolicySection(d: LessonReservationDetail) = Card {
    SectionHeader(Icons.Outlined.Cancel, "취소·변경 정책")
    if (d.freeCancelDays > 0) FreeCancelBanner(d.freeCancelDays)
    Text(
        if (d.deposit > 0) {
            "수업 24시간 전까지는 예약금 전액 환불, 24시간 이내에는 예약금이 환불되지 않습니다. " +
                "수업 시작 후·노쇼도 미환불이며, 강사·학원 취소나 수업 미제공은 전액 환불됩니다."
        } else {
            "결제 없는 예약으로 취소 수수료가 없습니다."
        },
        fontFamily = customFontFamily, fontSize = 14.sp, lineHeight = 20.sp, color = MuyeonColors.textHead,
    )
    d.lesson.cancelPolicy?.takeIf { it.isNotEmpty() }?.let {
        Text(
            "판매자 추가 안내 · $it\n공통 기준보다 소비자에게 유리한 경우 적용됩니다.",
            fontFamily = customFontFamily, fontSize = 13.sp, lineHeight = 18.sp, color = MuyeonColors.textSub,
        )
    }
}

@Composable
private fun DetailInfoSection(p: LessonResPlace) {
    val ctx = LocalContext.current
    Card {
        SectionHeader(Icons.Filled.Info, "상세정보")
        p.phone?.let { phone ->
            LabeledSlot("전화번호") {
                Text(
                    phone,
                    fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                    lineHeight = 18.sp, color = MuyeonColors.info,
                    modifier = Modifier.clickable {
                        val digits = phone.filter { it.isDigit() }
                        if (digits.isNotEmpty()) {
                            runCatching { ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$digits"))) }
                        }
                    },
                )
            }
        }
        p.description?.let { LabeledText("매장 소개", it) }
        p.businessHours?.takeIf { it.isNotEmpty() }?.let { hours ->
            LabeledSlot("영업시간") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    hours.forEach { h ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                h.day,
                                fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp,
                                lineHeight = 17.sp, color = MuyeonColors.textHead,
                                modifier = Modifier.width(20.dp),
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                Text(
                                    h.time,
                                    fontFamily = customFontFamily, fontSize = 14.sp, lineHeight = 17.sp,
                                    color = MuyeonColors.textHead,
                                )
                                h.lastOrder?.let {
                                    Text(
                                        "$it 까지 라스트오더",
                                        fontFamily = customFontFamily, fontSize = 12.sp, lineHeight = 15.sp,
                                        color = MuyeonColors.textSub,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        p.homepage?.let { hp ->
            LabeledSlot("홈페이지") {
                Text(
                    hp,
                    fontFamily = customFontFamily, fontSize = 14.sp, lineHeight = 17.sp,
                    color = MuyeonColors.info, maxLines = 1,
                    modifier = Modifier.clickable {
                        runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(hp))) }
                    },
                )
            }
        }
    }
}

// MARK: 공통 조각

@Composable
private fun Card(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MuyeonColors.surface).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

@Composable
private fun SectionHeader(icon: ImageVector, title: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MuyeonColors.textHead, modifier = Modifier.size(15.dp))
        Text(
            title,
            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp,
            lineHeight = 19.sp, color = MuyeonColors.textHead,
        )
    }
}

@Composable
private fun Kv(k: String, v: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            k,
            fontFamily = customFontFamily, fontSize = 14.sp, lineHeight = 17.sp,
            color = MuyeonColors.textSub, modifier = Modifier.weight(1f),
        )
        Text(
            v,
            fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
            lineHeight = 17.sp, color = MuyeonColors.textHead,
        )
    }
}

@Composable
private fun LabeledText(label: String, value: String) = LabeledSlot(label) {
    Text(value, fontFamily = customFontFamily, fontSize = 14.sp, lineHeight = 20.sp, color = MuyeonColors.textHead)
}

@Composable
private fun LabeledSlot(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            label,
            fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
            lineHeight = 16.sp, color = MuyeonColors.textSub,
        )
        content()
    }
}

/** 확인/취소 문구를 모두 지정하는 다이얼로그(QuoteDialog 는 취소 문구가 "취소" 고정). */
@Composable
private fun QuoteDialogTwoActions(
    title: String,
    message: String,
    confirmText: String,
    dismissText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { com.muyeon.app.ui.quote.DialogTitle(title) },
        text = { com.muyeon.app.ui.quote.DialogMessage(message) },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onConfirm) {
                com.muyeon.app.ui.quote.DialogAction(confirmText)
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                com.muyeon.app.ui.quote.DialogAction(dismissText)
            }
        },
    )
}
