package com.muyeon.domain.usecase.safety

import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.repository.SafetyRepository

class ReportUseCase(private val repo: SafetyRepository) {
    suspend operator fun invoke(targetType: String, targetId: Long, reason: String, detail: String?): ApiResult<Unit> =
        repo.report(targetType, targetId, reason, detail?.trim()?.ifEmpty { null })
}

class BlockUserUseCase(private val repo: SafetyRepository) {
    suspend operator fun invoke(userId: Long): ApiResult<Unit> = repo.block(userId)
}
