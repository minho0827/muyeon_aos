package com.muyeon.app.webview

import android.webkit.WebView
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import org.json.JSONObject

/**
 * ★ 2026-11-08 (b4) 관심 지역은 앱 화면(/myRegions·/addRegion)에서 고친다 — 웹 창에도 앱 값(localStorage muyeonRegions)을 넣는다.
 *   웹 창을 만들 때 문서 시작 스크립트로 넣고, 넣은 값을 돌려준다. 창을 떠날 때(syncRegionFromWeb) 웹 값이 넣은 값 그대로면
 *   앱으로 다시 가져오지 않는다 — 그 사이 앱 화면에서 바꾼 값을 옛 웹 값이 덮지 않게.
 *   앱에 값이 없으면 넣지 않는다(웹이 처음 위치로 정한 지역은 지금처럼 앱으로 가져온다).
 */
object WebRegion {
    fun install(webView: WebView, origin: String, json: String?): String? {
        if (json.isNullOrBlank()) return null
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) return null
        val js = "(function(){try{localStorage.setItem('muyeonRegions',${JSONObject.quote(json)});}catch(e){}})();"
        return runCatching { WebViewCompat.addDocumentStartJavaScript(webView, js, setOf(origin)); json }.getOrNull()
    }
}
