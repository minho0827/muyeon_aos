package com.muyeon.app.ui.quote

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors
import com.muyeon.app.ui.lesson.LessonRefundPolicy

/**
 * 제안 칸 부품 — 보내기 시트(QuoteRespondSheet)와 고치기 시트(QuoteResponseEditSheet)가 같이 쓴다.
 *  회당 금액 · 예약금 · 메시지. 모양은 보내기 시트 그대로 옮겼다.
 */

/** 회당 금액(원) — 숫자만, 비우면 협의. */
@Composable
internal fun QuoteAmountField(amountText: String, onChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        FieldLabel("회당 금액 (원)")
        OutlinedTextField(
            value = amountText,
            onValueChange = { v -> onChange(v.filter { it.isDigit() }) },
            placeholder = { Text("예: 60000 (비우면 협의)", fontFamily = customFontFamily, fontSize = 14.sp) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** 예약금 — 켜면 한도(관리자 '환불 규정') 안의 금액 칩 + 안내 + 문제 문구. */
@Composable
internal fun QuoteDepositField(
    takesDeposit: Boolean,
    onToggle: (Boolean) -> Unit,
    depositAmount: Int,
    onPick: (Int) -> Unit,
    policy: LessonRefundPolicy,
    problem: String?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            Modifier.fillMaxWidth().clickable { onToggle(!takesDeposit) },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "예약금 결제 후 확정",
                fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 15.sp,
                lineHeight = 18.sp, color = MuyeonColors.textHead, modifier = Modifier.weight(1f),
            )
            Switch(
                checked = takesDeposit, onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(checkedTrackColor = MuyeonColors.primary),
            )
        }
        if (takesDeposit) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                policy.depositChoices.forEach { v -> DepositChip(v, depositAmount == v) { onPick(v) } }
            }
            Text(policy.deposit.hint, fontFamily = customFontFamily, fontSize = 12.sp, lineHeight = 16.sp, color = MuyeonColors.secondary)
            problem?.let { Text(it, fontFamily = customFontFamily, fontSize = 12.sp, lineHeight = 16.sp, color = Color.Red) }
        }
    }
}

/** 견적 메시지 — 제목줄 오른쪽 [예시] + 여러 줄 입력. */
@Composable
internal fun QuoteMessageField(message: String, onChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            FieldLabel("견적 메시지", Modifier.weight(1f))
            // 예시 시트 — 열고 닫아도 쓰던 메시지는 그대로.
            QuoteMessageExamplesButton()
        }
        OutlinedTextField(value = message, onValueChange = onChange, modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp))
    }
}

@Composable
private fun FieldLabel(text: String, modifier: Modifier = Modifier) = Text(
    text,
    fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp,
    lineHeight = 16.sp, color = MuyeonColors.textSub, modifier = modifier,
)

@Composable
private fun DepositChip(value: Int, on: Boolean, onClick: () -> Unit) = Text(
    "${String.format(java.util.Locale.KOREA, "%,d", value)}원",
    fontFamily = customFontFamily,
    fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
    fontSize = 14.sp, lineHeight = 17.sp,
    color = if (on) Color.White else MuyeonColors.textHead,
    modifier = Modifier
        .clip(RoundedCornerShape(10.dp))
        .background(if (on) MuyeonColors.primary else Color(0xFFF2F2F7))
        .clickable(onClick = onClick)
        .padding(horizontal = 14.dp, vertical = 9.dp),
)
