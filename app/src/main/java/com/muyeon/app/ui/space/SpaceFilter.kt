package com.muyeon.app.ui.space

/**
 * ★ 2026-11-08 공간 대관 필터(사용자 요청) — 이용유형 · 가격(천원 단위로 입력) · 편의시설(DB spaces.facilities 키, 모두 갖춘 곳).
 *   서버(GET /spaces?facilities&bookingType&minPrice&maxPrice)가 거르고, 서버 배포 전에도 맞게 화면에서 한 번 더 본다(이용유형은 서버만).
 */
data class SpaceFilter(
    /** null = 전체 · HOURLY(시간당) · PACKAGE(패키지). */
    val bookingType: String? = null,
    /** 천원 단위 숫자 글자(입력 그대로). */
    val minK: String = "",
    val maxK: String = "",
    val facilities: Set<String> = emptySet(),
) {
    val minWon: Int? get() = minK.toIntOrNull()?.times(1000)
    val maxWon: Int? get() = maxK.toIntOrNull()?.times(1000)

    /** 걸린 조건 수(필터 알약 "필터 3"). */
    val count: Int get() = (if (bookingType != null) 1 else 0) + (if (minWon != null || maxWon != null) 1 else 0) + facilities.size
    val isEmpty: Boolean get() = count == 0

    /** 최대가 최소보다 작으면 문구, 맞으면 null. */
    val priceError: String? get() {
        val lo = minWon; val hi = maxWon
        return if (lo != null && hi != null && hi < lo) "최대 가격은 최소 가격보다 커야 해요." else null
    }

    fun matches(s: SpaceListItem): Boolean =
        facilities.all { it in s.facilities } &&
            (minWon == null || (s.minPrice ?: 0) >= minWon!!) &&
            (maxWon == null || (s.minPrice != null && s.minPrice <= maxWon!!))

    companion object {
        val BOOKING_TYPES = listOf(null to "전체", "HOURLY" to "시간당", "PACKAGE" to "패키지")
    }
}
