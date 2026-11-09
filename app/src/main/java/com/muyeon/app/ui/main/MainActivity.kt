package com.muyeon.app.ui.main

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.muyeon.app.R
import com.muyeon.app.navigation.AppNavHost
import com.muyeon.app.navigation.AppResults
import com.muyeon.app.result.ResultHost
import com.muyeon.app.result.ResultKeys
import com.muyeon.app.result.resultKeys
import com.muyeon.app.result.resultPayload
import com.muyeon.app.theme.MuyeonTheme
import com.muyeon.app.ui.chat.ChatActivity
import com.muyeon.app.ui.floating.FloatingApi
import com.muyeon.app.ui.floating.FloatingOverlay
import com.muyeon.app.ui.floating.FloatingState
import com.muyeon.app.ui.onboarding.OnboardingActivity
import com.muyeon.app.utils.TokenManager
import com.muyeon.app.webview.ActiveRole
import com.muyeon.app.webview.ActiveTypeSheet
import com.muyeon.app.webview.WebViewActivity
import com.muyeon.domain.model.member.ActiveType
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * ★ 2026-11-07 앱 루트 — 하단 탭바 + 앱 탭 화면(홈·강사·커뮤니티·공연·MY). docs/ARCHITECTURE.md
 *
 *  · 웹 페이지(상세·글쓰기·로그인·구인구직 탭 …)는 이 위에 [WebViewActivity] 창으로 쌓인다.
 *    웹 창에서 앱 탭 주소(/home 등)로 가면 그 창을 닫고 이 화면의 탭으로 돌아온다([openTab]).
 *  · 푸시·링크 목적지: 앱 탭 주소면 탭을 고르고, 아니면 웹 창으로 연다([handleEntry]).
 *  · 이 화면이 띄운 네이티브 화면의 결과 키는 [AppResults] 로 탭 화면 ViewModel 에 알린다.
 */
class MainActivity : ComponentActivity(), ResultHost {

    private val viewModel: MainViewModel by viewModel()

