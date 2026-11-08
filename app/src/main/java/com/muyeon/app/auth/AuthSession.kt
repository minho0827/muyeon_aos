package com.muyeon.app.auth

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.muyeon.app.chat.socket.ChatSocketLifecycleObserver
import com.muyeon.app.chat.socket.ChatSocketManager
import com.muyeon.app.chat.socket.ForceLogout
import com.muyeon.app.utils.TokenManager
import com.muyeon.app.webview.ActiveRole
import com.muyeon.app.webview.WebLogin
import com.muyeon.domain.model.auth.LoginSession
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * ★ 2026-11-08 로그인 직후 처리 한 곳 — 웹 로그인(AndroidToken.saveToken)과 앱 로그인 화면(LoginActivity)이 같이 쓴다.
 *
 *  순서(웹 AuthContext.login + TokenWebViewInterface.saveToken 과 같다):
 *   ① 토큰 저장(TokenManager — 웹 syncNativeAfterAuth 처럼 refresh 자리에도 access 토큰)
 *   ② 웹 창 저장소에 같은 로그인(localStorage muyeonAuth · 최근 로그인 — WebLogin)   ← 앱 로그인만
 *   ③ 강제 로그아웃 다시 받기(ForceLogout.reset)
 *   ④ 네이티브 소켓을 새 토큰으로(ChatSocketLifecycleObserver · ChatSocketManager)
 *   ⑤ 서버 활성유형으로 X-Active-Type 맞춤(ActiveRole.syncFromServer)
 *   ⑥ 로그아웃 상태에서 결제했거나 못 보낸 구글 구매 보내기(MembershipBilling.flushUnfinished)
 *   ⑦ 기기·푸시 토큰을 회원으로 저장(웹 setDeviceInfoSvc)                            ← 앱 로그인만(웹은 웹이 보낸다)
 *  그다음(부르는 쪽): 약관 동의 게이트 → 로그인 후 이어서 할 일(LoginResume) 또는 redirect 주소.
 */
object AuthSession {
    private val _webLoggedIn = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** 웹 창에서 로그인·가입이 끝났다(소셜·이메일 가입) — 아래에 깔린 앱 로그인 화면이 닫힌다. */
    val webLoggedIn: SharedFlow<Unit> = _webLoggedIn.asSharedFlow()

    /** 앱 로그인 화면에서 로그인 성공. */
    fun onNativeLogin(context: Context, session: LoginSession, provider: LoginProvider) {
        val app = context.applicationContext
        TokenManager.saveTokens(app, session.accessToken, session.accessToken)
        LoginProvider.save(app, provider)
        WebLogin.save(app, session.sessionId, session.userJson, provider)
        afterTokenSaved(app)
        DeviceRegistration.register(app)
    }

    /** 웹이 토큰을 넘겼다(AndroidToken.saveToken — 토큰 저장은 이미 했다). */
    fun onWebLogin(context: Context) {
        // 웹 창이 자기 저장소에 로그인을 이미 넣었다 — 앱 로그인 화면이 넣어 둔 옛 값은 버린다.
        WebLogin.clear(context)
        afterTokenSaved(context)
        _webLoggedIn.tryEmit(Unit)
    }

    private fun afterTokenSaved(context: Context) {
        ForceLogout.reset()
        ChatSocketLifecycleObserver.ensureAttached(context)
        ChatSocketManager.connect(context)
        ActiveRole.syncFromServer(context)
        Handler(Looper.getMainLooper()).post { com.muyeon.app.ui.membership.MembershipBilling.flushUnfinished() }
    }
}
