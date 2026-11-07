package com.muyeon.network.dto.recruit

import com.muyeon.domain.model.recruit.JobCard
import com.muyeon.domain.model.recruit.SeekerCard
import com.muyeon.domain.model.recruit.SubRow
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** jobs.service.ts listPage 카드(설명·사진 묶음 제외 + details{deadline}). */
@JsonClass(generateAdapter = true)
data class JobCardDto(
    val id: Long? = null, val title: String? = null, val academy: String? = null,
    val image: String? = null, val imageUrl: String? = null,
    val genre: String? = null, val job: String? = null, val region: String? = null,
    val days: String? = null, val time: String? = null, val pay: String? = null, val salary: String? = null,
    val fields: List<String>? = null, val details: JobDetailsDto? = null, val createdAt: String? = null,
) {
    fun toDomain() = id?.let {
        JobCard(
            it, title?.takeIf { t -> t.isNotBlank() }.orEmpty(), academy?.takeIf { a -> a.isNotBlank() },
            (image ?: imageUrl)?.takeIf { s -> s.isNotBlank() }, genre?.takeIf { s -> s.isNotBlank() }, job?.takeIf { s -> s.isNotBlank() },
            region?.takeIf { s -> s.isNotBlank() }, days?.takeIf { s -> s.isNotBlank() }, time?.takeIf { s -> s.isNotBlank() },
            pay?.takeIf { s -> s.isNotBlank() }, salary, fields.orEmpty(), details?.deadline, createdAt,
        )
    }
}

@JsonClass(generateAdapter = true)
data class JobDetailsDto(val deadline: String? = null)

/** profiles.service.ts listSeekersPage — teacherPreview/dancerPreview + _type("강사"/"무용수"). */
@JsonClass(generateAdapter = true)
data class SeekerCardDto(
    val id: Long? = null, val name: String? = null, val image: String? = null, val region: String? = null,
    val genres: List<String>? = null, val fields: List<String>? = null, val career: String? = null,
    val isNew: Boolean? = null, @Json(name = "_type") val type: String? = null,
) {
    fun toDomain() = id?.let {
        SeekerCard(it, if (type == "무용수") "DANCER" else "TEACHER", name.orEmpty(), image?.takeIf { s -> s.isNotBlank() },
            region?.takeIf { s -> s.isNotBlank() }, genres.orEmpty(), fields.orEmpty(), career, isNew == true)
    }
}

/** subs.service.ts list 행. 금액 변경은 웹 payChangeOf(constants/subDispatch.js)와 같은 규칙으로 계산. */
@JsonClass(generateAdapter = true)
data class SubRowDto(
    val id: Long? = null, val title: String? = null, val academy: String? = null, val region: String? = null,
    val genre: String? = null, val target: String? = null, val scheduleType: String? = null, val classCount: Int? = null,
    val classDate: String? = null, val classTime: String? = null, val salary: String? = null, val pay: String? = null,
    val payChangedAt: String? = null, val currentPay: Double? = null, val prevPay: Double? = null,
) {
    fun toDomain(nowMs: Long, parse: (String?) -> Long?) = id?.let {
        val changedAt = parse(payChangedAt)
        val recent = changedAt != null && nowMs - changedAt < 24 * 3600 * 1000L
        val change = if (!recent || currentPay == null || prevPay == null || currentPay == prevPay) 0 else if (currentPay > prevPay) 1 else -1
        SubRow(it, title?.takeIf { t -> t.isNotBlank() }, academy?.takeIf { a -> a.isNotBlank() }, region?.takeIf { s -> s.isNotBlank() },
            genre?.takeIf { s -> s.isNotBlank() }, target, scheduleType, classCount, classDate, classTime, salary, pay?.takeIf { s -> s.isNotBlank() }, change)
    }
}
