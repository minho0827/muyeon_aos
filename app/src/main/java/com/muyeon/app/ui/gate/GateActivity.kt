package com.muyeon.app.ui.gate

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.muyeon.app.navigation.Screen
import com.muyeon.app.theme.MuyeonTheme
import com.muyeon.app.ui.common.applyEdgeToEdge
import com.muyeon.app.ui.common.setEdgeContent
import com.muyeon.app.ui.main.MainActivity
import com.muyeon.app.utils.TokenManager
import com.muyeon.app.webview.VerifyGate
import com.muyeon.domain.repository.SessionController
import org.koin.android.ext.android.inject
import java.lang.ref.WeakReference

/**
 * ★ 2026-11-08 [b1] 앱 전체를 막는 안내 화면 — 웹 /verify-required(학원 인증 강제 차단) · /maintenance(시스템 점검).
 *  상단바·탭바 없이 단독(웹 #system_inspection). 차단 화면은 뒤로가기로 벗어나지 못한다(웹 AppRouter 가드 — 앱을 뒤로 보낼 뿐).
 *  차단 중에도 열 수 있는 공지·상담 채팅(웹 verifyGate ALLOWED)은 이 화면 안에 쌓는다 — 셸(MainActivity)로 나가면 VerifyGate 가 다시 막는다.
 *  [학원 인증하기] = 앱 인증 화면(OnboardingActivity 앱 완료 모드 — 서류 제출 뒤 VerifyGate.refresh, 승인되면 이 화면이 닫히고 홈).
 */
