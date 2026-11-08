package com.muyeon.app.ui.gate.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.muyeon.app.R
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography

/** 웹 #system_inspection 의 img-box(6rem) · 큰 제목(t-22 굵게 · 가운데) · 설명(t-16 · 회색 · 가운데). 점검·학원 인증 차단 화면 공용. */
@Composable
fun GateNoticeHeader(title: String, body: String) {
    Image(painterResource(R.drawable.img_inspection), contentDescription = null, modifier = Modifier.size(IMAGE))
    Text(title, style = AppTypography.Title22, color = AppColors.TextHead, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    Text(body, style = AppTypography.Section16.copy(fontWeight = FontWeight.Normal), color = AppColors.TextSub, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
}

/** 웹 #system_inspection .content-wrap — 위 여백 10.4rem · 간격 1.6rem · 좌우 2rem. */
object GateMetrics {
    val TOP = 104.dp
    val GAP = 16.dp
}

private val IMAGE = 60.dp
