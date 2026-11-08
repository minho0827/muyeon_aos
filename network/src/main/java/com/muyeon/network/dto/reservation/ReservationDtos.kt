package com.muyeon.network.dto.reservation

import com.muyeon.domain.model.reservation.CompletedLesson
import com.muyeon.domain.model.reservation.ReservationCard
import com.squareup.moshi.JsonClass

/** lesson-booking.service.ts reservationCards 한 줄. */
@JsonClass(generateAdapter = true)
data class ReservationCardDto(
    val id: Long? = null, val status: String? = null, val headcount: Int? = null,
    val date: String? = null, val startTime: String? = null, val endTime: String? = null,
    val lessonProductId: Long? = null, val title: String? = null, val place: String? = null,
    val ownerAcademyId: Long? = null, val academyName: String? = null,
) {
    fun toDomain() = id?.let {
        ReservationCard(
            it, status ?: "REQUESTED", headcount, date?.take(10), startTime?.take(5), endTime?.take(5), lessonProductId,
            title?.takeIf { t -> t.isNotBlank() }, place?.takeIf { p -> p.isNotBlank() },
            ownerAcademyId?.takeIf { a -> a != 0L }, academyName?.takeIf { n -> n.isNotBlank() },
        )
    }
}

/** lessons.service.ts completedLessons 한 줄. */
@JsonClass(generateAdapter = true)
data class CompletedLessonDto(
    val scheduleId: Long? = null, val teacherId: Long? = null, val teacherName: String? = null, val teacherImage: String? = null,
    val lessonLabel: String? = null, val startAt: String? = null, val place: String? = null,
    val reviewed: Boolean? = null, val myRating: Double? = null,
) {
    fun toDomain() = scheduleId?.let {
        CompletedLesson(
            it, teacherId, teacherName.orEmpty(), teacherImage?.takeIf { s -> s.isNotBlank() }, lessonLabel.orEmpty(),
            startAt, place?.takeIf { p -> p.isNotBlank() }, reviewed == true, myRating,
        )
    }
}
