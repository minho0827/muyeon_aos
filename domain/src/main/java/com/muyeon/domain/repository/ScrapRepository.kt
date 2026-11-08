package com.muyeon.domain.repository

import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.model.scrap.ScrapTarget
import kotlinx.coroutines.flow.Flow

/** 찜이 바뀜(어느 화면에서든) — 같은 대상을 보여 주는 다른 화면이 하트를 맞춘다(목록 ↔ 상세). */
data class ScrapChange(val target: ScrapTarget, val id: Long, val liked: Boolean)

/** 찜(스크랩). 로그인 회원만 서버에 저장된다 — 비회원 누름은 [pending] 에 두고 로그인 뒤 [takePending]. */
interface ScrapRepository {
    val changes: Flow<ScrapChange>

    /** 내가 찜한 대상 id(그 종류만). */
    suspend fun likedIds(target: ScrapTarget): ApiResult<Set<Long>>
    /** 찜 모아보기 — [target] null 이면 전체. */
    suspend fun likedItems(target: ScrapTarget?): ApiResult<List<com.muyeon.domain.model.scrap.LikedItem>>
    suspend fun setLiked(target: ScrapTarget, id: Long, liked: Boolean): ApiResult<Unit>

    /** 비회원이 누른 찜 하나(웹 useLikes pendingLike 와 같다 — 마지막 것만). */
    fun savePending(target: ScrapTarget, id: Long)
    /** 그 종류의 보류 찜이 있으면 꺼내고 지운다. */
    fun takePending(target: ScrapTarget): Long?
}
