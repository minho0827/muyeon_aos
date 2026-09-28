package com.muyeon.app.utils

import android.content.Intent
import android.net.Uri

/**
 * 카카오 알림톡 앱링크(muyeon://screen/home?type=…&path=…&roomId=…) 보관소.
 *
 * 링크는 SplashActivity 로 들어오지만, 스플래시 → (로그인) → 웹뷰로 가는 사이에
 *  인텐트가 끊기므로 여기에 맡겨 두고 WebViewActivity 가 처음 뜰 때 꺼내 쓴다.
 *  꺼낸 값은 푸시 탭 인텐트와 같은 모양(notification_url + data 키)으로 옮겨
 *  기존 푸시 라우팅을 그대로 탄다.
 *
 * ⚠️ path 는 웹 경로다. 절대 URL 은 WebViewActivity 가 baseUrl 을 붙여 만든다.
 */
object AppLinkManager {
    private var pending: Map<String, String>? = null

    /** type·path 가 있는 링크만 맡는다(QR 식사평가 qrPage 등은 무시). 맡았으면 true. */
    fun save(uri: Uri): Boolean {
        if (uri.getQueryParameter("type") == null && uri.getQueryParameter("path") == null) return false
        pending = uri.queryParameterNames.associateWith { uri.getQueryParameter(it).orEmpty() }
            .filterValues { it.isNotEmpty() }
        return true
    }

    /** 맡아 둔 링크를 푸시 인텐트 모양으로 옮기고 비운다. 옮겼으면 true. */
    fun consumeInto(intent: Intent): Boolean {
        val p = pending ?: return false
        pending = null
        p["path"]?.let { intent.putExtra("notification_url", it) }
        p.filterKeys { it != "path" }.forEach { (k, v) -> intent.putExtra(k, v) }
        return true
    }
}
