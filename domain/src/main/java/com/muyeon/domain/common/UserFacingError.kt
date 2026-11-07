package com.muyeon.domain.common

import java.io.IOException

/**
 * 사용자에게 보여 줄 오류 문구를 한 곳에서 정한다(PaceERA UserFacingError 와 같은 규칙).
 *
 *   NETWORK  — 연결이 안 됨
 *   SERVER   — 5xx·빈 응답·응답 모양 오류·그 밖
 *   REJECTED — 4xx 이면서 서버가 문구를 준 것. 그 문구를 그대로 쓴다.
 *
 *   :domain 은 Android 리소스를 모르므로, 문구는 앱 시작 때 [install] 로 strings.xml 을 연결한다.
 */
enum class ErrorKind { NETWORK, SERVER, REJECTED }

object UserFacingError {
    // install 전(유닛 테스트)에만 쓰는 기본값. 실제 문구는 앱 strings.xml(common_error_*).
    private var networkText: () -> String = { "network" }
    private var serverText: () -> String = { "server" }

    fun install(network: () -> String, server: () -> String) {
        networkText = network
        serverText = server
    }

    val network: String get() = networkText()
    val server: String get() = serverText()

    /** 연결 문제인가. Moshi 의 JsonEncodingException 도 IOException 이라 패키지로 거른다(응답 모양 오류는 SERVER). */
    fun isNetwork(t: Throwable): Boolean =
        t is IOException && !t.javaClass.name.startsWith("com.squareup.moshi.")

    fun kindOf(t: Throwable, statusCode: Int? = null): ErrorKind = when {
        statusCode != null && statusCode in 400..499 -> ErrorKind.REJECTED
        statusCode != null -> ErrorKind.SERVER
        isNetwork(t) -> ErrorKind.NETWORK
        else -> ErrorKind.SERVER
    }

    fun messageFor(kind: ErrorKind): String = if (kind == ErrorKind.NETWORK) network else server
}

/** HTTP 응답이 아닌 실패(연결·파싱)를 사용자 문구가 붙은 [ApiResult.Error] 로. 예외 원문을 화면에 올리지 않는다. */
fun Throwable.toApiError(): ApiResult.Error {
    val kind = UserFacingError.kindOf(this)
    return ApiResult.Error(this, UserFacingError.messageFor(kind), kind = kind)
}
