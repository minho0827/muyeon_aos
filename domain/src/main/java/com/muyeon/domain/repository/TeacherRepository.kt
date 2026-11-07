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
    /** 강사 프로필 전체(레슨 여부 무관) — 홈 '강사'(에이전시형 그리드). 지역은 가까운 순 정렬에만 쓰인다(서버 pageScore). */
    suspend fun teachersPage(genre: String?, region: ActiveRegion?, cursor: String?, limit: Int): ApiResult<CursorPage<TeacherCard>>
    suspend fun lessonTeachersPage(genre: String?, field: String?, region: ActiveRegion?, cursor: String?, limit: Int, sort: String? = null): ApiResult<CursorPage<TeacherCard>>
}
