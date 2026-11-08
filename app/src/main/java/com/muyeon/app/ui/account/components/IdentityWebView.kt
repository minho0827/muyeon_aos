package com.muyeon.app.ui.account.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import com.muyeon.app.R
import com.muyeon.app.ui.components.MainTopBar
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.domain.usecase.account.EditAccountUseCase

/**
 * 휴대폰 본인확인(NICE 표준창) — 앱 안 웹창. 표준창이 끝나 [EditAccountUseCase.RETURN_TO]?niceToken=…(또는 niceError=…)로
 *   돌아가려는 순간을 가로채 [onResult] 로 넘기고 닫는다(웹은 같은 창 이동 후 쿼리를 읽는다 — niceIdentity.js).
 *   뒤로·닫기는 취소([onResult](null, null)).
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun IdentityWebView(
    url: String,
    onResult: (token: String?, error: String?) -> Unit,
    /** 표준창이 돌아올 주소(가입 /register · 소셜 추가정보 /social-signup — [b1]). 기본은 개인정보 수정. */
    returnTo: String = EditAccountUseCase.RETURN_TO,
) {
    val result = rememberUpdatedState(onResult)
    val sent = androidx.compose.runtime.remember { booleanArrayOf(false) }
    fun check(u: String?): Boolean {
        val uri = u?.let(Uri::parse) ?: return false
        if (uri.path?.trimEnd('/')?.endsWith(returnTo) != true) return false
        val token = uri.getQueryParameter(EditAccountUseCase.TOKEN_PARAM)
        val error = uri.getQueryParameter(EditAccountUseCase.ERROR_PARAM)
        if (token == null && error == null) return false
        if (!sent[0]) { sent[0] = true; result.value(token, error) }
        return true
    }
    BackHandler { if (!sent[0]) { sent[0] = true; result.value(null, null) } }
    Column(Modifier.fillMaxSize().background(AppColors.Surface)) {
        MainTopBar(title = stringResource(R.string.account_identity_title), primary = false, onBack = { if (!sent[0]) { sent[0] = true; result.value(null, null) } })
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = check(request?.url?.toString())
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            if (check(url)) view?.stopLoading() else super.onPageStarted(view, url, favicon)
                        }
                    }
                    loadUrl(url)
                }
            },
        )
    }
}
