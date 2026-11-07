package com.muyeon.domain.usecase.home

import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.model.home.HomeLesson
import com.muyeon.domain.model.home.HomeRecruit
import com.muyeon.domain.model.region.ActiveRegion
import com.muyeon.domain.model.teacher.TeacherCard
import com.muyeon.domain.repository.HomeRepository
import com.muyeon.domain.repository.TeacherRepository

/** 홈 화면들이 쓰는 저장소 묶음(유스케이스 하나가 화면 하나를 맡는다 — 섹션마다 작은 함수). */
class HomeSwipeUseCase(private val repo: HomeRepository, private val teachers: TeacherRepository) {
    /** 회원님이 좋아할 강사 — 대표 지역 · 평점순 · 레슨 여는 강사 20, 사진 있는 강사 먼저(웹 photoFirst). */
    suspend fun likedTeachers(region: ActiveRegion?): List<TeacherCard> =
        (teachers.lessonTeachersPage(null, null, region, null, TEACHER_LIMIT, sort = "rating") as? ApiResult.Success)
            ?.data?.items.orEmpty().photoFirst()

    suspend fun lessonPreview(region: ActiveRegion?) = repo.lessonPreview(region)

    /** 장르별 강사 구인 — 채용+대타 합쳐 10(웹 recruitsF). */
    suspend fun recruits(): List<HomeRecruit> =
        (repo.recruits(HomeRecruit.Kind.JOB).getOrNull().orEmpty() + repo.recruits(HomeRecruit.Kind.SUB).getOrNull().orEmpty()).take(RECRUIT_LIMIT)

    /** 공지형 목록(강사 채용정보·대타공고) — 최신 5, 3일 안이면 새 글 표시는 화면이. */
    suspend fun recruitNotice(kind: HomeRecruit.Kind): List<HomeRecruit> =
        repo.recruits(kind).getOrNull().orEmpty().sortedByDescending { it.createdAt.orEmpty() }.take(NOTICE_LIMIT)

    suspend fun spaces() = repo.spaces().getOrNull().orEmpty().take(MARKET_LIMIT)
    suspend fun transfers() = repo.transfers(MARKET_LIMIT).getOrNull().orEmpty()
    suspend fun notices() = repo.notices().getOrNull().orEmpty().take(HOME_NOTICES)
    suspend fun homeQuotes() = repo.homeQuotes().getOrNull().orEmpty().take(QUOTE_LIMIT)
    suspend fun blockRecommendation(teacherId: Long) = repo.blockRecommendation(teacherId)

    // 학원 홈
    suspend fun academyProfile() = repo.academyProfile().getOrNull()
    suspend fun recommendedTeachers() = repo.recommendedTeachers().getOrNull().orEmpty().take(ACADEMY_REC_LIMIT).photoFirst()
    suspend fun myActiveLessons() = repo.myLessons().getOrNull().orEmpty().filter { (it.status ?: "ACTIVE") == "ACTIVE" }
    suspend fun myOpenJobs() = repo.myOpenJobs().getOrNull().orEmpty()

    companion object {
        /** 웹 FEATURED_HOME_LESSON_ID — '내 주변 추천 레슨' 맨 앞 고정. */
        const val FEATURED_LESSON_ID = 18L
        const val TEACHER_LIMIT = 20
        const val RECRUIT_LIMIT = 10
        const val NOTICE_LIMIT = 5
        const val MARKET_LIMIT = 10
        const val HOME_NOTICES = 3
        const val QUOTE_LIMIT = 10
        const val ACADEMY_REC_LIMIT = 12
        const val LESSON_LIMIT = 10

        fun List<TeacherCard>.photoFirst() = sortedByDescending { if (it.image != null) 1 else 0 }

        /** 추천 레슨 — 고정 레슨 맨 앞, 10개. */
        fun featuredFirst(list: List<HomeLesson>) = list.sortedByDescending { if (it.id == FEATURED_LESSON_ID) 1 else 0 }.take(LESSON_LIMIT)
    }
}

class TeacherHomeUseCase(private val repo: HomeRepository) {
    suspend operator fun invoke(region: ActiveRegion?, today: String) = repo.teacherPreview(region, today)
}

class HomeFeedUseCase(private val repo: HomeRepository) {
    suspend fun page(region: ActiveRegion?, domain: String, sub: String, cursor: String?) = repo.feedPage(region, domain, sub, cursor)
    suspend fun appliedJobIds() = repo.myAppliedJobIds()
    suspend fun viewedJobIds() = repo.myViewedJobIds()
}
