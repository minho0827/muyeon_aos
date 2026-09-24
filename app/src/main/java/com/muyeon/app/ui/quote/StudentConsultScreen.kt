package com.muyeon.app.ui.quote

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors
import kotlinx.coroutines.launch

/**
 * 「개인레슨 관리 > 수강생 상담」 — iOS `StudentConsultView.swift` 이식.
 *
 * 수강생 문의와 내가 보낸 제안을 **한 목록**에서 본다.
 *  전에는 '받은 제안'과 '보낸 제안'이 서로 다른 화면이었는데, 강사에게 둘은 같은 하나의
 *  대화다 — 물어봤고(문의), 조건을 보냈고(제안), 지금 일정을 맞추는 중(상담).
 *  나눠 두면 "답을 했던가?" 를 두 화면 오가며 확인해야 했다.
 *
 * 어디서 오는가 (새 API 없음)
 *  - 수강생 문의 : GET /quotes/available 중 지정(1:1) 요청.
 *    서버가 `targetTeacherId = 나 OR 브로드캐스트` 로 걸러 주고 내가 응답한 건 빼 준다.
 *  - 제안 보냄/상담 중 : GET /quotes/sent. 채팅방이 열렸으면 상담 중으로 본다.
 */
enum class ConsultKind(val chipTitle: String) {
    ALL("전체"), INQUIRY("수강생 문의"), PROPOSAL("제안 보냄"), CHATTING("상담 중")
}

/** 통합 목록 1줄. 원본을 들고 있다가 탭했을 때 어디로 보낼지 정한다. */
data class ConsultItem(
    val key: String,
    val kind: ConsultKind,
    val badgeText: String,
    val badgeColor: Color,
    val title: String,
    val subtitle: String,
    val timeText: String,
    val sortAt: String?,
    // 채택됨(0) → 진행 항목(1) → 마감(2). 같은 우선순위 안에서는 최신순.
    val sortPriority: Int,
    val quote: QuoteFull? = null,
    val sent: SentQuoteItem? = null,
)

private fun inquiryItem(q: QuoteFull) = ConsultItem(
    key = "Q${q.id}",
    kind = ConsultKind.INQUIRY,
    badgeText = "새 문의",
    badgeColor = MuyeonColors.primary,
    title = QuoteUi.categoryTitle(q.categoryId) + " 문의",
    subtitle = "수강생이 레슨을 요청했어요.",
    timeText = QuoteUi.relativeTime(q.createdAt),
    sortAt = q.createdAt,
    sortPriority = 1,
    quote = q,
)

private fun proposalItem(item: SentQuoteItem): ConsultItem {
    val closed = item.quoteStatus == "EXPIRED" || item.quoteStatus == "CANCELED"
    val talking = item.chatRoomId != null || item.status == "ACCEPTED"
    val badge = when {
        item.status == "ACCEPTED" -> "채택됨" to MuyeonColors.green
        closed -> "마감" to MuyeonColors.textSub
        item.chatRoomId != null -> "상담 중" to MuyeonColors.green
        else -> "제안 보냄" to MuyeonColors.primary
    }
    return ConsultItem(
        key = "S${item.id}",
        kind = if (talking && !closed) ConsultKind.CHATTING else ConsultKind.PROPOSAL,
        badgeText = badge.first,
        badgeColor = badge.second,
        title = QuoteUi.categoryTitle(item.categoryId) + " 제안",
        subtitle = if (item.chatRoomId != null) "일정 조율 중이에요." else "내가 조건을 제안했어요.",
        timeText = QuoteUi.relativeTime(item.createdAt),
        sortAt = item.createdAt,
        sortPriority = if (item.status == "ACCEPTED") 0 else if (closed) 2 else 1,
        sent = item,
    )
}