    private val nativeResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val keys = result.data.resultKeys()
        AppResults.publish(keys)
        // 네이티브 화면이 '로그인 필요'로 닫혔다 → 앱 로그인 화면(마치면 하려던 화면·동작을 이어서 연다 — LoginResume).
        if (ResultKeys.LOGIN_REQUIRED in keys) {
            pendingAfterLogin = com.muyeon.app.ui.common.market.LoginResume.fromBundle(result.data.resultPayload()?.getBundle(ResultKeys.LOGIN_REQUIRED))
            loginLauncher.launch(com.muyeon.app.ui.auth.LoginActivity.resultIntent(this))
        }
        if (ResultKeys.CHAT_ROOMS in keys || ResultKeys.CHAT_ROOM in keys || ResultKeys.NOTIFICATIONS in keys) FloatingState.requestRefresh()
    }

    override fun launchForResult(intent: Intent) = nativeResultLauncher.launch(intent)

    /** 로그인 후 이어서 할 일(LoginResume) — 앱 로그인 화면이 성공으로 닫히면 한 번 연다. */
    private var pendingAfterLogin: com.muyeon.app.ui.common.market.LoginResume? = null
    private val loginLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val r = pendingAfterLogin
        pendingAfterLogin = null
        viewModel.onResume()
        if (result.resultCode == RESULT_OK && r != null) com.muyeon.app.webview.NativeActionRouter.open(this, r.action, r.paramsJson())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        liveCount++
        // 상태바·내비게이션 바까지 앱이 그린다(위 바 주황 · 탭바 흰색이 그 아래까지). 아이콘은 밝은 바탕용이 기본,
        //  내비게이션 바 회색 막(대비 보정)은 끈다 — 흰 탭바가 그대로 이어지게.
        enableEdgeToEdge(
            statusBarStyle = androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = androidx.activity.SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
        )
        if (android.os.Build.VERSION.SDK_INT >= 29) window.isNavigationBarContrastEnforced = false
        super.onCreate(savedInstanceState)
        ActiveRole.bind(this)
        // 앱이 켜져 있고 토큰이 있으면 네이티브 소켓을 늘 붙여 둔다 — 어느 화면이 떠 있어도 force-logout·배지를 듣는다.
        com.muyeon.app.chat.socket.ChatSocketLifecycleObserver.ensureAttached(this)
        com.muyeon.app.chat.socket.ChatSocketManager.connect(this)

        setContent {
            MuyeonTheme(darkTheme = false, dynamicColor = false) {
                Box(Modifier.fillMaxSize()) {
                    AppNavHost(mainViewModel = viewModel, onMyLongPress = ::showTypeSheet, onOpenWeb = ::openWeb, onOpenPath = { path -> openPath(path, viaLink = false) })
                    FloatingOverlay(
                        api = remember { FloatingApi { TokenManager.getAccessToken(this@MainActivity) } },
                        onOpenChatList = { ChatActivity.startList(this@MainActivity, "") },
                        // 인증 완료 '시작하기' — 그 유형으로 바꾸고 첫 화면으로(웹 __onRoleApproved: 전문 유형은 /jobs).
                        onRoleApproved = { role -> viewModel.onTypePicked(role) },
                    )
                    // 연결 실패·타임아웃 → '다시 시도'(웹 GlobalNetworkOverlay 와 같은 문구·버튼).
                    val retry by NetworkRetryHost.pending.collectAsStateWithLifecycle()
                    com.muyeon.app.common_components.dialog.CustomDialog(
                        title = getString(R.string.net_retry_title),
                        content = getString(R.string.net_retry_body),
                        leftButtonText = getString(R.string.common_close),
                        rightButtonText = getString(R.string.common_retry),
                        buttonCount = 2,
                        alignment = com.muyeon.app.common_components.dialog.ContentAlignment.Middle,
                        onDismiss = { NetworkRetryHost.resolve(false) },
                        onLeftButtonClick = { NetworkRetryHost.resolve(false) },
                        onRightButtonClick = { NetworkRetryHost.resolve(true) },
                        showPopup = retry,
                    )
                    com.muyeon.app.common_components.dialog.CustomDialog(
                        title = getString(R.string.main_exit_title),
                        content = null,
                        leftButtonText = getString(R.string.common_cancel),
                        rightButtonText = getString(R.string.main_exit_confirm),
                        buttonCount = 2,
                        alignment = com.muyeon.app.common_components.dialog.ContentAlignment.Middle,
                        onDismiss = { showExit = false },
                        onLeftButtonClick = { showExit = false },
                        onRightButtonClick = { showExit = false; finish() },
                        showPopup = showExit,
                    )
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.events.collect(::onEvent) }
                launch {
                    viewModel.state.collect { s ->
                        // 플로팅(채팅 버튼) 표시 판정은 웹 라우트 기준이다 — 앱 탭도 같은 주소를 알린다.
                        if (!FloatingState.currentRoute.startsWith("/community/post") && !FloatingState.currentRoute.startsWith("/performance/") && !FloatingState.currentRoute.startsWith("/stage/"))
                            MainTab.fromKey(s.selectedKey)?.let { FloatingState.updateRoute(it.webPath) }
                    }
                }
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // 2계층(구인구직·커뮤니티·공간)이면 1계층으로, 홈이 아니면 홈으로, 홈이면 종료 확인.
                if (viewModel.backToMain()) return
                if (viewModel.state.value.selectedKey != MainTab.HOME.key) viewModel.selectTab(MainTab.HOME)
                else confirmExit()
            }
        })

        if (savedInstanceState == null) handleEntry(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleEntry(intent)
    }

    override fun onStart() {
        super.onStart()
        // 앱을 켰다·앞으로 돌아왔다 — 공연 칸 새 글 점.
        viewModel.onForeground()
    }

    override fun onPause() {
        NetworkRetryHost.hostVisible = false
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        NetworkRetryHost.hostVisible = true
        // 웹 창에서 로그인·로그아웃·유형 전환을 하고 돌아왔을 수 있다.
        viewModel.onResume()
        viewModel.detectRegionIfNeeded { DeviceLocation.lastKnown(this) }
        FloatingState.requestRefresh()
        ActiveRole.syncFromServer(this)
        if (!FloatingState.currentRoute.startsWith("/community/post") && !FloatingState.currentRoute.startsWith("/performance/") && !FloatingState.currentRoute.startsWith("/stage/"))
            MainTab.fromKey(viewModel.state.value.selectedKey)?.let { FloatingState.updateRoute(it.webPath) }
    }

    override fun onDestroy() {
        liveCount--
        super.onDestroy()
    }

    /** 들어온 목적지 — 탭 지정 / 웹 경로(네이티브 화면 복귀·푸시·링크). */
    private fun handleEntry(intent: Intent) {
        intent.getStringExtra(EXTRA_TAB)?.let { key ->
            intent.removeExtra(EXTRA_TAB)
            MainTab.fromKey(key)?.let(viewModel::selectTab)
        }
        if (intent.getStringExtra(EXTRA_NOTIFICATION_URL).isNullOrEmpty() && com.muyeon.app.utils.AppLinkManager.consumeInto(intent)) {
            com.muyeon.app.analytics.Analytics.deeplinkOpen(intent.getStringExtra("type"), intent.getStringExtra(EXTRA_NOTIFICATION_URL))
            intent.getStringExtra(EXTRA_NOTIFICATION_URL)?.let { openPath(it, viaLink = true) }
            intent.removeExtra(EXTRA_NOTIFICATION_URL)
            return
        }
        intent.getStringExtra(EXTRA_NOTIFICATION_URL)?.takeIf { it.isNotEmpty() }?.let { url ->
            intent.removeExtra(EXTRA_NOTIFICATION_URL)
            com.muyeon.app.analytics.Analytics.pushOpen(intent.getStringExtra("type"), url)
            // ★ 2026-11-08 푸시 data 우선(iOS CustomAppDelegate.routePushPayload 와 같다) —
            //  lessonId(레슨 일정·변경·취소·리마인더) → 그 레슨 상세, reviewTeacherId(후기 도착) → 받은 후기.
            val lessonId = intent.getStringExtra("lessonId")?.toIntOrNull()?.also { intent.removeExtra("lessonId") }
            val reviewTeacherId = intent.getStringExtra("reviewTeacherId")?.toIntOrNull()?.also { intent.removeExtra("reviewTeacherId") }
            when {
                lessonId != null && lessonId > 0 -> com.muyeon.app.ui.lesson.LessonActivity.startDetail(this, lessonId)
                reviewTeacherId != null && reviewTeacherId > 0 -> com.muyeon.app.ui.resume.ResumeActivity.startReviewList(this, reviewTeacherId)
                else -> openPath(url, viaLink = false)
            }
        }
        if (intent.getBooleanExtra(EXTRA_STAGE, false)) {
            intent.removeExtra(EXTRA_STAGE)
            val route = intent.getStringExtra(EXTRA_STAGE_ROUTE).also { intent.removeExtra(EXTRA_STAGE_ROUTE) }
            openCastingStage(route)
        }
        intent.getStringExtra(EXTRA_PATH)?.takeIf { it.isNotEmpty() }?.let { path ->
            intent.removeExtra(EXTRA_PATH)
            openPath(path, viaLink = false)
        }
    }

    /** 경로 열기 — 앱 탭 주소면 탭, 아니면 웹 창. 다른 사이트 주소는 웹 창이 그대로 연다. */
    /** 웹 창 열기 — 단, 웹이 열리자마자 네이티브로 넘기는 주소(이력서 관리·레슨 일정)는 웹 창 없이 바로(두 겹 방지). */
    private fun openWeb(path: String) {
        // ★ 2026-11-08 웹 로그인 주소(/login?redirect=…) — 앱 로그인 화면으로(셸의 login 액션·MY 비회원 [로그인] 이 모두 이 길).
        if (com.muyeon.app.ui.auth.LoginActivity.isLoginPath(path)) { com.muyeon.app.ui.auth.LoginActivity.startFromPath(this, path); return }
        val t = com.muyeon.app.navigation.NativeRoutes.resolve(path)
        if (t is com.muyeon.app.navigation.NativeRoutes.Target.Action &&
            com.muyeon.app.webview.NativeActionRouter.open(this, t.action, org.json.JSONObject(t.data))) return
        // ★ 2026-11-08 탭 위 앱 화면(공지·FAQ·찜 … MY 메뉴에서 옮긴 화면)도 웹 창 없이 바로.
        if (t is com.muyeon.app.navigation.NativeRoutes.Target.Route) { openRoute(t, path); return }
        WebViewActivity.startPath(this, path)
    }

    private fun openPath(urlOrPath: String, viaLink: Boolean) {
        val base = com.muyeon.app.utils.Constants.getBaseUrl(this).trimEnd('/')
        val path = if (urlOrPath.startsWith(base)) urlOrPath.removePrefix(base).ifEmpty { "/" } else urlOrPath
        if (com.muyeon.app.ui.auth.LoginActivity.isLoginPath(path)) { com.muyeon.app.ui.auth.LoginActivity.startFromPath(this, path); return }
        when (val target = com.muyeon.app.navigation.NativeRoutes.resolve(path)) {
            is com.muyeon.app.navigation.NativeRoutes.Target.Tab -> { viewModel.selectTab(target.tab); return }
            is com.muyeon.app.navigation.NativeRoutes.Target.Route -> { openRoute(target, path); return }
            is com.muyeon.app.navigation.NativeRoutes.Target.Action -> {
                if (com.muyeon.app.webview.NativeActionRouter.open(this, target.action, org.json.JSONObject(target.data))) return
            }
            null -> Unit
        }
        WebViewActivity.startPath(this, path, viaLink = viaLink)
    }

    /** 탭 위 앱 화면 — 로그인해야 보는 화면인데 비회원이면 앱 로그인 화면부터(마치면 [path] 로 openPath — 그 주소가 다시 이 화면을 연다). */
    private fun openRoute(t: com.muyeon.app.navigation.NativeRoutes.Target.Route, path: String) {
        if (t.auth && TokenManager.getAccessToken(this).isNullOrBlank()) {
            com.muyeon.app.ui.auth.LoginActivity.start(this, path)
            return
        }
        viewModel.openRoute(t.route)
    }

    private fun onEvent(e: MainEvent) {
        when (e) {
            is MainEvent.OpenWebPath -> openPath(e.path, viaLink = false)
            is MainEvent.TypeSwitched -> {
                Toast.makeText(this, getString(R.string.main_type_switched, ActiveRole.label(e.type)), Toast.LENGTH_SHORT).show()
                // 전문 유형으로 바뀌면 첫 화면은 홈(웹은 그 자리에 머문다 — 강사 탭은 새 목록에 없어 홈으로 간다).
            }
            MainEvent.TypeSwitchFailed -> Toast.makeText(this, R.string.main_type_switch_failed, Toast.LENGTH_SHORT).show()
            is MainEvent.Message -> Toast.makeText(this, e.text, Toast.LENGTH_SHORT).show()
            is MainEvent.OpenRoleManage -> OnboardingActivity.startRoleManage(this, e.launch.rolesJson, e.launch.heroImage, e.activeType)
        }
    }

    /**
     * 캐스팅·무대 열기 — 커뮤니티 2계층의 캐스팅 탭(모든 회원에게 있다).
     *  [route] 링크로 열 그 화면(CastingStageActivity.linkRoute 값) — 그 위에 쌓는다.
     */
    private fun openCastingStage(route: String?) {
        val r = com.muyeon.app.ui.castingstage.StageRoutes.SHELL
        viewModel.selectTab(MainTab.CASTING)
        route?.let { viewModel.openRoute(r.link(it)) }
    }

    /** MY 길게 누르기 — 내 유형 시트(웹 ActiveTypeSheet 와 같은 줄 구성). */
    private fun showTypeSheet() {
        val s = viewModel.state.value
        if (!s.isLoggedIn || s.switchableTypes.isEmpty()) return
        val rows = s.switchableTypes.map { it to getString(typeSheetLabel(it)) }
        ActiveTypeSheet.showNative(this, rows, s.activeType, emptySet(), viewModel::onTypePicked)
    }

    /** 종료 확인 — 웹뷰 시절과 같은 무용연 팝업(CustomDialog). */
    private var showExit by androidx.compose.runtime.mutableStateOf(false)

    private fun confirmExit() { showExit = true }

    companion object {
        const val EXTRA_TAB = "muyeon.main.tab"
        const val EXTRA_PATH = "muyeon.main.path"
        /** 푸시·링크 목적지(기존 WebViewActivity 와 같은 키 — SplashActivity·FCM 이 이 키로 싣는다). */
        const val EXTRA_NOTIFICATION_URL = "notification_url"

        @Volatile private var liveCount = 0
        val isAlive: Boolean get() = liveCount > 0

        fun typeSheetLabel(type: String): Int = when (type) {
            ActiveType.TEACHER -> R.string.type_sheet_teacher
            ActiveType.ACADEMY -> R.string.type_sheet_academy
            ActiveType.DANCER -> R.string.type_sheet_dancer
            ActiveType.TEAM -> R.string.type_sheet_team
            else -> R.string.type_sheet_general
        }

        private fun intent(context: Context) = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)

        /** 앱 탭으로 — 위에 쌓인 창(웹·네이티브)을 닫는다. 앱이 꺼져 있었으면 새로 띄운다. */
        fun openTab(context: Context, tab: MainTab) {
            val i = intent(context).putExtra(EXTRA_TAB, tab.key)
            if (!isAlive) i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            context.startActivity(i)
        }

        /** 경로로 — 앱 탭 주소면 탭, 아니면 웹 창(위 창들은 닫힌다). NativeWebRoute 가 쓴다. */
        fun openPath(context: Context, path: String) {
            val i = intent(context).putExtra(EXTRA_PATH, path)
            if (!isAlive) i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            context.startActivity(i)
        }

        const val EXTRA_STAGE = "muyeon.main.stage"
        const val EXTRA_STAGE_ROUTE = "muyeon.main.stage.route"

        /** 캐스팅·무대(웹 openCastingStage · 다른 화면 링크) — 셸 안 화면으로 연다. 위에 쌓인 창은 닫힌다. */
        fun openCastingStage(context: Context, route: String?) {
            val i = intent(context).putExtra(EXTRA_STAGE, true).putExtra(EXTRA_STAGE_ROUTE, route)
            if (!isAlive) i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            context.startActivity(i)
        }

        /** 앱 시작(SplashRouter) — 푸시·링크 extras 를 그대로 옮겨 싣는다. */
        fun launch(context: Context, extras: Bundle?) {
            val i = Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            extras?.let(i::putExtras)
            context.startActivity(i)
        }
    }
}
