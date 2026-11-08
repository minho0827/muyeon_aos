package com.muyeon.domain.model.reservation

/** '내가 예약한 내역' 분류 칩(웹 CATEGORY_TABS). [DONE] 은 다른 목록(레슨 완료 · 후기). */
enum class ReservationCategory { PERSONAL, ACADEMY, DONE, CANCELED }

/** 내가 예약한 내역 한 줄(서버 정렬 그대로). */
sealed interface MyReservationItem {
    val key: String
}

/** 레슨 예약 — [status] REQUESTED(대기)·CONFIRMED(확정)·CANCELED·ATTENDED(출석)·NOSHOW. */
data class ReservationCard(
    val id: Long,
    val status: String,
    val headcount: Int?,
    /** "2026-11-08". */
    val date: String?,
    val startTime: String?,
    val endTime: String?,
    val lessonProductId: Long?,
    val title: String?,
    val place: String?,
    val ownerAcademyId: Long?,
    val academyName: String?,
) : MyReservationItem {
    override val key: String get() = "L$id"
    val canceled: Boolean get() = status == "CANCELED"
}

/** 레슨 완료(후기 쓰기·내 후기). */
data class CompletedLesson(
    val scheduleId: Long,
    val teacherId: Long?,
    val teacherName: String,
    val teacherImage: String?,
    val lessonLabel: String,
    val startAt: String?,
    val place: String?,
    val reviewed: Boolean,
    val myRating: Double?,
) : MyReservationItem {
    override val key: String get() = "D$scheduleId"
}
