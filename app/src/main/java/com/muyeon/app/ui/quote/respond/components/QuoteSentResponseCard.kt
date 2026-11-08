package com.muyeon.app.ui.quote.respond.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.muyeon.app.R
import com.muyeon.app.ui.components.LevelBadge
import com.muyeon.app.ui.format.KstDates
import com.muyeon.app.ui.quote.QuoteUi
import com.muyeon.app.ui.quote.respond.MyQuoteResponse
import com.muyeon.app.ui.quote.respond.MyResponseBadge
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.Dimensions
import java.util.Locale

/**
 * 내가 보낸 견적서 — 웹 .quote-sent-card(요약 카드 규격 + 상태 배지·보낸 시각 · 금액 18 bold · 예약금 · 메모 · 메시지).
 *  price 는 메모가 있으면 메모, 없으면 서버 금액 문자열이라 금액과 같으면 메모로 다시 쓰지 않는다(웹과 같다).
 */
@Composable
fun QuoteSentResponseCard(mine: MyQuoteResponse, modifier: Modifier = Modifier) {
    val priceText = QuoteUi.priceText(mine.priceAmount, mine.priceUnit, mine.price)
    val memo = mine.price?.takeIf { it.isNotBlank() && it != priceText }
    val badge = MyResponseBadge.of(mine.status)
    val shape = RoundedCornerShape(Dimensions.cardRadius)
    Column(
        modifier.fillMaxWidth().background(AppColors.Surface, shape).border(1.dp, AppColors.Divider, shape).padding(Dimensions.spacingL),
        verticalArrangement = Arrangement.spacedBy(CARD_GAP),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            LevelBadge(stringResource(badge.label), badge.level)
            Spacer(Modifier.weight(1f))
            Text(KstDates.dotTime(mine.createdAt), style = AppTypography.Caption12, color = AppColors.TextSub)
        }
        Text(priceText, style = AppTypography.Title18, color = AppColors.TextHead)
        if (mine.paymentMode == "DEPOSIT" && mine.depositAmount > 0) {
            Text(stringResource(R.string.quote_respond_deposit, String.format(Locale.KOREA, "%,d", mine.depositAmount)), style = AppTypography.Sub13, color = AppColors.TextSub)
        }
        if (memo != null) Text(memo, style = AppTypography.Sub13, color = AppColors.TextSub)
        val message = mine.message?.takeIf { it.isNotBlank() }
        if (message != null) {
            HorizontalDivider(Modifier.padding(top = Dimensions.spacingXS), color = AppColors.Divider)
            Text(message, style = AppTypography.Body14, color = AppColors.TextBody, modifier = Modifier.padding(top = Dimensions.spacingXS))
        }
    }
}

/** 웹 flex-col gap 0.6rem. */
private val CARD_GAP = 6.dp
