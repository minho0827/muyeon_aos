package com.muyeon.app.ui.lesson

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors
import java.util.Locale

/** 가격 기준 선택지 — 서버 priceUnit 값 to 표시 문구(iOS LessonPaymentSection 과 같은 값). */
private val PRICE_UNITS = listOf("PER_PERSON" to "1인당", "PER_BOOKING" to "예약 1건당")

/**
 * 레슨 개설 4단계 '가격 기준 · 예약금' 섹션 — iOS `LessonPaymentSection.swift` 1:1.
 *  예약금 스위치를 끄면 NONE(결제 없이 예약 즉시 확정), 켜면 DEPOSIT(예약금 결제 후 확정).
 *  금액 선택지·한도 안내는 관리자 '환불 규정'([LessonRefundPolicyRepo])을 따르며, 최종 검사는 서버가 같은 값으로 한다.
 */
@Composable
internal fun LessonPaymentSection(d: LessonWizardDraft, onChange: (LessonWizardDraft) -> Unit) {
    var policy by remember { mutableStateOf(LessonRefundPolicyRepo.current()) }
    LaunchedEffect(Unit) { policy = LessonRefundPolicyRepo.get() }
    val takesDeposit = d.paymentMode == "DEPOSIT"
    // 이 레슨 가격에서 받을 수 있는 금액만 보여 준다(한도 밖 금액을 고르고 제출 단계에서 막히지 않게).
    val choices = policy.depositChoices.filter { policy.depositError(it, d.price) == null }

    WizardField("가격 기준") {
        WizardMenu(
            PRICE_UNITS.firstOrNull { it.first == d.priceUnit }?.second.orEmpty(),
            "가격 기준",
            PRICE_UNITS.map { it.second },
        ) { label -> PRICE_UNITS.firstOrNull { it.second == label }?.let { onChange(d.copy(priceUnit = it.first)) } }
    }
    WizardToggle("예약금 받기", takesDeposit) { on ->
        onChange(
            if (on) d.copy(paymentMode = "DEPOSIT", depositAmount = d.depositAmount.takeIf { it in choices } ?: choices.firstOrNull() ?: 0)
            else d.copy(paymentMode = "NONE", depositAmount = 0),
        )
    }
    if (!takesDeposit) {
        PaymentHint("예약금 없이 바로 예약이 확정돼요.")
        return
    }
    if (choices.isEmpty()) {
        PaymentHint("레슨 가격이 낮아 받을 수 있는 예약금이 없어요. ${policy.deposit.hint}")
        return
    }
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        choices.forEach { v ->
            val on = d.depositAmount == v
            Text(
                "${String.format(Locale.KOREA, "%,d", v)}원",
                fontFamily = customFontFamily,
                fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                fontSize = 14.sp, lineHeight = 17.sp,
                color = if (on) Color.White else MuyeonColors.textHead,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (on) MuyeonColors.primary else Color(0xFFF2F2F7))
                    .clickable { onChange(d.copy(depositAmount = v)) }
                    .padding(horizontal = 14.dp, vertical = 9.dp),
            )
        }
    }
    PaymentHint(
        (if (d.priceUnit == "PER_PERSON") "예약 인원수만큼 곱해서 받아요. " else "") +
            "예약금은 전체 레슨비에 포함돼요. ${policy.deposit.hint}",
    )
}

/** 미리보기 '예약 결제' 행 문구. */
internal fun paymentSummary(d: LessonWizardDraft): String =
    if (d.paymentMode == "DEPOSIT") "예약금 ${String.format(Locale.KOREA, "%,d", d.depositAmount)}원 결제 후 확정"
    else "결제 없이 예약"

@Composable
private fun PaymentHint(text: String) {
    Text(
        text,
        fontFamily = customFontFamily, fontSize = 12.sp, lineHeight = 16.sp,
        color = MuyeonColors.secondary,
    )
}
