package com.muyeon.network.interceptor

import com.muyeon.domain.common.ForceLogoutTrigger
import okhttp3.Interceptor
import okhttp3.Response

/**
 * 401/403 본문에 다른 기기 로그인·이용 정지 코드가 오면 강제 로그아웃(판정·화면 이동은 :app ForceLogout).
 *   본문은 peek 으로만 읽는다 — 아래(safeApiCall)가 오류 문구를 다시 읽어야 한다.
 */
class ForceLogoutInterceptor(private val trigger: () -> ForceLogoutTrigger) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val res = chain.proceed(chain.request())
        if (res.code == 401 || res.code == 403) {
            val body = runCatching { res.peekBody(PEEK_LIMIT).string() }.getOrNull()
            runCatching { trigger().check(res.code, body) }
        }
        return res
    }

    private companion object { const val PEEK_LIMIT = 16L * 1024 }
}
