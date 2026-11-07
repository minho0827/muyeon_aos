package com.muyeon.domain.model.scrap

/**
 * 찜(스크랩) 대상 — 서버 ScrapTargetType 과 같은 값. [pathSegment] 는 토글 경로 `/{segment}/:id/scrap`
 *   (scraps.controller.ts 가 영역별 경로로 받는다).
 */
enum class ScrapTarget(val serverType: String, val pathSegment: String) {
    PERFORMANCE("PERFORMANCE", "performances"),
    TEACHER("TEACHER", "teachers"),
    LESSON("LESSON", "lesson-products"),
    JOB("JOB", "jobs"),
    SUB("SUB", "subs"),
    CASTING("CASTING", "casting"),
    QUOTE("QUOTE", "quotes"),
    SPACE("SPACE", "spaces"),
}
