package com.muyeon.app.ui.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.muyeon.app.navigation.collectAppResults
import com.muyeon.app.result.ResultKeys
import kotlinx.coroutines.launch
import com.muyeon.app.ui.chat.ChatActivity
import com.muyeon.app.ui.common.market.MarketMessageDialog
import com.muyeon.app.ui.space.SpaceActivity
import com.muyeon.app.ui.space.SpaceApi
import com.muyeon.app.ui.space.SpaceListScreen
import com.muyeon.app.ui.transfer.TransferActivity
import com.muyeon.app.ui.transfer.TransferApi
import com.muyeon.app.ui.transfer.TransferChatConsent
import com.muyeon.app.ui.transfer.TransferChatConsentSheet
import com.muyeon.app.ui.transfer.TransferListScreen
import com.muyeon.app.ui.transfer.TransferListState

/**
 * ★ 2026-11-08 공간 탭 — 대관·양도양수 목록(기존 앱 화면)을 탭 안에 둔다(위 바 + 밑줄 탭은 [header]).
 *   상세·등록·내 글은 지금처럼 각 화면(SpaceActivity · SpaceHubActivity · TransferActivity)으로 연다.
 *   등록은 탭 오른쪽 아래 [+ 글쓰기](MainScreen — 채용 탭과 같은 부품)가 맡는다(위 바 ＋ 대신).
 *   등록·수정·삭제하고 돌아오면 결과 키(공간 SPACE · 양도 [TransferActivity.RESULT_TRANSFERS])로 목록을 다시 읽는다.
 */
@Composable
fun SpaceRentTab(header: @Composable (actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) -> Unit) {
    val context = LocalContext.current
    val api = remember { SpaceApi(context) }
    var message by remember { mutableStateOf<String?>(null) }
    var reloadTick by remember { mutableIntStateOf(0) }
    // 공간 화면(등록 위저드·상세)이 SPACE 를 들고 닫히면 목록·찜을 다시 읽는다(MainActivity → AppResults).
    LaunchedEffect(Unit) { collectAppResults(ResultKeys.SPACE) { reloadTick++ } }
    SpaceListScreen(
        api = api, canCreate = false, onBack = null,
        onOpen = { id, date -> SpaceActivity.start(context, id, date, false) },
        onCreate = {},
        onMessage = { message = it },
        header = { header {} },
        reloadTick = reloadTick,
    )
    message?.let { MarketMessageDialog(it) { message = null } }
}

@Composable
fun TransferTab(header: @Composable (actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) -> Unit) {
    val context = LocalContext.current
    val api = remember { TransferApi(context) }
    val listState = remember { TransferListState() }
    val chatConsent = remember { TransferChatConsent(api) }
    val myId = remember { api.http.myUserId() }
    var message by remember { mutableStateOf<String?>(null) }
    // 양도 글을 올리거나 고치고 돌아오면 처음부터 다시 읽는다.
    LaunchedEffect(Unit) { collectAppResults(TransferActivity.RESULT_TRANSFERS) { launch { listState.load(api, reset = true) } } }
    TransferListScreen(
        api = api, state = listState, canCreate = false, myUserId = myId, onBack = null,
        onOpen = { TransferActivity.startDetail(context, it) },
        onCreate = {},
        onChatRoom = { roomId, title -> ChatActivity.startRoom(context, roomId, title) },
        onMessage = { message = it },
        chatConsent = chatConsent,
        header = { header {} },
    )
    TransferChatConsentSheet(chatConsent) { message = it }
    message?.let { MarketMessageDialog(it) { message = null } }
}

