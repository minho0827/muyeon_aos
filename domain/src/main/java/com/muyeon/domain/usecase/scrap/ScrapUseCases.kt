package com.muyeon.domain.usecase.scrap

import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.common.TokenStore
import com.muyeon.domain.model.scrap.ScrapTarget
import com.muyeon.domain.repository.ScrapRepository

/**
 * 내 찜 목록 — 로그인 뒤 처음 부를 때 비회원 때 누른 찜(보류)을 먼저 저장한다(웹 useLikes 와 같은 순서).
 *   비회원이면 빈 집합.
 */
class GetLikedIdsUseCase(private val repo: ScrapRepository, private val tokens: TokenStore) {
    suspend operator fun invoke(target: ScrapTarget): Set<Long> {
        if (!tokens.isLoggedIn) return emptySet()
        repo.takePending(target)?.let { repo.setLiked(target, it, true) }
        return repo.likedIds(target).getOrNull() ?: emptySet()
    }
}

/**
 * ★ 2026-11-08 찜 모아보기(웹 Likes) — 공간 찜은 기능을 쉬는 동안 화면에서 뺀다(데이터는 서버에 남는다 · 웹과 같음).
 */
class GetLikedItemsUseCase(private val repo: ScrapRepository) {
    suspend operator fun invoke(target: ScrapTarget?): ApiResult<List<com.muyeon.domain.model.scrap.LikedItem>> =
        repo.likedItems(target).map { l -> l.filter { it.targetType != ScrapTarget.SPACE.serverType } }
}

/** 찜 토글 결과 — 비회원이면 [LoginRequired](화면이 로그인으로 보낸다, 누른 것은 보류에 남는다). */
sealed interface ToggleScrapResult {
    data class Done(val liked: Boolean) : ToggleScrapResult
    data class Failed(val message: String) : ToggleScrapResult
    data object LoginRequired : ToggleScrapResult
}

class ToggleScrapUseCase(private val repo: ScrapRepository, private val tokens: TokenStore) {
    suspend operator fun invoke(target: ScrapTarget, id: Long, currentlyLiked: Boolean): ToggleScrapResult {
        if (!tokens.isLoggedIn) {
            repo.savePending(target, id)
            return ToggleScrapResult.LoginRequired
        }
        return when (val r = repo.setLiked(target, id, !currentlyLiked)) {
            is ApiResult.Success -> ToggleScrapResult.Done(!currentlyLiked)
            is ApiResult.Error -> ToggleScrapResult.Failed(r.message)
        }
    }
}

/** 찜 변경 흐름(그 종류만). */
class ObserveScrapChangesUseCase(private val repo: ScrapRepository) {
    operator fun invoke(target: ScrapTarget): kotlinx.coroutines.flow.Flow<com.muyeon.domain.repository.ScrapChange> =
        kotlinx.coroutines.flow.flow { repo.changes.collect { if (it.target == target) emit(it) } }
}
