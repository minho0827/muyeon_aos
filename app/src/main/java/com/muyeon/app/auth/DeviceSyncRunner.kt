package com.muyeon.app.auth

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 보낼지 판단 → 보내기 → 성공했을 때만 기억(실패하면 다음 계기에 다시 보낸다).
 *  여러 계기(앞으로 나옴·권한 결과·새 토큰)가 한꺼번에 와도 Mutex 로 차례로 — 뒤엣것은 같은 상태면 건너뛴다.
 */
class DeviceSyncRunner(private val store: DeviceSyncStore) {
    private val mutex = Mutex()

    /** @return 보냈고 성공했으면 true. */
    suspend fun run(current: DeviceFingerprint?, force: Boolean, send: suspend () -> Boolean): Boolean =
        mutex.withLock {
            if (!DeviceSyncPolicy.shouldSend(current, store.load(), force)) return@withLock false
            val ok = runCatching { send() }.getOrDefault(false)
            if (ok && current != null) store.save(current)
            ok
        }

    fun clear() = store.clear()
}
