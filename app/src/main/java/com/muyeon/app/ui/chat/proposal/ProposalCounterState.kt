package com.muyeon.app.ui.chat.proposal

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * ★ 2026-11-27 받은 제안에 [다른 시간 제안] — 날짜·시간(+메모)만 바꿔 되묻는다(POST /lesson-proposals/:id/counter).
 *  보내는 중엔 다시 보내지 않는다(연타 방지). 실패면 서버 문장을 [errorText] 에 둔다.
 */
class ProposalCounterState(
    initialStartAt: Long,
    private val fallbackError: String,
    private val send: suspend (startAt: Long, memo: String) -> Result<Unit>,
) {
    var startAt by mutableLongStateOf(initialStartAt)
    var memo by mutableStateOf("")
    var sending by mutableStateOf(false)
        private set
    var errorText by mutableStateOf<String?>(null)
        private set

    val canSend: Boolean get() = !sending

    /** 성공이면 true. 보내는 중이면 아무것도 하지 않고 false. */
    suspend fun submit(): Boolean {
        if (sending) return false
        sending = true
        errorText = null
        val r = runCatching { send(startAt, memo.trim()) }.getOrElse { Result.failure(it) }
        sending = false
        r.exceptionOrNull()?.let { errorText = it.message?.ifBlank { null } ?: fallbackError }
        return r.isSuccess
    }
}