@Composable
fun StudentConsultScreen(
    api: QuoteApi,
    onOpenSent: (SentQuoteItem) -> Unit,
) {
    var items by remember { mutableStateOf<List<ConsultItem>>(emptyList()) }
    var filter by remember { mutableStateOf(ConsultKind.ALL) }
    var loading by remember { mutableStateOf(true) }
    var respondFor by remember { mutableStateOf<QuoteFull?>(null) }
    var attachmentRole by remember { mutableStateOf("TEACHER") }
    var toast by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun load() {
        // 수신 조건(applyPrefs)은 끈다 — 나를 지정한 문의는 조건과 무관하게 다 보여야 한다.
        // ★ 실패한 쪽은 '빈 목록'이 아니라 '모르는 상태'다. 실패를 빈 배열로 덮어쓰면
        //   새로고침 한 번 실패에 보고 있던 상담이 통째로 사라진다(iOS 에서 실제로 났던 문제).
        val available = api.getAvailableQuotes(page = 0, applyPrefs = false).getOrNull()
        val sent = api.getSentQuotes().getOrNull()
        if (available == null && sent == null) {
            if (items.isNotEmpty()) toast = "목록을 새로 불러오지 못했어요."
            loading = false
            return
        }
        val inquiries = available?.items?.filter { it.isDirect }?.map(::inquiryItem)
            ?: items.filter { it.kind == ConsultKind.INQUIRY }
        val proposals = sent?.map(::proposalItem)
            ?: items.filter { it.kind != ConsultKind.INQUIRY }
        items = (inquiries + proposals).sortedWith(
            compareBy<ConsultItem> { it.sortPriority }
                .thenByDescending { it.sortAt.orEmpty() }
        )
        loading = false
    }

    LaunchedEffect(Unit) {
        attachmentRole = api.myAttachmentType()
        load()
    }
    LaunchedEffect(toast) { if (toast != null) { kotlinx.coroutines.delay(1800); toast = null } }

    val shown = if (filter == ConsultKind.ALL) items else items.filter { it.kind == filter }

    Box(Modifier.fillMaxSize().background(MuyeonColors.groupedBg)) {
        Column(Modifier.fillMaxSize()) {
            // ⚠️ 가로 스크롤을 쓰지 않는다 — 칩은 넷뿐이라 한 줄에 들어간다.
            Row(
                Modifier.fillMaxWidth().background(MuyeonColors.surface)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                ConsultKind.entries.forEach { kind ->
                    val count = if (kind == ConsultKind.ALL) items.size else items.count { it.kind == kind }
                    ConsultChip(
                        label = if (count == 0) kind.chipTitle else "${kind.chipTitle} $count",
                        selected = filter == kind,
                        modifier = Modifier.weight(1f),
                    ) { filter = kind }
                }
            }

            when {
                loading -> Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                    CircularProgressIndicator(color = MuyeonColors.primary)
                }
                shown.isEmpty() -> Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                    QuoteEmptyState(
                        Icons.Outlined.ChatBubbleOutline,
                        if (items.isEmpty()) "아직 상담이 없어요" else "이 상태의 상담이 없어요",
                        if (items.isEmpty()) "수강생 찾기에서 조건이 맞는 요청에 제안을 보내면 여기에 쌓여요."
                        else "다른 칩을 눌러 확인해 보세요.",
                    )
                }
                else -> LazyColumn(
                    Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(shown, key = { it.key }) { item ->
                        ConsultRow(item) {
                            if (item.sent != null) onOpenSent(item.sent) else respondFor = item.quote
                        }
                    }
                }
            }
        }

        toast?.let {
            Text(
                it,
                fontFamily = customFontFamily, fontSize = 14.sp, color = Color.White,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 40.dp)
                    .clip(RoundedCornerShape(12.dp)).background(Color(0xD9000000))
                    .padding(horizontal = 20.dp, vertical = 13.dp),
            )
        }
    }

    respondFor?.let { quote ->
        QuoteRespondSheet(
            quote = quote,
            attachmentRole = attachmentRole,
            onDismiss = { respondFor = null },
        ) { amount, deposit, message, attachmentType ->
            val ok = api.sendQuoteResponse(quote.id, amount, deposit, message, attachmentType).isSuccess
            if (ok) {
                respondFor = null
                toast = "제안을 보냈어요. 수강생이 확인하면 알림으로 알려드릴게요."
                scope.launch { load() }
            }
            ok
        }
    }
}

@Composable
private fun ConsultChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Text(
        label,
        fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp, lineHeight = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
        color = if (selected) Color.White else MuyeonColors.textSub,
        modifier = modifier.clip(RoundedCornerShape(50))
            .background(if (selected) MuyeonColors.primary else Color(0xFFF2F2F7))
            .clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 7.dp),
    )
}

@Composable
private fun ConsultRow(item: ConsultItem, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(MuyeonColors.surface).clickable(onClick = onClick).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                item.badgeText,
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold,
                fontSize = 11.sp, lineHeight = 14.sp, color = item.badgeColor,
                modifier = Modifier.clip(RoundedCornerShape(50))
                    .background(item.badgeColor.copy(alpha = 0.12f))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            )
            Spacer(Modifier.weight(1f))
            Text(
                item.timeText,
                fontFamily = customFontFamily, fontSize = 12.sp, lineHeight = 16.sp,
                color = MuyeonColors.textSub,
            )
        }
        Text(
            item.title,
            fontFamily = customFontFamily, fontWeight = FontWeight.Bold,
            fontSize = 15.sp, lineHeight = 20.sp, color = MuyeonColors.textHead,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        Text(
            item.subtitle,
            fontFamily = customFontFamily, fontSize = 13.sp, lineHeight = 17.sp,
            color = MuyeonColors.textSub, maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}
