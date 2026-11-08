package com.muyeon.domain.model.region

/**
 * ★ 2026-11-08 (b4) 관심 지역 목록 — 웹 context/RegionContext.js 의 `{ regions, repCode }`(localStorage muyeonRegions 와 같은 모양).
 *   최대 [MAX] 개. 대표([repCode])가 홈·목록의 지역(= [ActiveRegion]).
 */
data class SavedRegions(val regions: List<ActiveRegion> = emptyList(), val repCode: String? = null) {
    val representative: ActiveRegion? get() = regions.firstOrNull { it.code == repCode }

    companion object {
        /** 웹 MAX_REGIONS. */
        const val MAX = 5
    }
}

/** `GET /regions` 한 줄 — 시군구(code 5자리). 웹 AddRegion·RegionSelect 가 읽는 값. */
data class RegionEntry(val code: String, val sido: String, val sigungu: String, val name: String)

/** 지역 추가·삭제 결과 — 웹 addRegion/removeRegion 의 `{ ok, reason }`. */
enum class RegionEditResult { OK, ALREADY, MAX, REPRESENTATIVE }
