package com.muyeon.app.ui.main

import com.muyeon.domain.common.NetworkRetryPrompt
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking

/**
 * ★ 2026-11-08 '다시 시도' 팝업 연결 — 네트워크 인터셉터(요청 스레드)가 묻고, 앱 셸(MainActivity)이 팝업을 띄워 답한다.
 *   여러 요청이 동시에 실패해도 팝업은 하나 — 같은 답을 같이 받는다(웹 presentRetry 와 같다).
 *   앱 셸이 화면에 없으면(다른 화면·백그라운드) 묻지 않고 실패로 돌려준다 — 기다리는 스레드가 묶이지 않게.
 */
object NetworkRetryHost : NetworkRetryPrompt {
    private val _pending = MutableStateFlow(false)
    val pending: StateFlow<Boolean> = _pending.asStateFlow()
    private val lock = Any()
    private var deferred: CompletableDeferred<Boolean>? = null
    @Volatile var hostVisible: Boolean = false
        set(value) { field = value; if (!value) resolve(false) }

    override fun askBlocking(): Boolean {
        if (!hostVisible) return false
        val d = synchronized(lock) { deferred ?: CompletableDeferred<Boolean>().also { deferred = it; _pending.value = true } }
        return runBlocking { d.await() }
    }

    fun resolve(retry: Boolean) {
        val d = synchronized(lock) { deferred.also { deferred = null; _pending.value = false } }
        d?.complete(retry)
    }
}
