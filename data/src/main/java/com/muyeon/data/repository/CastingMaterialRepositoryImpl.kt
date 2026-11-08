package com.muyeon.data.repository

import com.muyeon.data.common.safeApiCall
import com.muyeon.data.common.safeUnitCall
import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.model.castingmaterial.CastingMaterialBatch
import com.muyeon.domain.model.castingmaterial.CastingMaterialKind
import com.muyeon.domain.repository.CastingMaterialRepository
import com.muyeon.network.api.CastingMaterialApi
import com.muyeon.network.dto.castingmaterial.CreateCastingMaterialBody
import com.squareup.moshi.Moshi

/** ★ 2026-11-08 묶음 2 — 캐스팅·공연 자료 업로드. */
class CastingMaterialRepositoryImpl(private val api: CastingMaterialApi, private val moshi: Moshi) : CastingMaterialRepository {
    override suspend fun access(): ApiResult<Boolean> = safeApiCall(moshi) { api.access() }.map { it.allowed == true }
    override suspend fun list(kind: CastingMaterialKind): ApiResult<List<CastingMaterialBatch>> =
        safeApiCall(moshi) { api.list(kind.code) }.map { d -> d.items.orEmpty().mapNotNull { it.toDomain() } }
    override suspend fun create(kind: CastingMaterialKind, imageUrls: List<String>, instagramUrl: String?, linkUrl: String?): ApiResult<Unit> =
        safeUnitCall(moshi) { api.create(CreateCastingMaterialBody(kind.code, imageUrls, instagramUrl, linkUrl)) }
}
