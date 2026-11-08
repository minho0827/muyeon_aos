package com.muyeon.app.auth

import android.content.Context

/**
 * 마지막으로 쓴 로그인 수단 — 로그인 화면 '최근 로그인' 표시(웹 common/lastLoginProvider.js 와 같은 값).
 *   웹 창 저장소(localStorage muyeon.lastLoginProvider)에도 같은 값을 넣는다(WebLogin).
 */
enum class LoginProvider { EMAIL, KAKAO, NAVER, APPLE;

    companion object {
        private const val PREFS = "muyeon.auth"
        private const val KEY = "lastLoginProvider"

        fun last(context: Context): LoginProvider? =
            context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
                ?.let { v -> entries.firstOrNull { it.name == v } }

        fun save(context: Context, provider: LoginProvider) {
            context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, provider.name).apply()
        }
    }
}
