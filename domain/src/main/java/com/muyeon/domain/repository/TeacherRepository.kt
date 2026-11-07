package com.muyeon.domain.repository

import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.common.CursorPage
import com.muyeon.domain.model.region.ActiveRegion
import com.muyeon.domain.model.teacher.TeacherCard

interface TeacherRepository {
    /**
     * 레슨을 여는 강사 목록(lesson=true). [genre]·[field] 중 하나. [region] 은 대표 지역(없으면 전체).
     * @param sort "rating" 등 — 홈 '회원님이 좋아할 강사'.
     */
    suspend fun lessonTeachersPage(genre: String?, field: String?, region: ActiveRegion?, cursor: String?, limit: Int, sort: String? = null): ApiResult<CursorPage<TeacherCard>>
}
