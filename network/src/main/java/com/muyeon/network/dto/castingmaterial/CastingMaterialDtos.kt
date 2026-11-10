package com.muyeon.network.dto.castingmaterial

import com.muyeon.domain.model.castingmaterial.CastingMaterialBatch
import com.muyeon.domain.model.castingmaterial.CastingMaterialCreated
import com.muyeon.domain.model.castingmaterial.CastingMaterialDeleted
import com.muyeon.domain.model.castingmaterial.CastingMaterialSkipped
import com.squareup.moshi.JsonClass

/** ★ 2026-11-08 묶음 2 — casting-materials.service list() 의 items. result 는 AI 처리 결과(created · skipped). */
@JsonClass(generateAdapter = true)
data class CastingMaterialCreatedDto(val title: String? = null, val castingId: Long? = null, val performanceId: Long? = null)

@JsonClass(generateAdapter = true)
data class CastingMaterialSkippedDto(val title: String? = null, val image: Int? = null, val reason: String? = null)

@JsonClass(generateAdapter = true)
data class CastingMaterialResultDto(val created: List<CastingMaterialCreatedDto>? = null, val skipped: List<CastingMaterialSkippedDto>? = null)

@JsonClass(generateAdapter = true)
data class CastingMaterialBatchDto(
    val id: Long? = null, val status: String? = null, val imageCount: Int? = null,
    val instagramUrl: String? = null, val linkUrl: String? = null, val createdAt: String? = null,
    val result: CastingMaterialResultDto? = null, val errorMessage: String? = null,
) {
    fun toDomain(): CastingMaterialBatch? = id?.let {
        CastingMaterialBatch(
            it, status.orEmpty(), imageCount ?: 0, instagramUrl?.takeIf { s -> s.isNotBlank() }, linkUrl?.takeIf { s -> s.isNotBlank() }, createdAt,
            result?.created.orEmpty().mapNotNull { c -> if (c.castingId == null && c.performanceId == null) null else CastingMaterialCreated(c.title.orEmpty(), c.castingId, c.performanceId) },
            result?.skipped.orEmpty().map { s -> CastingMaterialSkipped(s.title?.takeIf { t -> t.isNotBlank() }, s.image, s.reason) },
            errorMessage?.takeIf { s -> s.isNotBlank() },
        )
    }
}

@JsonClass(generateAdapter = true)
data class CastingMaterialListDto(val items: List<CastingMaterialBatchDto>? = null)

/** ★ 2026-11-10 DELETE /casting-materials/:id — keptCreated = 이 묶음으로 만든(지우지 않고 남는) 공고·공연. */
@JsonClass(generateAdapter = true)
data class CastingMaterialDeleteDto(val ok: Boolean? = null, val id: Long? = null, val keptCreated: List<CastingMaterialCreatedDto>? = null) {
    fun toDomain(fallbackId: Long) = CastingMaterialDeleted(
        id ?: fallbackId,
        keptCreated.orEmpty().map { c -> CastingMaterialCreated(c.title.orEmpty(), c.castingId, c.performanceId) },
    )
}

/** POST /casting-materials — 빈 링크는 null(Moshi 가 빼고 보낸다). */
@JsonClass(generateAdapter = true)
data class CreateCastingMaterialBody(val kind: String, val imageUrls: List<String>, val instagramUrl: String? = null, val linkUrl: String? = null)
