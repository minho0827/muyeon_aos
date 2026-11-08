package com.muyeon.network.dto.application

import com.muyeon.domain.model.application.MyApplication
import com.muyeon.domain.model.application.ReceivedApplication
import com.squareup.moshi.JsonClass

/** me.service.ts getApplications 한 줄(+ canWithdraw · progressStatus). */
@JsonClass(generateAdapter = true)
data class MyApplicationDto(
    val id: Long? = null, val kind: String? = null, val postingId: Long? = null, val title: String? = null,
    val status: String? = null, val progressStatus: String? = null, val canceledBy: String? = null,
    val postingStatus: String? = null, val postingDeadline: String? = null, val createdAt: String? = null, val appliedAt: String? = null,
    val viewedAt: String? = null, val resultReason: String? = null, val acceptedPay: Double? = null, val postingCurrentPay: Double? = null,
    val postingConfirmedApplicationId: Long? = null, val canWithdraw: Boolean? = null,
) {
    fun toDomain() = if (id == null || kind == null) null else MyApplication(
        id, kind, postingId, title?.takeIf { it.isNotBlank() }, status.orEmpty(), progressStatus, canceledBy, postingStatus,
        postingDeadline?.takeIf { it.isNotBlank() }, appliedAt ?: createdAt, viewedAt, resultReason, acceptedPay?.toInt(), postingCurrentPay?.toInt(),
        postingConfirmedApplicationId, canWithdraw,
    )
}

@JsonClass(generateAdapter = true)
data class MyApplicationCountsDto(val ALL: Int? = null, val PROGRESS: Int? = null, val CONFIRMED: Int? = null, val REJECTED: Int? = null)

@JsonClass(generateAdapter = true)
data class MyApplicationsPageDto(
    val items: List<MyApplicationDto>? = null, val counts: MyApplicationCountsDto? = null,
    val hasMore: Boolean? = null, val nextCursor: String? = null,
) {
    val next: String? get() = nextCursor?.takeIf { it.isNotBlank() && hasMore != false }
}

/** received-applications.service.ts 한 줄(종류별 listReceivedApplications + kind). */
@JsonClass(generateAdapter = true)
data class ReceivedApplicationDto(
    val id: Long? = null, val kind: String? = null, val postingId: Long? = null, val postingTitle: String? = null,
    val applicantName: String? = null, val applicantImage: String? = null, val status: String? = null, val canceledBy: String? = null,
    val viewedAt: String? = null, val createdAt: String? = null, val resultReason: String? = null, val postingStatus: String? = null,
    val postingConfirmedApplicationId: Long? = null, val acceptedPay: Double? = null, val effectivePay: Double? = null,
    val viaDispatch: Boolean? = null, val applicantCancels90d: Int? = null,
) {
    fun toDomain() = if (id == null || kind == null) null else ReceivedApplication(
        id, kind, postingId, postingTitle?.takeIf { it.isNotBlank() }, applicantName?.takeIf { it.isNotBlank() },
        applicantImage?.takeIf { it.isNotBlank() }, status.orEmpty(), canceledBy, viewedAt, createdAt, resultReason, postingStatus,
        postingConfirmedApplicationId, acceptedPay?.toInt(), effectivePay?.toInt(), viaDispatch == true, applicantCancels90d,
    )
}

@JsonClass(generateAdapter = true)
data class ReceivedApplicationsPageDto(
    val items: List<ReceivedApplicationDto>? = null, val archivedCount: Int? = null,
    val hasMore: Boolean? = null, val nextCursor: String? = null,
) {
    val next: String? get() = nextCursor?.takeIf { it.isNotBlank() && hasMore != false }
}
