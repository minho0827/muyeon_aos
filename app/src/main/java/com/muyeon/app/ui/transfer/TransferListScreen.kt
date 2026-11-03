package com.muyeon.app.ui.transfer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.DmPlaneButton
import com.muyeon.app.ui.common.FormChipWrap
import com.muyeon.app.ui.common.FormField
import com.muyeon.app.ui.common.FormTextField
import com.muyeon.app.ui.common.MarketBadge
import com.muyeon.app.ui.common.MarketHeart
import com.muyeon.app.ui.common.MarketThumb
import com.muyeon.app.ui.common.MuyeonColors
import com.muyeon.app.ui.common.MuyeonEmptyState
import com.muyeon.app.ui.common.MuyeonHeaderIcon
import com.muyeon.app.ui.common.MuyeonList
import com.muyeon.app.ui.common.MuyeonListHeader
import com.muyeon.app.ui.common.MuyeonPillTab
import com.muyeon.app.ui.common.MuyeonPillTabs
import kotlinx.coroutines.launch

/**
 * 목록 상태 — 액티비티에 붙들어 상세에 다녀와도 스크롤·목록이 남는다(돌아오면 조용히 1쪽부터 다시 읽는다).
 */
class TransferListState {
    var region by mutableStateOf<String?>(null)
    var filter by mutableStateOf(TransferFilter())
    var items by mutableStateOf<List<TransferCard>>(emptyList())
    var hasMore by mutableStateOf(true)
    var loading by mutableStateOf(false)
    var loadedOnce by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    val listState = LazyListState()
    private var cursor: String? = null
    private var generation = 0

    suspend fun load(api: TransferApi, reset: Boolean) {
        if (!reset && (loading || !hasMore)) return
        val gen = if (reset) ++generation else generation
        if (reset) cursor = null
        loading = true; error = null
        api.page(region, filter, cursor)
            .onSuccess { p ->
                if (gen != generation) return@onSuccess
                items = if (reset) p.items else (items + p.items).distinctBy { it.id }
                hasMore = p.hasMore && p.nextCursor != null
                cursor = p.nextCursor
            }
            .onFailure { if (gen == generation) error = it.message }
        if (gen == generation) { loading = false; loadedOnce = true }
    }

    fun patch(id: Int, block: (TransferCard) -> TransferCard) {
        items = items.map { if (it.id == id) block(it) else it }
    }
}

@Composable
fun TransferListScreen(
    api: TransferApi,
    state: TransferListState,
    canCreate: Boolean,
    myUserId: Int?,
    onBack: () -> Unit,
    onOpen: (Int) -> Unit,
    onCreate: () -> Unit,
    onChatRoom: (Int, String) -> Unit,
    onMessage: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var showFilter by remember { mutableStateOf(false) }
    var chatBusy by remember { mutableStateOf(false) }

    LaunchedEffect(state.region, state.filter) { state.load(api, reset = true) }

    val nearEnd by remember {
        derivedStateOf {
            val info = state.listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            info.totalItemsCount > 0 && last >= info.totalItemsCount - 3
        }
    }
    LaunchedEffect(nearEnd) { if (nearEnd && state.loadedOnce) state.load(api, reset = false) }

    fun toggleScrap(c: TransferCard) {
        if (!api.isLoggedIn) { onMessage("로그인 후 이용할 수 있어요."); return }
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
        if (!api.isLoggedIn) { onMessage("로그인 후 이용할 수 있어요."); return }
        if (chatBusy) return
        chatBusy = true
        scope.launch {
            api.directRoom(c.authorId, c.id).onSuccess { onChatRoom(it, c.authorName) }.onFailure { onMessage(it.message ?: "채팅방을 열지 못했어요.") }
            chatBusy = false
        }
    }

    Column(Modifier.fillMaxSize().background(MuyeonList.pageBg)) {
        MuyeonListHeader("무용센터 양도양수", onBack = onBack) {
            if (canCreate) MuyeonHeaderIcon(Icons.Filled.Add, "양도 글 등록", onClick = onCreate)
        }
        MuyeonPillTabs(
            TransferLabels.regions.map { (name, code) -> MuyeonPillTab(code ?: "", name) },
            state.region ?: "", { state.region = it.ifEmpty { null } },
            tagPrefix = "transfer.region", compact = true,
        ) {
            Box(
                Modifier.padding(end = 8.dp).size(40.dp).clip(RoundedCornerShape(12.dp)).clickable { showFilter = true },
                Alignment.Center,
            ) {
                Icon(Icons.Filled.Tune, "필터", tint = if (state.filter.isActive) MuyeonColors.primary else MuyeonColors.textHead, modifier = Modifier.size(22.dp))
            }
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                !state.loadedOnce && state.loading -> CircularProgressIndicator(color = MuyeonColors.primary, modifier = Modifier.align(Alignment.Center))
                state.items.isEmpty() && state.error != null -> MuyeonEmptyState(
                    state.error ?: "", actionTitle = "다시 시도", onAction = { scope.launch { state.load(api, true) } },
                    modifier = Modifier.align(Alignment.Center),
                )
                state.items.isEmpty() && state.loadedOnce -> MuyeonEmptyState(
                    "아직 올라온 양도 글이 없어요.", icon = Icons.Outlined.Storefront, modifier = Modifier.align(Alignment.Center),
                )
                else -> LazyColumn(
                    state = state.listState, modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.items, key = { it.id }) { c ->
                        TransferCardRow(
                            c, mine = myUserId != null && c.authorId == myUserId,
                            onClick = { onOpen(c.id) }, onHeart = { toggleScrap(c) }, onChat = { chat(c) },
                        )
                    }
                    if (state.loading && state.items.isNotEmpty()) item(key = "more") {
                        Box(Modifier.fillMaxWidth().padding(12.dp), Alignment.Center) { CircularProgressIndicator(color = MuyeonColors.primary, modifier = Modifier.size(24.dp)) }
                    }
                }
            }
        }
    }

    if (showFilter) {
        TransferFilterSheet(state.filter, onApply = { state.filter = it; showFilter = false }, onDismiss = { showFilter = false })
    }
}

