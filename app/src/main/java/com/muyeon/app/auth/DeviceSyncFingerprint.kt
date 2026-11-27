package com.muyeon.app.auth

/**
 * ★ 2026-11-27 서버에 마지막으로 보낸 기기 상태(saveDeviceInfo) — 같으면 다시 보내지 않는다.
 *  로그인 때만 보내던 탓에 로그인 뒤 알림을 허용해도 서버엔 '꺼짐(N)'이 남았다(DeviceSync).
 */
data class DeviceFingerprint(
    val token: String,
    val allowed: Boolean,
    val appVersion: String,
    val userId: String,
)

object DeviceSyncPolicy {
    /**
     * @param current 지금 상태. 비로그인이면 null.
     * @param last 마지막으로 보내기에 성공한 상태(로그아웃 때 지운다).
     * @param force 로그인 직후 — 같아도 보낸다.
     */
    fun shouldSend(current: DeviceFingerprint?, last: DeviceFingerprint?, force: Boolean): Boolean {
        if (current == null || current.userId.isBlank()) return false
        if (!isUsableToken(current.token)) return false
        if (force) return true
        return current != last
    }

    /** FCM 토큰을 못 받았으면("unknown"·빈 값) 서버의 좋은 토큰을 덮어쓰지 않는다. */
    fun isUsableToken(token: String): Boolean = token.isNotBlank() && token != "unknown"
}
