package com.muyeon.app.ui.quote

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 견적 모아보기(강사) — iOS `QuoteBrowseView.swift` 1:1.
 *  일반 회원들이 올린 견적요청을 그리드로 탐색하고, 탭하면 덱 스와이프 카드로 넘겨보며 바로 견적을 보낸다.
 *   - 목록: GET /quotes/available (서버 카테고리 필터+페이징, 이미 응답한 요청 제외)
 *   - 필터: 장르(서버) + 지역 시도(클라이언트, 로딩된 목록 기준)
 *   - 카드: 문진 Q/A 데이터 중심 레이아웃(흰 카드 + 카테고리 헤더 + 답변 리스트 + 첨부 썸네일)
 */

/** 견적 모아보기 상태 — iOS QuoteBrowseVM 1:1. */
class QuoteBrowseState(private val api: QuoteApi) {
    var items by mutableStateOf<List<QuoteFull>>(emptyList())
    var isLoading by mutableStateOf(true)
    var hasMore by mutableStateOf(false)
    var categoryId by mutableStateOf("")     // "" = 전체(서버 필터)
    var regionSido by mutableStateOf("")     // "" = 전체(클라 필터)
    var needsGenre by mutableStateOf(false)
    var myCats by mutableStateOf<List<String>>(emptyList())
    var toast by mutableStateOf<String?>(null)
    var newIds by mutableStateOf(setOf<Int>())   // 서버 판정 신규(N) — 계정 기준(기기 무관)
    // 견적 수신 조건(레슨 설정) — 기본 적용, 배너에서 임시 해제 가능.
    var applyPrefs by mutableStateOf(true)
    var prefsApplied by mutableStateOf(false)
    var prefsSummary by mutableStateOf<List<String>>(emptyList())
    var prefsFilteredOut by mutableStateOf(0)
    var hasPrefs by mutableStateOf(false)
    private var page = 0
    private var loadingMore = false

    fun isNew(quote: QuoteFull) = newIds.contains(quote.id)

    /** 지역 시도 필터 적용 목록(그리드·덱 공용). */
    val filtered: List<QuoteFull>
        get() = if (regionSido.isEmpty()) items else items.filter { (it.region ?: "").contains(regionSido) }

    /** 로딩된 목록에서 시도 칩 후보 추출(등장 순서 유지). */
    val sidoOptions: List<String>
        get() {
            val seen = mutableListOf<String>()
            items.forEach { q ->
                (q.region ?: "").split(",").forEach { part ->
                    val sido = part.trim().split(" ").firstOrNull().orEmpty()
                    if (sido.isNotEmpty() && !seen.contains(sido)) seen.add(sido)
                }
            }
            return seen
        }

    suspend fun load(reset: Boolean = true) {
        if (reset) { isLoading = true; page = 0 }
        api.getAvailableQuotes(categoryId.ifEmpty { null }, page, applyPrefs).onSuccess { res ->
            items = if (page == 0) res.items else items + res.items
            hasMore = res.hasMore ?: false
            needsGenre = res.needsGenre ?: false
            res.myCategoryIds?.let { myCats = it }
            prefsApplied = res.prefsApplied ?: false
            prefsSummary = res.prefsSummary ?: emptyList()
            prefsFilteredOut = res.prefsFilteredOut ?: 0
            hasPrefs = res.hasPrefs ?: false
            val pageNew = (res.newIds ?: emptyList()).toSet()
            newIds = if (page == 0) pageNew else newIds + pageNew
        }
        isLoading = false
    }

    suspend fun loadMore() {
        if (!hasMore || loadingMore) return
        loadingMore = true
        page += 1
        load(reset = false)
        loadingMore = false
    }

    /** 카드 열람 → N 즉시 제거(낙관) + 서버 기록(fire-and-forget, 계정 영속). */
    suspend fun markSeen(id: Int) {
        if (!newIds.contains(id)) return
        newIds = newIds - id
        api.markQuoteBrowseSeen(id)
    }

    suspend fun endVisit() { api.endQuoteBrowseVisit() }

    /** 견적 발송 — 성공 시 목록에서 제거(available 은 응답한 요청을 제외하므로 정합). */
    suspend fun respond(quoteId: Int, priceAmount: Int?, message: String): Boolean {
        var ok = false
        api.sendQuoteResponse(quoteId, priceAmount, message)
            .onSuccess {
                items = items.filterNot { it.id == quoteId }
                toast = "견적을 보냈어요. 고객이 확인하면 알림으로 알려드릴게요."
                ok = true
            }
            .onFailure { toast = it.message?.ifEmpty { null } ?: "견적을 보내지 못했어요. 잠시 후 다시 시도해 주세요." }
        return ok
    }
}

