package com.muyeon.domain.usecase.home

import com.muyeon.domain.common.KeyValueStore
import com.muyeon.domain.model.home.TabUpdateDot
import com.muyeon.domain.repository.HomeRepository

/**
 * ★ 2026-11-09 하단 [공연] 칸 새 글 점 — 서버 최신 시각과 기기에 저장한 '마지막으로 본 시각'을 비교한다.
 *   실패(서버 아직 없음 404 포함)면 점 없음.
 */
class TabUpdatesUseCase(private val repo: HomeRepository, private val store: KeyValueStore) {
    /** 마지막으로 받은 서버 최신 시각 — 칸을 열 때 '본 것'으로 저장한다. */
    @Volatile private var latest: String? = null

    /** @return 점을 켤지. */
    suspend fun check(onTab: Boolean): Boolean {
        val l = repo.tabUpdates().getOrNull()?.latest
        if (l != null) latest = l
        val d = TabUpdateDot.decide(l, store.getString(KEY), onTab)
        d.saveSeen?.let { store.putString(KEY, it) }
        return d.dot
    }

    /** 공연·캐스팅 칸을 열었다 — 지금까지 받은 최신 것을 본 것으로. */
    fun markSeen() {
        val l = latest ?: return
        val d = TabUpdateDot.decide(l, store.getString(KEY), onTab = true)
        d.saveSeen?.let { store.putString(KEY, it) }
    }

    companion object {
        const val KEY = "tabUpdates.performance.lastSeen"
    }
}
