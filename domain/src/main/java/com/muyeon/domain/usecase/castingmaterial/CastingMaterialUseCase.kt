package com.muyeon.domain.usecase.castingmaterial

import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.model.castingmaterial.CastingMaterialKind
import com.muyeon.domain.repository.CastingMaterialRepository

/**
 * ★ 2026-11-08 묶음 2 — 캐스팅·공연 자료 업로드(웹 CastingMaterials.js). 링크는 앞뒤 공백을 지우고 비면 보내지 않는다.
 *   링크 모양 확인(인스타그램·https)은 서버가 한다(거절 문구를 화면이 그대로 보여 준다).
 */
class CastingMaterialUseCase(private val repo: CastingMaterialRepository) {
    suspend fun access() = repo.access()
    suspend fun list(kind: CastingMaterialKind) = repo.list(kind)
    suspend fun delete(id: Long) = repo.delete(id)

    suspend fun submit(kind: CastingMaterialKind, images: List<String>, instagram: String, link: String): ApiResult<Unit> {
        if (images.isEmpty()) return ApiResult.Error(IllegalArgumentException("images"), NO_IMAGES)
        return repo.create(kind, images, instagram.trim().ifEmpty { null }, link.trim().ifEmpty { null })
    }

    companion object {
        const val MAX_IMAGES = 20
        const val NO_IMAGES = "캡처 사진을 1장 이상 올려 주세요."
    }
}
