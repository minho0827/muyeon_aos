package com.muyeon.app.ui.chat.proposal

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.FormToggle
import com.muyeon.app.ui.common.MuyeonColors
import java.util.Locale

/**
 * ★ 2026-11-14 (회원만) 예약금 안내 + 취소·환불 규정 확인 스위치(무용연 스위치).
 *  규정 문구는 서버(관리자 '환불 규정')가 준 값 그대로.
 */
@Composable
internal fun ProposalDepositCard(
    totalPrice: Int,
    depositAmount: Int,
    policyText: String,
    agreed: Boolean,
    onAgree: (Boolean) -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        Modifier.fillMaxWidth().border(1.dp, MuyeonColors.border, shape).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("예약금 안내", fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MuyeonColors.textHead)
        Text(
            if (depositAmount > 0) "예약금 결제 후 일정이 확정돼요." else "이 견적은 결제 없이 일정이 확정돼요.",
            fontFamily = customFontFamily, fontSize = 13.sp, lineHeight = 18.sp, color = MuyeonColors.textSub,
        )
        PriceRow("총 레슨비", totalPrice)
        PriceRow("지금 결제할 예약금", depositAmount, accent = true)
        PriceRow("현장 결제 예정액", maxOf(0, totalPrice - depositAmount))
        HorizontalDivider(color = MuyeonColors.border)
        Text(
            "예약금은 레슨비에 포함돼요.\n$policyText",
            fontFamily = customFontFamily, fontSize = 13.sp, lineHeight = 20.sp, color = MuyeonColors.textSub,
        )
        FormToggle("취소·환불 규정을 확인했어요.", agreed, onAgree)
    }
}

@Composable
private fun PriceRow(title: String, amount: Int, accent: Boolean = false) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontFamily = customFontFamily, fontSize = 14.sp, color = MuyeonColors.textSub, modifier = Modifier.weight(1f))
        Text(
            "${String.format(Locale.KOREA, "%,d", amount)}원",
            fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
            color = if (accent) MuyeonColors.primary else MuyeonColors.textHead,
        )
    }
}
