package com.muyeon.app.webview

import android.content.Context
import android.webkit.WebView
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature

/**
 * ★ 2026-11-07 앱(탭 셸 MY)에서 로그아웃 — 웹 저장소(localStorage muyeonAuth·muyeon.activeType)도 지워야 한다.
 *   웹은 열릴 때 자기 저장소의 토큰을 앱에 다시 넣는다(AuthContext syncNativeAfterAuth) — 안 지우면 다시 로그인된다.
 *   다음에 열리는 웹 창의 문서 시작 스크립트로 **한 번만** 지운다(로그아웃마다 새 번호 — 그 창에서 다시 로그인하면 지우지 않는다).
 */
object WebLogout {
    private const val PREFS = "muyeon.webLogout"
    private const val KEY = "nonce"

    fun mark(context: Context) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, System.currentTimeMillis().toString()).apply()
    }

    /** 웹뷰를 만들 때 — 로그아웃이 남아 있으면 지우는 스크립트를 문서 시작에 건다. */
    fun install(context: Context, webView: WebView, origin: String) {
        val nonce = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null) ?: return
        val js = "(function(){try{var k='muyeon.webLogoutNonce';if(localStorage.getItem(k)!=='$nonce'){" +
            "localStorage.removeItem('muyeonAuth');localStorage.removeItem('muyeon.activeType');localStorage.setItem(k,'$nonce');}}catch(e){}})();"
        if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            runCatching { WebViewCompat.addDocumentStartJavaScript(webView, js, setOf(origin)) }
        }
    }
}
