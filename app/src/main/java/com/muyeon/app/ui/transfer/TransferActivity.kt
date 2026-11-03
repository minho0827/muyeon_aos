package com.muyeon.app.ui.transfer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.muyeon.app.result.ResultActivity
import com.muyeon.app.result.launchScreen
import com.muyeon.app.ui.chat.ChatActivity
import com.muyeon.app.ui.common.MuyeonDialog
import com.muyeon.app.ui.common.MuyeonDialogAction
import com.muyeon.app.webview.ActiveRole

/**
 * 무용센터 양도양수 진입점(2026-11-03) — 웹 브리지 openStudioTransfers · openStudioTransferDetail{id} ·
 *  openStudioTransferEditor{id?} · openMyStudioTransfers. 한 액티비티 안 NavHost(list · detail · editor · mine).
 */
class TransferActivity : ResultActivity() {

    companion object {
        private const val EXTRA_ROUTE = "route"
        private const val EXTRA_ID = "id"
        /** ★ 로그인 후 이어서 할 일(chat·like) — LoginResume "then". */
        private const val EXTRA_THEN = "then"

        private fun intent(context: Context, route: String, id: Int = 0) =
            Intent(context, TransferActivity::class.java).putExtra(EXTRA_ROUTE, route).putExtra(EXTRA_ID, id)

        fun startList(context: Context) = context.launchScreen(intent(context, "list"))
        fun startDetail(context: Context, id: Int, then: String? = null) =
            context.launchScreen(intent(context, "detail", id).putExtra(EXTRA_THEN, then.orEmpty()))
        fun startEditor(context: Context, id: Int?) = context.launchScreen(intent(context, "editor", id ?: 0))
        fun startMine(context: Context) = context.launchScreen(intent(context, "mine"))

        /** 등록은 학원·강사 활동유형만(서버 ActiveRoleGuard 와 같은 기준 — 화면은 막는 쪽으로). */
        fun canPost(context: Context): Boolean = ActiveRole.current(context) in setOf(ActiveRole.ACADEMY, ActiveRole.TEACHER)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val route = intent.getStringExtra(EXTRA_ROUTE) ?: "list"
        val startId = intent.getIntExtra(EXTRA_ID, 0)
        val start = when (route) {
            // ★ startDestination 은 경로 '패턴'이어야 한다 — 값을 넣으면 인자가 안 넘어간다(실기기 11-03). 값은 navArgument 기본값으로.
            "detail" -> "detail/{id}"
            "editor" -> "editor/{id}"
            "mine" -> "mine"
            else -> "list"
        }

        setContent {
            val nav = rememberNavController()
            com.muyeon.app.analytics.TrackNavigation("StudioTransfer", nav)
            val api = remember { TransferApi(this) }
            val listState = remember { TransferListState() }
            val myId = remember { api.http.myUserId() }
            var message by remember { mutableStateOf<String?>(null) }
            var changedTick by remember { mutableIntStateOf(0) }

            fun back() { if (!nav.popBackStack()) finish() }
            fun openChat(roomId: Int, title: String) = ChatActivity.startRoom(this, roomId, title)
            fun create() {
                if (!api.isLoggedIn) { message = com.muyeon.app.ui.common.market.loginRequired("openStudioTransferEditor"); return }
                if (!canPost(this)) { message = "양도 글은 학원·강사 유형에서 올릴 수 있어요. MY > 회원유형에서 전환해 주세요."; return }
                nav.navigate("editor/0")
            }

            NavHost(nav, startDestination = start) {
                composable("list") {
                    TransferListScreen(
                        api = api, state = listState, canCreate = canPost(this@TransferActivity), myUserId = myId,
                        onBack = { back() }, onOpen = { nav.navigate("detail/$it") }, onCreate = { create() },
                        onChatRoom = ::openChat, onMessage = { message = it },
                    )
                }
                composable("detail/{id}", arguments = listOf(navArgument("id") { type = NavType.IntType; defaultValue = if (route == "detail") startId else 0 })) { e ->
                    val id = e.arguments?.getInt("id") ?: 0
                    TransferDetailScreen(
                        api = api, id = id, onBack = { back() }, onEdit = { nav.navigate("editor/$it") },
                        onChatRoom = ::openChat, onChanged = { changedTick++ }, onMessage = { message = it },
                        // 시작 화면(웹·로그인 후 다시 열기)으로 연 그 글에만 이어서 할 일을 넘긴다.
                        resumeAction = intent.getStringExtra(EXTRA_THEN)?.takeIf { it.isNotEmpty() && route == "detail" && id == startId },
                    )
                }
                composable("editor/{id}", arguments = listOf(navArgument("id") { type = NavType.IntType; defaultValue = if (route == "editor") startId else 0 })) { e ->
                    val id = e.arguments?.getInt("id")?.takeIf { it > 0 }
                    TransferWizardScreen(
                        api = api, editId = id, onClose = { back() },
                        onSaved = { savedId ->
                            changedTick++
                            message = if (id != null) "수정했어요." else "양도 글을 등록했어요."
                            // 위저드를 걷어내고 상세로. 수정은 이전 상세가 스택에 있으면 그 화면이 다시 읽는다.
                            if (id != null && nav.previousBackStackEntry?.destination?.route == "detail/{id}") nav.popBackStack()
                            else nav.navigate("detail/$savedId") { popUpTo("editor/{id}") { inclusive = true } }
                        },
                    )
                }
                composable("mine") {
                    MyTransfersScreen(
                        api = api, reloadTick = changedTick, onBack = { back() }, onOpen = { nav.navigate("detail/$it") },
                        onEdit = { nav.navigate("editor/$it") }, onCreate = { create() }, onChanged = { changedTick++ },
                        onMessage = { message = it },
                    )
                }
            }

            message?.let { msg -> com.muyeon.app.ui.common.market.MarketMessageDialog(msg) { message = null } }
        }
    }
}
