package com.muyeon.app.webview

import android.content.Context

/**
 * 활성 회원유형 저장소 — iOS `RoleGate.activeType` 대응.
 *
 * 웹이 로그인·유형 전환 때마다 `syncActiveType` 브릿지로 보내주는데(muyeon-front AuthContext),
 * AOS 는 그동안 이 값을 **받고도 버리고 있었다**(SILENT_ACTIONS 로만 처리).
 * 그래서 네이티브 화면이 "지금 이 사람이 학원인가 강사인가"를 알 방법이 없었다.
 *
 * ★ 서버 판정은 여전히 요청 헤더(X-Active-Type)와 토큰이 한다. 이 값은 **화면 분기 전용**이다.
 *   여기 값이 틀려도 서버가 막아주므로, UI 는 보수적으로(막는 쪽으로) 쓰는 게 안전하다.
 */
object ActiveRole {
    private const val PREFS = "muyeon.role"
    private const val KEY = "activeType"
    const val ACADEMY = "ACADEMY"

    fun store(context: Context, type: String?) {
        val v = (type ?: "").trim().uppercase().ifEmpty { "GENERAL" }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, v).apply()
    }

    fun current(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, "GENERAL") ?: "GENERAL"

    fun isAcademy(context: Context): Boolean = current(context) == ACADEMY

    /** iOS `RoleGate.roleLabels` 와 동일 — 전환 토스트 문구에 쓴다. */
    val labels = mapOf(
        "TEACHER" to "강사", "DANCER" to "무용수", "ACADEMY" to "학원·원장",
        "TEAM" to "공연팀·기획자", "SPACE" to "공간 보유자", "GENERAL" to "일반",
    )

    fun label(type: String): String = labels[type.uppercase()] ?: type
}
