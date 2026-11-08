package com.muyeon.network.dto.region

import com.muyeon.domain.model.region.ActiveRegion
import com.squareup.moshi.JsonClass

/** `GET /regions/nearest` → regions 행 `{ code, name, sido, … }` 또는 null. */
@JsonClass(generateAdapter = true)
data class RegionDto(val code: String? = null, val name: String? = null, val sido: String? = null) {
    /** 수도권은 시군구, 그 밖은 시·도로(웹 RegionProvider). */
    fun toActive(): ActiveRegion? {
        val c = code?.takeIf { it.isNotBlank() } ?: return null
        val s = sido.orEmpty()
        return if (s in ActiveRegion.METRO) ActiveRegion(ActiveRegion.SIGUNGU, c, name.orEmpty())
        else ActiveRegion(ActiveRegion.SIDO, c.take(2), s.ifEmpty { name.orEmpty() })
    }
}

/** 웹 localStorage muyeonRegions 의 지역 한 칸. */
@JsonClass(generateAdapter = true)
data class WebRegionDto(val kind: String? = null, val code: String? = null, val name: String? = null)

@JsonClass(generateAdapter = true)
data class WebRegionsDto(val regions: List<WebRegionDto>? = null, val repCode: String? = null)

@JsonClass(generateAdapter = true)
data class UiFlagsDto(val flags: Map<String, Any?>? = null)

/** ★ (b4) `GET /regions` 한 줄. */
@JsonClass(generateAdapter = true)
data class RegionRowDto(val code: String? = null, val sido: String? = null, val sigungu: String? = null, val name: String? = null) {
    fun toDomain(): com.muyeon.domain.model.region.RegionEntry? {
        val c = code?.takeIf { it.isNotBlank() } ?: return null
        return com.muyeon.domain.model.region.RegionEntry(c, sido.orEmpty(), sigungu.orEmpty(), name ?: listOfNotNull(sido, sigungu).joinToString(" "))
    }
}
