package com.muyeon.data.repository

import com.muyeon.data.common.safeApiCall
import com.muyeon.data.common.safeUnitCall
import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.model.write.CommunityDraft
import com.muyeon.domain.model.write.SubEditForm
import com.muyeon.domain.model.write.SubUpdate
import com.muyeon.domain.repository.CommunityWriteRepository
import com.muyeon.domain.repository.SubEditRepository
import com.muyeon.network.api.WriteApi
import com.muyeon.network.dto.write.CommunityPostBody
import com.muyeon.network.dto.write.SubUpdateBody
import com.squareup.moshi.Moshi

/** ★ 2026-11-08 묶음 3 — 대타공고 수정(GET·PATCH /subs/:id). */
class SubEditRepositoryImpl(private val api: WriteApi, private val moshi: Moshi) : SubEditRepository {
    override suspend fun sub(id: Long): ApiResult<SubEditForm> = safeApiCall(moshi) { api.sub(id) }.map { it.toDomain() }
    override suspend fun updateSub(id: Long, update: SubUpdate): ApiResult<Unit> =
        safeUnitCall(moshi) { api.updateSub(id, SubUpdateBody.of(moshi, update)) }
}

/** ★ 2026-11-08 묶음 3 — 커뮤니티 글쓰기(POST)·수정(PATCH). */
class CommunityWriteRepositoryImpl(private val api: WriteApi, private val moshi: Moshi) : CommunityWriteRepository {
    override suspend fun create(draft: CommunityDraft): ApiResult<Unit> = safeUnitCall(moshi) { api.createPost(CommunityPostBody.of(draft, edit = false)) }
    override suspend fun update(id: Long, draft: CommunityDraft): ApiResult<Unit> = safeUnitCall(moshi) { api.updatePost(id, CommunityPostBody.of(draft, edit = true)) }
}
