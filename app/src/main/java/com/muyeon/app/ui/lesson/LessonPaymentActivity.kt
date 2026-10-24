package com.muyeon.app.ui.lesson

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.muyeon.app.BuildConfig
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors
import com.muyeon.app.webview.ExternalScheme
import com.muyeon.app.webview.TokenWebViewInterface
import org.json.JSONObject

/**
 * 레슨 예약금 결제 창 — iOS `LessonPaymentWebView`(전체화면 커버)와 같은 흐름(2026-10-24).
 *  예약 화면 위에 웹 `/reservations/:id?pay=1` 을 연다 → 토스 결제창 → `/payment/result` 가 승인·확정.
 *  - 웹에 결제 창임을 알린다: `__muyeonPaymentAppScheme`(카드 앱 복귀 스킴), `__muyeonPaymentSheetClosable`
 *    → 웹이 결제 창 안에서는 탭바·뒤로가기를 숨기고, 결과 화면 '확인'으로 closePaymentSheet 를 보낸다(웹 util/paymentSheet.js).
 *  - 웹 → 앱 신호는 이 창 전용 AppBridge 로 받는다: closePaymentSheet(닫기), paymentCompleted(승인 성공 기억).
 *  - 닫힐 때 결과(paid)를 돌려준다 — 결제됐으면 예약 화면을 닫고 내 예약으로, 아니면 달력에 남아 다시 고른다.
 *  ★ 메인 웹뷰로 보내지 않는 이유: 예전엔 예약 화면을 닫고 메인에서 결제해, 취소하면 예약 화면이 사라져 있었다(iOS 와 달랐다).
 */
class LessonPaymentActivity : ComponentActivity() {

    private var paid = false
    private lateinit var webView: WebView

    companion object {
        private const val EXTRA_RESERVATION_ID = "reservationId"
        const val EXTRA_PAID = "paid"

        /** iOS 와 같은 표시 — 카드 앱 인증 후 돌아올 스킴 + 웹에서 창을 닫을 수 있는 앱. */
        private const val PRELOAD_JS =
            "window.__muyeonPaymentAppScheme = 'muyeon://'; window.__muyeonPaymentSheetClosable = true;"

        fun intent(context: Context, reservationId: Int): Intent =
            Intent(context, LessonPaymentActivity::class.java).putExtra(EXTRA_RESERVATION_ID, reservationId)
    }

    /** 예약 화면이 결과(결제됨 여부)를 받는 계약. */
    class Contract : ActivityResultContract<Int, Boolean>() {
        override fun createIntent(context: Context, input: Int): Intent = intent(context, input)
        override fun parseResult(resultCode: Int, intent: Intent?): Boolean =
            intent?.getBooleanExtra(EXTRA_PAID, false) == true
    }

    private fun finishWithResult() {
        setResult(Activity.RESULT_OK, Intent().putExtra(EXTRA_PAID, paid))
        finish()
    }

    /** 웹 → 결제 창 신호. 다른 동작(화면 이동 등)은 이 창에서 받지 않는다. */
    private inner class PaymentBridge {
        @JavascriptInterface
        fun postMessage(raw: String?) {
            val action = runCatching { JSONObject(raw ?: "").optString("action") }.getOrNull() ?: return
            runOnUiThread {
                when (action) {
                    // 승인 성공은 버튼과 무관하게 기억한다 — 상단 '닫기'로 나가도 '결제됨'으로 처리해야 내 예약으로 보낸다.
                    "paymentCompleted" -> paid = true
                    // 웹 결과 화면의 '확인' — 상단 '닫기'와 똑같이 닫는다.
                    "closePaymentSheet" -> finishWithResult()
                }
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val reservationId = intent.getIntExtra(EXTRA_RESERVATION_ID, 0)
        val url = "${BuildConfig.API_BASE_URL}/reservations/$reservationId?pay=1"

        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            addJavascriptInterface(PaymentBridge(), "AppBridge")
            addJavascriptInterface(TokenWebViewInterface(this@LessonPaymentActivity, this), "AndroidToken")
            // 문서 시작 전에 표시를 심는다(웹이 첫 그림부터 결제 창으로 그리게). 미지원 기기는 페이지 시작 때 넣는다.
            val docStart = WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)
            if (docStart) {
                WebViewCompat.addDocumentStartJavaScript(this, PRELOAD_JS, setOf(BuildConfig.API_BASE_URL))
            }
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    // http/https 는 이 창에서, 카드사·간편결제 앱(intent:// 등)은 밖으로(메인 웹뷰와 같은 규칙).
                    val scheme = request.url.scheme?.lowercase()
                    if (scheme == "http" || scheme == "https") return false
                    ExternalScheme.open(this@LessonPaymentActivity, view, request.url.toString())
                    return true
                }

                override fun onPageStarted(view: WebView?, u: String?, favicon: android.graphics.Bitmap?) {
                    super.onPageStarted(view, u, favicon)
                    if (!docStart) view?.evaluateJavascript(PRELOAD_JS, null)
                }
            }
            loadUrl(url)
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // 결제창 안에서 뒤로 갈 곳이 있으면 웹 뒤로, 없으면 닫기(iOS 는 스와이프로 닫히지 않고 '닫기'만 있다).
                if (webView.canGoBack()) webView.goBack() else finishWithResult()
            }
        })

        setContent {
            Column(Modifier.fillMaxSize().background(Color.White).statusBarsPadding().navigationBarsPadding()) {
                // iOS 내비게이션 막대 — 왼쪽 '닫기', 가운데 '예약금 결제'.
                Box(Modifier.fillMaxWidth().height(44.dp)) {
                    Text(
                        "닫기", fontFamily = customFontFamily, fontSize = 17.sp, color = MuyeonColors.primary,
                        modifier = Modifier.align(Alignment.CenterStart).clickable { finishWithResult() }.padding(horizontal = 16.dp, vertical = 10.dp),
                    )
                    Text(
                        "예약금 결제", fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp,
                        color = MuyeonColors.textHead, modifier = Modifier.align(Alignment.Center),
                    )
                }
                HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFC6C6C8))
                AndroidView(factory = { webView }, modifier = Modifier.fillMaxSize())
            }
        }
    }

    override fun onDestroy() {
        if (::webView.isInitialized) {
            webView.removeJavascriptInterface("AppBridge")
            webView.destroy()
        }
        super.onDestroy()
    }
}
