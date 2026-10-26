package com.muyeon.app.utils

import android.content.Intent
import android.net.Uri

/**
 * 앱 밖에서 들어온 링크 보관소.
 *  - 카카오 알림톡 앱링크(muyeon://screen/home?type=…&path=…&roomId=…)
 *  - 앱 링크(https://muyeon.co.kr/jobs/1, /app/chat/12 …) — 받는 경로는 AndroidManifest 인텐트 필터,
 *    웹 확인 파일은 muyeon-front public/.well-known(assetlinks.json·apple-app-site-association)
 *
 * 링크는 SplashActivity 로 들어오지만, 스플래시 → (로그인) → 웹뷰로 가는 사이에
 *  인텐트가 끊기므로 여기에 맡겨 두고 WebViewActivity 가 처음 뜰 때 꺼내 쓴다.
 *  꺼낸 값은 푸시 탭 인텐트와 같은 모양(notification_url + data 키)으로 옮겨
 *  기존 푸시 라우팅을 그대로 탄다.
 *
 * ⚠️ path 는 웹 경로다. 절대 URL 은 WebViewActivity 가 baseUrl 을 붙여 만든다.
 */
object AppLinkManager {
    private val WEB_HOSTS = setOf("muyeon.co.kr", "www.muyeon.co.kr")
    /** 앱 링크로 들어온 경우의 type — 이용 로그(deeplink_open)에서 알림톡과 구분한다. */
    private const val UNIVERSAL_LINK_TYPE = "universal_link"

    private var pending: Map<String, String>? = null

    /** 맡을 링크면 맡고 true. 알림톡은 type·path 가 있어야 하고, QR 식사평가 qrPage 등은 무시. */
    fun save(uri: Uri): Boolean {
        if (uri.scheme == "https" && uri.host in WEB_HOSTS) return saveWebLink(uri)
        if (uri.getQueryParameter("type") == null && uri.getQueryParameter("path") == null) return false
        pending = uri.queryParameterNames.associateWith { uri.getQueryParameter(it).orEmpty() }
            .filterValues { it.isNotEmpty() }
        return true
    }

    /**
     * 웹 주소 → 알림톡 링크와 같은 모양. 웹 연결 페이지(AppLink.js)와 규칙이 같다.
     *  /app/<경로> 는 앱 실행용 연결 페이지라 앞의 /app 을 뗀다. /chat/<방> 은 채팅 푸시처럼 roomId 를 싣는다.
     */
    private fun saveWebLink(uri: Uri): Boolean {
        val path = (uri.path ?: "").replaceFirst(Regex("^/app(?=/|$)"), "").ifEmpty { "/home" }
        val target = path + (uri.encodedQuery?.let { "?$it" } ?: "")
        val roomId = Regex("^/chat/(\\d+)$").find(path)?.groupValues?.get(1)
        pending = if (roomId != null) {
            mapOf("type" to "chat_message", "path" to target, "roomId" to roomId)
        } else {
            mapOf("type" to UNIVERSAL_LINK_TYPE, "path" to target)
        }
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