/** 견적 카테고리 칩(웹 QUOTE_CATEGORIES 7종 — 취미무용은 위저드에서 제거됨). */
private val BROWSE_CATEGORIES: List<Pair<String, String>> = listOf(
    "" to "전체", "ballet" to "발레", "barre" to "바레", "korean" to "한국무용",
    "modern" to "현대무용", "practical" to "실용무용", "balletfit" to "발레핏", "musical" to "뮤지컬",
)

@Composable
fun QuoteBrowseScreen(
    api: QuoteApi,
    onClose: () -> Unit,
    onGoGenreSettings: (() -> Unit)? = null,
    onGoLessonSettings: (() -> Unit)? = null,
) {
    val state = remember { QuoteBrowseState(api) }
    var deckIndex by remember { mutableStateOf<Int?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { state.load() }

    // 토스트 1.8초 자동 소멸(iOS showToast)
    LaunchedEffect(state.toast) { if (state.toast != null) { delay(1800); state.toast = null } }

    val deck = deckIndex
    if (deck != null) {
        QuoteDeckScreen(
            state = state,
            initialIndex = deck,
            onClose = { deckIndex = null },
        )
        return
    }

    Box(Modifier.fillMaxSize().background(MuyeonColors.groupedBg)) {
        Column(Modifier.fillMaxSize()) {
            // 뒤로 = 화면 이탈 → 서버에 방문 종료 기록(다음 방문의 N 기준 시각). iOS onDisappear 대응.
            QuoteNavBar(title = "견적 모아보기", onBack = { scope.launch { state.endVisit() }; onClose() })
            PrefsBanner(state, onGoLessonSettings) { scope.launch { state.load() } }
            FilterChips(state) { scope.launch { state.load() } }

            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = MuyeonColors.primary)
                }
                state.needsGenre -> NeedsGenre(onGoGenreSettings)
                state.filtered.isEmpty() -> QuoteEmptyState(
                    Icons.Outlined.Inbox, "지금 응답할 수 있는 요청이 없어요",
                    "새 견적요청이 오면 알림으로 알려드릴게요.",
                    Modifier.fillMaxSize().wrapContentHeight(Alignment.CenterVertically),
                )
                else -> LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(state.filtered, key = { it.id }) { quote ->
                        GridCell(
                            quote = quote,
                            isNew = state.isNew(quote),
                            onClick = {
                                scope.launch { state.markSeen(quote.id) }
                                deckIndex = state.filtered.indexOfFirst { it.id == quote.id }.coerceAtLeast(0)
                            },
                        )
                        // 마지막 항목 노출 시 다음 페이지
                        if (quote.id == state.filtered.lastOrNull()?.id) {
                            LaunchedEffect(quote.id) { state.loadMore() }
                        }
                    }
                }
            }
        }
        state.toast?.let { BrowseToast(it, Modifier.align(Alignment.BottomCenter)) }
    }
}

