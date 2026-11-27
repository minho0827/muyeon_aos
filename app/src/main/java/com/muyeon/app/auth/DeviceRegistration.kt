package com.muyeon.app.auth

import android.content.Context
import com.muyeon.app.domain.models.device_info.DeviceInfo
import com.muyeon.app.ui.common.NativeHttp
import org.json.JSONObject

/**
 * 기기·푸시 토큰을 서버에 회원으로 묶어 저장 — 웹 AuthContext.syncNativeAfterAuth →
 *   common/deviceService.setDeviceInfoSvc(POST /user/account/saveDeviceInfo)와 같은 값.
 *   ★ 2026-11-27 언제 보낼지는 DeviceSync 가 정한다(로그인·앞으로 나옴·알림 허용 결과·새 토큰).
 */
object DeviceRegistration {
    fun body(d: DeviceInfo): JSONObject = JSONObject()
        .put("deviceOsCd", d.systemName)
        .put("deviceId", d.deviceId)
        .put("deviceBrand", d.deviceModel)
        .put("deviceSystemVer", d.systemVersion)
        .put("deviceUniqId", "")
        .put("deviceAppVer", d.appVersion)
        .put("deviceToken", d.deviceToken)
        .put("osType", "Android")
        .put("notiPermissionYn", if (d.pushNotificationStatus == com.muyeon.app.utils.Constants.AUTHORIZED) "Y" else "N")

    /** @return 서버가 2xx 로 받았으면 true. */
    suspend fun send(context: Context, d: DeviceInfo): Boolean =
        NativeHttp(context.applicationContext).call(PATH, "POST", body(d)).isSuccess

    private const val PATH = "/user/account/saveDeviceInfo"
}
