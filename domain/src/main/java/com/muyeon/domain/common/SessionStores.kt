package com.muyeon.domain.common

/**
 * 인증 토큰 저장소 추상 — 구현은 :app(TokenManager, AES-GCM 암호화 prefs).
 *   :network(AuthInterceptor)가 이 인터페이스만 알도록 :domain 에 둔다(의존 방향 :network → :domain).
 */
interface TokenStore {
    /** 없으면 null(비회원). */
    fun accessToken(): String?
    val isLoggedIn: Boolean get() = !accessToken().isNullOrBlank()
}

/**
 * 활성 회원유형 저장소 — 구현은 :app(ActiveRole, prefs `muyeon.role`).
 *   모든 요청의 `X-Active-Type` 헤더 출처. 웹 axios·iOS AppNetwork 와 같은 값.
 */
interface ActiveTypeStore {
    /** 바인드 전이면 null(헤더 생략 → 서버가 GENERAL 로 본다). */
    fun current(): String?
}

/**
 * 강제 로그아웃 판정 — 구현은 :app(ForceLogout.checkResponse).
 *   다른 기기 로그인(SESSION_REPLACED)·이용 정지 코드가 401/403 본문에 오면 로그인 화면으로 보낸다.
 */
interface ForceLogoutTrigger {
    /** @return 강제 로그아웃을 시작했으면 true. */
    fun check(httpCode: Int, body: String?): Boolean
}
