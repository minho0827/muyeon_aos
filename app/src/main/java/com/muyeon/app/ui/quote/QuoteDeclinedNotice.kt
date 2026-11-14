package com.muyeon.app.ui.quote

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.R
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors

/**
 * ★ 2026-11-14 지정 강사가 정중히 거절한 요청 — 요청자(원장·회원)에게 사유 문장 + '비슷한 강사님께 다시 요청' 안내
 *  (iOS QuoteDeclinedNotice 1:1). 아래에는 기존 추천 카드를 그대로 붙인다.
 */
@Composable
fun QuoteDeclinedNotice(notice: QuoteDeclineNotice, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = 16.dp).clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF8F8F8)).padding(16.dp).testTag("quote.declined"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(stringResource(R.string.quote_declined_title), fontFamily = customFontFamily, fontWeight = FontWeight.Bold,
            fontSize = 16.sp, color = MuyeonColors.textHead)
        notice.message?.takeIf { it.isNotBlank() }?.let {
            Text(it, fontFamily = customFontFamily, fontSize = 14.sp, lineHeight = 20.sp, color = MuyeonColors.textHead)
        }
        Text(stringResource(R.string.quote_declined_hint), fontFamily = customFontFamily, fontSize = 13.sp, color = MuyeonColors.textSub)
    }
}
