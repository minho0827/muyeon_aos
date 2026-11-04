package com.muyeon.app.ui.space

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
import com.muyeon.app.result.ResultKeys
import com.muyeon.app.result.launchScreen
import com.muyeon.app.ui.common.MuyeonDialog
import com.muyeon.app.ui.common.MuyeonDialogAction
import com.muyeon.app.webview.ActiveRole

/**
 * 공간 대관 진입점(2026-11-03) — 웹 브리지 openSpaces · openSpaceEditor{id?} · openMySpaces ·
 *  openMySpaceReservations(호스트 예약 관리) · openMySpaceBookings(내 예약). 상세는 기존 SpaceActivity.
 *  결과: SPACE·RESERVATIONS(웹 부모가 공간·예약 목록을 다시 읽는다).
 */
class SpaceHubActivity : ResultActivity() {

    override val defaultResultKeys = setOf(ResultKeys.SPACE, ResultKeys.RESERVATIONS)

    companion object {
        private const val EXTRA_ROUTE = "route"
        private const val EXTRA_ID = "id"

        private fun intent(context: Context, route: String, id: Int = 0) =
            Intent(context, SpaceHubActivity::class.java).putExtra(EXTRA_ROUTE, route).putExtra(EXTRA_ID, id)

        fun startList(context: Context) = context.launchScreen(intent(context, "list"))
        fun startEditor(context: Context, id: Int?) = context.launchScreen(intent(context, "editor", id ?: 0))
        fun startMine(context: Context) = context.launchScreen(intent(context, "mine"))
        fun startHosting(context: Context) = context.launchScreen(intent(context, "hosting"))
        fun startBookings(context: Context) = context.launchScreen(intent(context, "bookings"))

        /** 공간 등록은 학원·강사 활동유형(서버는 공간 사업자 SPACE 도 받지만 앱 진입은 스펙대로 둘만). */
        fun canHost(context: Context): Boolean = ActiveRole.current(context) in setOf(ActiveRole.ACADEMY, ActiveRole.TEACHER, ActiveRole.DANCER) // 공간 등록 = 학원원장·강사·무용수(11-03)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val route = intent.getStringExtra(EXTRA_ROUTE) ?: "list"
        val startId = intent.getIntExtra(EXTRA_ID, 0)
        val start = when (route) {
            // ★ startDestination 은 경로 '패턴'이어야 한다 — "editor/14" 처럼 값을 넣으면 인자가 안 넘어가 새 등록으로 열렸다(실기기 11-03).
            //   값은 navArgument 기본값(startId)으로 넘긴다.
            "editor" -> "editor/{id}"
            "mine", "hosting", "bookings" -> route
            else -> "list"
        }

        setContent {
            val nav = rememberNavController()
            com.muyeon.app.analytics.TrackNavigation("SpaceHub", nav)
            val api = remember { SpaceApi(this) }
            var message by remember { mutableStateOf<String?>(null) }
            var changedTick by remember { mutableIntStateOf(0) }

            fun back() { if (!nav.popBackStack()) finish() }
            fun openSpace(id: Int, date: String? = null) = SpaceActivity.start(this, id, date, false)
            fun create() {
                if (!api.isLoggedIn) { message = com.muyeon.app.ui.common.market.loginRequired("openSpaceEditor"); return }
                if (!canHost(this)) { message = "공간은 학원·강사 유형에서 등록할 수 있어요. MY > 회원유형에서 전환해 주세요."; return }
                nav.navigate("editor/0")
            }
            fun changed() { changedTick++; addResultKeys(ResultKeys.SPACE, ResultKeys.RESERVATIONS) }

            NavHost(nav, startDestination = start, popEnterTransition = com.muyeon.app.ui.common.nav.MuyeonNav.popEnter, popExitTransition = com.muyeon.app.ui.common.nav.MuyeonNav.popExit) {
                composable("list") {
                    SpaceListScreen(
                        api = api, canCreate = canHost(this@SpaceHubActivity), onBack = { back() },
                        onOpen = { id, date -> openSpace(id, date) }, onCreate = { create() }, onMessage = { message = it },
                    )
                }
                composable("editor/{id}", arguments = listOf(navArgument("id") { type = NavType.IntType; defaultValue = if (route == "editor") startId else 0 })) { e ->
                    val id = e.arguments?.getInt("id")?.takeIf { it > 0 }
                    SpaceWizardScreen(
                        api = api, editId = id, onClose = { back() },
                        onSaved = { savedId ->
                            changed()
                            message = if (id != null) "수정했어요." else "공간을 등록했어요."
                            // 위저드를 걷어내고 내 공간 목록으로 — 등록한 공간 상세도 바로 띄운다.
                            if (nav.previousBackStackEntry?.destination?.route == "mine") nav.popBackStack()
                            else nav.navigate("mine") { popUpTo("editor/{id}") { inclusive = true } }
                            if (id == null) openSpace(savedId)
                        },
                    )
                }
                composable("mine") {
                    SpaceMineScreen(
                        api = api, reloadTick = changedTick, onBack = { back() }, onOpen = { openSpace(it) },
                        onEdit = { nav.navigate("editor/$it") }, onCreate = { create() },
                        onReservations = { nav.navigate("hosting") }, onChanged = { changed() }, onMessage = { message = it },
                    )
                }
                composable("hosting") {
                    SpaceHostReservationsScreen(
                        api = api, onBack = { back() }, onChanged = { changed() }, onMessage = { message = it },
                        onChat = { roomId, title -> com.muyeon.app.ui.chat.ChatActivity.startRoom(this@SpaceHubActivity, roomId, title) },
                    )
                }
                composable("bookings") {
                    SpaceMyBookingsScreen(api = api, onBack = { back() }, onOpenSpace = { openSpace(it) }, onChanged = { changed() }, onMessage = { message = it })
                }
            }

            message?.let { msg -> com.muyeon.app.ui.common.market.MarketMessageDialog(msg) { message = null } }
        }
    }
}