/** 견적 수신 조건 배너 — 적용중/해제 상태 + [해제]·[내 조건 적용] 토글 + [조건 수정]. */
@Composable
private fun PrefsBanner(state: QuoteBrowseState, onGoLessonSettings: (() -> Unit)?, onReload: () -> Unit) {
    if (!(state.hasPrefs || state.prefsApplied || !state.applyPrefs)) return

    val detail = if (state.prefsApplied) {
        val cond = if (state.prefsSummary.isEmpty()) "설정한 조건" else state.prefsSummary.joinToString(" · ")
        if (state.prefsFilteredOut > 0) "$cond · 이번 목록에서 ${state.prefsFilteredOut}건 숨김" else cond
    } else "레슨 설정의 수신 조건을 잠시 끄고 모든 견적을 보고 있어요"

    Row(
        Modifier.fillMaxWidth().background(MuyeonColors.surface).padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.FilterList, null,
            tint = if (state.prefsApplied) MuyeonColors.primary else MuyeonColors.secondary,
            modifier = Modifier.size(15.dp),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                if (state.prefsApplied) "내 수신 조건 적용중" else "전체 견적 보는 중",
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                lineHeight = 16.sp, color = MuyeonColors.textHead,
            )
            Text(
                detail,
                fontFamily = customFontFamily, fontWeight = FontWeight.Normal, fontSize = 11.sp,
                lineHeight = 13.sp, color = MuyeonColors.textSub,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        if (onGoLessonSettings != null) {
            Text(
                "조건 수정",
                fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 12.sp,
                lineHeight = 14.sp, color = MuyeonColors.secondary,
                modifier = Modifier.clickable { onGoLessonSettings() },
            )
        }
        Text(
            if (state.prefsApplied) "해제" else "내 조건 적용",
            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp,
            lineHeight = 14.sp, color = Color.White,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(MuyeonColors.primary)
                .clickable { state.applyPrefs = !state.applyPrefs; onReload() }
                .padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}

/** 필터 — 1줄: 장르(서버) / 2줄: 지역 시도(클라, 목록에서 추출). */
@Composable
private fun FilterChips(state: QuoteBrowseState, onReload: () -> Unit) {
    // 칩 정렬: 전체 → 내 전공(서버 myCats) → 나머지(카탈로그 순 유지).
    val ordered = remember(state.myCats) {
        if (state.myCats.isEmpty()) BROWSE_CATEGORIES
        else {
            val mine = BROWSE_CATEGORIES.filter { it.first.isNotEmpty() && state.myCats.contains(it.first) }
            val rest = BROWSE_CATEGORIES.filter { it.first.isEmpty() || !state.myCats.contains(it.first) }
            val all = rest.firstOrNull()
            if (all != null && all.first.isEmpty()) listOf(all) + mine + rest.drop(1) else mine + rest
        }
    }

    Column(
        Modifier.fillMaxWidth().background(MuyeonColors.surface).padding(vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            ordered.forEach { (id, label) ->
                BrowseChip(label, state.categoryId == id) { state.categoryId = id; onReload() }
            }
        }
        if (state.sidoOptions.size > 1) {
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                BrowseChip("지역 전체", state.regionSido.isEmpty()) { state.regionSido = "" }
                state.sidoOptions.forEach { sido ->
                    BrowseChip(sido, state.regionSido == sido) { state.regionSido = sido }
                }
            }
        }
    }
}

@Composable
private fun BrowseChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text,
        fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp,
        lineHeight = 16.sp, color = if (selected) Color.White else MuyeonColors.textSub,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) MuyeonColors.primary else Color(0xFFF2F2F7))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
    )
}

@Composable
private fun NeedsGenre(onGo: (() -> Unit)?) {
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        QuoteEmptyState(
            Icons.Outlined.Badge, "전공을 먼저 등록해 주세요",
            "레슨 전공 설정에서 장르를 등록하면 맞는 요청이 보여요.",
        )
        if (onGo != null) {
            Text(
                "전공 등록하러 가기",
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                lineHeight = 18.sp, color = Color.White,
                modifier = Modifier
                    .padding(top = 14.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MuyeonColors.primary)
                    .clickable(onClick = onGo)
                    .padding(horizontal = 24.dp, vertical = 12.dp),
            )
        }
    }
}

/** 그리드 셀 — 카테고리·시간 / 문진 첫 답 요약 / 지역 (+첨부 썸네일, N 배지). */
@Composable
private fun GridCell(quote: QuoteFull, isNew: Boolean, onClick: () -> Unit) {
    val details = remember(quote) { QuoteQuestions.describe(quote.categoryId, quote.answers ?: emptyList()) }
    val photo = (quote.answers ?: emptyList()).flatMap { it.images ?: emptyList() }.firstOrNull()

    Box {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MuyeonColors.surface)
                .clickable(onClick = onClick)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (photo != null) {
                AsyncImage(
                    QuoteUi.imageUrl(photo), null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(84.dp).clip(RoundedCornerShape(10.dp)),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                QuotePill(QuoteUi.categoryLabel(quote.categoryId), MuyeonColors.primary, filled = true)
                Spacer(Modifier.weight(1f))
                Text(
                    QuoteUi.relativeTime(quote.createdAt),
                    fontFamily = customFontFamily, fontWeight = FontWeight.Normal, fontSize = 11.sp,
                    lineHeight = 13.sp, color = MuyeonColors.secondary,
                )
            }
            Text(
                details.firstOrNull()?.second ?: "레슨 견적요청",
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                lineHeight = 17.sp, color = MuyeonColors.textHead,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            Text(
                quote.region ?: "지역 미지정",
                fontFamily = customFontFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp,
                lineHeight = 14.sp, color = MuyeonColors.textSub,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        // 신규(N) 배지 — 직전 방문 이후 올라온 미열람 견적.
        if (isNew) {
            Box(
                Modifier.align(Alignment.TopEnd).padding(6.dp).size(18.dp)
                    .clip(RoundedCornerShape(50)).background(Color.Red),
                contentAlignment = Alignment.Center,
            ) {
                Text("N", fontWeight = FontWeight.ExtraBold, fontSize = 10.sp, lineHeight = 12.sp, color = Color.White)
            }
        }
    }
}

@Composable
private fun BrowseToast(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp,
        lineHeight = 17.sp, color = Color.White, textAlign = TextAlign.Center,
        modifier = modifier
            .padding(bottom = 40.dp, start = 24.dp, end = 24.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.85f))
            .padding(horizontal = 20.dp, vertical = 13.dp),
    )
}

