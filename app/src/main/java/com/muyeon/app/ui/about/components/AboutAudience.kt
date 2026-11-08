package com.muyeon.app.ui.about.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.muyeon.app.R
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.Dimensions

/** 대상 — 웹 .about-audience(머리 + 줄 3개: 왼쪽 80 굵은 이름 · 오른쪽 설명, 줄 사이 선). */
@Composable
fun AboutAudience() {
    val rows = listOf(
        R.string.about_aud1_title to R.string.about_aud1_desc, R.string.about_aud2_title to R.string.about_aud2_desc,
        R.string.about_aud3_title to R.string.about_aud3_desc,
    )
    Column(Modifier.fillMaxWidth().padding(horizontal = Dimensions.gutter, vertical = 64.dp)) {
        AboutSectionHead(stringResource(R.string.about_audience_eyebrow), stringResource(R.string.about_audience_title))
        Column(Modifier.padding(top = 32.dp)) {
            rows.forEach { (t, d) ->
                HorizontalDivider(thickness = 1.dp, color = AppColors.Divider)
                Row(Modifier.fillMaxWidth().padding(vertical = 20.dp)) {
                    Text(stringResource(t), style = AppTypography.Body15.copy(fontWeight = FontWeight.Bold), color = AppColors.TextHead, modifier = Modifier.width(96.dp))
                    Text(stringResource(d), style = AppTypography.Body14, color = AppColors.TextSub, modifier = Modifier.weight(1f).padding(start = 16.dp))
                }
            }
            HorizontalDivider(thickness = 1.dp, color = AppColors.Divider)
        }
    }
}
