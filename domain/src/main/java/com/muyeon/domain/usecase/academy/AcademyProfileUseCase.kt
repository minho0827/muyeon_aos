package com.muyeon.domain.usecase.academy

import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.model.academy.AcademyProfileForm
import com.muyeon.domain.repository.AcademyProfileRepository

/**
 * ★ 2026-11-08 묶음 2 — 학원 정보 관리(웹 AcademyProfile.js).
 *   저장 규칙(웹 save 그대로): 키워드는 쉼표로 나눠 앞뒤 공백 제거·빈 칸 빼고 최대 8개, 학원명은 앞뒤 공백 제거.
 *   필수(학원명·사진 1장 이상·학원 소개·원하는 장르 또는 분야 1개 이상) 확인은 화면의 RequiredForm 이 먼저 막는다 —
 *   여기서는 같은 규칙을 한 번 더 지켜 빈 값이 서버로 가지 않게 한다.
 */
class AcademyProfileUseCase(private val repo: AcademyProfileRepository) {
    suspend fun load() = repo.load()
    suspend fun owner() = repo.owner()
    suspend fun lessons(academyId: Long) = repo.lessons(academyId)

    /** 빠진 필수 칸 — 웹 save 의 확인 순서 그대로. 다 채웠으면 null. */
    fun missing(form: AcademyProfileForm): Missing? = when {
        form.academyName.isBlank() -> Missing.NAME
        form.academyImages.isEmpty() -> Missing.IMAGES
        form.academyIntro.isBlank() -> Missing.INTRO
        form.wantGenres.isEmpty() && form.wantFields.isEmpty() -> Missing.WANT
        else -> null
    }

    suspend fun save(form: AcademyProfileForm, keywordText: String): ApiResult<Unit> {
        missing(form)?.let { return ApiResult.Error(IllegalArgumentException(it.name), it.message) }
        return repo.save(form.copy(academyName = form.academyName.trim(), academyKeywords = toKeywords(keywordText)))
    }

    enum class Missing(val message: String) {
        // 웹 setErr 문구 그대로(서버 계약 문구가 아니라 화면 검증 문구 — 화면은 RequiredForm 토스트를 쓴다).
        NAME("공개 프로필에 표시할 학원명을 입력해 주세요."),
        IMAGES("학원을 보여주는 대표 사진을 1장 이상 등록해 주세요."),
        INTRO("학원 소개를 입력해 주세요."),
        WANT("맞춤 강사 추천을 받으려면 원하는 장르·분야를 1개 이상 선택해 주세요."),
    }

    companion object {
        const val MAX_IMAGES = 8
        const val MAX_KEYWORDS = 8
        const val MAX_INTRO = 1500
        /** 웹 toKeywords — 쉼표로 나눠 다듬고 최대 8개. */
        fun toKeywords(text: String): List<String> = text.split(",").map { it.trim() }.filter { it.isNotEmpty() }.take(MAX_KEYWORDS)
    }
}
