package com.muyeon.domain.common

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.CopyOnWriteArrayList

/**
 * ★ 2026-11-09 계정이 바뀌는 순간(로그아웃 · 다른 계정으로 로그인) 한 곳 신호.
 *
 *  토큰 저장소(:app TokenManager)가 토큰을 저장·지울 때 [onAccount] 를 부른다.
 *   로그아웃 길이 여러 개여도(MY·설정·탈퇴·웹 로그아웃·강제 로그아웃·세션 만료) 모두 토큰을 지우므로 빠짐없이 지나간다.
 *  · 회원 기억값(미리 눌러 둔 찜·최근 검색·배지·캐시 …)은 [register] 로 지우개를 걸어 둔다 — 바뀌는 즉시 지운다.
 *  · 화면(ViewModel·Compose)은 [state] 의 epoch 가 오르면 회원 것을 비우고(로그아웃) 새 계정 것으로 다시 받는다(로그인).
 *  같은 회원의 토큰 갱신은 바뀜이 아니다(아무것도 하지 않는다).
 *  iOS(Muyeon) SessionEvents 와 같은 규칙.
 */
object SessionEvents {
    /** @param userKey 회원 번호(JWT sub). 비회원은 null. */
    data class Change(val epoch: Int, val userKey: String?) {
        val loggedIn: Boolean get() = userKey != null
    }

    /** 바뀜 지우개 — previous 가 있으면(누군가 로그인해 있다 떠난다) 그 계정 것을 지운다. */
    fun interface Reset {
        fun onAccountChanged(previous: String?, current: String?)
    }

    private val _state = MutableStateFlow(Change(0, null))
    val state: StateFlow<Change> = _state.asStateFlow()
    private val resets = CopyOnWriteArrayList<Reset>()

    /** 앱 시작 — 지금 회원을 적어 둔다(바뀜 아님 · epoch 그대로). */
    @Synchronized
    fun start(userKey: String?) {
        _state.value = _state.value.copy(userKey = userKey)
    }

    fun register(reset: Reset) { resets += reset }

    /**
     * 토큰을 저장했다(userKey) · 지웠다(null).
     * @return 계정이 바뀌었으면 true(지우개를 돌리고 epoch +1).
     */
    @Synchronized
    fun onAccount(userKey: String?): Boolean {
        val prev = _state.value
        if (prev.userKey == userKey) return false
        resets.forEach { runCatching { it.onAccountChanged(prev.userKey, userKey) } }
        _state.value = Change(prev.epoch + 1, userKey)
        return true
    }

    /** 테스트용 — 처음 상태로. */
    @Synchronized
    fun resetForTest() {
        resets.clear()
        _state.value = Change(0, null)
    }
}

/**
 * ★ 2026-11-09 화면 기억값 저장소(KeyValueStore · prefs `muyeon.uiState`) 키 분류 — 로그아웃 때 지울지.
 *
 *  회원 것(지움): 그 계정이 남긴 것 — 다음 사람에게 보이면 안 된다.
 *  기기 화면값(남김): 칩·보기 방식·탭 같은 화면 기억(웹 usePersistedState localStorage 와 같다 — 웹도 로그아웃 때 남긴다).
 *  새 키를 만들면 둘 중 하나에 넣는다(SessionKeysTest 가 겹침·빠짐을 본다). iOS 와 같은 목록.
 */
object SessionKeys {
    /** 회원 것 — 계정이 떠나면 지운다. */
    val PER_USER: List<String> = listOf(
        "scrap.pending", // 로그인 전에 누른 찜(로그인하면 그 계정으로 넣는다 — 로그아웃 뒤에는 버린다)
    )

    /** 기기 화면값 — 남긴다. */
    val DEVICE: List<String> = listOf(
        "home.mode", "home.genreOrder",
        "recruit.hire.view",
        "likes.tab", "likes.view",
        "quoteFeed.category", "quoteFeed.view",
        "lessonList.genre", "lessonList.view",
        "lesson.genre", "lesson.view", "lesson.regionFilter",
        "performance.genre", "performance.detail",
        "community.category",
        "dancers.major", "dancers.gender", "dancers.hasVideo",
        "faq.tab",
        // 하단 [공연] 칸 점 — 공개 글의 '마지막으로 본 시각'이라 기기 것(계정마다 다를 이유가 없다).
        "tabUpdates.performance.lastSeen",
    )

    fun clearPerUser(store: KeyValueStore) = PER_USER.forEach { store.putString(it, null) }
}
