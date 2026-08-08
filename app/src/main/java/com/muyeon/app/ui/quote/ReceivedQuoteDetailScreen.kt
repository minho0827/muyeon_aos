package com.muyeon.app.ui.quote

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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
import java.util.Locale

/**
 * 받은견적 상세 — iOS `ReceivedQuotesDetailView.swift` + `QuoteRecommendationSection.swift` 1:1.
 *  요청 요약 + 받은 견적 카드(강사 프로필·평점·메시지) + 액션(프로필/채팅/채택) + 철회.
 *  응답 0건이면 "이런 강사는 어때요?" 추천 섹션(원터치 지정 재요청).
 *
 * ⚠️ 카드 수치는 iOS 와 동일: 카드 라운드 14 / 패딩 16 / 좌우 16 / 섹션 간격 16 / 아바타 52.
 * ⚠️ 채팅·공개프로필은 네이티브 미이식 — 호출부(QuoteHubActivity)가 폴백 처리.
 */

/** 받은 견적 정렬 모드 — iOS QuoteSortMode. */
enum class QuoteSortMode { LATEST, CHEAPEST }

/**
 * 단위 정규화 최저가 비교 — 회당 환산(월 ÷4, 총액 ÷8 가정)으로 단위 혼재 오해 방지.
 *  iOS ReceivedQuotesViewModel.normalizedPrice 와 동일 가정(주1회 월4회 / 8회 커리큘럼).
 */
internal fun normalizedPrice(r: QuoteResponseItem): Int? {
    val amt = r.priceAmount ?: return null
    return when (r.priceUnit) {
        "PER_MONTH" -> amt / 4
        "TOTAL" -> amt / 8
        else -> amt
    }
}

