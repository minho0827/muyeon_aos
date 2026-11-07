package com.muyeon.app.ui.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.muyeon.app.R
import com.muyeon.app.ui.components.TopBarIconButton
import com.muyeon.app.ui.chat.ChatActivity
import com.muyeon.app.ui.common.market.MarketMessageDialog
import com.muyeon.app.ui.common.market.loginRequired
import com.muyeon.app.ui.space.SpaceActivity
import com.muyeon.app.ui.space.SpaceApi
import com.muyeon.app.ui.space.SpaceHubActivity
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
 *   등록 권한·로그인 안내 문구는 각 액티비티와 같다.
 */
@Composable
fun SpaceRentTab(header: @Composable (actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) -> Unit) {
    val context = LocalContext.current
    val api = remember { SpaceApi(context) }
    var message by remember { mutableStateOf<String?>(null) }
    val canHost = SpaceHubActivity.canHost(context)
    val create = {
        when {
            !api.isLoggedIn -> message = loginRequired("openSpaceEditor")
            !canHost -> message = SPACE_ROLE_MSG
            else -> SpaceHubActivity.startEditor(context, null)
        }
    }
    SpaceListScreen(
        api = api, canCreate = canHost, onBack = null,
        onOpen = { id, date -> SpaceActivity.start(context, id, date, false) },
        onCreate = create,
        onMessage = { message = it },
        header = { header { if (canHost) TopBarIconButton(R.drawable.ic_add, stringResource(R.string.space_create), onClick = create) } },
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
    val canPost = TransferActivity.canPost(context)
    val create = {
        when {
            !api.isLoggedIn -> message = loginRequired("openStudioTransferEditor")
            !canPost -> message = TRANSFER_ROLE_MSG
            else -> TransferActivity.startEditor(context, null)
        }
    }
    TransferListScreen(
        api = api, state = listState, canCreate = canPost, myUserId = myId, onBack = null,
        onOpen = { TransferActivity.startDetail(context, it) },
        onCreate = create,
        onChatRoom = { roomId, title -> ChatActivity.startRoom(context, roomId, title) },
        onMessage = { message = it },
        chatConsent = chatConsent,
        header = { header { if (canPost) TopBarIconButton(R.drawable.ic_add, stringResource(R.string.transfer_create), onClick = create) } },
    )
    TransferChatConsentSheet(chatConsent) { message = it }
    message?.let { MarketMessageDialog(it) { message = null } }
}

// 액티비티(SpaceHubActivity · TransferActivity)와 같은 안내.
private const val SPACE_ROLE_MSG = "공간은 학원·강사 유형에서 등록할 수 있어요. MY > 회원유형에서 전환해 주세요."
private const val TRANSFER_ROLE_MSG = "양도 글은 학원·강사 유형에서 올릴 수 있어요. MY > 회원유형에서 전환해 주세요."
