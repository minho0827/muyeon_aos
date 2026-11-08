package com.muyeon.data.repository

import com.muyeon.data.common.safeApiCall
import com.muyeon.data.common.safeUnitCall
import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.model.academy.AcademyLinkedLesson
import com.muyeon.domain.model.academy.AcademyOwner
import com.muyeon.domain.model.academy.AcademyProfileForm
import com.muyeon.domain.repository.AcademyProfileRepository
import com.muyeon.network.api.AcademyProfileApi
import com.muyeon.network.dto.academy.AcademyProfileDto
import com.squareup.moshi.Moshi

/** ★ 2026-11-08 묶음 2 — 학원 정보 관리. */
class AcademyProfileRepositoryImpl(private val api: AcademyProfileApi, private val moshi: Moshi) : AcademyProfileRepository {
    override suspend fun load(): ApiResult<AcademyProfileForm> = safeApiCall(moshi) { api.load() }.map { it.toDomain() }
    override suspend fun save(form: AcademyProfileForm): ApiResult<Unit> = safeUnitCall(moshi) { api.save(AcademyProfileDto.from(form)) }
    override suspend fun owner(): ApiResult<AcademyOwner> = safeApiCall(moshi) { api.owner() }.map { it.toDomain() }
    override suspend fun lessons(academyId: Long): ApiResult<List<AcademyLinkedLesson>> =
        safeApiCall(moshi) { api.academy(academyId) }.map { it.toLessons() }
}
