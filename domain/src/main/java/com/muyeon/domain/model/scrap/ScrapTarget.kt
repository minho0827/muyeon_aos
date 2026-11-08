package com.muyeon.domain.model.scrap

/**
 * 찜(스크랩) 대상 — 서버 ScrapTargetType 과 같은 값. [pathSegment] 는 토글 경로 `/{segment}/:id/scrap`
 *   (scraps.controller.ts 가 영역별 경로로 받는다).
 */
/**
 * ★ 2026-11-08 찜 모아보기 한 칸(웹 Likes cardOf 의 재료) — 종류마다 채워지는 칸이 다르다.
 *   [target] 은 앱이 아는 종류일 때만(모르면 null — 화면은 그래도 보여 주고 누르면 아무것도 안 한다).
 */
data class LikedItem(
    val targetType: String,
    val targetId: Long,
    val title: String,
    val image: String?,
    val region: String?,
    val genre: String?,
    val venue: String?,
    val price: Int?,
    val pricePerHour: Int?,
    val fields: List<String>,
    val genres: List<String>,
    val career: String?,
    val salary: String?,
    val category: String?,
    val status: String?,
) {
    val target: ScrapTarget? get() = ScrapTarget.entries.firstOrNull { it.serverType == targetType }
    /** 공고류(JOB·SUB·CASTING) 마감. */
    val closed: Boolean get() = targetType in setOf("JOB", "SUB", "CASTING") && status == "CLOSED"
}

enum class ScrapTarget(val serverType: String, val pathSegment: String) {
    PERFORMANCE("PERFORMANCE", "performances"),
    TEACHER("TEACHER", "teachers"),
    DANCER("DANCER", "dancers"),
    LESSON("LESSON", "lesson-products"),
    JOB("JOB", "jobs"),
    SUB("SUB", "subs"),
    CASTING("CASTING", "casting"),
    QUOTE("QUOTE", "quotes"),
    SPACE("SPACE", "spaces"),
    /** 커뮤니티 글 — 경로 `/community/posts/:id/scrap`(가운데 '/' 는 Retrofit encoded 경로로 넣는다). */
    POST("POST", "community/posts"),
}
