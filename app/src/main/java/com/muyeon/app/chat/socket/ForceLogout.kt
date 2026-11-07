package com.muyeon.app.chat.socket

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.muyeon.app.BuildConfig
import com.muyeon.app.utils.TokenManager
import com.muyeon.app.webview.WebViewActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * ★ 2026-11-04 강제 로그아웃(다른 기기 로그인 SESSION_REPLACED · 계정 정지 ACCOUNT_SUSPENDED) 한 곳 처리.
 *
 *  예전에는 소켓 'force-logout' → ChatEventBus → WebViewActivity 의 구독자가 웹에 알렸다. 그래서
 *   ① 백그라운드·화면 꺼짐 동안 온 신호는 놓쳤고(소켓이 쉬는 중), 복귀해서 옛 토큰으로 다시 붙으면 서버가 말없이
 *     끊기만 해(chat.gateway handleConnection → disconnect) 아무도 알아채지 못했다.
 *   ② 네이티브 API 가 401 SESSION_REPLACED 를 받아도 화면 안내로만 끝났다.
 *   ③ 본문(WebViewActivity)이 시스템에 정리돼 있으면 구독자가 없어 신호가 사라졌다.
 *
 *  지금은 신호가 어디서 오든(소켓 이벤트 · 소켓이 서버에 끊김/거절 → 세션 확인 · 네이티브 API 응답) [trigger] 한 번:
 *   네이티브 토큰을 바로 지우고 소켓을 끊은 뒤, 본문을 앞으로 불러(살아 있으면 CLEAR_TOP|SINGLE_TOP, 없으면 새로)
 *   code·message 를 실어 보낸다. 본문은 웹이 준비되면 'muyeonNativeForceLogout' 이벤트를 한 번 보낸다.
 *  같은 로그아웃에 여러 신호가 와도 한 번만(새 토큰을 저장하면 [reset]).
 */
object ForceLogout {
    private const val TAG = "ForceLogout"
    const val SESSION_REPLACED = "SESSION_REPLACED"
    const val ACCOUNT_SUSPENDED = "ACCOUNT_SUSPENDED"
    const val EXTRA_CODE = "forceLogoutCode"
    const val EXTRA_MESSAGE = "forceLogoutMessage"

    private val handled = AtomicBoolean(false)
    private val verifying = AtomicBoolean(false)
    private val client = OkHttpClient.Builder().connectTimeout(8, TimeUnit.SECONDS).readTimeout(8, TimeUnit.SECONDS).build()

    fun isForceCode(code: String?): Boolean = code == SESSION_REPLACED || code == ACCOUNT_SUSPENDED

    /** 새 토큰 저장(로그인) — 다음 강제 로그아웃을 다시 받을 수 있게. */
    fun reset() { handled.set(false) }

    /** 강제 로그아웃 실행(어느 스레드에서 불러도 된다). 이미 처리 중이면 무시. */
    fun trigger(context: Context, code: String, message: String?) {
        if (!handled.compareAndSet(false, true)) return
        val app = context.applicationContext
        Handler(Looper.getMainLooper()).post {
            Log.i(TAG, "trigger code=$code")
            TokenManager.clearTokens(app)
            ChatSocketManager.disconnect()
            // ★ 2026-11-07 앱 루트(탭 셸)를 새로 세우고 그 위에 웹 로그인 창 — 웹(common/forceLogout)이 안내 팝업을 띄운다.
            com.muyeon.app.webview.ActiveRole.clear(app)
            runCatching {
                app.startActivities(
                    arrayOf(
                        Intent(app, com.muyeon.app.ui.main.MainActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
                        Intent(app, WebViewActivity::class.java)
                            .putExtra(WebViewActivity.EXTRA_START_PATH, "/login")
                            .putExtra(EXTRA_CODE, code).putExtra(EXTRA_MESSAGE, message.orEmpty()),
                    ),
                )
            }.onFailure { Log.w(TAG, "본문 열기 실패: ${it.message}") }
        }
    }

    /** 응답 본문({code, message})이 강제 로그아웃이면 trigger 하고 true. 네이티브 API 공용. */
    fun checkResponse(context: Context, httpCode: Int, body: String?): Boolean {
        if (httpCode != 401 && httpCode != 403) return false
        val o = runCatching { JSONObject(body.orEmpty()) }.getOrNull() ?: return false
        val code = o.optString("code").takeIf { isForceCode(it) } ?: return false
        trigger(context, code, o.optString("message").takeIf { it.isNotBlank() && it != "null" })
        return true
    }

    /**
     * 소켓이 서버에 끊겼거나(옛 토큰 거절) 연결이 거절됐을 때 — 지금 토큰의 세션이 살아 있는지 한 번 묻는다.
     *  401/403 + 세션 교체·정지 코드일 때만 로그아웃(네트워크 오류·토큰 만료 등은 건드리지 않는다).
     */
    fun verifySession(context: Context) {
        val app = context.applicationContext
        val token = TokenManager.getAccessToken(app)?.takeIf { it.isNotBlank() } ?: return
        if (handled.get() || !verifying.compareAndSet(false, true)) return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val req = Request.Builder().url("${BuildConfig.API_BASE_URL}/api/me/roles")
                    .addHeader("Authorization", "Bearer $token").get().build()
                client.newCall(req).execute().use { res ->
                    // 그사이 다른 토큰으로 바뀌었으면(이 기기에서 방금 로그인) 옛 토큰의 결과라 무시한다.
                    if (TokenManager.getAccessToken(app) != token) return@use
                    checkResponse(app, res.code, res.body?.string())
                }
            } catch (e: Exception) {
                Log.w(TAG, "verifySession 실패(무시): ${e.message}")
            } finally {
                verifying.set(false)
            }
        }
    }
}
