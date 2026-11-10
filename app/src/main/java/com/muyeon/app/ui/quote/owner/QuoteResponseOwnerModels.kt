package com.muyeon.app.ui.quote.owner

import com.muyeon.app.ui.common.jInt
import com.muyeon.app.ui.common.jStr
import com.muyeon.app.ui.format.KstDates
import org.json.JSONObject
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 보낸 제안 고치기·철회 — 화면이 같이 쓰는 값(서버 PATCH·withdraw /quotes/:id/responses/:responseId).
 *  iOS 도 같은 이름으로 옮긴다.
 */

/** 제안 칸 — 보낼 때와 같은 칸. depositAmount null = 예약금 없음. */
data class QuoteResponseDraft(
    val priceAmount: Int?,
    val priceUnit: String?,
    val depositAmount: Int?,
    val message: String,
) {
    /** 바뀐 칸만 담은 PATCH 본문 — 바뀐 게 없으면 null(서버도 400 '바뀐 내용이 없어요.'). */
    fun patchFrom(before: QuoteResponseDraft): JSONObject? {
        val o = JSONObject()
        if (priceAmount != before.priceAmount) {
            o.put("priceAmount", priceAmount ?: JSONObject.NULL)
            o.put("priceUnit", if (priceAmount == null) JSONObject.NULL else before.priceUnit ?: "PER_SESSION")
        }
        if (depositAmount != before.depositAmount) {
            o.put("paymentMode", if (depositAmount == null) "NONE" else "DEPOSIT")
            o.put("depositAmount", depositAmount ?: 0)
        }
        if (message.trim() != before.message.trim()) o.put("message", message.trim())
        return o.takeIf { it.length() > 0 }
    }

    companion object {
        fun of(priceAmount: Int?, priceUnit: String?, paymentMode: String?, depositAmount: Int?, message: String?) = QuoteResponseDraft(
            priceAmount, priceUnit,
            depositAmount?.takeIf { paymentMode == "DEPOSIT" && it > 0 },
            message.orEmpty(),
        )
    }
}

/** 고친·철회한 뒤 서버가 돌려준 제안 한 줄. */
data class QuoteResponseUpdate(
    val id: Int,
    val status: String?,
    val priceAmount: Int?,
    val priceUnit: String?,
    val price: String?,
    val paymentMode: String?,
    val depositAmount: Int?,
    val message: String?,
    val editedAt: String?,
    val withdrawnAt: String?,
    val withdrawReason: String?,
) {
    companion object {
        fun from(o: JSONObject) = QuoteResponseUpdate(
            id = o.jInt("id") ?: 0,
            status = o.jStr("status"),
            priceAmount = o.jInt("priceAmount"),
            priceUnit = o.jStr("priceUnit"),
            price = o.jStr("price"),
            paymentMode = o.jStr("paymentMode"),
            depositAmount = o.jInt("depositAmount"),
            message = o.jStr("message"),
            editedAt = o.jStr("editedAt"),
            withdrawnAt = o.jStr("withdrawnAt"),
            withdrawReason = o.jStr("withdrawReason"),
        )
    }
}

/** 제안 표시 — 철회됨 > 수정됨 > 없음. 화면은 이것으로 부품을 고른다. */
sealed interface QuoteResponseMark {
    data class Withdrawn(val at: String?, val reason: String?) : QuoteResponseMark
    data class Edited(val at: String) : QuoteResponseMark
    data object None : QuoteResponseMark

    companion object {
        fun of(status: String?, editedAt: String?, withdrawnAt: String?, withdrawReason: String? = null): QuoteResponseMark = when {
            status == WITHDRAWN -> Withdrawn(withdrawnAt, withdrawReason?.takeIf { it.isNotBlank() })
            !editedAt.isNullOrBlank() -> Edited(editedAt)
            else -> None
        }

        /** 'M.d HH:mm'(한국 시간) — 못 읽으면 빈칸. */
        fun timeText(iso: String?): String = KstDates.parse(iso)?.atZone(ZoneId.of("Asia/Seoul"))?.format(TIME).orEmpty()

        private val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("M.d HH:mm")
    }
}

const val WITHDRAWN = "WITHDRAWN"

/** 고치기·철회는 보낸 상태(SENT)이고 요청이 아직 열려 있을 때만 — 나머지는 서버도 409. */
fun canChangeResponse(status: String?, quoteOpen: Boolean): Boolean = status == "SENT" && quoteOpen