@Composable
fun ReceivedQuoteDetailScreen(
    api: QuoteApi,
    quoteId: Int,
    onBack: () -> Unit,
    onOpenProfile: (Int) -> Unit,
    onOpenChat: (Int) -> Unit,
    highlightResponseId: Int? = null,   // 알림 딥링크(?r=)로 방금 온 견적 강조
) {
    var quote by remember { mutableStateOf<QuoteFull?>(null) }
    var responses by remember { mutableStateOf<List<QuoteResponseItem>>(emptyList()) }
    var recommendations by remember { mutableStateOf<List<RecommendedTeacher>>(emptyList()) }
    var requestedTeacherIds by remember { mutableStateOf(setOf<Int>()) }
    var isLoading by remember { mutableStateOf(false) }
    var notFound by remember { mutableStateOf(false) }
    var busyResponseId by remember { mutableStateOf<Int?>(null) }
    var reRequesting by remember { mutableStateOf(false) }
    var sortMode by remember { mutableStateOf(QuoteSortMode.LATEST) }
    var expanded by remember { mutableStateOf(setOf<Int>()) }
    var toast by remember { mutableStateOf<String?>(null) }
    var confirmTarget by remember { mutableStateOf<QuoteResponseItem?>(null) }
    var confirmRec by remember { mutableStateOf<RecommendedTeacher?>(null) }
    var dialogFor by remember { mutableStateOf<RecommendedTeacher?>(null) }
    var showWithdrawConfirm by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // 채택 완료 판정 — 서버가 quote.status 를 MATCHED 로 안 내려도 방어적으로 감지(iOS hasAccepted).
    val matched = quote?.status == "MATCHED"
    val hasAccepted = matched || quote?.acceptedResponseId != null || responses.any { it.status == "ACCEPTED" }
    fun isAccepted(r: QuoteResponseItem) = r.status == "ACCEPTED" || quote?.acceptedResponseId == r.id

    // 채택 견적 최상단 고정 → 정렬 모드
    val sortedResponses = remember(responses, sortMode, quote) {
        responses.sortedWith(Comparator { a, b ->
            val aAcc = isAccepted(a); val bAcc = isAccepted(b)
            if (aAcc != bAcc) return@Comparator if (aAcc) -1 else 1
            when (sortMode) {
                QuoteSortMode.CHEAPEST -> {
                    val pa = normalizedPrice(a); val pb = normalizedPrice(b)
                    if (pa != pb) {
                        // 금액 없는(협의) 것은 뒤로
                        if (pa == null) return@Comparator 1
                        if (pb == null) return@Comparator -1
                        return@Comparator pa.compareTo(pb)
                    }
                    (b.createdAt ?: "").compareTo(a.createdAt ?: "")
                }
                QuoteSortMode.LATEST -> (b.createdAt ?: "").compareTo(a.createdAt ?: "")
            }
        })
    }

    suspend fun load() {
        api.getQuote(quoteId)
            .onSuccess { res ->
                quote = res.quote
                responses = res.responses
                notFound = false
                // 응답 0건일 때만 추천 조회 — 응답이 생기면 섹션 숨김(iOS loadRecommendations).
                recommendations = if (res.responses.isEmpty()) {
                    api.getQuoteRecommendations(quoteId).getOrDefault(emptyList())
                } else emptyList()
            }
            .onFailure { if (quote == null) notFound = true }
    }

    LaunchedEffect(quoteId) { isLoading = true; load(); isLoading = false }

    // 토스트 2초 후 자동 소멸(iOS onAppear asyncAfter 2)
    LaunchedEffect(toast) { if (toast != null) { delay(2000); toast = null } }

    Box(Modifier.fillMaxSize().background(MuyeonColors.groupedBg)) {
        Column(Modifier.fillMaxSize()) {
            QuoteNavBar(title = "받은 견적", onBack = onBack)

            if (notFound) {
                QuoteEmptyState(
                    icon = Icons.Filled.HelpOutline,
                    title = "요청을 찾을 수 없어요",
                    message = "이미 취소되었거나 삭제된 견적요청이에요.",
                    modifier = Modifier.fillMaxSize().wrapContentHeight(Alignment.CenterVertically),
                )
                return@Column
            }

            LazyColumn(
                Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item { RequestHeaderCard(quote) }

                // 지정(1:1) 요청 — 누구에게 요청했는지 + 진행상황(응답 유무와 무관하게 표시).
                val q = quote
                if (q != null && q.isDirect && q.targetTeacher != null) {
                    item {
                        val progress = when {
                            hasAccepted -> "매칭 완료"
                            responses.isNotEmpty() -> "견적 도착"
                            q.status == "EXPIRED" -> "응답 없음"
                            else -> "응답 대기중"
                        }
                        DirectTeacherCard(q.targetTeacher, progress) { onOpenProfile(q.targetTeacher.id ?: 0) }
                    }
                }

                item {
                    Text(
                        "받은 견적 ${responses.size}",
                        fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                        lineHeight = 19.sp, color = MuyeonColors.textHead,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }

                // 정렬 토글 — 견적 2개 이상일 때만
                if (responses.size > 1) {
                    item { SortSegmented(sortMode) { sortMode = it } }
                }

                if (responses.isEmpty() && !isLoading) {
                    if (quote?.isDirect == true) {
                        // 지정 요청: 그 강사의 응답만 기다림 — 브로드캐스트 추천 미노출.
                        item {
                            QuoteEmptyState(
                                Icons.Filled.HourglassEmpty, "응답을 기다리고 있어요",
                                "지정한 강사가 확인 후 견적을 보내드려요.",
                                Modifier.height(140.dp).wrapContentHeight(Alignment.CenterVertically),
                            )
                        }
                    } else {
                        item {
                            QuoteEmptyState(
                                Icons.Filled.HourglassEmpty, "아직 받은 견적이 없어요",
                                "강사의 견적을 기다려 주세요.",
                                Modifier.height(140.dp).wrapContentHeight(Alignment.CenterVertically),
                            )
                        }
                        // 무한 대기 방지 — 요청 조건과 비슷한 강사 추천(원터치 재요청)
                        if (recommendations.isNotEmpty()) {
                            item { RecommendationHeader() }
                            items(recommendations, key = { it.id }) { teacher ->
                                RecommendCard(
                                    teacher = teacher,
                                    requested = (teacher.alreadyRequested == true) || requestedTeacherIds.contains(teacher.id),
                                    disabled = reRequesting,
                                    onProfile = { onOpenProfile(teacher.id) },
                                    onRequest = {
                                        // 기존 채팅/진행 요청이 있으면 분기 다이얼로그
                                        if (teacher.existingRoomId != null || teacher.activeQuoteId != null) dialogFor = teacher
                                        else confirmRec = teacher
                                    },
                                )
                            }
                        }
                    }
                } else {
                    items(sortedResponses, key = { it.id }) { r ->
                        ResponseCard(
                            r = r,
                            accepted = isAccepted(r),
                            isClosed = hasAccepted && !isAccepted(r),
                            hasAccepted = hasAccepted,
                            isNew = highlightResponseId == r.id,
                            busy = busyResponseId == r.id,
                            anyBusy = busyResponseId != null,
                            expanded = expanded.contains(r.id),
                            onToggleExpand = {
                                expanded = if (expanded.contains(r.id)) expanded - r.id else expanded + r.id
                            },
                            onProfile = { r.responder?.id?.let(onOpenProfile) },
                            onChat = {
                                scope.launch {
                                    if (busyResponseId != null) return@launch
                                    busyResponseId = r.id
                                    api.startQuoteChat(quoteId, r.id)
                                        .onSuccess { if (it > 0) onOpenChat(it) else toast = "채팅방을 여는 데 실패했어요. 잠시 후 다시 시도해 주세요." }
                                        .onFailure { toast = "채팅 연결 중 오류가 발생했어요." }
                                    busyResponseId = null
                                }
                            },
                            onAccept = { confirmTarget = r },
                        )
                    }
                }
            }

            // 진행중(OPEN·미채택) 요청만 하단에 '견적 철회하기'
            if (quote?.status == "OPEN" && !hasAccepted) {
                WithdrawButton { showWithdrawConfirm = true }
            }
        }

        toast?.let { ToastBubble(it, Modifier.align(Alignment.BottomCenter)) }
    }

    // MARK: - 다이얼로그

    confirmTarget?.let { target ->
        QuoteDialog(
            title = "${target.responder?.displayName ?: "이 강사"}님을 채택할까요?",
            message = "채택하면 이 요청은 마감되고 다른 견적은 받을 수 없어요.",
            confirmText = "채택하기",
            onConfirm = {
                confirmTarget = null
                scope.launch {
                    if (busyResponseId != null) return@launch
                    busyResponseId = target.id
                    api.acceptQuote(quoteId, target.id)
                        .onSuccess { rid ->
                            load()
                            toast = "채택 완료! 채팅방에서 레슨 일정을 정해 주세요."
                            if (rid > 0) onOpenChat(rid)
                        }
                        .onFailure { toast = "채택에 실패했습니다." }
                    busyResponseId = null
                }
            },
            onDismiss = { confirmTarget = null },
        )
    }

    confirmRec?.let { teacher ->
        QuoteDialog(
            title = "견적을 요청할까요?",
            message = "${teacher.displayName}님에게 지금 요청 내용(과목·지역·문진) 그대로 전달돼요.",
            confirmText = "요청 보내기",
            onConfirm = {
                confirmRec = null
                scope.launch {
                    if (reRequesting) return@launch
                    reRequesting = true
                    api.reRequestQuote(quoteId, teacher.id)
                        .onSuccess {
                            requestedTeacherIds = requestedTeacherIds + teacher.id
                            toast = "${teacher.displayName}님에게 요청을 보냈어요. 견적이 도착하면 알려드릴게요."
                        }
                        .onFailure { toast = "요청을 보내지 못했어요. 잠시 후 다시 시도해 주세요." }
                    reRequesting = false
                }
            },
            onDismiss = { confirmRec = null },
        )
    }

    // 기존 채팅/진행 중 요청이 있는 강사 — 이동/새 요청 분기(MATCHED 는 이동만: 채택 보호)
    dialogFor?.let { teacher ->
        AlertDialog(
            onDismissRequest = { dialogFor = null },
            title = { DialogTitle("레슨 요청") },
            text = {
                DialogMessage(
                    if (teacher.activeQuoteStatus == "MATCHED")
                        "${teacher.displayName}님과 진행 중인 레슨이 있어요."
                    else "${teacher.displayName}님과 진행 중인 요청/채팅이 있어요. 어떻게 할까요?"
                )
            },
            confirmButton = {
                Column {
                    teacher.existingRoomId?.let { rid ->
                        TextButton(onClick = { dialogFor = null; onOpenChat(rid) }) { DialogAction("기존 채팅방으로 이동") }
                    }
                    if (teacher.activeQuoteStatus != "MATCHED") {
                        TextButton(onClick = {
                            dialogFor = null
                            scope.launch {
                                reRequesting = true
                                api.reRequestQuote(quoteId, teacher.id, force = true)
                                    .onSuccess {
                                        requestedTeacherIds = requestedTeacherIds + teacher.id
                                        toast = "${teacher.displayName}님에게 요청을 보냈어요. 견적이 도착하면 알려드릴게요."
                                    }
                                    .onFailure { toast = "요청을 보내지 못했어요. 잠시 후 다시 시도해 주세요." }
                                reRequesting = false
                            }
                        }) { DialogAction("그래도 새로 견적 요청") }
                    }
                }
            },
            dismissButton = { TextButton(onClick = { dialogFor = null }) { DialogAction("취소") } },
        )
    }

    if (showWithdrawConfirm) {
        QuoteDialog(
            title = "견적 요청을 철회할까요?",
            message = "철회하면 강사에게 보낸 요청이 사라져요. 되돌릴 수 없어요.",
            confirmText = "철회하기",
            onConfirm = {
                showWithdrawConfirm = false
                scope.launch {
                    api.cancelQuote(quoteId)
                        .onSuccess { onBack() }
                        .onFailure {
                            toast = it.message?.ifEmpty { null } ?: "철회에 실패했어요. 잠시 후 다시 시도해 주세요."
                            load()   // 이미 채택(MATCHED) 등 상태 변화 반영
                        }
                }
            },
            onDismiss = { showWithdrawConfirm = false },
        )
    }
}

// MARK: - 요청 요약 헤더

@Composable
private fun RequestHeaderCard(quote: QuoteFull?) {
    // 내가 작성한 견적 내용(문진 답변 전체 — 선택형 라벨 포함)
    val details = remember(quote) { QuoteQuestions.describe(quote?.categoryId, quote?.answers ?: emptyList()) }
    val photos = remember(quote) { (quote?.answers ?: emptyList()).flatMap { it.images ?: emptyList() } }
    val expiryDays = if (quote?.status == "OPEN") QuoteUi.daysUntilExpiry(quote.createdAt) else null

    QuoteCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                QuoteUi.categoryTitle(quote?.categoryId),
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp,
                lineHeight = 21.sp, color = MuyeonColors.textHead,
            )
            QuotePill(QuoteUi.statusLabel(quote?.status), QuoteUi.statusColor(quote?.status), filled = true)
            // 견적 만료 D-day — 진행중(OPEN)일 때만. (요청 생성 +14일)
            if (expiryDays != null) {
                Spacer(Modifier.weight(1f))
                Text(
                    if (expiryDays > 0) "${expiryDays}일 후 만료" else "오늘 만료",
                    fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                    lineHeight = 16.sp, color = MuyeonColors.primary,
                )
            }
        }
        if (details.isNotEmpty()) {
            Column(
                Modifier.padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                details.forEach { (label, value) ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            label,
                            fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp,
                            lineHeight = 16.sp, color = MuyeonColors.textSub, modifier = Modifier.width(84.dp),
                        )
                        Text(
                            value,
                            fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                            lineHeight = 16.sp, color = MuyeonColors.textHead, modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
        if (photos.isNotEmpty()) {
            Row(
                Modifier.padding(top = 8.dp).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                photos.forEach { src -> QuoteThumb(QuoteUi.imageUrl(src)) }
            }
        }
    }
}

/** 가로 스크롤 썸네일(첨부 사진) — iOS KFImageThumb(80 정사각, r8). */
@Composable
fun QuoteThumb(url: String?) {
    Box(Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFF0F0F0))) {
        if (url != null) {
            AsyncImage(url, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
    }
}

// MARK: - 받은 견적 카드

@Composable
private fun ResponseCard(
    r: QuoteResponseItem,
    accepted: Boolean,
    isClosed: Boolean,
    hasAccepted: Boolean,
    isNew: Boolean,
    busy: Boolean,
    anyBusy: Boolean,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onProfile: () -> Unit,
    onChat: () -> Unit,
    onAccept: () -> Unit,
) {
    val pro = r.responder
    val genres = (pro?.genres ?: emptyList()).take(3)
    val msg = (r.message ?: "").trim()
    val cornerText = if (accepted) "✓ 채택한 강사" else if (isClosed) "마감" else "서비스 견적"

    QuoteCard(borderColor = if (isNew) MuyeonColors.primary else null) {
        // 카드 상단 — 상태 라벨 + NEW + 시간
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                cornerText,
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                lineHeight = 16.sp, color = if (accepted) MuyeonColors.primary else MuyeonColors.textSub,
            )
            if (isNew) {
                Spacer(Modifier.width(6.dp))
                QuotePill("NEW", MuyeonColors.primary, filled = true)
            }
            Spacer(Modifier.weight(1f))
            Text(
                QuoteUi.relativeTime(r.createdAt),
                fontFamily = customFontFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp,
                lineHeight = 14.sp, color = MuyeonColors.secondary,
            )
        }

        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuoteAvatar(pro?.image, pro?.displayName ?: "강사", 52.dp)
            ProInfo(pro, genres, Modifier.weight(1f))
            PriceColumn(r)
        }

        // 견적 메시지 — 3줄 미리보기 + 더보기
        if (msg.isNotEmpty()) {
            Column(Modifier.padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    msg,
                    fontFamily = customFontFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp,
                    lineHeight = 17.sp, color = MuyeonColors.body,
                    maxLines = if (expanded) Int.MAX_VALUE else 3,
                    overflow = TextOverflow.Ellipsis,
                )
                if (msg.length > 80 || msg.contains("\n")) {
                    Text(
                        if (expanded) "접기" else "더보기",
                        fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp,
                        lineHeight = 16.sp, color = MuyeonColors.textSub,
                        modifier = Modifier.clickable(onClick = onToggleExpand),
                    )
                }
            }
        }

        // 카드 액션 — 강사 프로필 / 채팅 / 채택
        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (pro?.id != null) {
                CardButton("강사 프로필", filled = false, modifier = Modifier.weight(1f), onClick = onProfile)
            }
            if (!isClosed) {
                CardButton(
                    if (busy) "연결 중…" else "채팅",
                    filled = false, enabled = !anyBusy, modifier = Modifier.weight(1f), onClick = onChat,
                )
            }
            if (!hasAccepted) {
                CardButton("채택하기", filled = true, enabled = !anyBusy, modifier = Modifier.weight(1f), onClick = onAccept)
            }
        }
    }
}