// MARK: - 덱 화면 (견적 카드 스와이프 + 견적 보내기)

@Composable
private fun QuoteDeckScreen(state: QuoteBrowseState, initialIndex: Int, onClose: () -> Unit) {
    val list = state.filtered
    if (list.isEmpty()) { onClose(); return }

    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(0, list.lastIndex),
        pageCount = { list.size },
    )
    var respondFor by remember { mutableStateOf<QuoteFull?>(null) }
    var confirmOutside by remember { mutableStateOf<QuoteFull?>(null) }
    val scope = rememberCoroutineScope()
    val current = list.getOrNull(pagerState.currentPage)

    LaunchedEffect(pagerState.currentPage) { current?.let { state.markSeen(it.id) } }
    LaunchedEffect(state.toast) { if (state.toast != null) { delay(1800); state.toast = null } }

    Box(Modifier.fillMaxSize().background(MuyeonColors.groupedBg)) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxWidth().height(44.dp), contentAlignment = Alignment.Center) {
                Text(
                    "${minOf(pagerState.currentPage + 1, list.size)} / ${list.size}",
                    fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp,
                    lineHeight = 17.sp, color = MuyeonColors.textSub,
                )
                Box(
                    Modifier.align(Alignment.CenterStart).padding(start = 4.dp).size(44.dp).clickable(onClick = onClose),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로", tint = MuyeonColors.textHead, modifier = Modifier.size(18.dp))
                }
            }

            DeckPager(items = list, state = pagerState, modifier = Modifier.weight(1f).padding(top = 12.dp)) { quote, _ ->
                QuoteRequestCard(quote)
            }

            Text(
                "견적 보내기",
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                lineHeight = 19.sp, color = Color.White, textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(MuyeonColors.primary)
                    .clickable(enabled = current != null) {
                        val q = current ?: return@clickable
                        // 전공 외 카테고리 요청이면 한 번 더 확인(전공/카테고리 없으면 판별 불가 → 바로 진행)
                        val cat = q.categoryId ?: ""
                        if (state.myCats.isNotEmpty() && cat.isNotEmpty() && !state.myCats.contains(cat)) {
                            confirmOutside = q
                        } else {
                            respondFor = q
                        }
                    }
                    .padding(vertical = 15.dp),
            )
        }
        state.toast?.let { BrowseToast(it, Modifier.align(Alignment.BottomCenter)) }
    }

    confirmOutside?.let { quote ->
        QuoteDialog(
            title = "전공 외 요청이에요",
            message = "${QuoteUi.categoryLabel(quote.categoryId)} 요청이에요. 등록된 전공과 달라요.\n그래도 견적을 보낼까요?",
            confirmText = "견적 보내기",
            onConfirm = { confirmOutside = null; respondFor = quote },
            onDismiss = { confirmOutside = null },
        )
    }

    respondFor?.let { quote ->
        QuoteRespondSheet(
            quote = quote,
            onDismiss = { respondFor = null },
            onSend = { amount, message ->
                scope.launch {
                    if (state.respond(quote.id, amount, message)) {
                        respondFor = null
                        if (state.filtered.isEmpty()) onClose()
                    }
                }
            },
        )
    }
}

