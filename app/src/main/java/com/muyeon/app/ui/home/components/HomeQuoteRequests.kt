package com.muyeon.app.ui.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.muyeon.app.R
import com.muyeon.app.ui.components.HeartButton
import com.muyeon.app.ui.format.timeAgo
import com.muyeon.app.ui.home.QuoteLabels
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.domain.model.home.HomeQuote

/**
 * 레슨을 찾는 회원 — 웹 components/muyeon/HomeQuoteRequests.js: 제목(+NEW) · 설명 · 더보기 · 견적 요청 카드 가로 넘김(카드 폭 min(78%, 300)).
 *   카드: "{카테고리}을 가르쳐주실 강사님을 찾고 있어요" · "{카테고리} 레슨" · 지역 · 희망 요일/시간/형태 · 태그 · [견적서 보내기].
 */
@Composable
fun HomeQuoteRequests(
    quotes: List<HomeQuote>,
    liked: Set<Long>,
    onOpen: (HomeQuote) -> Unit,
    onToggleLike: (Long) -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
    showTitleIcon: Boolean = true,
) {
    Column(modifier.fillMaxWidth().padding(top = 20.dp, bottom = 24.dp)) {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 12.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (showTitleIcon) Icon(painterResource(R.drawable.ic_tab_my), null, tint = AppColors.TextHead, modifier = Modifier.size(20.dp))
                    Text(stringResource(R.string.home_quote_title), style = AppTypography.Section16, color = AppColors.TextHead)
                    Text("NEW", style = AppTypography.Badge12, color = AppColors.Primary, modifier = Modifier.background(AppColors.PrimaryTint, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 3.dp))
                }
                Text(stringResource(R.string.home_quote_desc), style = AppTypography.Sub13, color = AppColors.TextHint, modifier = Modifier.padding(top = 4.dp))
            }
            HomeMoreButton(onMore, Modifier.padding(top = 2.dp))
        }
        if (quotes.isEmpty()) {
            Text(stringResource(R.string.home_quote_empty), style = AppTypography.Body14, color = AppColors.TextHint,
                modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth().border(1.dp, AppColors.Divider, RoundedCornerShape(16.dp)).padding(vertical = 24.dp, horizontal = 20.dp))
        } else {
            val w = slideWidth(0.78f, 300.dp)
            LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(quotes, key = { it.id }) { q -> QuoteCard(q, q.id in liked, { onOpen(q) }, { onToggleLike(q.id) }, Modifier.width(w)) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuoteCard(q: HomeQuote, liked: Boolean, onClick: () -> Unit, onToggleLike: () -> Unit, modifier: Modifier) {
    val context = LocalContext.current
    val str: (Int) -> String = context::getString
    val cat = stringResource(QuoteLabels.category(q.categoryId))
    val generic = QuoteLabels.isGeneric(q.categoryId)
    val sent = !q.isMine && !q.canRespond
    Box(modifier.border(1.dp, AppColors.Divider, RoundedCornerShape(16.dp)).background(AppColors.Surface, RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                if (generic) stringResource(R.string.home_quote_seeking_generic) else stringResource(R.string.home_quote_seeking, cat + QuoteLabels.objectParticle(cat)),
                style = AppTypography.Sub13SemiBold, color = AppColors.Primary, modifier = Modifier.padding(end = 32.dp),
            )
            Text(if (generic) stringResource(R.string.home_quote_generic_title) else stringResource(R.string.lesson_genre_lesson, cat), style = AppTypography.CardTitle16.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), color = AppColors.TextHead)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(painterResource(R.drawable.ic_web_location), null, tint = AppColors.TextHint, modifier = Modifier.size(16.dp))
                Text(q.region ?: stringResource(R.string.home_quote_region_any), style = AppTypography.Sub13, color = AppColors.ChipText)
            }
            HorizontalDivider(thickness = 1.dp, color = AppColors.Divider)
            listOf(
                Triple(R.drawable.ic_web_calendar, R.string.home_quote_days, QuoteLabels.answer(q, "days", str) ?: stringResource(R.string.home_quote_days_any)),
                Triple(R.drawable.ic_web_clock, R.string.home_quote_time, QuoteLabels.answer(q, "time", str) ?: stringResource(R.string.home_quote_time_any)),
                Triple(R.drawable.ic_web_community, R.string.home_quote_format, QuoteLabels.answer(q, "format", str) ?: stringResource(R.string.home_quote_format_any)),
            ).forEach { (icon, label, value) ->
                Row(verticalAlignment = Alignment.Top) {
                    Icon(painterResource(icon), null, tint = AppColors.TextHint, modifier = Modifier.size(14.dp).padding(top = 1.dp))
                    Text(stringResource(label), style = AppTypography.Caption12.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Normal), color = AppColors.TextHint, modifier = Modifier.padding(start = 4.dp).weight(1f))
                    Text(value, style = AppTypography.Caption12, color = AppColors.ChipText)
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("#$cat", style = AppTypography.Pill11.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium), color = AppColors.Primary,
                    modifier = Modifier.background(AppColors.PrimaryTint, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 5.dp))
                if (q.createdAt != null) Text(stringResource(R.string.home_quote_registered, timeAgo(q.createdAt)), style = AppTypography.Pill11.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium), color = AppColors.TextHint,
                    modifier = Modifier.background(AppColors.SurfaceMuted, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 5.dp))
            }
            Box(
                Modifier.fillMaxWidth().heightIn(min = 40.dp).border(BorderStroke(1.dp, if (sent) AppColors.Divider else AppColors.Primary), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(when { q.isMine -> R.string.home_quote_mine; sent -> R.string.home_quote_sent; else -> R.string.home_quote_send }) + "  ›",
                    style = AppTypography.Sub13SemiBold, color = if (sent) AppColors.TextHint else AppColors.Primary,
                )
            }
        }
        HeartButton(liked, onToggleLike, size = 20.dp, modifier = Modifier.align(Alignment.TopEnd).padding(0.dp))
    }
}
