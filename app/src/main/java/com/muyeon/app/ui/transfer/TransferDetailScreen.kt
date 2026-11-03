package com.muyeon.app.ui.transfer

import androidx.compose.runtime.saveable.rememberSaveable
import com.muyeon.app.ui.common.market.loginRequired
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.R
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.*
import com.muyeon.app.ui.common.market.*
import com.muyeon.app.ui.transfer.components.*
import kotlinx.coroutines.launch

/**
 * 상세 화면이 그릴 섹션 — 없는 섹션은 null. 이 매핑 한 곳에서만 "보일지"를 정한다.
 */
private data class TransferDetailView(
    val infoRows: List<Pair<String, String>>,
    val options: List<String>?,
    val description: String?,
    val locationName: String,
    val locationAddress: String?,
    val lat: Double?,
    val lng: Double?,
    val approxNote: String?,
    val contactName: String?,
    val phone: String?,
    val reportable: Boolean,
    /** 맨 아래 안내 — 서버 notices, 없으면 예전 안내 한 줄. */
    val notices: List<String>,
)

private fun TransferDetail.toView(): TransferDetailView {
    val addr = listOfNotNull(roadAddress, addressDetail).joinToString(" ").ifBlank { null }
    return TransferDetailView(
        infoRows = transferInfoRows(this),
        options = card.options.takeIf { it.isNotEmpty() },
        description = description?.takeIf { it.isNotBlank() },
        locationName = roadAddress ?: card.region ?: "위치",
        locationAddress = addr ?: card.region,
        lat = lat,
        lng = lng,
        approxNote = if (locationApprox) "대략적인 위치예요. 정확한 주소는 채팅으로 확인해 주세요." else null,
        contactName = contactName,
        phone = contactPhone?.takeIf { it.isNotBlank() },
        reportable = !isAuthor,
        notices = notices.ifEmpty { listOf(TRANSFER_NOTICE_FALLBACK) },
    )
}

/**
 * 양도 글 상세 — 사진 갤러리 → 요약 → 기본정보 표 → 옵션 → 접는 소개 → 지도 → 담당자 → 고지 → 신고,
 *  고정 하단 바: 보는 사람 [TransferViewerBottomBar] / 글쓴이 [TransferAuthorBottomBar].
 */