/** 견적요청 카드(데이터 중심) — iOS QuoteRequestCard 1:1. 항목 >6 이면 밀도 압축(compact). */
@Composable
fun QuoteRequestCard(quote: QuoteFull) {
    val details = remember(quote) { QuoteQuestions.describe(quote.categoryId, quote.answers ?: emptyList()) }
    val photos = (quote.answers ?: emptyList()).flatMap { it.images ?: emptyList() }
    val compact = details.size > 6

    Column(
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(20.dp))
            .background(MuyeonColors.surface),
    ) {
        // 헤더 — 카테고리 pill + 등록시간 / 큰 제목 / 지역
        Column(
            Modifier.padding(horizontal = 20.dp).padding(top = 20.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                QuotePill(QuoteUi.categoryLabel(quote.categoryId), MuyeonColors.primary, filled = true)
                Spacer(Modifier.weight(1f))
                Text(
                    QuoteUi.relativeTime(quote.createdAt),
                    fontFamily = customFontFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp,
                    lineHeight = 14.sp, color = MuyeonColors.textSub,
                )
            }
            Text(
                QuoteUi.categoryTitle(quote.categoryId),
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 21.sp,
                lineHeight = 25.sp, color = MuyeonColors.textHead,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.LocationOn, null, tint = MuyeonColors.textSub, modifier = Modifier.size(11.dp))
                Text(
                    quote.region ?: "지역 미지정",
                    fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp,
                    lineHeight = 16.sp, color = MuyeonColors.textSub,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }

        HorizontalDivider(Modifier.padding(horizontal = 20.dp), thickness = 0.5.dp, color = Color(0xFFE5E5EA))

        // 문진 Q/A — 스크롤 없이 전부 노출(밀도 적응). 예산 답변은 브랜드색 강조.
        Column(
            Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 9.dp else 13.dp),
        ) {
            details.forEach { (label, value) ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        label,
                        fontFamily = customFontFamily, fontWeight = FontWeight.Normal, fontSize = 11.sp,
                        lineHeight = 13.sp, color = MuyeonColors.textSub,
                    )
                    Text(
                        value,
                        fontFamily = customFontFamily, fontWeight = FontWeight.Bold,
                        fontSize = if (compact) 14.sp else 15.sp,
                        lineHeight = if (compact) 17.sp else 18.sp,
                        color = if (label.contains("예산")) MuyeonColors.primary else MuyeonColors.textHead,
                        maxLines = if (compact) 1 else 2, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            // 첨부 사진 — 정적 3장 + "+N"(가로 스크롤 없음: 덱 스와이프와 충돌 방지)
            if (photos.isNotEmpty()) {
                Row(Modifier.padding(top = 2.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    photos.take(3).forEachIndexed { i, url ->
                        Box(Modifier.size(64.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFFF2F2F7))) {
                            AsyncImage(
                                QuoteUi.imageUrl(url), null,
                                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(),
                            )
                            if (i == 2 && photos.size > 3) {
                                Box(
                                    Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        "+${photos.size - 3}",
                                        fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                                        lineHeight = 17.sp, color = Color.White,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** 견적 보내기 시트 — iOS QuoteRespondSheet(금액 + 메시지). */
@Composable
private fun QuoteRespondSheet(quote: QuoteFull, onDismiss: () -> Unit, onSend: (Int?, String) -> Unit) {
    var amountText by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    val canSend = message.trim().isNotEmpty() && !sending

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(MuyeonColors.surface)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "${QuoteUi.categoryLabel(quote.categoryId)} 견적 보내기",
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 17.sp,
                lineHeight = 20.sp, color = MuyeonColors.textHead,
            )
            Text(
                "회당 금액 (원)",
                fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp,
                lineHeight = 16.sp, color = MuyeonColors.textSub,
            )
            OutlinedTextField(
                value = amountText,
                onValueChange = { v -> amountText = v.filter { it.isDigit() } },
                placeholder = { Text("예: 60000 (비우면 협의)", fontFamily = customFontFamily, fontSize = 14.sp) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "견적 메시지",
                fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp,
                lineHeight = 16.sp, color = MuyeonColors.textSub,
            )
            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
            )
            Text(
                if (sending) "보내는 중…" else "견적 보내기",
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                lineHeight = 19.sp, color = Color.White, textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(if (canSend) MuyeonColors.primary else Color.Gray.copy(alpha = 0.4f))
                    .clickable(enabled = canSend) {
                        sending = true
                        onSend(amountText.toIntOrNull(), message.trim())
                    }
                    .padding(vertical = 15.dp),
            )
        }
    }
}
