package com.muyeon.domain.model.application

/** 지원 종류 — 서버 kind. */
object ApplicationKind {
    const val JOB = "JOB"
    const val SUB = "SUB"
    const val CASTING = "CASTING"
}

private val SUB_PENDING = setOf("APPLIED", "REVIEWING", "OFFERED")

/** 대타 지원 결과 사유 — 서버 resultReason. */
object SubResultReason {
    val LABELED = setOf("CONFIRMED_OTHER", "CLOSED", "EXPIRED", "DELETED")
}

/** 내 지원 진행 단계(웹 MyApplications progressKey). */
enum class ApplicationProgress { CONFIRMED, CHATTING, VIEWED, APPLIED, REJECTED, CANCELED }

/** 내가 지원한 공고 한 줄. */
data class MyApplication(
    val id: Long,
    val kind: String,
    val postingId: Long?,
    val title: String?,
    val status: String,
    val progressStatus: String?,
    val canceledBy: String?,
    val postingStatus: String?,
    /** 마감일 "2026-11-30" · 상시 등 서버 문구. */
    val postingDeadline: String?,
    val appliedAt: String?,
    val viewedAt: String?,
    val resultReason: String?,
    val acceptedPay: Int?,
    val postingCurrentPay: Int?,
    val postingConfirmedApplicationId: Long?,
    val canWithdrawFlag: Boolean?,
) {
    val key: String get() = "$kind-$id"

    /** 대기 중인데 공고가 닫혔다(다른 강사 확정·마감·삭제) — 대타 예전 데이터. */
    val subPendingClosed: Boolean
        get() = kind == ApplicationKind.SUB && status in SUB_PENDING && (
            (postingConfirmedApplicationId != null && postingConfirmedApplicationId != id) || postingStatus == "CLOSED" || postingStatus == "ARCHIVED")

    val progress: ApplicationProgress
        get() = when {
            status == "CANCELED" || progressStatus == "CANCELED" -> ApplicationProgress.CANCELED
            subPendingClosed -> ApplicationProgress.REJECTED
            progressStatus != null && progressStatus in ApplicationProgress.entries.map { it.name } -> ApplicationProgress.valueOf(progressStatus)
            status == "ACCEPTED" || status == "HIRED" -> ApplicationProgress.CONFIRMED
            status == "REJECTED" -> ApplicationProgress.REJECTED
            status == "OFFERED" || status == "CONTACTED" -> ApplicationProgress.CHATTING
            status == "VIEWED" || viewedAt != null -> ApplicationProgress.VIEWED
            else -> ApplicationProgress.APPLIED
        }

    /** 지원 취소 가능(서버 canWithdraw 우선). */
    val canWithdraw: Boolean
        get() = canWithdrawFlag ?: if (kind == ApplicationKind.CASTING) status in setOf("APPLIED", "REVIEWING") else status in SUB_PENDING

    /** 공고 상세 주소(웹 postingPath). */
    val postingPath: String?
        get() = postingId?.let {
            when (kind) { ApplicationKind.JOB -> "/jobs/$it"; ApplicationKind.SUB -> "/subs/$it"; ApplicationKind.CASTING -> "/casting/$it"; else -> null }
        }
}

/** '내가 지원한 공고' 탭 — 서버 tab. */
enum class MyApplicationTab { ALL, PROGRESS, CONFIRMED, REJECTED }

data class MyApplicationsPage(val items: List<MyApplication>, val counts: Map<MyApplicationTab, Int>, val nextCursor: String?)

/** 받은 지원 한 줄. */
data class ReceivedApplication(
    val id: Long,
    val kind: String,
    val postingId: Long?,
    val postingTitle: String?,
    val applicantName: String?,
    val applicantImage: String?,
    val status: String,
    val canceledBy: String?,
    val viewedAt: String?,
    val createdAt: String?,
    val resultReason: String?,
    val postingStatus: String?,
    val postingConfirmedApplicationId: Long?,
    val acceptedPay: Int?,
    val effectivePay: Int?,
    val viaDispatch: Boolean,
    val applicantCancels90d: Int?,
    /**
     * ★ 2026-11-10 레슨 요청으로 인증 없이 올린 채용 공고 — 학원 인증 전에는 이력서를 못 연다(서버 locked).
     *  잠긴 줄은 이름 대신 '지원자 N', 이력서는 장르·경력만 온다. 누르면 인증 안내.
     */
    val locked: Boolean = false,
    /** 잠금을 푸는 인증 화면 경로(서버 verification.path) — 없으면 기본 학원 인증. */
    val verificationPath: String? = null,
    val resumeGenres: List<String> = emptyList(),
    /** 경력 버킷(NEW/Y1_3/…). */
    val resumeCareer: String? = null,
) {
    val key: String get() = "$kind-$id"
    /** 지원자가 거둔 지원 — 열람 불가. */
    val withdrawn: Boolean get() = status == "CANCELED" && canceledBy == "WITHDRAW"
    val isNew: Boolean get() = !withdrawn && viewedAt == null
    val subPendingClosed: Boolean
        get() = kind == ApplicationKind.SUB && status in SUB_PENDING && (
            (postingConfirmedApplicationId != null && postingConfirmedApplicationId != id) || postingStatus == "CLOSED" || postingStatus == "ARCHIVED")
}

data class ReceivedApplicationsPage(val items: List<ReceivedApplication>, val archivedCount: Int, val nextCursor: String?)
