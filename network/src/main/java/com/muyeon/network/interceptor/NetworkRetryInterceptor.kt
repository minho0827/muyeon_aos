package com.muyeon.network.interceptor

import com.muyeon.domain.common.NetworkRetryPrompt
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * ★ 2026-11-08 연결 실패·타임아웃 → '다시 시도' 팝업(웹 MuyeonApi 응답 인터셉터 isRetryableError → presentRetry 와 같은 규칙).
 *   · 서버가 답한 오류(4xx·5xx)는 대상이 아니다 — 응답 자체가 없을 때만.
 *   · 취소된 요청(화면을 떠남)은 묻지 않는다.
 *   · 뒤에서 조용히 도는 요청(배지·역할·기록 등 — 웹 silent)은 묻지 않는다 — 사용자를 막지 않게.
 *   · '다시 시도'면 같은 요청을 다시 보내고, 또 실패하면 다시 묻는다. '닫기'면 원래 오류를 그대로 던진다(화면의 오류 표시).
 */
class NetworkRetryInterceptor(private val prompt: () -> NetworkRetryPrompt) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val silent = SILENT_PATHS.any { request.url.encodedPath.endsWith(it) } || request.header(SILENT_HEADER) != null
        while (true) {
            try {
                return chain.proceed(request)
            } catch (e: IOException) {
                if (silent || chain.call().isCanceled() || !prompt().askBlocking()) throw e
            }
        }
    }

    companion object {
        /** 요청에 붙이면 묻지 않는다(웹 config.silent). */
        const val SILENT_HEADER = "X-Muyeon-Silent"

        /** 웹에서 silent 로 부르는 뒤쪽 요청들(배지 폴링·역할·열람 기록·찜 목록·지역 추정). */
        private val SILENT_PATHS = listOf(
            "/notifications/unread-count", "/me/roles", "/me/ui-flags", "/ui-flags", "/jobs/views/mine",
            "/me/applications", "/me/scraps", "/regions/nearest", "/app-menus", "/badges", "/home/tab-updates",
        )
    }
}
