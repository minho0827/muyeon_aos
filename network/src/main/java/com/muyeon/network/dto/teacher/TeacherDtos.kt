package com.muyeon.network.dto.teacher

import com.muyeon.domain.model.teacher.TeacherCard
import com.squareup.moshi.JsonClass

/** profiles.service.ts teacherPreview. ⚠️ 서버 null 칸은 전부 `?`. */
@JsonClass(generateAdapter = true)
data class TeacherCardDto(
    val id: Long? = null,
    val name: String? = null,
    val image: String? = null,
    val imageUrl: String? = null,
    val region: String? = null,
    val activeRegion: String? = null,
    val genres: List<String>? = null,
    val fields: List<String>? = null,
    val career: String? = null,
    val isDemoProfile: Boolean? = null,
    val promotedUntil: String? = null,
) {
    fun toDomain(): TeacherCard? {
        val tid = id ?: return null
        return TeacherCard(
            id = tid,
            name = name.orEmpty(),
            image = (image ?: imageUrl)?.takeIf { it.isNotBlank() },
            region = (region ?: activeRegion)?.takeIf { it.isNotBlank() },
            genres = genres.orEmpty().filter { it.isNotBlank() },
            fields = fields.orEmpty().filter { it.isNotBlank() },
            career = career?.takeIf { it.isNotBlank() },
            isDemoProfile = isDemoProfile == true,
            promotedUntil = promotedUntil,
        )
    }
}
