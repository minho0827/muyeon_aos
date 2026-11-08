package com.muyeon.network.dto.academy

import com.muyeon.domain.model.academy.AcademyLinkedLesson
import com.muyeon.domain.model.academy.AcademyOwner
import com.muyeon.domain.model.academy.AcademyProfileForm
import com.squareup.moshi.JsonClass

/** ★ 2026-11-08 묶음 2 — GET·PATCH /auth/me/academy(auth.service getAcademyProfile 칸 그대로). PATCH 도 같은 모양으로 보낸다(웹 ...form). */
@JsonClass(generateAdapter = true)
data class AcademyProfileDto(
    val academyName: String? = null,
    val academyImages: List<String>? = null,
    val academyGenres: List<String>? = null,
    val academyKeywords: List<String>? = null,
    val academyRegion: String? = null,
    val academyAddress: String? = null,
    val academyBusinessHours: String? = null,
    val academyFacilities: List<String>? = null,
    val academyParkingInfo: String? = null,
    val academyHomepage: String? = null,
    val academyIntro: String? = null,
    val wantGenres: List<String>? = null,
    val wantFields: List<String>? = null,
) {
    fun toDomain() = AcademyProfileForm(
        academyName.orEmpty(), academyImages.orEmpty(), academyGenres.orEmpty(), academyKeywords.orEmpty(),
        academyRegion.orEmpty(), academyAddress.orEmpty(), academyBusinessHours.orEmpty(), academyFacilities.orEmpty(),
        academyParkingInfo.orEmpty(), academyHomepage.orEmpty(), academyIntro.orEmpty(), wantGenres.orEmpty(), wantFields.orEmpty(),
    )

    companion object {
        fun from(f: AcademyProfileForm) = AcademyProfileDto(
            f.academyName, f.academyImages, f.academyGenres, f.academyKeywords, f.academyRegion, f.academyAddress,
            f.academyBusinessHours, f.academyFacilities, f.academyParkingInfo, f.academyHomepage, f.academyIntro, f.wantGenres, f.wantFields,
        )
    }
}

/** GET /auth/me — 대표원장 이름·내 번호만. */
@JsonClass(generateAdapter = true)
data class AcademyOwnerDto(val id: Long? = null, val name: String? = null) {
    fun toDomain() = AcademyOwner(id, name.orEmpty())
}

@JsonClass(generateAdapter = true)
data class AcademyLessonDto(
    val id: Long? = null, val title: String? = null, val images: List<String>? = null,
    val genre: String? = null, val scheduleText: String? = null,
)

/** GET /academies/:id — 미리보기 '수업 안내'만 읽는다. */
@JsonClass(generateAdapter = true)
data class AcademyPublicDto(val lessons: List<AcademyLessonDto>? = null) {
    fun toLessons(): List<AcademyLinkedLesson> = lessons.orEmpty().mapNotNull { l ->
        l.id?.let { AcademyLinkedLesson(it, l.title, l.images?.firstOrNull(), l.genre, l.scheduleText) }
    }
}