class GateActivity : ComponentActivity() {
    private val session: SessionController by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        applyEdgeToEdge()
        super.onCreate(savedInstanceState)
        val mode = intent.getStringExtra(EXTRA_MODE) ?: MODE_VERIFY
        if (mode == MODE_VERIFY) liveVerify = WeakReference(this)
        val errorJson = intent.getStringExtra(EXTRA_ERROR)
        setEdgeContent {
            MuyeonTheme(darkTheme = false, dynamicColor = false) {
                if (mode == MODE_MAINTENANCE) {
                    MaintenanceScreen(errorJson, onHome = ::goHomeFromMaintenance)
                    return@MuyeonTheme
                }
                val nav = rememberNavController()
                NavHost(nav, startDestination = ROUTE_GATE) {
                    composable(ROUTE_GATE) {
                        BackHandler { moveTaskToBack(true) }
                        VerifyRequiredScreen(
                            onNotice = { nav.navigate(Screen.Notices.route) },
                            onVerify = { com.muyeon.app.ui.onboarding.OnboardingActivity.startVerification(this@GateActivity, ACADEMY, completeInApp = true) },
                            onChat = { nav.navigate(Screen.SupportChatList.route) },
                            onLogout = ::logout,
                        )
                    }
                    // 차단 중 허용 화면(웹 verifyGate ALLOWED: /notice · /noticeDetail · /support/chat) — 셸과 같은 앱 화면을 그대로 쓴다.
                    composable(Screen.Notices.route) {
                        com.muyeon.app.ui.support.NoticeListScreen(
                            onBack = { nav.popBackStack() },
                            onOpen = { id -> nav.navigate(Screen.NoticeDetail.build(id)) },
                        )
                    }
                    composable(Screen.NoticeDetail.route, arguments = listOf(navArgument(Screen.NoticeDetail.ARG_ID) { type = NavType.StringType })) { e ->
                        val id = e.arguments?.getString(Screen.NoticeDetail.ARG_ID)?.toLongOrNull() ?: 0L
                        com.muyeon.app.ui.support.NoticeDetailScreen(
                            viewModel = org.koin.androidx.compose.koinViewModel(parameters = { org.koin.core.parameter.parametersOf(id) }),
                            onBack = { nav.popBackStack() },
                        )
                    }
                    composable(Screen.SupportChatList.route) {
                        com.muyeon.app.ui.support.SupportChatListScreen(
                            onBack = { nav.popBackStack() },
                            onOpenRoom = { id -> nav.navigate(Screen.SupportChatRoom.build(id)) },
                            onOpenPath = { p -> roomOf(p)?.let { nav.navigate(Screen.SupportChatRoom.build(it)) } },
                        )
                    }
                    composable(Screen.SupportChatRoom.route, arguments = listOf(navArgument(Screen.SupportChatRoom.ARG_ROOM) { type = NavType.StringType })) { e ->
                        val id = e.arguments?.getString(Screen.SupportChatRoom.ARG_ROOM)?.toLongOrNull() ?: 0L
                        com.muyeon.app.ui.support.SupportChatRoomScreen(
                            viewModel = org.koin.androidx.compose.koinViewModel(parameters = { org.koin.core.parameter.parametersOf(id) }),
                            onBack = { nav.popBackStack() },
                            onOpenPath = {},
                            onMy = { nav.popBackStack(ROUTE_GATE, inclusive = false) },
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 인증 화면·상담에서 돌아오면 바로 다시 묻는다(웹 VerifyRequired onRecheck) — 풀렸으면 VerifyGate 가 이 화면을 닫고 홈으로.
        if (intent.getStringExtra(EXTRA_MODE) != MODE_MAINTENANCE) VerifyGate.refresh(this)
    }

    override fun onDestroy() {
        if (liveVerify?.get() === this) liveVerify = null
        super.onDestroy()
    }

    /** 로그아웃(확인 뒤) — 웹: logout → /login. 차단 상태도 비운다(토큰 없음 = 차단 아님). */
    private fun logout() {
        session.logout()
        VerifyGate.refresh(this)
        com.muyeon.app.ui.auth.LoginActivity.start(this, null)
        finish()
    }

    /** [홈으로 이동] — 웹은 /login 으로 보낸다. 앱은 로그인돼 있으면 홈, 아니면 로그인 화면. */
    private fun goHomeFromMaintenance() {
        if (TokenManager.getAccessToken(this).isNullOrBlank()) com.muyeon.app.ui.auth.LoginActivity.start(this, null)
        else MainActivity.openPath(this, HOME)
        finish()
    }

    companion object {
        private const val EXTRA_MODE = "muyeon.gate.mode"
        private const val EXTRA_ERROR = "muyeon.gate.error"
        private const val MODE_VERIFY = "verify"
        private const val MODE_MAINTENANCE = "maintenance"
        private const val ROUTE_GATE = "gate"
        private const val ACADEMY = "ACADEMY"
        private const val HOME = "/home"

        @Volatile private var liveVerify: WeakReference<GateActivity>? = null

        /** 학원 인증 차단 화면 — 이미 떠 있으면 그 화면을 앞으로(쌓지 않는다). */
        fun startVerifyRequired(context: Context) {
            val i = Intent(context, GateActivity::class.java).putExtra(EXTRA_MODE, MODE_VERIFY)
                .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            if (context !is Activity) i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(i)
        }

        /** 차단이 풀렸다 — 차단 화면이 떠 있으면 닫고 홈으로(웹: 풀리면 홈). */
        fun closeVerifyRequired(context: Context) {
            val a = liveVerify?.get()?.takeIf { !it.isFinishing } ?: return
            MainActivity.openPath(context, HOME)
            a.finish()
        }

        /** 시스템 점검 화면 — [errorJson] = 웹 sessionStorage lastError(개발용 상자). */
        fun startMaintenance(context: Context, errorJson: String?) {
            val i = Intent(context, GateActivity::class.java).putExtra(EXTRA_MODE, MODE_MAINTENANCE).putExtra(EXTRA_ERROR, errorJson)
            if (context !is Activity) i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(i)
        }

        /** 상담 채팅 안의 링크 중 차단 중에도 열 수 있는 것(/support/chat?room=N)만. */
        private fun roomOf(path: String): Long? =
            path.takeIf { it.startsWith("/support/chat") }?.substringAfter("room=", "")?.substringBefore('&')?.toLongOrNull()

        /** VerifyGate 가 차단 중 남겨 둘 화면인가. */
        fun isGate(activity: Activity) = activity is GateActivity
    }
}
