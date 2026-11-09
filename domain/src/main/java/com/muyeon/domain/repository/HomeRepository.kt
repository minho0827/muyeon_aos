package com.muyeon.domain.repository

import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.common.CursorPage
import com.muyeon.domain.model.home.AcademyHomeProfile
import com.muyeon.domain.model.home.HomeFeedItem
import com.muyeon.domain.model.home.HomeLesson
import com.muyeon.domain.model.home.HomeLessonPreview
import com.muyeon.domain.model.home.HomeMyJob
import com.muyeon.domain.model.home.HomeNotice
import com.muyeon.domain.model.home.HomeQuote
import com.muyeon.domain.model.home.HomeRecruit
import com.muyeon.domain.model.home.HomeSpace
import com.muyeon.domain.model.home.HomeTransfer
import com.muyeon.domain.model.home.TeacherHomePreview
import com.muyeon.domain.model.region.ActiveRegion
import com.muyeon.domain.model.teacher.TeacherCard

/** 홈 화면들(스와이프·강사·클래식)이 읽는 것 — 웹 Home·TeacherHome·HomeClassic 과 같은 API. */
interface HomeRepository {
    /** @param genre 한글 장르(발레 …) — null = 전체. */
    suspend fun lessonPreview(genre: String?, region: ActiveRegion?): ApiResult<HomeLessonPreview>
    suspend fun recruits(kind: HomeRecruit.Kind): ApiResult<List<HomeRecruit>>
    suspend fun spaces(): ApiResult<List<HomeSpace>>
    suspend fun transfers(limit: Int): ApiResult<List<HomeTransfer>>
    suspend fun notices(): ApiResult<List<HomeNotice>>
    suspend fun homeQuotes(): ApiResult<List<HomeQuote>>
    /** 이 강사 추천 안 받기(`POST /quotes/recommend-blocks`). */
    suspend fun blockRecommendation(teacherId: Long): ApiResult<Unit>

    suspend fun academyProfile(): ApiResult<AcademyHomeProfile>
    suspend fun recommendedTeachers(): ApiResult<List<TeacherCard>>
    /** 홈 '전체보기' 레슨 목록 — [type] popular·experience·oneday(그 밖은 조건 없음 = 새로 열린 레슨). [genre] 는 포함(contains). */
    suspend fun lessonListPage(type: String?, genre: String?, region: ActiveRegion?, cursor: String?, limit: Int): ApiResult<com.muyeon.domain.common.CursorPage<HomeLesson>>
    suspend fun myLessons(): ApiResult<List<HomeLesson>>
    suspend fun myOpenJobs(): ApiResult<List<HomeMyJob>>

    /** @param today 기기 날짜 YYYY-MM-DD(급구 판정). */
    suspend fun teacherPreview(region: ActiveRegion?, today: String): ApiResult<TeacherHomePreview>

    suspend fun feedPage(region: ActiveRegion?, domain: String, sub: String, cursor: String?): ApiResult<CursorPage<HomeFeedItem>>
    suspend fun myAppliedJobIds(): Set<Long>
    suspend fun myViewedJobIds(): Set<Long>

    /** ★ 2026-11-09 공연·캐스팅·무대 소식 최신 시각(`GET /home/tab-updates`) — 하단 [공연] 칸 점. */
    suspend fun tabUpdates(): ApiResult<com.muyeon.domain.model.home.TabUpdates>
}
