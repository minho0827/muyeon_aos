package com.muyeon.app.auth

import android.content.Context
import com.muyeon.app.ui.common.NativeHttp
import com.muyeon.app.ui.device_info.DeviceInfoViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * 로그인 직후 기기·푸시 토큰을 서버에 회원으로 묶어 저장 — 웹 AuthContext.syncNativeAfterAuth →
 *   common/deviceService.setDeviceInfoSvc(POST /user/account/saveDeviceInfo)와 같은 값.
 *   웹 로그인은 웹이 직접 보낸다. 네이티브 로그인은 웹 창이 안 열릴 수 있어 여기서 보낸다(안 보내면 푸시가 안 온다).
 */
object DeviceRegistration {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun register(context: Context) {
        val app = context.applicationContext
        // 기기 정보(FCM 토큰·권한 상태)는 웹 브리지 getDeviceInfo 와 같은 곳에서 모은다.
        DeviceInfoViewModel(app).getDeviceInfo { d ->
            val body = JSONObject()
                .put("deviceOsCd", d.systemName)
                .put("deviceId", d.deviceId)
                .put("deviceBrand", d.deviceModel)
                .put("deviceSystemVer", d.systemVersion)
                .put("deviceUniqId", "")
                .put("deviceAppVer", d.appVersion)
                .put("deviceToken", d.deviceToken)
                .put("osType", "Android")
                .put("notiPermissionYn", if (d.pushNotificationStatus == com.muyeon.app.utils.Constants.AUTHORIZED) "Y" else "N")
            scope.launch { NativeHttp(app).call(PATH, "POST", body) }
        }
    }

    private const val PATH = "/user/account/saveDeviceInfo"
}
