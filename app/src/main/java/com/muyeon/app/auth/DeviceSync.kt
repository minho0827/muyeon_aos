package com.muyeon.app.auth

import android.content.Context
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.muyeon.app.ui.device_info.DeviceInfoViewModel
import com.muyeon.app.utils.TokenManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * ★ 2026-11-27 기기·푸시 토큰·알림 허용 상태를 서버(saveDeviceInfo)와 맞춘다(Koin single).
 *
 *  전에는 로그인 때 한 번만 보냈다 — 알림 허용 안내(PushPrimer)는 로그인 뒤라 서버엔 '꺼짐'이 남았다.
 *  계기: 로그인(force) · 앱이 앞으로 나옴(DeviceSyncLifecycleObserver) · 알림 허용 결과 · 새 FCM 토큰(onNewToken).
 *  (토큰·허용·앱 버전·회원)이 마지막으로 보낸 것과 같으면 건너뛴다. 로그아웃 때 기억을 지운다(SessionResets).
 */
class DeviceSync(context: Context, store: DeviceSyncStore) {
    private val app = context.applicationContext
    private val runner = DeviceSyncRunner(store)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun sync(force: Boolean = false) {
        val userId = TokenManager.accountKey(TokenManager.getAccessToken(app)) ?: return
        DeviceInfoViewModel(app).getDeviceInfo { d ->
            val fp = DeviceFingerprint(
                token = d.deviceToken,
                allowed = NotificationManagerCompat.from(app).areNotificationsEnabled(),
                appVersion = d.appVersion,
                userId = userId,
            )
            scope.launch {
                // 기다리는 사이 계정이 바뀌었으면 보내지 않는다.
                if (TokenManager.accountKey(TokenManager.getAccessToken(app)) != userId) return@launch
                val sent = runner.run(fp, force) { DeviceRegistration.send(app, d) }
                if (sent) Log.i(TAG, "saveDeviceInfo 보냄 allowed=${fp.allowed} force=$force")
            }
        }
    }

    fun clear() = runner.clear()

    private companion object { const val TAG = "DeviceSync" }
}
