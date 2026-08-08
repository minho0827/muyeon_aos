package com.muyeon.app.ui.quote

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors

/**
 * '보낸 견적' 상세(강사·원장 관점) — iOS `SentQuoteDetailView.swift` 1:1.
 *  고객 요청 요약 + 내가 보낸 견적 + 채팅.
 *   - 요청 컨텍스트: GET /quotes/:id (소유자 게이팅 없음 → 응답한 강사도 조회 가능)
 *   - 채팅 시작은 고객만 가능 → chatRoomId 가 이미 있을 때만 '채팅 이어가기' 노출.
 */
@Composable
fun SentQuoteDetailScreen(
    api: QuoteApi,
    item: SentQuoteItem,
    onBack: () -> Unit,
    onOpenChat: (Int) -> Unit,
) {
    var request by remember { mutableStateOf<QuoteFull?>(null) }

    LaunchedEffect(item.quoteId) {
        api.getQuote(item.quoteId).onSuccess { request = it.quote }
    }

    val (badgeText, badgeColor) = sentQuoteBadge(item)

    Column(Modifier.fillMaxSize().background(MuyeonColors.groupedBg)) {
        QuoteNavBar(title = "보낸 견적", onBack = onBack)
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            RequestSummaryCard(item, request)
            MyQuoteCard(item, badgeText, badgeColor)
            ChatSection(item, onOpenChat)
        }
    }
}

/** 고객 요청 요약 — 고객 + 카테고리·지역·요청상태 + 문진 자유텍스트/사진. */
@Composable
private fun RequestSummaryCard(item: SentQuoteItem, request: QuoteFull?) {
    // 문진 자유텍스트(요청 컨텍스트)
    val texts = (request?.answers ?: emptyList()).mapNotNull { it.text?.ifEmpty { null } }
    val photos = (request?.answers ?: emptyList()).flatMap { it.images ?: emptyList() }

    SentCard {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            QuoteAvatar(item.customer?.image, item.customer?.displayName ?: "회원", 44.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    item.customer?.displayName ?: "회원",
                    fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                    lineHeight = 18.sp, color = MuyeonColors.textHead,
                )
                Text(
                    listOfNotNull(QuoteUi.categoryLabel(item.categoryId) + " 레슨", item.region).joinToString(" · "),
                    fontFamily = customFontFamily, fontWeight = FontWeight.Normal, fontSize = 13.sp,
                    lineHeight = 16.sp, color = MuyeonColors.textSub,
                )
            }
            QuotePill(QuoteUi.statusLabel(item.quoteStatus), QuoteUi.statusColor(item.quoteStatus), filled = true)
        }
        if (texts.isNotEmpty()) {
            HorizontalDivider(Modifier.padding(vertical = 10.dp), color = MuyeonColors.border)
            Text(
                texts.joinToString("\n"),
                fontFamily = customFontFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp,
                lineHeight = 17.sp, color = MuyeonColors.textSub,
            )
        }
        if (photos.isNotEmpty()) {
            Row(
                Modifier.padding(top = 10.dp).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                photos.forEach { src -> QuoteThumb(QuoteUi.imageUrl(src)) }
            }
        }
    }
}

/** 내가 보낸 견적 — 금액 + 메시지 + 내 상태. */
@Composable
private fun MyQuoteCard(item: SentQuoteItem, badgeText: String, badgeColor: Color) {
    val msg = (item.message ?: "").trim()
    SentCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "내가 보낸 견적",
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                lineHeight = 19.sp, color = MuyeonColors.textHead,
            )
            QuotePill(badgeText, badgeColor, filled = true)
            Spacer(Modifier.weight(1f))
            Text(
                QuoteUi.relativeTime(item.createdAt),
                fontFamily = customFontFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp,
                lineHeight = 14.sp, color = MuyeonColors.secondary,
            )
        }
        Text(
            QuoteUi.priceText(item.priceAmount, item.priceUnit, item.price),
            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp,
            lineHeight = 24.sp, color = MuyeonColors.textHead,
            modifier = Modifier.padding(top = 12.dp),
        )
        if (msg.isNotEmpty()) {
            Text(
                msg,
                fontFamily = customFontFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp,
                lineHeight = 17.sp, color = MuyeonColors.body,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

/** 채팅 — 방이 이미 있으면 이어가기, 없으면 안내(채팅 시작은 고객 권한). */
@Composable
private fun ChatSection(item: SentQuoteItem, onOpenChat: (Int) -> Unit) {
    val rid = item.chatRoomId ?: 0
    if (rid > 0) {
        Box(
            Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MuyeonColors.primary)
                .clickable { onOpenChat(rid) }
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "채팅 이어가기",
                fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                lineHeight = 18.sp, color = Color.White,
            )
        }
    } else if (item.status != "REJECTED") {
        Text(
            "고객이 채팅을 시작하면 알림으로 알려드려요.",
            fontFamily = customFontFamily, fontWeight = FontWeight.Normal, fontSize = 13.sp,
            lineHeight = 16.sp, color = MuyeonColors.textSub, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        )
    }
}

/** 카드 컨테이너 — iOS: r14 + systemBackground + 좌우 16 + 패딩 16. */
@Composable
private fun SentCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MuyeonColors.surface)
            .padding(16.dp),
        content = content,
    )
}
