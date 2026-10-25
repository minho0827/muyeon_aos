package com.muyeon.app.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.content.edit

/**
 * 알림 허용을 '필요한 순간'에 묻는다 — iOS `PushPrimer` 와 같은 규칙(2026-10-25).
 *  첫 실행 권한 흐름(PermissionManager)에서 알림을 빼고, 아래 순간에 NotificationConsentSheet 를 띄운다.
 *   - 견적 요청 완료 화면(QuoteWizardActivity)
 *   - 강사 유형 추가 직후(RoleManageScreen)
 *   - 웹 openNotificationConsent(AppBridgeInterface → OnboardingActivity "notification")
 *  ★ 한 번만 묻는다 — 첫 실행에서 이미 물은 기존 설치자도 같은 키(notif_requested_once)라 다시 묻지 않는다.
 *  ★ 결과는 웹에 알린다(WebCallbacks.notificationConsent → window.__onNotificationConsent) —
 *    로그인 때 '알림 꺼짐'으로 저장된 기기를 웹이 다시 저장해 켠다.
 */
object PushPrimer {
    fun shouldAsk(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED) return false
        return !prefs(context).getBoolean(PermissionManager.KEY_NOTIF_REQUESTED, false)
    }

    /** 안내를 띄우는 순간 기록 — 같은 사람에게 두 번 묻지 않는다. */
    fun markAsked(context: Context) =
        prefs(context).edit { putBoolean(PermissionManager.KEY_NOTIF_REQUESTED, true) }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PermissionManager.PREFS_NAME, Context.MODE_PRIVATE)
}
