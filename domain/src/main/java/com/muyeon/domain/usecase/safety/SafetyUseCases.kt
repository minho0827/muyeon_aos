package com.muyeon.domain.usecase.safety

import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.repository.SafetyRepository

class ReportUseCase(private val repo: SafetyRepository) {
    suspend operator fun invoke(targetType: String, targetId: Long, reason: String, detail: String?): ApiResult<Unit> =
        repo.report(targetType, targetId, reason, detail?.trim()?.ifEmpty { null })
}

/** 차단한 사용자 목록·해제(웹 BlockedUsers). */
class BlockedUsersUseCase(private val repo: SafetyRepository) {
    suspend fun list() = repo.blockedUsers()
    suspend fun unblock(userId: Long) = repo.unblock(userId)
}

class BlockUserUseCase(private val repo: SafetyRepository) {
    suspend operator fun invoke(userId: Long): ApiResult<Unit> = repo.block(userId)
}
