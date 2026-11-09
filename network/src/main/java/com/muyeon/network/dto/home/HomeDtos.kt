package com.muyeon.network.dto.home

import com.muyeon.domain.model.home.HomeFeedItem
import com.muyeon.domain.model.home.HomeLesson
import com.muyeon.domain.model.home.HomeMyJob
import com.muyeon.domain.model.home.HomeNotice
import com.muyeon.domain.model.home.HomeQuote
import com.muyeon.domain.model.home.HomeRecruit
import com.muyeon.domain.model.home.HomeSpace
import com.muyeon.domain.model.home.HomeTransfer
import com.muyeon.domain.model.home.QuoteAnswer
import com.muyeon.domain.model.home.TeacherHomePosting
import com.squareup.moshi.JsonClass

/** 홈 응답들 — ⚠️ 서버 null 칸은 전부 `?`. 목록 응답이 배열 또는 { items } 인 곳은 저장소가 둘 다 읽는다. */

@JsonClass(generateAdapter = true)
data class HomeLessonDto(
    val id: Long? = null, val title: String? = null, val images: List<String>? = null, val imageUrl: String? = null,
    val genre: String? = null, val region: String? = null, val place: String? = null, val scheduleText: String? = null,
    val price: Double? = null, val promotedUntil: String? = null, val status: String? = null,
) {
    fun toDomain() = id?.let {
        HomeLesson(it, title.orEmpty(), (images?.firstOrNull { s -> s.isNotBlank() } ?: imageUrl)?.takeIf { s -> s.isNotBlank() }, genre,
            (region ?: place)?.takeIf { s -> s.isNotBlank() }, scheduleText?.takeIf { s -> s.isNotBlank() }, price?.toInt(), promotedUntil, status)
    }
}

@JsonClass(generateAdapter = true)
data class HomeLessonPreviewDto(val popular: List<HomeLessonDto>? = null, val experience: List<HomeLessonDto>? = null, val oneday: List<HomeLessonDto>? = null)

@JsonClass(generateAdapter = true)
data class HomeRecruitDto(
    val id: Long? = null, val title: String? = null, val image: String? = null, val imageUrl: String? = null, val genre: String? = null,
    val region: String? = null, val salary: String? = null, val pay: String? = null, val createdAt: String? = null, val classDate: String? = null,
) {
    fun toDomain(kind: HomeRecruit.Kind) = id?.let {
        HomeRecruit(kind, it, title?.takeIf { s -> s.isNotBlank() }, (image ?: imageUrl)?.takeIf { s -> s.isNotBlank() }, genre?.takeIf { s -> s.isNotBlank() },
            region?.takeIf { s -> s.isNotBlank() }, salary, pay?.takeIf { s -> s.isNotBlank() }, createdAt, classDate)
    }
}

@JsonClass(generateAdapter = true)
data class HomeSpaceDto(val id: Long? = null, val name: String? = null, val images: List<String>? = null, val region: String? = null, val minPrice: Double? = null, val pricePerHour: Double? = null) {
    fun toDomain() = id?.let { HomeSpace(it, name.orEmpty(), images?.firstOrNull { s -> s.isNotBlank() }, region, (minPrice ?: pricePerHour)?.toInt()) }
}

@JsonClass(generateAdapter = true)
data class HomeTransferDto(val id: Long? = null, val title: String? = null, val image: String? = null, val region: String? = null, val areaPyeong: Double? = null, val deposit: Double? = null, val monthlyRent: Double? = null) {
    fun toDomain() = id?.let { HomeTransfer(it, title.orEmpty(), image?.takeIf { s -> s.isNotBlank() }, region, areaPyeong, deposit?.toInt(), monthlyRent?.toInt()) }
}

@JsonClass(generateAdapter = true)
data class HomeTransferPageDto(val items: List<HomeTransferDto>? = null)

