package com.muyeon.app.ui.chat.proposal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.FormField
import com.muyeon.app.ui.common.MuyeonColors

/**
 * ★ 2026-11-27 견적에서 정해져 바꿀 수 없는 칸 — 읽기 전용 줄(예: "60분 · 강사님 견적").
 *  회원 약속잡기(길이·장소)와 다른 시간 제안(그대로 가는 길이·장소)이 쓴다.
 */
@Composable
internal fun ProposalFixedRow(label: String, value: String, source: String?, tag: String) {
    FormField(label) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MuyeonColors.border.copy(alpha = 0.35f))
                .padding(horizontal = 14.dp, vertical = 13.dp).testTag(tag),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                value, fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 15.sp,
                lineHeight = 18.sp, color = MuyeonColors.textHead, modifier = Modifier.weight(1f),
            )
            source?.let {
                Text(it, fontFamily = customFontFamily, fontSize = 12.sp, lineHeight = 15.sp, color = MuyeonColors.textSub)
            }
        }
    }
}
