package com.muyeon.app.ui.application

import com.muyeon.app.R
import com.muyeon.app.ui.theme.CategoryColors
import com.muyeon.domain.model.application.ApplicationKind
import com.muyeon.domain.model.application.ApplicationProgress
import com.muyeon.domain.model.application.MyApplication
import com.muyeon.domain.model.application.ReceivedApplication

/** 지원 상태 문구 — 웹 MyApplications progress · constants/statusOptions(statusLabel·subStatusLabel). 문자열 id 만 돌려준다. */
object ApplicationLabels {
    fun kind(kind: String): Int = when (kind) {
        ApplicationKind.SUB -> R.string.application_kind_sub
        ApplicationKind.CASTING -> R.string.application_kind_casting
        else -> R.string.application_kind_job
    }

    private fun subReason(reason: String?): Int? = when (reason) {
        "CONFIRMED_OTHER" -> R.string.application_sub_confirmed_other
        "CLOSED", "EXPIRED" -> R.string.application_posting_closed
        "DELETED" -> R.string.application_posting_deleted
        else -> null
    }

    /** 내 지원 상태 배지(문구 · 색). */
    fun progress(a: MyApplication): Pair<Int, CategoryColors.Level> = when (a.progress) {
        ApplicationProgress.CONFIRMED -> (when (a.kind) {
            ApplicationKind.SUB -> R.string.application_confirmed_sub
            ApplicationKind.CASTING -> R.string.application_confirmed_casting
            else -> R.string.application_confirmed_job
        }) to CategoryColors.Level.High
        ApplicationProgress.CHATTING -> R.string.application_chatting to CategoryColors.Level.High
        ApplicationProgress.VIEWED -> R.string.application_viewed to CategoryColors.Level.Middle
        ApplicationProgress.REJECTED -> (if (a.kind == ApplicationKind.SUB) {
            subReason(a.resultReason) ?: when {
                a.postingConfirmedApplicationId != null && a.postingConfirmedApplicationId != a.id -> R.string.application_sub_confirmed_other
                a.postingStatus == "ARCHIVED" -> R.string.application_posting_deleted
                a.postingStatus == "CLOSED" -> R.string.application_posting_closed
                else -> R.string.application_rejected
            }
        } else R.string.application_rejected) to CategoryColors.Level.End
        ApplicationProgress.CANCELED -> (if (a.canceledBy == "WITHDRAW") R.string.application_withdrawn_label else R.string.application_canceled_confirmed) to CategoryColors.Level.End
        ApplicationProgress.APPLIED -> (if (a.kind == ApplicationKind.SUB && a.postingStatus == "HOLD") R.string.application_posting_hold else R.string.application_applied) to CategoryColors.Level.Middle
    }

    /** 미선정 안내 문장(대타는 사유별). */
    fun rejectedMessage(a: MyApplication): Int = when (a.resultReason.takeIf { a.kind == ApplicationKind.SUB }) {
        "CONFIRMED_OTHER" -> R.string.application_msg_confirmed_other
        "CLOSED" -> R.string.application_msg_closed
        "EXPIRED" -> R.string.application_msg_expired
        "DELETED" -> R.string.application_msg_deleted
        else -> R.string.application_msg_rejected
    }

    /** 받은 지원 상태 배지(웹 applicationStatusLabel). */
    fun received(a: ReceivedApplication): Pair<Int, CategoryColors.Level> {
        if (a.withdrawn) return R.string.application_withdrawn_label to CategoryColors.Level.End
        if (a.kind == ApplicationKind.SUB) {
            when (a.status) {
                "ACCEPTED" -> return R.string.application_confirmed_sub to CategoryColors.Level.High
                "HIRED" -> return R.string.received_sub_done to CategoryColors.Level.High
            }
            if (a.status == "REJECTED") subReason(a.resultReason)?.let { return it to CategoryColors.Level.Row }
            if (a.subPendingClosed || a.postingStatus == "HOLD") {
                return when {
                    a.postingConfirmedApplicationId != null && a.postingConfirmedApplicationId != a.id -> R.string.application_sub_confirmed_other
                    a.postingStatus == "ARCHIVED" -> R.string.application_posting_deleted
                    a.postingStatus == "CLOSED" -> R.string.application_posting_closed
                    else -> R.string.application_posting_hold
                } to CategoryColors.Level.Row
            }
        }
        if (a.kind == ApplicationKind.CASTING && (a.status == "ACCEPTED" || a.status == "HIRED")) {
            return (if (a.status == "HIRED") R.string.received_casting_done else R.string.application_confirmed_casting) to CategoryColors.Level.High
        }
        return when (a.status) {
            "APPLIED", "REVIEWING" -> R.string.received_reviewing to CategoryColors.Level.Middle
            "OFFERED" -> R.string.application_chatting to CategoryColors.Level.High
            "ACCEPTED" -> R.string.application_confirmed_job to CategoryColors.Level.High
            "HIRED" -> R.string.received_hired to CategoryColors.Level.High
            "REJECTED" -> R.string.received_closed to CategoryColors.Level.Row
            "CANCELED" -> R.string.application_canceled_confirmed to CategoryColors.Level.Row
            "VIEWED" -> R.string.received_viewed to CategoryColors.Level.End
            "CONTACTED" -> R.string.received_contacted to CategoryColors.Level.High
            else -> R.string.received_check to CategoryColors.Level.Row
        }
    }
}
