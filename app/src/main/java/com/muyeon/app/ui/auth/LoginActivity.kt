package com.muyeon.app.ui.auth

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.muyeon.app.R
import com.muyeon.app.auth.AuthSession
import com.muyeon.app.auth.LoginProvider
import com.muyeon.app.common_components.dialog.ContentAlignment
import com.muyeon.app.common_components.dialog.CustomDialog
import com.muyeon.app.theme.MuyeonTheme
import com.muyeon.app.ui.common.applyEdgeToEdge
import com.muyeon.app.ui.common.setEdgeContent
import com.muyeon.app.ui.main.MainActivity
import com.muyeon.app.ui.onboarding.SignupTermsScreen
import com.muyeon.app.webview.WebViewActivity
import com.muyeon.domain.model.auth.LoginSession
import com.muyeon.domain.repository.SessionController
import com.muyeon.domain.usecase.auth.AgreeTermsUseCase
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

/**
 * ★ 2026-11-08 로그인 앱 화면 — 웹 /login(pages/login/Login.js)·/accountRecovery 를 대신한다.
 *
 *  들어오는 길(전부 [start] / [resultIntent]): 탭 위 화면(Route auth=true)·MY 비회원 [로그인]·셸의 login 액션(웹 주소 /login 을 가로챔),
 *   웹 창 안 /login 이동(WebViewActivity 가 가로챔), 네이티브 화면 '로그인 필요'(LoginResume), 강제 로그아웃(ForceLogout).
 *  성공하면: [AuthSession.onNativeLogin](토큰·웹 저장소·소켓·유형·결제·기기) → 약관 게이트(필요 시 SignupTermsScreen)
 *   → [resultIntent] 로 열렸으면 RESULT_OK 로 닫는다(부르는 쪽이 이어서 할 일을 연다), 아니면 redirect 주소(없으면 홈)로
 *   [MainActivity.openPath] — 셸이 다시 보이며 MainViewModel.onResume 이 로그인 상태를 다시 읽는다.
 *  소셜(카카오·네이버)은 SDK 가 없어 웹 OAuth 창으로 연다([WebViewActivity.startSocialLogin]) — 웹이 토큰을 넘기면
 *   ([AuthSession.webLoggedIn]) 이 화면은 닫힌다.
 *  ★ 2026-11-08 [b1] 회원가입도 앱 화면 — 약관 동의(가입 모드, 웹 /signupTerms?mode=signup) → 이메일 가입(/register) →
 *   완료 팝업 → 회원유형 온보딩(OnboardingActivity 앱 완료 모드 — 웹 __onRoleComplete·ROLE_LANDING 과 같은 이동).
 *   소셜 신규·추가정보 미완료는 /social-signup 앱 화면. 웹 창이 이 세 주소로 가면 WebViewActivity 가 [startSignup] 으로 넘긴다.
 *   휴대폰 본인확인 표준창만 앱 안 웹창(IdentityWebView).
 */
class LoginActivity : ComponentActivity() {
    private val agreeTerms: AgreeTermsUseCase by inject()
    private val session: SessionController by inject()

    private val redirect: String? get() = intent.getStringExtra(EXTRA_REDIRECT)?.takeIf { it.isNotBlank() }
    private val returnResult: Boolean get() = intent.getBooleanExtra(EXTRA_RETURN_RESULT, false)

    private var notice by mutableStateOf<Pair<String?, String?>?>(null)

    // ★ [b1] 가입 — 약관 화면 동의값(웹 sessionStorage 캐리 대신) · 소셜 추가정보에 넘길 로그인 정보(웹 저장소 갱신용).
    private var signupAgreements: Map<String, Boolean> = emptyMap()
    private var socialSessionId: String? = null
    private var socialUserName: String? = null
    private val startScreen: String? get() = intent.getStringExtra(EXTRA_START)

