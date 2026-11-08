package com.muyeon.domain.usecase.write

import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.model.write.CommunityDraft
import com.muyeon.domain.model.write.SubEditForm
import com.muyeon.domain.model.write.SubUpdate
import com.muyeon.domain.repository.CommunityWriteRepository
import com.muyeon.domain.repository.SubEditRepository

/** ★ 2026-11-08 묶음 3 — 대타공고 수정: 기존 값 읽기 · 저장(웹 SubCreate 수정 모드). */
class EditSubUseCase(private val repo: SubEditRepository) {
    suspend fun load(id: Long): ApiResult<SubEditForm> = repo.sub(id)
    suspend fun save(id: Long, update: SubUpdate): ApiResult<Unit> = repo.updateSub(id, update)
}

/**
 * ★ 2026-11-08 묶음 3 — 커뮤니티 글 저장(웹 CommunityWrite submit). [editId] 가 있으면 수정.
 *   제목은 앞뒤 공백을 뗀다(본문은 그대로 — 웹과 같다).
 */
class WriteCommunityPostUseCase(private val repo: CommunityWriteRepository) {
    suspend fun save(editId: Long?, draft: CommunityDraft): ApiResult<Unit> {
        val d = draft.copy(title = draft.title.trim(), images = draft.images.take(IMAGES_MAX))
        return if (editId != null) repo.update(editId, d) else repo.create(d)
    }

    companion object {
        const val TITLE_MAX = 50
        const val BODY_MAX = 1000
        const val IMAGES_MAX = 10
        /** 웹 constants/communityCategories.js 순서. */
        val CATEGORIES = listOf("QUESTION", "CHAT", "ACADEMY_REVIEW", "SHOW_REVIEW", "MEETING", "MARKET")
    }
}