@Composable
fun TransferDetailScreen(
    api: TransferApi,
    id: Int,
    onBack: () -> Unit,
    onEdit: (Int) -> Unit,
    onChatRoom: (Int, String) -> Unit,
    onChanged: () -> Unit,
    onMessage: (String) -> Unit,
    /** 문의 채팅 전 이용약관 동의(액티비티가 하나를 만들어 넘기고 시트도 그린다). */
    chatConsent: TransferChatConsent,
    /** ★ 로그인 후 이어서 할 일(chat·like) — 상세를 다 불러온 뒤 한 번만 한다(LoginResume). */
    resumeAction: String? = null,
) {
    var detail by remember { mutableStateOf<TransferDetail?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var chatBusy by remember { mutableStateOf(false) }
    var statusOpen by remember { mutableStateOf(false) }
    var reportOpen by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    suspend fun load() {
        api.detail(id).onSuccess { detail = it; error = null }.onFailure { error = it.message }
        loading = false
    }
    LaunchedEffect(id) { load() }

    fun toggleScrap(d: TransferDetail) {
        if (!api.isLoggedIn) { onMessage(loginRequired("openStudioTransferDetail", "id" to id, "then" to "like")); return }
        val next = !d.card.scrapped
        detail = d.copy(card = d.card.copy(scrapped = next))
        scope.launch {
            api.setScrap(d.card.id, next).onSuccess { onChanged() }.onFailure { e ->
                detail = detail?.let { it.copy(card = it.card.copy(scrapped = !next)) }
                onMessage(e.message ?: "찜 처리에 실패했어요.")
            }
        }
    }

    fun chat(d: TransferDetail) {
        if (!api.isLoggedIn) { onMessage(loginRequired("openStudioTransferDetail", "id" to id, "then" to "chat")); return }
        // 이용약관 동의 확인 → 채팅방 열기(동의 필요 403 이면 같은 동의 시트 후 다시). 로그인 후 이어서(then=chat)도 이 길.
        fun openRoom() {
            chatBusy = true
            scope.launch {
                api.directRoom(d.card.authorId, d.card.id)
                    .onSuccess { onChatRoom(it, d.card.authorName) }
                    .onFailure { e ->
                        if (e.isTransferConsentRequired()) chatConsent.onRequired { openRoom() } else onMessage(e.message ?: "채팅방을 열지 못했어요.")
                    }
                chatBusy = false
            }
        }
        scope.launch { chatConsent.ensure { openRoom() } }
    }

    // 로그인 후 다시 연 화면 — 상세를 받은 뒤(이벤트) 하려던 동작을 한 번만. 본인 글이면 하지 않는다.
    var resumed by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(detail, resumeAction) {
        val d = detail ?: return@LaunchedEffect
        if (resumed || resumeAction.isNullOrEmpty() || !api.isLoggedIn) return@LaunchedEffect
        resumed = true
        when (resumeAction) {
            "chat" -> if (!d.isAuthor) chat(d)
            "like" -> if (!d.card.scrapped) toggleScrap(d)
        }
    }

    Column(Modifier.fillMaxSize().background(Color.White)) {
        TransferDetailTopBar(onBack)
        val d = detail
        val phase = when { d != null -> DetailPhase.Content; loading -> DetailPhase.Loading; else -> DetailPhase.Error }
        // 본문 자리는 늘 같은 크기(weight) — 자리(스켈레톤) → 내용은 겹쳐 바꾸기만.
        StableSwap(phase, Modifier.weight(1f).fillMaxWidth()) { p ->
            when (p) {
                DetailPhase.Loading -> TransferDetailSkeleton()
                DetailPhase.Error -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    MuyeonEmptyState(error ?: "양도 글을 찾을 수 없어요.", actionTitle = "다시 시도", onAction = { loading = true; scope.launch { load() } })
                }
                DetailPhase.Content -> detail?.let { dd ->
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                        MarketGallery(dd.images, placeholderRes = R.drawable.transfer_noimage)
                        TransferDetailBody(dd.card, dd.toView(), onReport = {
                            if (!api.isLoggedIn) onMessage(loginRequired("openStudioTransferDetail", "id" to id)) else reportOpen = true
                        })
                    }
                }
            }
        }
        // 하단 바 자리 — 불러오는 중엔 같은 높이의 자리, 오류면 같은 높이의 빈칸.
        when {
            d != null && d.isAuthor -> TransferAuthorBottomBar(onEdit = { onEdit(d.card.id) }, onStatus = { statusOpen = true })
            d != null -> TransferViewerBottomBar(d.card.scrapped, d.toView().phone, chatBusy, onHeart = { toggleScrap(d) }, onChat = { chat(d) })
            loading -> MarketBottomBarSkeleton()
            else -> BottomBarSpace()
        }
    }

    if (statusOpen) {
        val d = detail
        TransferStatusDialog(
            current = d?.card?.status,
            onPick = { s ->
                if (d != null) scope.launch {
                    api.setStatus(d.card.id, s).onSuccess { detail = d.copy(card = d.card.copy(status = s)); onChanged() }
                        .onFailure { onMessage(it.message ?: "상태를 바꾸지 못했어요.") }
                }
            },
            onDismiss = { statusOpen = false },
        )
    }
    if (reportOpen) {
        TransferReportDialog(
            onConfirm = {
                scope.launch { api.report(id).onSuccess { onMessage("신고가 접수됐어요.") }.onFailure { onMessage(it.message ?: "신고하지 못했어요.") } }
            },
            onDismiss = { reportOpen = false },
        )
    }
}

/** 본문 — 섹션 조립만(보일지는 [toView] 가 정했다). */
@Composable
private fun TransferDetailBody(card: TransferCard, view: TransferDetailView, onReport: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        TransferSummary(card)
        TransferInfoTable(view.infoRows)
        view.options?.let { TransferOptionChips(it) }
        view.description?.let { TransferDescription(it) }
        TransferLocationSection(view.locationName, view.locationAddress, view.lat, view.lng, view.approxNote)
        view.contactName?.let { TransferContactSection(it) }
        TransferNoticeSection(view.notices)
        if (view.reportable) TransferReportLink(onReport)
        Spacer(Modifier.height(8.dp))
    }
}