    override fun onCreate(savedInstanceState: Bundle?) {
        liveCount++
        applyEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            intent.getStringExtra(EXTRA_NOTICE_CODE)?.let { code -> notice = code to intent.getStringExtra(EXTRA_NOTICE_MESSAGE) }
        }
        // 웹 창(소셜·이메일 가입)에서 로그인이 끝났다 — 그 창이 다음 화면으로 간다. 이 화면은 닫는다.
        lifecycleScope.launch {
            AuthSession.webLoggedIn.collect {
                setResult(Activity.RESULT_OK)
                finish()
            }
        }
        setEdgeContent {
            MuyeonTheme(darkTheme = false, dynamicColor = false) {
                val nav = rememberNavController()
                navRef = nav
                val start = when (startScreen) {
                    START_SOCIAL -> ROUTE_SOCIAL
                    START_SIGNUP -> ROUTE_SIGNUP_TERMS
                    else -> ROUTE_LOGIN
                }
                NavHost(nav, startDestination = start) {
                    composable(ROUTE_LOGIN) {
                        LoginScreen(
                            onBack = ::finish,
                            onLoggedIn = { s -> onLoggedIn(s, nav) },
                            onFindId = { nav.navigate(recoveryRoute(AccountRecoveryViewModel.TYPE_ID)) },
                            onFindPassword = { nav.navigate(recoveryRoute(AccountRecoveryViewModel.TYPE_PW)) },
                            onSocial = ::openSocial,
                            onSignupEmail = ::openSignupEmail,
                            showNaver = NAVER_LOGIN_ENABLED,
                        )
                    }
                    composable("$ROUTE_RECOVERY/{type}") { e ->
                        AccountRecoveryScreen(
                            type = e.arguments?.getString("type").orEmpty(),
                            onBack = { nav.popBackStack() },
                            onGoLogin = { nav.popBackStack(ROUTE_LOGIN, inclusive = false) },
                        )
                    }
                    composable(ROUTE_TERMS) {
                        // 동의 안 함·뒤로 = 로그아웃 후 로그인 화면(웹 __onSignupTermsDeclined, mode gate).
                        val decline = { declineTerms(nav) }
                        BackHandler(onBack = decline)
                        SignupTermsScreen(onAgree = ::agree, onDecline = decline, onOpenPolicy = ::openPolicy)
                    }
                    // ★ [b1] 가입 모드 약관(웹 SignupTerms mode=signup) — 동의 → 가입 화면 / 뒤로 → 로그인(레슨 요청 중이면 그 화면).
                    composable(ROUTE_SIGNUP_TERMS) {
                        val cancel = { cancelSignupTerms(nav) }
                        BackHandler(onBack = cancel)
                        SignupTermsScreen(
                            onAgree = { m -> signupAgreements = m; nav.navigate(ROUTE_REGISTER) },
                            onDecline = cancel, onOpenPolicy = ::openPolicy,
                        )
                    }
                    composable(ROUTE_REGISTER) {
                        com.muyeon.app.ui.auth.signup.RegisterScreen(
                            agreements = signupAgreements,
                            onBack = { if (!nav.popBackStack()) finish() },
                            onRegistered = { s -> AuthSession.onNativeLogin(this@LoginActivity, s, LoginProvider.EMAIL) },
                            onDone = ::afterSignup,
                        )
                    }
                    composable(ROUTE_SOCIAL) {
                        com.muyeon.app.ui.auth.signup.SocialSignupScreen(
                            currentName = socialUserName ?: intent.getStringExtra(EXTRA_USER_NAME),
                            onQuit = ::quitSocialSignup,
                            onCompleted = { userJson ->
                                // 웹 창 저장소의 user 도 바꾼다(signupIncomplete=false) — 안 바꾸면 웹 게이트가 다시 /social-signup 으로 보낸다.
                                val sid = socialSessionId ?: intent.getStringExtra(EXTRA_SESSION_ID)
                                com.muyeon.app.webview.WebLogin.save(this@LoginActivity, sid, userJson, LoginProvider.last(this@LoginActivity) ?: LoginProvider.EMAIL)
                            },
                            onDone = ::afterSignup,
                        )
                    }
                }
                notice?.let { (code, message) ->
                    val close = { notice = null }
                    CustomDialog(
                        title = stringResource(noticeTitle(code)),
                        content = message?.takeIf { it.isNotBlank() } ?: stringResource(R.string.auth_login_notice_body),
                        leftButtonText = null, rightButtonText = stringResource(R.string.auth_login_confirm),
                        buttonCount = 1, alignment = ContentAlignment.Middle,
                        onDismiss = close, onRightButtonClick = close, showPopup = true,
                    )
                }
            }
        }
    }

    private var navRef: NavHostController? = null

    override fun onDestroy() {
        liveCount--
        super.onDestroy()
    }

    /** 로그인 성공 — 후처리 뒤 게이트(웹 AppRouter: 소셜 가입 미완료 → 약관 미동의) → 다음 화면. */
    private fun onLoggedIn(s: LoginSession, nav: NavHostController) {
        AuthSession.onNativeLogin(this, s, LoginProvider.EMAIL)
        when {
            // 소셜 가입을 끝내지 않은 계정(이메일 로그인은 해당 없음) — 앱 추가정보 화면으로([b1]).
            s.signupIncomplete -> {
                socialSessionId = s.sessionId
                socialUserName = userNameOf(s.userJson)
                nav.navigate(ROUTE_SOCIAL) { popUpTo(ROUTE_LOGIN) { inclusive = true } }
            }
            s.termsAgreed == false -> nav.navigate(ROUTE_TERMS)
            else -> finishLoggedIn()
        }
    }

    /** 약관 동의 — 저장 실패해도 넘어간다(웹 __onSignupTermsAgreed 와 같다. 다음 진입 때 서버가 다시 묻는다). */
    private fun agree(agreements: Map<String, Boolean>) {
        lifecycleScope.launch {
            agreeTerms(agreements)
            finishLoggedIn()
        }
    }

    private fun declineTerms(nav: NavHostController) {
        session.logout()
        nav.popBackStack(ROUTE_LOGIN, inclusive = false)
    }

    private fun finishLoggedIn() {
        setResult(Activity.RESULT_OK)
        if (!returnResult) MainActivity.openPath(this, redirect ?: HOME)
        finish()
    }

    private fun openSocial(provider: LoginProvider) = WebViewActivity.startSocialLogin(this, provider, redirect ?: HOME)

    /** [이메일로 가입하기] — 앱 약관 동의(가입 모드) → 앱 가입 화면([b1]). */
    private fun openSignupEmail() = navRef?.navigate(ROUTE_SIGNUP_TERMS)

    /** 가입 모드 약관 [뒤로] — 웹: 레슨 요청 중 가입이면 그 화면, 아니면 /login. */
    private fun cancelSignupTerms(nav: NavHostController) {
        val r = redirect
        if (r != null && r.startsWith("/quoteRequest")) {
            MainActivity.openPath(this, r)
            finish()
            return
        }
        if (!nav.popBackStack(ROUTE_LOGIN, inclusive = false)) {
            nav.navigate(ROUTE_LOGIN) { popUpTo(ROUTE_SIGNUP_TERMS) { inclusive = true } }
        }
    }

    /**
     * 가입 완료 팝업 [확인] — 웹 Register/SocialSignup closeModal:
     *  광고 공고·레슨 요청 중 가입 → 유형 선택 없이 하던 일로 / 그 밖 → 회원유형 온보딩(앱 완료 모드 — 선택 뒤 유형별 화면).
     */
    private fun afterSignup() {
        setResult(Activity.RESULT_OK)
        val r = com.muyeon.domain.usecase.auth.SignupRedirect.safe(redirect)
        if (com.muyeon.domain.usecase.auth.SignupRedirect.isTaskResume(r)) MainActivity.openPath(this, r!!)
        else com.muyeon.app.ui.onboarding.OnboardingActivity.startRoleOnboarding(this, completeInApp = true)
        finish()
    }

    /** 소셜 추가정보 [뒤로] = 가입 그만두기 — 로그아웃 후 홈(웹 SocialSignup: logout → /home). */
    private fun quitSocialSignup() {
        session.logout()
        MainActivity.openPath(this, HOME)
        finish()
    }

    /** 약관 전문 — 외부 브라우저(OnboardingActivity.openPolicy 와 같다). */
    private fun openPolicy(doc: String) {
        val base = com.muyeon.app.utils.Constants.getBaseUrl(this).trimEnd('/')
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("$base/policy?doc=$doc&embed=1"))) }
    }

    companion object {
        private const val EXTRA_REDIRECT = "muyeon.login.redirect"
        private const val EXTRA_RETURN_RESULT = "muyeon.login.returnResult"
        private const val EXTRA_NOTICE_CODE = "muyeon.login.noticeCode"
        private const val EXTRA_NOTICE_MESSAGE = "muyeon.login.noticeMessage"
        private const val ROUTE_LOGIN = "login"
        private const val ROUTE_RECOVERY = "recovery"
        private const val ROUTE_TERMS = "terms"
        // ★ [b1] 가입
        private const val ROUTE_SIGNUP_TERMS = "signupTerms"
        private const val ROUTE_REGISTER = "register"
        private const val ROUTE_SOCIAL = "socialSignup"
        private const val EXTRA_START = "muyeon.login.start"
        private const val EXTRA_SESSION_ID = "muyeon.login.sessionId"
        private const val EXTRA_USER_NAME = "muyeon.login.userName"
        private const val START_SIGNUP = "signup"
        private const val START_SOCIAL = "social"
        private val SIGNUP_PATHS = setOf("/signupTerms", "/register")
        private const val SOCIAL_PATH = "/social-signup"
        private const val HOME = "/home"
        private const val LOGIN_PATH = "/login"

        /**
         * 네이버 로그인 버튼 — 웹 REACT_APP_NAVER_LOGIN_ENABLED(운영 false: 네이버 검수 전)와 같게 둔다.
         *  검수 승인 뒤 웹과 함께 true 로.
         */
        const val NAVER_LOGIN_ENABLED = false

        @Volatile private var liveCount = 0
        /** 로그인 화면이 떠 있다 — 웹 창이 /login 으로 가면 새로 띄우지 않고 그 창만 닫는다. */
        val isAlive: Boolean get() = liveCount > 0

        private fun recoveryRoute(type: String) = "$ROUTE_RECOVERY/$type"

        private fun noticeTitle(code: String?) = when (code) {
            "SESSION_REPLACED" -> R.string.auth_login_notice_session_replaced
            "ACCOUNT_SUSPENDED" -> R.string.auth_login_notice_suspended
            "ACCOUNT_WITHDRAWN" -> R.string.auth_login_notice_withdrawn
            else -> R.string.auth_login_notice_default
        }

        /** 웹 로그인 주소인가(/login, /login?redirect=…). */
        fun isLoginPath(path: String?): Boolean {
            val p = path?.substringBefore('#')?.substringBefore('?')?.trimEnd('/') ?: return false
            return p == LOGIN_PATH
        }

        /** /login?redirect=… 의 redirect(풀어서). 없으면 null. */
        fun redirectOf(path: String?): String? =
            path?.substringBefore('#')?.substringAfter('?', "")?.split('&')
                ?.firstOrNull { it.startsWith("redirect=") }?.substringAfter('=')
                ?.let(Uri::decode)?.takeIf { it.startsWith("/") && !isLoginPath(it) }

        /** 로그인 화면 — 마치면 [redirect](없으면 홈)로. [noticeCode]·[noticeMessage] = 강제 로그아웃 안내. */
        fun start(context: Context, redirect: String?, noticeCode: String? = null, noticeMessage: String? = null) {
            context.startActivity(intent(context, redirect, noticeCode, noticeMessage).apply { if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
        }

        /** 웹 주소 /login?redirect=… 로 열기. */
        fun startFromPath(context: Context, loginPath: String) = start(context, redirectOf(loginPath))

        fun intent(context: Context, redirect: String?, noticeCode: String? = null, noticeMessage: String? = null): Intent =
            Intent(context, LoginActivity::class.java)
                .putExtra(EXTRA_REDIRECT, redirect)
                .apply { noticeCode?.let { putExtra(EXTRA_NOTICE_CODE, it); putExtra(EXTRA_NOTICE_MESSAGE, noticeMessage) } }

        /**
         * ★ [b1] 가입 주소인가 — /signupTerms(가입 모드 · mode=gate 는 기존 약관 게이트라 제외) · /register · /social-signup.
         *  [query] 는 '?' 뒤(없으면 빈 글자).
         */
        fun isSignupPath(path: String?, query: String = ""): Boolean {
            val p = path?.substringBefore('#')?.substringBefore('?')?.trimEnd('/') ?: return false
            if (p == SOCIAL_PATH) return true
            if (p !in SIGNUP_PATHS) return false
            val q = query.ifEmpty { path.substringAfter('?', "") }
            return !(p == "/signupTerms" && q.split('&').any { it == "mode=gate" })
        }

        /**
         * 가입 화면 열기 — /signupTerms·/register = 약관 동의(가입 모드)부터(동의값이 있어야 가입된다),
         *  /social-signup = 소셜 추가정보([sessionId]·[userName] = 웹 저장소의 로그인 — 저장 뒤 웹 저장소 user 를 바꿀 때 쓴다).
         */
        fun startSignup(context: Context, path: String, sessionId: String? = null, userName: String? = null) {
            val p = path.substringBefore('#').substringBefore('?').trimEnd('/')
            val r = redirectOf(path)
            context.startActivity(
                intent(context, r)
                    .putExtra(EXTRA_START, if (p == SOCIAL_PATH) START_SOCIAL else START_SIGNUP)
                    .putExtra(EXTRA_SESSION_ID, sessionId)
                    .putExtra(EXTRA_USER_NAME, userName)
                    .apply { if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) },
            )
        }

        /** user JSON 의 name(소셜 기본 이름 판단용). */
        internal fun userNameOf(userJson: String?): String? =
            runCatching { org.json.JSONObject(userJson ?: "").optString("name") }.getOrNull()?.takeIf { it.isNotBlank() }

        /** 결과로 받기 — 성공이면 RESULT_OK 로 닫힌다(부르는 쪽이 LoginResume 등 이어서 할 일을 연다). */
        fun resultIntent(context: Context): Intent =
            Intent(context, LoginActivity::class.java).putExtra(EXTRA_RETURN_RESULT, true)
    }
}
