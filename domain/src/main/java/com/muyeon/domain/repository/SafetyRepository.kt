package com.muyeon.domain.repository

import com.muyeon.domain.common.ApiResult

/** 신고·차단(Apple 1.2 안전 기능). 웹 ReportButton·BlockUserButton 과 같은 서버 계약. */
interface SafetyRepository {
    /** POST /reports — 같은 대상을 다시 신고해도 성공. */
    suspend fun report(targetType: String, targetId: Long, reason: String, detail: String?): ApiResult<Unit>
    /** POST /users/:id/block */
    suspend fun block(userId: Long): ApiResult<Unit>
}
