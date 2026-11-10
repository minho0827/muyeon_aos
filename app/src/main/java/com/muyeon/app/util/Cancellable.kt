package com.muyeon.app.util

import kotlin.coroutines.cancellation.CancellationException

/**
 * runCatching 과 같지만 코루틴 취소(CancellationException)는 다시 던진다.
 *  당겨서 새로고침·화면 나감으로 취소된 요청을 '실패'로 잡아 오류 문구를 띄우지 않게.
 */
inline fun <T> runCatchingCancellable(block: () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Result.failure(e)
    }
