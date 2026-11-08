package com.muyeon.domain.repository

import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.model.write.CommunityDraft
import com.muyeon.domain.model.write.SubEditForm
import com.muyeon.domain.model.write.SubUpdate

/** ★ 2026-11-08 묶음 3 — 대타공고 수정(작성자만 — 서버가 403). */
interface SubEditRepository {
    suspend fun sub(id: Long): ApiResult<SubEditForm>
    suspend fun updateSub(id: Long, update: SubUpdate): ApiResult<Unit>
}

/** ★ 2026-11-08 묶음 3 — 커뮤니티 글쓰기/수정(수정은 본인 글만 — 서버가 403). 고칠 글 읽기는 CommunityRepository.post. */
interface CommunityWriteRepository {
    suspend fun create(draft: CommunityDraft): ApiResult<Unit>
    suspend fun update(id: Long, draft: CommunityDraft): ApiResult<Unit>
}
