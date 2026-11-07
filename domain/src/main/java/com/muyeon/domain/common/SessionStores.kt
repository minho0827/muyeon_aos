package com.muyeon.domain.common

/**
 * 인증 토큰 저장소 추상 — 구현은 :app(TokenManager, AES-GCM 암호화 prefs).
 *   :network(AuthInterceptor)가 이 인터페이스만 알도록 :domain 에 둔다(의존 방향 :network → :domain).
 */
interface TokenStore {
    /** 없으면 null(비회원). */
    fun accessToken(): String?
    val isLoggedIn: Boolean get() = !accessToken().isNullOrBlank()
    /** 내 회원 번호(토큰 sub). 비회원·해석 실패는 null — 서버가 isMine 을 주지 않는 곳(댓글)의 '내 것' 판정. */
    fun myUserId(): Long?
}

/**
 * 활성 회원유형 저장소 — 구현은 :app(ActiveRole, prefs `muyeon.role`).
 *   모든 요청의 `X-Active-Type` 헤더 출처. 웹 axios·iOS AppNetwork 와 같은 값.
 */
interface ActiveTypeStore {
    /** 바인드 전이면 null(헤더 생략 → 서버가 GENERAL 로 본다). */
    fun current(): String?
    /** 로컬 저장(서버 저장은 MemberRepository.setActiveType 가 먼저 한다). */
    fun save(type: String)
}

/**
 * 강제 로그아웃 판정 — 구현은 :app(ForceLogout.checkResponse).
 *   다른 기기 로그인(SESSION_REPLACED)·이용 정지 코드가 401/403 본문에 오면 로그인 화면으로 보낸다.
 */
interface ForceLogoutTrigger {
    /** @return 강제 로그아웃을 시작했으면 true. */
    fun check(httpCode: Int, body: String?): Boolean
}

/**
 * 작은 화면 기억값(칩 선택·보기 방식) — 웹 usePersistedState(localStorage)와 같은 역할. 구현은 :data(SharedPreferences).
 *   서버 데이터가 아니다. 지워져도 기본값으로 돌아갈 뿐이다.
 */
interface KeyValueStore {
    fun getString(key: String): String?
    fun putString(key: String, value: String?)
}

/**
 * ★ 2026-11-08 네트워크 실패 '다시 시도' 묻기 — 웹 GlobalNetworkOverlay(presentRetry)와 같은 일. 구현은 :app(화면 팝업).
 *   연결 실패·30초 타임아웃일 때만 부른다(서버가 답한 오류는 아님). 요청 스레드에서 사용자가 고를 때까지 기다린다.
 */
interface NetworkRetryPrompt {
    /** @return 다시 시도면 true, 닫기면 false. 띄울 화면이 없으면 false. */
    fun askBlocking(): Boolean
}
