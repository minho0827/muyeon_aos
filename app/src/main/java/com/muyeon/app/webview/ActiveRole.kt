package com.muyeon.app.webview

import android.content.Context
import android.widget.Toast
import okhttp3.Request

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
    const val TEACHER = "TEACHER"
    const val DANCER = "DANCER"

    /** 무용수 유형 고객측 차단 문구(레슨 요청·예약) — iOS RoleGate 와 동일 문구. */
    const val DANCER_CUSTOMER_BLOCK = "무용수 유형에서는 레슨을 요청·예약할 수 없어요. 일반 유형으로 전환한 뒤 이용해 주세요."
    /** 무용수 유형 제공측 차단 문구(레슨 운영·견적 응답) — iOS RoleGate 와 동일 문구. */
    const val DANCER_PROVIDER_BLOCK = "레슨은 강사·학원 회원만 운영할 수 있어요. 레슨을 하려면 MY > 역할 설정에서 강사 유형을 추가해 주세요."

    /**
     * 헤더 부착용 앱 컨텍스트 — 네이티브 API 클라이언트는 Context 없이 토큰만 받는 구조라,
     *  여기 한 번 붙들어 두고 [withActiveType] 이 꺼내 쓴다. (TokenManager.getAccessToken 이 매번 bind)
     */
    @Volatile private var appContext: Context? = null

    fun bind(context: Context) {
        if (appContext == null) appContext = context.applicationContext
    }

    fun store(context: Context, type: String?) {
        bind(context)
        val v = (type ?: "").trim().uppercase().ifEmpty { "GENERAL" }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, v).apply()
    }

    fun current(context: Context): String {
        bind(context)
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, "GENERAL") ?: "GENERAL"
    }

    /** 컨텍스트 없이 현재 활성유형 — 바인드 전이면 null(헤더 생략 → 서버가 기본 판정). */
    fun currentOrNull(): String? = appContext?.let { current(it) }

    fun isAcademy(context: Context): Boolean = current(context) == ACADEMY

    /** 무용수 유형 활성 여부 — 레슨 관련 활동 전면 차단 기준(2026-09-26 정책). */
    fun isDancer(context: Context): Boolean = current(context) == DANCER

    /** 고객측(레슨 요청·예약) 게이트 — 무용수면 안내 토스트 후 false. */
    fun allowLessonCustomer(context: Context): Boolean {
        if (!isDancer(context)) return true
        Toast.makeText(context, DANCER_CUSTOMER_BLOCK, Toast.LENGTH_LONG).show()
        return false
    }

    /** 제공측(레슨 운영·견적 응답) 게이트 — 무용수면 안내 토스트 후 false. */
    fun allowLessonProvider(context: Context): Boolean {
        if (!isDancer(context)) return true
        Toast.makeText(context, DANCER_PROVIDER_BLOCK, Toast.LENGTH_LONG).show()
        return false
    }

    /** iOS `RoleGate.roleLabels` 와 동일 — 전환 토스트 문구에 쓴다. */
    val labels = mapOf(
        "TEACHER" to "강사", "DANCER" to "무용수", "ACADEMY" to "학원·원장",
        "TEAM" to "공연팀·기획자", "SPACE" to "공간 보유자", "GENERAL" to "일반",
    )

    fun label(type: String): String = labels[type.uppercase()] ?: type
}

/**
 * 네이티브 REST 요청에 X-Active-Type 부착 — 서버 유형 판정(무용수 레슨 차단 SWITCH_REQUIRED 등)은
 *  이 헤더가 있어야만 동작한다. 웹 axios 인터셉터·iOS APIClient 와 동일 값.
 */
fun Request.Builder.withActiveType(): Request.Builder = apply {
    ActiveRole.currentOrNull()?.let { header("X-Active-Type", it) }
}
