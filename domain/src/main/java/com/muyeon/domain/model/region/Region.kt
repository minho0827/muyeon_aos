package com.muyeon.domain.model.region

/**
 * 대표(활성) 지역 — 웹 context/RegionContext.js 의 active. 수도권은 시군구(kind=sigungu, code 5자리),
 *   그 밖은 시·도(kind=sido, code 2자리). 목록 API 에는 웹과 같은 방식으로 실어 보낸다.
 */
data class ActiveRegion(val kind: String, val code: String, val name: String) {
    companion object {
        const val SIGUNGU = "sigungu"
        const val SIDO = "sido"
        /** 시군구까지 나눠 보는 수도권(웹 METRO_SIDO). */
        val METRO = setOf("서울", "경기", "인천")
    }
}

/** 좌표 — 앱(:app)이 위치 권한·센서로 구한다. */
data class Coordinates(val lat: Double, val lng: Double)