@Composable
fun TransferCardRow(c: TransferCard, mine: Boolean, onClick: () -> Unit, onHeart: (() -> Unit)?, onChat: (() -> Unit)?) {
    Row(
        Modifier.fillMaxWidth().clip(MuyeonList.cardShape).background(Color.White).clickable(onClick = onClick).padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box {
            MarketThumb(c.image, Modifier.size(96.dp).clip(RoundedCornerShape(12.dp)))
            if (c.status != "OPEN") {
                Box(Modifier.align(Alignment.TopStart).padding(6.dp)) {
                    MarketBadge(
                        TransferLabels.status(c.status),
                        bg = if (c.status == "HOLD") MuyeonColors.primary else MuyeonColors.textSub,
                    )
                }
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                c.title, fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp,
                color = MuyeonColors.textHead, maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            if (c.metaLine.isNotEmpty()) {
                Text(c.metaLine, fontFamily = customFontFamily, fontSize = 13.sp, color = MuyeonColors.textSub, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(
                c.priceLine, fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                color = MuyeonColors.textHead, maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (onHeart != null) MarketHeart(c.scrapped, 20.dp, onHeart)
            // 본인 글엔 비행기를 숨긴다(자기에게 채팅할 일이 없다).
            if (!mine && onChat != null) DmPlaneButton("바로 채팅", onChat)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransferFilterSheet(initial: TransferFilter, onApply: (TransferFilter) -> Unit, onDismiss: () -> Unit) {
    var f by remember { mutableStateOf(initial) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = Color.White) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding()) {
            Text(
                "필터", fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MuyeonColors.textHead,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            Column(
                Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                FormField("시설 유형") {
                    FormChipWrap(TransferLabels.facility, { f.facilityType == it }) { f = f.copy(facilityType = if (f.facilityType == it) null else it) }
                }
                FormField("평수 범위", hint = "(평)") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f)) { FormTextField(f.minArea, { f = f.copy(minArea = it.filter(Char::isDigit)) }, "최소", 5, keyboardType = KeyboardType.Number) }
                        Text("~", fontFamily = customFontFamily, color = MuyeonColors.textSub)
                        Box(Modifier.weight(1f)) { FormTextField(f.maxArea, { f = f.copy(maxArea = it.filter(Char::isDigit)) }, "최대", 5, keyboardType = KeyboardType.Number) }
                    }
                }
                FormField("보증금 상한", hint = "(만원)") { FormTextField(f.maxDeposit, { f = f.copy(maxDeposit = it.filter(Char::isDigit)) }, "예) 3000", 8, keyboardType = KeyboardType.Number) }
                FormField("월세 상한", hint = "(만원)") { FormTextField(f.maxRent, { f = f.copy(maxRent = it.filter(Char::isDigit)) }, "예) 200", 8, keyboardType = KeyboardType.Number) }
                FormField("권리금 상한", hint = "(만원)") { FormTextField(f.maxPremium, { f = f.copy(maxPremium = it.filter(Char::isDigit)) }, "예) 2000", 8, keyboardType = KeyboardType.Number) }
                FormField("옵션") {
                    FormChipWrap(TransferLabels.options, { it in f.options }) { v -> f = f.copy(options = if (v in f.options) f.options - v else f.options + v) }
                }
                Spacer(Modifier.height(4.dp))
            }
            SheetActions(onReset = { f = TransferFilter() }, onApply = { onApply(f) })
        }
    }
}

/** 시트 하단 [초기화] [적용]. */
@Composable
fun SheetActions(onReset: () -> Unit, onApply: () -> Unit, applyLabel: String = "적용") {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            Modifier.weight(1f).height(52.dp).clip(RoundedCornerShape(16.dp)).background(MuyeonList.ink100).clickable(onClick = onReset),
            Alignment.Center,
        ) { Text("초기화", fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MuyeonColors.textHead) }
        Box(Modifier.weight(2f)) { com.muyeon.app.ui.castingstage.components.StageBottomButton(applyLabel, onClick = onApply) }
    }
}
