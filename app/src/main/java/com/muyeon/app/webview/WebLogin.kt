package com.muyeon.app.webview

import android.content.Context
import android.webkit.WebView
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.muyeon.app.auth.LoginProvider
import com.muyeon.app.utils.TokenManager
import org.json.JSONObject

/**
 * ★ 2026-11-08 앱 로그인 화면(LoginActivity)에서 로그인 — 웹 저장소(localStorage muyeonAuth)에도 같은 로그인을 넣는다.
 *   웹은 자기 저장소로만 로그인 상태를 안다(AuthContext) — 안 넣으면 다음에 여는 웹 창은 비회원으로 보인다.
 *   [WebLogout] 의 짝: 다음에 열리는 웹 창의 문서 시작 스크립트로 **한 번만** 넣는다(로그인마다 새 번호).
 *   이미 떠 있는 웹 창은 [applyNow] 로 바로 넣고 다시 읽힌다.
 *   토큰은 여기 두지 않고 넣는 순간 TokenManager 에서 읽는다(로그아웃·강제 로그아웃 뒤면 넣지 않는다).
 */
object WebLogin {
    private const val PREFS = "muyeon.webLogin"
    private const val KEY_NONCE = "nonce"
    private const val KEY_SESSION = "sessionId"
    private const val KEY_USER = "user"
    private const val KEY_PROVIDER = "provider"
    private const val LOGOUT_PREFS = "muyeon.webLogout"

    fun save(context: Context, sessionId: String?, userJson: String, provider: LoginProvider) {
        prefs(context).edit()
            .putString(KEY_NONCE, System.currentTimeMillis().toString())
            .putString(KEY_SESSION, sessionId)
            .putString(KEY_USER, userJson)
            .putString(KEY_PROVIDER, provider.name)
            .apply()
    }

    /** 로그아웃·웹 로그인 — 넣을 것이 없다. */
    fun clear(context: Context) = prefs(context).edit().clear().apply()

    /** 웹뷰를 만들 때 — 넣을 로그인이 남아 있으면 문서 시작에 건다([WebLogout.install] 다음에 부른다). */
    fun install(context: Context, webView: WebView, origin: String) {
        val js = script(context) ?: return
        if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            runCatching { WebViewCompat.addDocumentStartJavaScript(webView, js, setOf(origin)) }
        }
    }

    /** 떠 있는 웹 창 — 지금 넣고 [then](다시 읽기 등). 넣을 것이 없으면 [then] 만. */
    fun applyNow(context: Context, webView: WebView, then: () -> Unit) {
        val js = script(context) ?: return then()
        webView.evaluateJavascript(js) { then() }
    }

    private fun script(context: Context): String? {
        val p = prefs(context)
        val nonce = p.getString(KEY_NONCE, null) ?: return null
        val token = TokenManager.getAccessToken(context)?.takeIf { it.isNotBlank() } ?: return null
        val user = runCatching { JSONObject(p.getString(KEY_USER, null) ?: "{}") }.getOrElse { JSONObject() }
        val auth = JSONObject().put("accessToken", token).put("user", user)
        p.getString(KEY_SESSION, null)?.let { auth.put("sessionId", it) }
        val provider = p.getString(KEY_PROVIDER, null) ?: LoginProvider.EMAIL.name
        // 아직 웹 창에 못 흘린 로그아웃이 있으면 그 번호를 '처리됨'으로 둔다 — 로그아웃 스크립트가 방금 넣은 로그인을 지우지 않게.
        val logoutNonce = context.applicationContext.getSharedPreferences(LOGOUT_PREFS, Context.MODE_PRIVATE).getString("nonce", null)
        val logoutLine = logoutNonce?.let { "localStorage.setItem('muyeon.webLogoutNonce',${JSONObject.quote(it)});" }.orEmpty()
        return "(function(){try{var k='muyeon.webLoginNonce';if(localStorage.getItem(k)!==${JSONObject.quote(nonce)}){" +
            "localStorage.setItem('muyeonAuth',${JSONObject.quote(auth.toString())});" +
            "localStorage.setItem('muyeon.lastLoginProvider',${JSONObject.quote(provider)});" +
            logoutLine +
            "localStorage.setItem(k,${JSONObject.quote(nonce)});}}catch(e){}})();"
    }

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