/** 강사 정보 — 이름·평점(후기수)·장르·메타. iOS proInfo. */
@Composable
private fun ProInfo(pro: QuotePro?, genres: List<String>, modifier: Modifier = Modifier) {
    val meta = listOfNotNull(pro?.service?.let { "$it 레슨" }, pro?.region).joinToString(" · ")
    Column(modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                pro?.displayName ?: "강사",
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                lineHeight = 18.sp, color = MuyeonColors.textHead,
            )
            val rc = pro?.reviewCount ?: 0
            if (rc > 0) {
                Text(
                    String.format(Locale.KOREA, "★ %.1f (%d)", pro?.rating ?: 0.0, rc),
                    fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp,
                    lineHeight = 14.sp, color = MuyeonColors.primary,
                )
            }
        }
        if (genres.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { genres.forEach { QuotePill(it) } }
        }
        if (meta.isNotEmpty()) {
            Text(
                meta,
                fontFamily = customFontFamily, fontWeight = FontWeight.Normal, fontSize = 13.sp,
                lineHeight = 16.sp, color = MuyeonColors.textSub,
            )
        }
    }
}

/** 예상 금액 컬럼(우측 강조) — 구조화 금액 우선, 메모는 보조 캡션. iOS priceColumn(maxWidth 120). */
@Composable
private fun PriceColumn(r: QuoteResponseItem) {
    Column(
        Modifier.widthIn(max = 120.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            "예상 금액",
            fontFamily = customFontFamily, fontWeight = FontWeight.Normal, fontSize = 11.sp,
            lineHeight = 13.sp, color = MuyeonColors.textSub,
        )
        Text(
            QuoteUi.priceText(r.priceAmount, r.priceUnit, r.price),
            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp,
            lineHeight = 21.sp, color = MuyeonColors.textHead, textAlign = TextAlign.End,
        )
        val memo = r.price
        if (r.priceAmount != null && !memo.isNullOrEmpty()) {
            Text(
                memo,
                fontFamily = customFontFamily, fontWeight = FontWeight.Normal, fontSize = 11.sp,
                lineHeight = 13.sp, color = MuyeonColors.textSub, textAlign = TextAlign.End,
            )
        }
    }
}

