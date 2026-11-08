package com.muyeon.domain.repository

import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.model.academy.AcademyLinkedLesson
import com.muyeon.domain.model.academy.AcademyOwner
import com.muyeon.domain.model.academy.AcademyProfileForm

/** ★ 2026-11-08 묶음 2 — 학원 정보 관리(GET·PATCH /auth/me/academy) · 대표원장(GET /auth/me) · 미리보기 수업(GET /academies/:id). */
interface AcademyProfileRepository {
    suspend fun load(): ApiResult<AcademyProfileForm>
    suspend fun save(form: AcademyProfileForm): ApiResult<Unit>
    suspend fun owner(): ApiResult<AcademyOwner>
    suspend fun lessons(academyId: Long): ApiResult<List<AcademyLinkedLesson>>
}
