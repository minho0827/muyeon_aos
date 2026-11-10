package com.muyeon.app.ui.transfer

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.*
import com.muyeon.app.ui.common.market.*
import com.muyeon.app.ui.transfer.components.TransferCardView
import com.muyeon.app.ui.transfer.components.TransferFilterSheet
import com.muyeon.app.ui.transfer.components.TransferListEmpty
import com.muyeon.app.ui.transfer.components.TransferListError
import com.muyeon.app.ui.transfer.components.TransferListItems
import com.muyeon.app.ui.transfer.components.TransferListLoading
import com.muyeon.app.ui.transfer.components.TransferOwnCardActions
import com.muyeon.app.ui.transfer.components.TransferRegionChips
import com.muyeon.app.ui.transfer.components.TransferViewerCardActions
import kotlinx.coroutines.launch

/** 양도양수 목록 — 제목줄(＋) · 지역 칩/필터 · 카드 목록(커서). 화면은 상태 연결과 조립만 한다. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferListScreen(
    api: TransferApi,
    state: TransferListState,
    canCreate: Boolean,
    myUserId: Int?,
    /** null = 탭(뒤로 없음). */
    onBack: (() -> Unit)?,
    onOpen: (Int) -> Unit,
    onCreate: () -> Unit,
    onChatRoom: (Int, String) -> Unit,
    onMessage: (String) -> Unit,
    /** 공간 탭 안 머리(위 바 + 대관·양도양수 탭). 주면 이 화면 위 바 대신 그린다 — 등록(＋)은 머리가 맡는다. */
    header: (@Composable () -> Unit)? = null,
    /** 문의 채팅 전 이용약관 동의(액티비티가 하나를 만들어 넘기고 시트도 그린다). */
    chatConsent: TransferChatConsent,
) {
    val scope = rememberCoroutineScope()
    var showFilter by remember { mutableStateOf(false) }
    var chatBusy by remember { mutableStateOf(false) }

    LaunchedEffect(state.region, state.filter, com.muyeon.app.auth.accountEpoch()) {
        // 같은 조건으로 이미 받았으면(다른 화면에서 돌아옴) 그대로 — 다시 받으면 첫 쪽만 남아 스크롤이 맨 위로 갔다.
        val sig = state.region to state.filter
        if (state.loadedFor == sig) return@LaunchedEffect
        val changed = state.loadedFor != null
        state.loadedFor = sig
        state.load(api, reset = true)
        if (changed) state.listState.scrollToItem(0)
    }

    // 당겨서 새로고침 — 화면 scope 에서 돌려 손을 떼도 요청이 끊기지 않는다. 실패해도 있던 목록은 그대로.
    var refreshing by remember { mutableStateOf(false) }
    fun refresh() {
        if (refreshing) return
        refreshing = true
        scope.launch { try { state.load(api, reset = true) } finally { refreshing = false } }
    }

    val nearEnd by remember {
        derivedStateOf {
            val info = state.listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            info.totalItemsCount > 0 && last >= info.totalItemsCount - 3
        }
    }
    LaunchedEffect(nearEnd) { if (nearEnd && state.loadedOnce) state.load(api, reset = false) }

    fun toggleScrap(c: TransferCard) {
        if (!api.isLoggedIn) { onMessage(loginRequired("openStudioTransferDetail", "id" to c.id, "then" to "like")); return }
        val next = !c.scrapped
        state.patch(c.id) { it.copy(scrapped = next) }
        scope.launch {
            api.setScrap(c.id, next).onFailure { e ->
                state.patch(c.id) { it.copy(scrapped = !next) }
                onMessage(e.message ?: "찜 처리에 실패했어요.")
            }
        }
    }

    fun chat(c: TransferCard) {
        if (!api.isLoggedIn) { onMessage(loginRequired("openStudioTransferDetail", "id" to c.id, "then" to "chat")); return }
        if (chatBusy) return
        chatBusy = true
        // 이용약관 동의 확인 → 채팅방 열기(동의 필요 403 이면 같은 동의 시트 후 다시).
        fun openRoom() {
            chatBusy = true
            scope.launch {
                api.directRoom(c.authorId, c.id).onSuccess { onChatRoom(it, c.authorName) }.onFailure { e ->
                    if (e.isTransferConsentRequired()) chatConsent.onRequired { openRoom() } else onMessage(e.message ?: "채팅방을 열지 못했어요.")
                }
                chatBusy = false
            }
        }
        scope.launch { chatConsent.ensure { openRoom() } }
    }

    Column(Modifier.fillMaxSize().background(MuyeonList.pageBg)) {
        if (header != null) header() else MuyeonListHeader("무용센터 양도양수", onBack = onBack) {
            if (canCreate) MuyeonHeaderIcon(Icons.Filled.Add, "양도 글 등록", onClick = onCreate)
        }
        TransferRegionChips(state.region, state.filter.isActive, onRegion = { state.region = it }, onFilter = { showFilter = true })

        val phase = when {
            !state.loadedOnce && state.loading -> ListPhase.Loading
            state.items.isEmpty() && state.error != null -> ListPhase.Error
            state.items.isEmpty() && state.loadedOnce -> ListPhase.Empty
            else -> ListPhase.Items
        }
        PullToRefreshBox(refreshing, onRefresh = ::refresh, modifier = Modifier.weight(1f).fillMaxWidth()) {
        StableSwap(phase, Modifier.fillMaxSize()) { p ->
          Box(Modifier.fillMaxSize()) {
            when (p) {
                ListPhase.Loading -> TransferListLoading()
                ListPhase.Error -> TransferListError(state.error ?: "") { scope.launch { state.load(api, true) } }
                ListPhase.Empty -> TransferListEmpty()
                ListPhase.Items -> TransferListItems(state.listState, state.items, { it.id }, hasMore = state.hasMore, loadingMore = state.loading && state.items.isNotEmpty(),
                    bottomPadding = if (onBack == null) com.muyeon.app.ui.main.tabBarBottomPadding() else 16.dp) { c ->
                    // 카드 오른쪽 칸 — 내 글이면 하트만, 남의 글이면 하트 + 바로 채팅.
                    val mine = myUserId != null && c.authorId == myUserId
                    if (mine) TransferCardView(c, onClick = { onOpen(c.id) }) { TransferOwnCardActions(c.scrapped) { toggleScrap(c) } }
                    else TransferCardView(c, onClick = { onOpen(c.id) }) { TransferViewerCardActions(c.scrapped, { toggleScrap(c) }, { chat(c) }) }
                }
            }
          }
        }
        }
    }

    if (showFilter) {
        TransferFilterSheet(state.filter, onApply = { state.filter = it; showFilter = false }, onDismiss = { showFilter = false })
    }
}
