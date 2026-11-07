package com.muyeon.data.common

import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.common.ErrorKind
import com.muyeon.domain.common.UserFacingError
import com.muyeon.domain.common.toApiError
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import retrofit2.Response

/**
 * Retrofit Response<T> → ApiResult<T> (PaceERA safeApiCall 과 같은 규칙).
 *   무용연 서버는 성공 시 객체를 그대로, 실패 시 4xx/5xx + `{ statusCode, message(문자열|배열), code? }`.
 *   - 4xx + 서버 문구 → REJECTED(그 문구 그대로)  - 그 밖 → SERVER 공통 문구
 *   - 취소(CancellationException)는 실패가 아니다 — 반드시 다시 던진다(떠난 화면에 오류가 뜨지 않게).
 */
suspend fun <T : Any> safeApiCall(moshi: Moshi, call: suspend () -> Response<T>): ApiResult<T> = try {
    val response = call()
    if (response.isSuccessful) {
        val body = response.body()
        if (body != null) ApiResult.Success(body)
        else ApiResult.Error(IllegalStateException("Empty response body"), UserFacingError.server, response.code(), kind = ErrorKind.SERVER)
    } else {
        val err = parseErrorBody(moshi, response.errorBody()?.string())
        val serverMessage = err.message?.takeIf { it.isNotBlank() && !ROUTE_NOT_FOUND.containsMatchIn(it) }
        val kind = if (response.code() in 400..499 && serverMessage != null) ErrorKind.REJECTED else ErrorKind.SERVER
        ApiResult.Error(
            exception = HttpException(response),
            message = if (kind == ErrorKind.REJECTED) serverMessage!! else UserFacingError.server,
            statusCode = response.code(),
            code = err.code,
            kind = kind,
        )
    }
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    // 연결 실패·응답 모양 오류. 예외 원문(“Unable to resolve host …”, Moshi 문구)은 화면에 올리지 않는다.
    e.toApiError()
}

/** 본문이 없는 성공(204 등)도 성공으로 — 찜 토글처럼 결과 값이 필요 없는 호출. */
suspend fun safeUnitCall(moshi: Moshi, call: suspend () -> Response<*>): ApiResult<Unit> = try {
    val response = call()
    if (response.isSuccessful) ApiResult.Success(Unit)
    else {
        val err = parseErrorBody(moshi, response.errorBody()?.string())
        val serverMessage = err.message?.takeIf { it.isNotBlank() && !ROUTE_NOT_FOUND.containsMatchIn(it) }
        val kind = if (response.code() in 400..499 && serverMessage != null) ErrorKind.REJECTED else ErrorKind.SERVER
        ApiResult.Error(
            HttpException(response),
            if (kind == ErrorKind.REJECTED) serverMessage!! else UserFacingError.server,
            response.code(), err.code, kind,
        )
    }
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    e.toApiError()
}

internal data class ErrorBody(val message: String?, val code: String?)

internal fun parseErrorBody(moshi: Moshi, raw: String?): ErrorBody {
    if (raw.isNullOrBlank()) return ErrorBody(null, null)
    return try {
        val type = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
        val map = moshi.adapter<Map<String, Any?>>(type).fromJson(raw)
        // NestJS ValidationPipe 는 message 를 배열로 준다 — 첫 줄만(웹 NativeHttp.messageOf 와 같음).
        val msg = when (val m = map?.get("message")) {
            is String -> m
            is List<*> -> m.filterIsInstance<String>().firstOrNull()
            else -> null
        }
        ErrorBody(msg, (map?.get("code") as? String)?.takeIf { it.isNotBlank() })
    } catch (_: Exception) {
        ErrorBody(null, null)
    }
}

/** NestJS 기본 404 문구 — "Cannot GET /api/…". 사용자에게 보일 말이 아니다. */
private val ROUTE_NOT_FOUND = Regex("^Cannot (GET|POST|PUT|PATCH|DELETE) /")