@JsonClass(generateAdapter = true)
data class HomeNoticeDto(val id: Long? = null, val title: String? = null, val createdAt: String? = null) {
    fun toDomain() = id?.let { HomeNotice(it, title.orEmpty(), createdAt) }
}

@JsonClass(generateAdapter = true)
data class QuoteAnswerDto(val questionId: String? = null, val optionIds: List<String>? = null, val text: String? = null)

@JsonClass(generateAdapter = true)
data class HomeQuoteDto(
    val id: Long? = null, val categoryId: String? = null, val region: String? = null, val answers: List<QuoteAnswerDto>? = null,
    val createdAt: String? = null, val isMine: Boolean? = null, val canRespond: Boolean? = null,
) {
    fun toDomain() = id?.let {
        HomeQuote(it, categoryId, region?.takeIf { s -> s.isNotBlank() },
            answers.orEmpty().mapNotNull { a -> a.questionId?.let { q -> QuoteAnswer(q, a.optionIds.orEmpty(), a.text) } },
            createdAt, isMine == true, canRespond == true)
    }
}

@JsonClass(generateAdapter = true)
data class RecommendBlockBody(val teacherId: Long)

@JsonClass(generateAdapter = true)
data class AcademyProfileDto(val academyName: String? = null, val wantGenres: List<String>? = null, val wantFields: List<String>? = null)

@JsonClass(generateAdapter = true)
data class MyPostingDto(
    val kind: String? = null, val status: String? = null, val id: Long? = null, val title: String? = null, val imageUrl: String? = null,
    val genre: String? = null, val region: String? = null, val days: String? = null, val deadline: String? = null, val applicants: Int? = null, val views: Int? = null,
) {
    fun toJob() = id?.let { HomeMyJob(it, title, imageUrl?.takeIf { s -> s.isNotBlank() }, genre, region, days, deadline, applicants ?: 0, views ?: 0) }
}

@JsonClass(generateAdapter = true)
data class TeacherHomePostingDto(
    val kind: String? = null, val id: Long? = null, val title: String? = null, val image: String? = null, val genre: String? = null,
    val sub: String? = null, val info: String? = null, val createdAt: String? = null,
) {
    fun toDomain() = id?.let { i -> kind?.let { k -> TeacherHomePosting(k, i, title.orEmpty(), image?.takeIf { it.isNotBlank() }, genre, sub?.takeIf { it.isNotBlank() }, info?.takeIf { it.isNotBlank() }, createdAt) } }
}

@JsonClass(generateAdapter = true)
data class TeacherHomePreviewDto(
    val recommendedAcademies: List<TeacherHomePostingDto>? = null,
    val recommendedPostings: List<TeacherHomePostingDto>? = null,
    val urgent: List<TeacherHomePostingDto>? = null,
)

@JsonClass(generateAdapter = true)
data class HomeFeedItemDto(
    val id: String? = null, val domain: String? = null, val path: String? = null, val image: String? = null, val title: String? = null,
    val sub: String? = null, val info: List<String?>? = null, val status: String? = null, val statusColor: String? = null, val categories: List<String>? = null,
) {
    fun toDomain() = id?.let { i -> HomeFeedItem(i, domain.orEmpty(), path.orEmpty(), image?.takeIf { it.isNotBlank() }, title.orEmpty(), sub,
        info.orEmpty().map { it.orEmpty() }, status?.takeIf { it.isNotBlank() }, statusColor, categories.orEmpty()) }
}

@JsonClass(generateAdapter = true)
data class MyApplicationDto(val kind: String? = null, val postingId: Long? = null)

/** `GET /home/tab-updates` — 칸마다 최신 시각(ISO). */
@JsonClass(generateAdapter = true)
data class TabUpdatesDto(val performance: String? = null, val casting: String? = null, val stageNews: String? = null) {
    fun toDomain() = com.muyeon.domain.model.home.TabUpdates(performance?.takeIf { it.isNotBlank() }, casting?.takeIf { it.isNotBlank() }, stageNews?.takeIf { it.isNotBlank() })
}
