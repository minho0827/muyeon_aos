package com.muyeon.domain.model.auth

/**
 * 로그인 결과(서버 issueToken) — 웹 AuthContext 가 localStorage muyeonAuth 에 두는 { accessToken, sessionId, user } 와 같다.
 *   [userJson] 은 서버 user 그대로(JSON 문자열) — 앱 웹 창 저장소에 같은 값을 넣는다.
 */
data class LoginSession(
    val accessToken: String,
    val sessionId: String?,
    val userJson: String,
    /** 필수 약관 동의 여부 — false 면 약관 동의 화면부터(웹 AppRouter 약관 게이트). null 은 옛 응답(게이트 없음). */
    val termsAgreed: Boolean?,
    /** 소셜 가입을 끝내지 않음(이메일 로그인은 항상 false) — 웹 /social-signup 으로. */
    val signupIncomplete: Boolean,
)