// MARK: - 추천 강사

@Composable
private fun RecommendationHeader() {
    Column(
        Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            "이런 강사는 어때요?",
            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp,
            lineHeight = 19.sp, color = MuyeonColors.textHead,
        )
        Text(
            "요청 조건과 어울리는 강사님이에요. 지금 요청 내용 그대로 바로 견적을 요청할 수 있어요.",
            fontFamily = customFontFamily, fontWeight = FontWeight.Normal, fontSize = 13.sp,
            lineHeight = 16.sp, color = MuyeonColors.textSub,
        )
    }
}

/** 추천 카드 — iOS QuoteRecommendationSection.card 1:1(responseCard 와 동일 카드 어휘). */
@Composable
private fun RecommendCard(
    teacher: RecommendedTeacher,
    requested: Boolean,
    disabled: Boolean,
    onProfile: () -> Unit,
    onRequest: () -> Unit,
) {
    val genres = (teacher.genres ?: emptyList()).take(3)
    val meta = listOfNotNull(teacher.service?.let { "$it 레슨" }, teacher.region).joinToString(" · ")

    QuoteCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "추천 강사",
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                lineHeight = 16.sp, color = MuyeonColors.textSub,
            )
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                (teacher.reasons ?: emptyList()).forEach { QuotePill(it, MuyeonColors.primary) }
            }
        }
        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuoteAvatar(teacher.image, teacher.displayName, 52.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        teacher.displayName,
                        fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                        lineHeight = 18.sp, color = MuyeonColors.textHead,
                    )
                    val rc = teacher.reviewCount ?: 0
                    if (rc > 0) {
                        Text(
                            String.format(Locale.KOREA, "★ %.1f (%d)", teacher.rating ?: 0.0, rc),
                            fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp,
                            lineHeight = 14.sp, color = MuyeonColors.primary,
                        )
                    }
                }
                if (genres.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { genres.forEach { QuotePill(it) } }
                }
                if (meta.isNotEmpty()) {
                    Text(
                        meta,
                        fontFamily = customFontFamily, fontWeight = FontWeight.Normal, fontSize = 13.sp,
                        lineHeight = 16.sp, color = MuyeonColors.textSub,
                    )
                }
            }
        }
        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RecommendButton("강사 프로필", filled = false, modifier = Modifier.weight(1f), onClick = onProfile)
            RecommendButton(
                if (requested) "요청 완료" else "이 강사에게 요청하기",
                filled = true, check = requested, enabled = !requested && !disabled,
                modifier = Modifier.weight(1f), onClick = onRequest,
            )
        }
    }
}

