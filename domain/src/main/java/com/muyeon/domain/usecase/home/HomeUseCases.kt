package com.muyeon.domain.usecase.home

import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.common.CursorPage
import com.muyeon.domain.model.home.HomeLesson
import com.muyeon.domain.model.home.HomeLessonPreview
import com.muyeon.domain.model.region.ActiveRegion
import com.muyeon.domain.model.teacher.TeacherCard
import com.muyeon.domain.repository.HomeRepository
import com.muyeon.domain.repository.TeacherRepository

/**
 * ★ 2026-11-08 예시(더미) 숨김 — 화면에서만 뺀다(서버 데이터는 그대로, 사용자 결정).
 *   강사: 프로필 예시 표시(isDemoProfile) 또는 서버가 '운영 예시 강사'로 늘 맨 앞에 두는 두 명
 *         (profiles.service.ts listProfilePage `CASE WHEN u.id=23 … u.id=6` — 바뀌면 여기도).
 *   레슨·공고: 제목이 '[예시]' 로 시작.
 */
object DemoContent {
    private val DEMO_TEACHER_IDS = setOf(23L, 6L)
    fun isDemo(t: TeacherCard) = t.isDemoProfile || t.id in DEMO_TEACHER_IDS
    fun isDemoTitle(title: String?) = title?.trimStart()?.startsWith("[예시]") == true
}

/** 홈 '강사' — 에이전시형 강사 그리드(커서). */
class HomeArtistsUseCase(private val teachers: TeacherRepository) {
    suspend fun page(genre: String?, region: ActiveRegion?, cursor: String?): ApiResult<CursorPage<TeacherCard>> =
        // ★ 2026-11-14 예시 강사(23·6·isDemoProfile)도 보인다(사용자 요청) — 서버가 운영 예시 강사를 맨 앞에 두는 규칙과 맞춘다(iOS 같음).
        teachers.teachersPage(genre?.takeIf { it != ALL }, region, cursor, PAGE)

    companion object {
        const val ALL = "ALL"
        const val PAGE = 20
    }
}

/**
 * 홈 '레슨' — 추천 묶음(추천·체험·원데이) · 새로 열린 레슨. 예시 레슨은 뺀다.
 *   ★ 2026-11-09 장르 탭도 추천 탭과 같은 칸들 — [genre] 한글 장르(발레 …)로 거른다. null = 추천(전체).
 */
class HomeLessonsUseCase(private val repo: HomeRepository) {
    suspend fun preview(genre: String?, region: ActiveRegion?): ApiResult<HomeLessonPreview> = repo.lessonPreview(genre, region).map { p ->
        HomeLessonPreview(
            featuredFirst(p.popular.clean()).take(SECTION),
            p.experience.clean().take(SECTION),
            p.oneday.clean().take(SECTION),
        )
    }

    /** 새로 열린 레슨 — 조건 없는 목록 첫 장(지역은 안 건다, 예전과 같다). 장르는 포함(contains). */
    suspend fun newest(genre: String?): List<HomeLesson> = repo.lessonListPage(null, genre, null, null, SECTION).getOrNull()?.items.orEmpty().clean()

    /** 홈 '전체보기' 목록(웹 /lessonList?type=) — [genre] null = 전체. */
    suspend fun listPage(type: String?, genre: String?, region: ActiveRegion?, cursor: String?): ApiResult<CursorPage<HomeLesson>> =
        repo.lessonListPage(type, genre, region, cursor, PAGE).map { p -> p.copy(items = p.items.clean()) }

    suspend fun notices() = repo.notices().getOrNull().orEmpty().take(NOTICES)

    private fun List<HomeLesson>.clean() = filterNot { DemoContent.isDemoTitle(it.title) }

    companion object {
        /** 웹 FEATURED_HOME_LESSON_ID — 추천 레슨 맨 앞 고정. */
        const val FEATURED_LESSON_ID = 18L
        const val SECTION = 10
        const val PAGE = 20
        const val NOTICES = 3
        fun featuredFirst(list: List<HomeLesson>) = list.sortedByDescending { if (it.id == FEATURED_LESSON_ID) 1 else 0 }
    }
}
