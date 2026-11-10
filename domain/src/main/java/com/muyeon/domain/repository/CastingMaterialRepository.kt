package com.muyeon.domain.repository

import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.model.castingmaterial.CastingMaterialBatch
import com.muyeon.domain.model.castingmaterial.CastingMaterialDeleted
import com.muyeon.domain.model.castingmaterial.CastingMaterialKind

/** ★ 2026-11-08 묶음 2 — 캐스팅·공연 자료 업로드(casting-materials.controller.ts, 무용연 운영 계정 전용). */
interface CastingMaterialRepository {
    suspend fun access(): ApiResult<Boolean>
    suspend fun list(kind: CastingMaterialKind): ApiResult<List<CastingMaterialBatch>>
    /** ★ 11-10 묶음 지우기(올린 계정·관리자, 처리 중이면 409). */
    suspend fun delete(id: Long): ApiResult<CastingMaterialDeleted>
    suspend fun create(kind: CastingMaterialKind, imageUrls: List<String>, instagramUrl: String?, linkUrl: String?): ApiResult<Unit>
}
