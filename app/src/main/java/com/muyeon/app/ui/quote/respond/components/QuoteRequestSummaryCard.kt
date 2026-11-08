package com.muyeon.app.ui.quote.respond.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.muyeon.app.R
import com.muyeon.app.ui.components.LevelBadge
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.CategoryColors
import com.muyeon.app.ui.theme.Dimensions

/**
 * 요청 요약 카드 — 웹 #quote_respond .quote-summary-card(테두리 1 · 모서리 16 · 패딩 16 · 간격 6).
 *  제목 '○○ 레슨 요청'(16 bold) · 지역 핀(13) · [meta](공개 상세의 요청일·받은 견적 수) · 비회원 요청 안내.
 */
@Composable
fun QuoteRequestSummaryCard(
    categoryLabel: String,
    region: String?,
    modifier: Modifier = Modifier,
    meta: String? = null,
    guestRequest: Boolean = false,
) {
    val shape = RoundedCornerShape(Dimensions.cardRadius)
    Column(
        modifier.fillMaxWidth()
            .background(AppColors.Surface, shape)
            .border(1.dp, AppColors.Divider, shape)
            .padding(Dimensions.spacingL),
        verticalArrangement = Arrangement.spacedBy(SUMMARY_GAP),
    ) {
        Text(stringResource(R.string.quote_respond_request_title, categoryLabel), style = AppTypography.Section16, color = AppColors.TextHead)
        Row(horizontalArrangement = Arrangement.spacedBy(SUMMARY_GAP), verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.ic_web_location), null, tint = AppColors.TextSub, modifier = Modifier.size(Dimensions.iconS))
            Text(region?.ifBlank { null } ?: stringResource(R.string.quote_respond_region_none), style = AppTypography.Sub13, color = AppColors.TextSub)
        }
        if (meta != null) Text(meta, style = AppTypography.Sub13, color = AppColors.TextSub)
        // ★ 2026-11-05 비회원 레슨 요청 — 견적은 보낼 수 있고, 채팅은 고객이 가입한 뒤 열린다(웹 같은 문구).
        if (guestRequest) {
            Column(Modifier.padding(top = Dimensions.spacingXXS), verticalArrangement = Arrangement.spacedBy(SUMMARY_GAP)) {
                LevelBadge(stringResource(R.string.quote_respond_guest_tag), CategoryColors.Level.Middle)
                Text(stringResource(R.string.quote_respond_guest_notice), style = AppTypography.Sub13, color = AppColors.TextBody)
            }
        }
    }
}

/** 웹 flex-col gap 0.6rem. */
private val SUMMARY_GAP = 6.dp
