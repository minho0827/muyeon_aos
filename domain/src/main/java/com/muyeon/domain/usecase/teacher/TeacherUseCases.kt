package com.muyeon.domain.usecase.teacher

import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.common.CursorPage
import com.muyeon.domain.common.CursorPaging
import com.muyeon.domain.model.region.ActiveRegion
import com.muyeon.domain.model.teacher.LessonGenres
import com.muyeon.domain.model.teacher.TeacherCard
import com.muyeon.domain.repository.TeacherRepository

/** 강사 탭 목록 — 장르 칩이 바레·발레핏이면 분야 코드로 거른다(웹 GENRE_FIELD). [genre] null = 전체. */
class GetLessonTeachersPageUseCase(private val repo: TeacherRepository) {
    suspend operator fun invoke(genre: String?, region: ActiveRegion?, cursor: String?): ApiResult<CursorPage<TeacherCard>> {
        val field = genre?.let { LessonGenres.FIELD_OF[it] }
        return repo.lessonTeachersPage(genre.takeIf { field == null }, field, region, cursor, CursorPaging.DEFAULT_LIMIT)
    }
}

/** 받은 목록 안에서 시·도로 거르기(웹: 서버 필터가 아니라 화면 필터). */
fun List<TeacherCard>.inSido(sido: String?): List<TeacherCard> =
    if (sido.isNullOrBlank()) this else filter { it.region.orEmpty().contains(sido) }
