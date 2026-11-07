package com.muyeon.data.repository

import com.muyeon.data.common.safeApiCall
import com.muyeon.data.common.safeUnitCall
import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.common.CursorPage
import com.muyeon.domain.common.CursorPaging
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
import com.muyeon.domain.repository.HomeRepository
import com.muyeon.network.api.HomeApi
import com.muyeon.network.dto.home.HomeQuoteDto
import com.muyeon.network.dto.home.HomeRecruitDto
import com.muyeon.network.dto.home.RecommendBlockBody
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import okhttp3.ResponseBody

class HomeRepositoryImpl(private val api: HomeApi, private val moshi: Moshi) : HomeRepository {

    /** 웹 regionParams — 시군구면 regionCode, 시·도면 sido, 없으면 둘 다 null(전체). */
    private fun ActiveRegion?.code() = this?.takeIf { it.kind == ActiveRegion.SIGUNGU }?.code
    private fun ActiveRegion?.sido() = this?.takeIf { it.kind == ActiveRegion.SIDO }?.code

    /** 배열 또는 { items } 응답을 한 모양으로. */
    private inline fun <reified T> ResponseBody.listOrItems(): List<T> {
        val raw = string().trim()
        val listType = Types.newParameterizedType(List::class.java, T::class.java)
        return runCatching {
            if (raw.startsWith("[")) moshi.adapter<List<T>>(listType).fromJson(raw)
            else {
                val wrap = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
                val items = moshi.adapter<Map<String, Any?>>(wrap).fromJson(raw)?.get("items")
                moshi.adapter<List<T>>(listType).fromJsonValue(items)
            }
        }.getOrNull().orEmpty()
    }

    override suspend fun lessonPreview(region: ActiveRegion?): ApiResult<HomeLessonPreview> =
        safeApiCall(moshi) { api.lessonPreview(region.code(), region.sido()) }.map { d ->
            HomeLessonPreview(
                d.popular.orEmpty().mapNotNull { it.toDomain() },
                d.experience.orEmpty().mapNotNull { it.toDomain() },
                d.oneday.orEmpty().mapNotNull { it.toDomain() },
            )
        }

    override suspend fun lessonPage(genre: String?, cursor: String?, limit: Int): ApiResult<com.muyeon.domain.common.CursorPage<HomeLesson>> =
        safeApiCall(moshi) { api.lessonPage(genre, cursor, limit) }
            .map { com.muyeon.domain.common.CursorPage(it.items.orEmpty().mapNotNull { d -> d.toDomain() }, it.next) }

    override suspend fun recruits(kind: HomeRecruit.Kind): ApiResult<List<HomeRecruit>> =
        safeApiCall(moshi) { if (kind == HomeRecruit.Kind.JOB) api.jobs() else api.subs() }
            .map { body -> body.listOrItems<HomeRecruitDto>().mapNotNull { it.toDomain(kind) } }

    override suspend fun spaces(): ApiResult<List<HomeSpace>> = safeApiCall(moshi) { api.spaces() }.map { l -> l.mapNotNull { it.toDomain() } }
    override suspend fun transfers(limit: Int): ApiResult<List<HomeTransfer>> = safeApiCall(moshi) { api.transfers(limit) }.map { p -> p.items.orEmpty().mapNotNull { it.toDomain() } }
    override suspend fun notices(): ApiResult<List<HomeNotice>> = safeApiCall(moshi) { api.notices() }.map { l -> l.mapNotNull { it.toDomain() } }
    override suspend fun homeQuotes(): ApiResult<List<HomeQuote>> = safeApiCall(moshi) { api.homeQuotes() }.map { b -> b.listOrItems<HomeQuoteDto>().mapNotNull { it.toDomain() } }
    override suspend fun blockRecommendation(teacherId: Long): ApiResult<Unit> = safeUnitCall(moshi) { api.blockRecommendation(RecommendBlockBody(teacherId)) }

    override suspend fun academyProfile(): ApiResult<AcademyHomeProfile> = safeApiCall(moshi) { api.academyProfile() }.map { d ->
        AcademyHomeProfile(d.academyName?.takeIf { it.isNotBlank() }, d.wantGenres.orEmpty().size + d.wantFields.orEmpty().size > 0)
    }
    override suspend fun recommendedTeachers(): ApiResult<List<TeacherCard>> = safeApiCall(moshi) { api.recommendedTeachers() }.map { l -> l.mapNotNull { it.toDomain() } }
    override suspend fun myLessons(): ApiResult<List<HomeLesson>> = safeApiCall(moshi) { api.myLessons() }.map { l -> l.mapNotNull { it.toDomain() } }
    override suspend fun myOpenJobs(): ApiResult<List<HomeMyJob>> = safeApiCall(moshi) { api.myPostings() }.map { l ->
        l.filter { it.kind == "JOB" && it.status == "OPEN" }.mapNotNull { it.toJob() }
    }

    override suspend fun teacherPreview(region: ActiveRegion?, today: String): ApiResult<TeacherHomePreview> =
        safeApiCall(moshi) { api.teacherPreview(region.code(), region.sido(), region?.name, today) }.map { d ->
            TeacherHomePreview(
                d.recommendedAcademies.orEmpty().mapNotNull { it.toDomain() },
                d.recommendedPostings.orEmpty().mapNotNull { it.toDomain() },
                d.urgent.orEmpty().mapNotNull { it.toDomain() },
            )
        }

    override suspend fun feedPage(region: ActiveRegion?, domain: String, sub: String, cursor: String?): ApiResult<CursorPage<HomeFeedItem>> =
        safeApiCall(moshi) { api.feedPage(region.code(), region.sido(), domain, sub, cursor, CursorPaging.DEFAULT_LIMIT) }
            .map { CursorPage(it.items.orEmpty().mapNotNull { d -> d.toDomain() }, it.next) }

    override suspend fun myAppliedJobIds(): Set<Long> =
        safeApiCall(moshi) { api.myApplications() }.getOrNull().orEmpty().filter { it.kind == "JOB" }.mapNotNull { it.postingId }.toSet()

    override suspend fun myViewedJobIds(): Set<Long> = safeApiCall(moshi) { api.myViewedJobs() }.getOrNull().orEmpty().toSet()
}
