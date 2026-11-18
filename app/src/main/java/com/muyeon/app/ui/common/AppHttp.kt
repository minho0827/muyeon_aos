package com.muyeon.app.ui.common

import android.content.Context
import com.muyeon.app.chat.socket.ForceLogout
import com.muyeon.app.utils.TokenManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response

/**
 * ★ 2026-11-18 네이티브 화면 공용 OkHttp — 강제 로그아웃 확인을 거친다.
 *  종전엔 레슨·채팅·견적·알림 등 30여 곳이 `OkHttpClient()` 를 따로 만들어 불러,
 *  로그인이 끊겨도(다른 기기 로그인 SESSION_REPLACED · 토큰 만료 TOKEN_EXPIRED · 정지) 로그아웃 없이
 *  화면에 '불러오지 못했어요' 만 떴다(2026-11-18 iOS 에서 실제로 난 'Unauthorized' 화면과 같은 종류).
 *  새 네이티브 API 는 `OkHttpClient()` 대신 [client](시간 제한을 바꾸려면 `client.newBuilder()`)를 쓴다.
 *  ⚠️ AiAutofill 은 일부러 빼 둔다 — 작성 중인 내용을 날리지 않게 401 이면 안내만 한다(iOS 와 같다).
 */
object AppHttp {
    /**
     * 401/403 + 강제 로그아웃 코드면 [ForceLogout.checkResponse]. 본문은 peek 으로만 읽는다(호출한 쪽이 다시 읽는다).
     *  요청에 실었던 토큰이 지금 토큰과 같을 때만 — 이 기기에서 방금 새로 로그인했으면 옛 요청의 401 은 무시(iOS 와 같다).
     */
    private val forceLogoutInterceptor = Interceptor { chain ->
        val req = chain.request()
        val res: Response = chain.proceed(req)
        if (res.code == 401 || res.code == 403) {
            runCatching {
                val ctx = org.koin.core.context.GlobalContext.getOrNull()?.get<Context>() ?: return@runCatching
                val sent = req.header("Authorization")?.removePrefix("Bearer ")?.trim()
                val now = TokenManager.getAccessToken(ctx)
                if (!sent.isNullOrEmpty() && sent == now) {
                    ForceLogout.checkResponse(ctx, res.code, res.peekBody(PEEK_LIMIT).string())
                }
            }
        }
        res
    }

    /** 공용 연결(연결 풀을 나눠 쓴다). 기본 시간 제한은 OkHttp 기본값 그대로 — 종전 `OkHttpClient()` 와 같다. */
    val client: OkHttpClient by lazy {
        OkHttpClient.Builder().addInterceptor(forceLogoutInterceptor).build()
    }

    private const val PEEK_LIMIT = 16L * 1024
}
