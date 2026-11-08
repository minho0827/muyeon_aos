package com.muyeon.data.repository

import com.muyeon.data.common.safeUnitCall
import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.repository.SafetyRepository
import com.muyeon.network.api.SafetyApi
import com.muyeon.network.dto.safety.ReportBody
import com.squareup.moshi.Moshi

class SafetyRepositoryImpl(private val api: SafetyApi, private val moshi: Moshi) : SafetyRepository {
    override suspend fun report(targetType: String, targetId: Long, reason: String, detail: String?): ApiResult<Unit> =
        safeUnitCall(moshi) { api.report(ReportBody(targetType, targetId, reason, detail)) }

    override suspend fun block(userId: Long): ApiResult<Unit> = safeUnitCall(moshi) { api.block(userId) }

    override suspend fun unblock(userId: Long): ApiResult<Unit> = safeUnitCall(moshi) { api.unblock(userId) }

    override suspend fun blockedUsers(): ApiResult<List<com.muyeon.domain.model.safety.BlockedUser>> =
        com.muyeon.data.common.safeApiCall(moshi) { api.myBlocks() }.map { l -> l.mapNotNull { it.toDomain() } }
}
