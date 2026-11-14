package com.muyeon.app.ui.review.quick

import com.muyeon.app.R
import com.muyeon.app.ui.review.ReviewOptions

/**
 * ★ 2026-11-14 빠른 후기(당근 '거래 후기 보내기' 흐름, iOS QuickReviewOptions 1:1).
 *  얼굴 3개 → 별점(별로예요 2 · 좋아요 4 · 최고예요 5). 별점은 3축(강의력·소통·시설) 모두 같은 값으로 보낸다(서버가 평균).
 */
enum class ReviewFace(val label: String, val rating: Int, val offRes: Int, val onRes: Int) {
    BAD("별로예요", 2, R.drawable.review_face_bad_off, R.drawable.review_face_bad_on),
    GOOD("좋아요!", 4, R.drawable.review_face_good_off, R.drawable.review_face_good_on),
    BEST("최고예요!", 5, R.drawable.review_face_best_off, R.drawable.review_face_best_on),
}

object QuickReviewOptions {
    /** 좋았던 점 — 서버 REVIEW_TAGS(공개, 강사 프로필 집계). */
    val goodTags: List<Pair<String, String>> get() = ReviewOptions.tags

    /** 아쉬운 점 — 서버 REVIEW_PRIVATE_TAGS(비공개: 강사 프로필·후기 목록에 안 보인다). */
    val badTags: List<Pair<String, String>> = listOf(
        "LATE" to "시간 약속을 안 지켜요",
        "UNCLEAR" to "설명이 어려워요",
        "UNPREPARED" to "수업 준비가 부족해요",
        "SLOW_REPLY" to "응답이 느려요",
        "MISMATCH" to "소개와 달라요",
    )

    fun tagsFor(face: ReviewFace): List<Pair<String, String>> = if (face == ReviewFace.BAD) badTags else goodTags

    fun reasonQuestion(face: ReviewFace): String = when (face) {
        ReviewFace.BAD -> "어떤 점이 아쉬웠나요?"
        ReviewFace.GOOD -> "어떤 점이 좋았나요?"
        ReviewFace.BEST -> "어떤 점이 최고였나요?"
    }

    fun ratingsOf(face: ReviewFace): Map<String, Int> = ReviewOptions.ratingAxes.associate { it.first to face.rating }
}
