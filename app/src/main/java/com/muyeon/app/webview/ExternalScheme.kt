package com.muyeon.app.webview

import android.app.Activity
import android.content.Intent
import android.util.Log
import android.webkit.WebView

/**
 * 웹뷰 밖으로 보내야 하는 주소(카드사·간편결제 앱, tel, mailto, market 등)를 연다.
 *  메인 웹뷰(WebViewActivity)와 예약금 결제 창(LessonPaymentActivity)이 같이 쓴다(2026-10-24 결제 창 분리).
 *
 * ★ intent:// 는 반드시 Intent.parseUri(URI_INTENT_SCHEME) 로 풀어야 한다.
 *   토스 결제창은 안드로이드에서 카드사·카카오페이 앱을
 *   `intent://…#Intent;scheme=ispmobile;package=kvp.jjy.MispAndroid320;end` 형태로 부른다.
 *   이걸 ACTION_VIEW 에 그대로 넣으면 받을 앱이 없어 예외가 나고, 예전 코드는 그 예외를
 *   삼켜서 결제 버튼을 눌러도 아무 일도 일어나지 않았다(테스트 키에선 안 드러나고 운영 키에서 터진다).
 *
 * ★ 앱이 설치돼 있지 않으면 browser_fallback_url → 없으면 플레이스토어 설치 화면으로 보낸다.
 * ★ 보안: 웹 페이지가 만든 intent 로 우리 앱·다른 앱의 내부 화면을 직접 열지 못하게
 *   component·selector 를 지우고 BROWSABLE 카테고리만 허용한다.
 */
object ExternalScheme {
    fun open(activity: Activity, webView: WebView, url: String) {
        try {
            if (url.startsWith("intent:", ignoreCase = true)) {
                val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME).apply {
                    addCategory(Intent.CATEGORY_BROWSABLE)
                    component = null
                    selector = null
                }
                try {
                    activity.startActivity(intent)
                } catch (e: android.content.ActivityNotFoundException) {
                    val fallback = intent.getStringExtra("browser_fallback_url")
                    val pkg = intent.`package`
                    when {
                        !fallback.isNullOrBlank() -> webView.loadUrl(fallback)
                        !pkg.isNullOrBlank() -> activity.startActivity(
                            Intent(Intent.ACTION_VIEW, android.net.Uri.parse("market://details?id=$pkg"))
                        )
                        else -> Log.w("ExternalScheme", "외부 앱 없음: $url")
                    }
                }
                return
            }
            activity.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)))
        } catch (e: Exception) {
            // 앱이 없거나 잘못된 주소 — 결제가 조용히 멈추지 않게 원인을 남긴다.
            Log.w("ExternalScheme", "외부 스킴 열기 실패: $url (${e.message})")
        }
    }
}