// MARK: - 공용 조각

/** 카드 컨테이너 — iOS: r14 + systemBackground + shadow(0.06, r8, y2) + 좌우 16. */
@Composable
private fun QuoteCard(borderColor: Color? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadowCard()
            .then(if (borderColor != null) Modifier.border(2.dp, borderColor, RoundedCornerShape(14.dp)) else Modifier)
            .padding(16.dp),
        content = content,
    )
}

private fun Modifier.shadowCard() = this
    .clip(RoundedCornerShape(14.dp))
    .background(MuyeonColors.surface)

/** 카드 액션 버튼 — iOS cardButtonLabel(13 semibold, r10, v10). */
@Composable
private fun CardButton(
    text: String,
    filled: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (filled) MuyeonColors.primary else Color.Transparent)
            .then(if (filled) Modifier else Modifier.border(1.dp, MuyeonColors.border, RoundedCornerShape(10.dp)))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
            lineHeight = 16.sp, color = if (filled) Color.White else MuyeonColors.textHead,
        )
    }
}

/** 추천 카드 버튼 — iOS buttonLabel(13 bold, r10, v10, 미채움은 primary 8% 배경). */
@Composable
private fun RecommendButton(
    text: String,
    filled: Boolean,
    modifier: Modifier = Modifier,
    check: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Row(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (filled) MuyeonColors.primary else MuyeonColors.primary.copy(alpha = 0.08f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (check) {
            Icon(
                Icons.Filled.CheckCircle, null,
                tint = if (filled) Color.White else MuyeonColors.primary, modifier = Modifier.size(13.dp),
            )
        }
        Text(
            text,
            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp,
            lineHeight = 16.sp, color = if (filled) Color.White else MuyeonColors.primary,
        )
    }
}

/** 지정(1:1) 요청 대상 강사 카드 — iOS DirectTeacherCard. */
@Composable
private fun DirectTeacherCard(target: QuotePro, progress: String, onOpen: () -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "지정 요청한 강사",
            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp,
            lineHeight = 19.sp, color = MuyeonColors.textHead,
        )
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MuyeonColors.surface)
                .clickable(onClick = onOpen)
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            QuoteAvatarStack(listOf(target.image), target.displayName, 48.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    target.displayName,
                    fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                    lineHeight = 18.sp, color = MuyeonColors.textHead,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Star, null, tint = MuyeonColors.yellow, modifier = Modifier.size(12.dp))
                    Text(
                        String.format(Locale.KOREA, "%.1f", target.rating ?: 0.0),
                        fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                        lineHeight = 16.sp, color = MuyeonColors.textHead,
                    )
                    Text(
                        "(${target.reviewCount ?: 0})",
                        fontFamily = customFontFamily, fontSize = 13.sp,
                        lineHeight = 16.sp, color = MuyeonColors.secondary,
                    )
                }
            }
            QuotePill(progress, MuyeonColors.primary)
            Icon(Icons.Filled.KeyboardArrowRight, null, tint = MuyeonColors.chevron, modifier = Modifier.size(16.dp))
        }
    }
}

