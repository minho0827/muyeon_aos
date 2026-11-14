package com.muyeon.app.ui.review.quick

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.muyeon.app.ui.review.ReviewApi
import com.muyeon.app.ui.review.ReviewSubmit

/**
 * ★ 2026-11-14 빠른 후기 상태 한 곳(iOS QuickReviewCoordinator 1:1) — 단계 화면들은 이 값만 읽고 바꾼다.
 *  화면을 그리지 않는다. 단계 이동은 NavHost(QuickReviewActivity)가 맡는다.
 */
class QuickReviewCoordinator(
    val teacherId: Int,
    val teacherName: String,
    val lessonLine: String,
    private val api: ReviewApi,
) {
    var face by mutableStateOf<ReviewFace?>(null)
        private set
    var tags by mutableStateOf<Set<String>>(emptySet())
        private set
    var content by mutableStateOf("")
    var images by mutableStateOf<List<String>>(emptyList())
    var uploading by mutableStateOf(false)
    var submitting by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)

    /** 얼굴을 바꾸면 고른 사유는 비운다(좋았던 점 ↔ 아쉬운 점 목록이 다르다). */
    fun selectFace(f: ReviewFace) {
        if (face != f) tags = emptySet()
        face = f
    }

    fun toggleTag(code: String) {
        tags = if (code in tags) tags - code else tags + code
    }

    val canGoNext: Boolean get() = face != null && tags.isNotEmpty()

    /** 고른 사유 문구(완료 화면 요약). */
    val chosenLabels: List<String>
        get() = face?.let { f -> QuickReviewOptions.tagsFor(f).filter { it.first in tags }.map { it.second } } ?: emptyList()

    suspend fun submit(): Boolean {
        val f = face ?: return false
        if (submitting) return false
        submitting = true
        error = null
        val r = api.submit(
            ReviewSubmit(
                teacherId = teacherId,
                ratings = QuickReviewOptions.ratingsOf(f),
                revisit = null,
                content = content.trim().ifEmpty { null },
                lessonType = null,
                purpose = null,
                durationBucket = null,
                tags = tags.toList(),
                images = images,
                quick = true,
            ),
        )
        submitting = false
        r.onFailure { error = it.message ?: "후기를 보내지 못했어요. 잠시 뒤 다시 시도해 주세요." }
        return r.isSuccess
    }
}
