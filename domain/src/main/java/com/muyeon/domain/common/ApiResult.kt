package com.muyeon.domain.common

/**
 * 저장소·유스케이스 결과의 단일 sealed 타입(PaceERA ApiResult 와 같은 모양).
 *   ViewModel 은 `when (r) { is Success -> … is Error -> … }` 로 가른다.
 *
 *   무용연 서버(NestJS) 오류 본문은 `{ statusCode, message, code?, data? }` 다.
 *   [code] 는 화면이 분기하는 서버 오류 코드(예: SWITCH_REQUIRED, ROLE_REQUIRED, RECRUIT_CHECK).
 */
sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(
        val exception: Throwable,
        /** 화면에 그대로 띄울 문구. 비어 있지 않다 — 원문이 없으면 [UserFacingError] 문구. */
        val message: String = UserFacingError.server,
        val statusCode: Int? = null,
        val code: String? = null,
        val kind: ErrorKind = UserFacingError.kindOf(exception, statusCode),
    ) : ApiResult<Nothing>()

    inline fun <R> map(transform: (T) -> R): ApiResult<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> this
    }

    inline fun onSuccess(block: (T) -> Unit): ApiResult<T> {
        if (this is Success) block(data)
        return this
    }

    inline fun onError(block: (Error) -> Unit): ApiResult<T> {
        if (this is Error) block(this)
        return this
    }

    fun getOrNull(): T? = (this as? Success)?.data
}