/** 하단 '견적 철회하기' 버튼(파괴적 톤) — iOS WithdrawButton(높이 52, r12, 상단 구분선). */
@Composable
private fun WithdrawButton(onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(MuyeonColors.surface)) {
        HorizontalDivider(color = MuyeonColors.border)
        Box(
            Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, MuyeonColors.danger.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "견적 철회하기",
                fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp,
                lineHeight = 19.sp, color = MuyeonColors.danger,
            )
        }
    }
}

/** 토스트 — iOS overlay(캡슐 검정 0.8, 하단 32). */
@Composable
fun ToastBubble(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp,
        lineHeight = 16.sp, color = Color.White, textAlign = TextAlign.Center,
        modifier = modifier
            .padding(bottom = 32.dp, start = 24.dp, end = 24.dp)
            .clip(RoundedCornerShape(50))
            .background(Color.Black.copy(alpha = 0.8f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
    )
}

@Composable
internal fun QuoteDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { DialogTitle(title) },
        text = { DialogMessage(message) },
        confirmButton = { TextButton(onClick = onConfirm) { DialogAction(confirmText) } },
        dismissButton = { TextButton(onClick = onDismiss) { DialogAction("취소") } },
    )
}

@Composable
internal fun DialogTitle(text: String) = Text(
    text, fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp,
    lineHeight = 19.sp, color = MuyeonColors.textHead,
)

@Composable
internal fun DialogMessage(text: String) = Text(
    text, fontFamily = customFontFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp,
    lineHeight = 17.sp, color = MuyeonColors.textSub,
)

@Composable
internal fun DialogAction(text: String) = Text(
    text, fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
    lineHeight = 17.sp, color = MuyeonColors.primary,
)

/** 정렬 토글 — iOS Picker(.segmented) 대응(최신순 / 최저가순). */
@Composable
private fun SortSegmented(mode: QuoteSortMode, onSelect: (QuoteSortMode) -> Unit) {
    Row(
        Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFEFEFF0))
            .padding(2.dp),
    ) {
        listOf(QuoteSortMode.LATEST to "최신순", QuoteSortMode.CHEAPEST to "최저가순").forEach { (m, label) ->
            val on = mode == m
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(7.dp))
                    .background(if (on) MuyeonColors.surface else Color.Transparent)
                    .clickable { onSelect(m) }
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    fontFamily = customFontFamily,
                    fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                    fontSize = 13.sp, lineHeight = 16.sp, color = MuyeonColors.textHead,
                )
            }
        }
    }
}
