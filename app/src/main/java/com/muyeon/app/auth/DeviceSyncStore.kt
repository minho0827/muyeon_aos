package com.muyeon.app.auth

import android.content.Context
import androidx.core.content.edit

/** 마지막으로 보낸 기기 상태 저장소 — 로그아웃 때 [clear](다음 로그인은 꼭 보낸다). */
interface DeviceSyncStore {
    fun load(): DeviceFingerprint?
    fun save(fp: DeviceFingerprint)
    fun clear()
}

class PrefsDeviceSyncStore(context: Context) : DeviceSyncStore {
    private val prefs = context.applicationContext.getSharedPreferences("muyeon.deviceSync", Context.MODE_PRIVATE)

    override fun load(): DeviceFingerprint? {
        val user = prefs.getString(K_USER, null) ?: return null
        return DeviceFingerprint(
            token = prefs.getString(K_TOKEN, null) ?: return null,
            allowed = prefs.getBoolean(K_ALLOWED, false),
            appVersion = prefs.getString(K_VER, null) ?: return null,
            userId = user,
        )
    }

    override fun save(fp: DeviceFingerprint) = prefs.edit {
        putString(K_TOKEN, fp.token).putBoolean(K_ALLOWED, fp.allowed)
            .putString(K_VER, fp.appVersion).putString(K_USER, fp.userId)
    }

    override fun clear() = prefs.edit { clear() }

    private companion object {
        const val K_TOKEN = "token"
        const val K_ALLOWED = "allowed"
        const val K_VER = "appVersion"
        const val K_USER = "userId"
    }
}
