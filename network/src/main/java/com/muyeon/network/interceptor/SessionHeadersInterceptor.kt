package com.muyeon.network.interceptor

import com.muyeon.domain.common.ActiveTypeStore
import com.muyeon.domain.common.TokenStore
import okhttp3.Interceptor
import okhttp3.Response

/**
 * 모든 요청에 로그인 토큰·활성유형을 싣는다.
 *   - 토큰은 요청 직전마다 새로 읽는다(화면을 띄운 뒤 로그인·갱신이 일어나도 낡은 토큰을 쓰지 않게 — NativeHttp 와 같은 규약).
 *   - `X-Active-Type`: 서버 유형 게이트(SWITCH_REQUIRED·ROLE_REQUIRED)·메뉴·추천이 이 헤더를 본다.
 *   - `X-Muyeon-Client: app`: 서버가 앱·브라우저 세션을 따로 센다(웹 axios 인터셉터와 같은 값).
 */
class SessionHeadersInterceptor(
    private val tokens: TokenStore,
    private val activeType: ActiveTypeStore,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val b = chain.request().newBuilder()
        tokens.accessToken()?.takeIf { it.isNotBlank() }?.let { b.header("Authorization", "Bearer $it") }
        activeType.current()?.let { b.header("X-Active-Type", it) }
        b.header("X-Muyeon-Client", "app")
        return chain.proceed(b.build())
    }
}
