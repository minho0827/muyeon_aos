package com.muyeon.app.ui.quote.respond

import androidx.annotation.StringRes
import com.muyeon.app.R
import com.muyeon.app.ui.common.jBool
import com.muyeon.app.ui.common.jInt
import com.muyeon.app.ui.common.jObjects
import com.muyeon.app.ui.common.jStr
import com.muyeon.app.ui.quote.QuoteFull
import org.json.JSONObject

/**
 * 견적 응답 화면(웹 /availableQuotes/:id — QuoteRespond.js) 모델.
 *  요청 본문은 기존 견적 모델 [QuoteFull] 을 그대로 쓰고, 이 화면만 보는 칸(마감 플래그·내가 보낸 견적서·공개 상세 사진 수)만 더한다.
 */

/** 내가 이 요청에 보낸 견적서 — GET /quotes/:id 의 responses 중 responderId == 나. */
data class MyQuoteResponse(
    val id: Int,
    val status: String?,          // SENT | ACCEPTED | REJECTED | WITHDRAWN(내가 철회 — 다시 보낼 수 있다)
    val priceAmount: Int?,
    val priceUnit: String?,
    val price: String?,           // 메모가 있으면 메모, 없으면 서버가 만든 금액 문자열
    val paymentMode: String?,     // NONE | DEPOSIT
    val depositAmount: Int,
    val message: String?,
    val chatRoomId: Int?,
    val createdAt: String?,
    // ★ 2026-11-10 고치기·철회
    val editedAt: String? = null,
    val withdrawnAt: String? = null,
    val withdrawReason: String? = null,
) {
    companion object {
        fun from(o: JSONObject) = MyQuoteResponse(
            id = o.optInt("id"),
            status = o.jStr("status"),
            priceAmount = o.jInt("priceAmount"),
            priceUnit = o.jStr("priceUnit"),
            price = o.jStr("price"),
            paymentMode = o.jStr("paymentMode"),
            depositAmount = o.jInt("depositAmount") ?: 0,
            message = o.jStr("message"),
            chatRoomId = o.jInt("chatRoomId")?.takeIf { it > 0 },
            createdAt = o.jStr("createdAt"),
            editedAt = o.jStr("editedAt"),
            withdrawnAt = o.jStr("withdrawnAt"),
            withdrawReason = o.jStr("withdrawReason"),
        )
    }
}

/** 강사·학원 상세 — GET /quotes/:id → { quote(+expired), responses }. */
data class QuoteRespondDetail(val quote: QuoteFull, val expired: Boolean, val myResponse: MyQuoteResponse?) {
    companion object {
        fun from(o: JSONObject, myUserId: Int?): QuoteRespondDetail? {
            val q = o.optJSONObject("quote") ?: return null
            if (q.jInt("id") == null) return null
            val mine = o.optJSONArray("responses")?.jObjects()
                ?.firstOrNull { myUserId != null && it.jInt("responderId") == myUserId }
            return QuoteRespondDetail(QuoteFull.from(q), q.jBool("expired"), mine?.let(MyQuoteResponse::from))
        }
    }
}

/** 공개 상세(비회원·일반회원) — GET /public/quotes/:id → { quote(사진 대신 photoCount), responseCount }. */
data class PublicQuoteDetail(val quote: QuoteFull, val expired: Boolean, val photoCount: Int, val responseCount: Int) {
    val closed: Boolean get() = quote.status != "OPEN" || expired

    companion object {
        fun from(o: JSONObject): PublicQuoteDetail? {
            val q = o.optJSONObject("quote") ?: return null
            if (q.jInt("id") == null) return null
            return PublicQuoteDetail(QuoteFull.from(q), q.jBool("expired"), q.jInt("photoCount") ?: 0, o.jInt("responseCount") ?: 0)
        }
    }
}

/** 상세를 열 수 없는 사유 — 웹 GONE_* · classifyLoadError. retryable 이면 [다시 시도]. */
enum class QuoteRespondGone(@StringRes val title: Int, @StringRes val body: Int, val retryable: Boolean = false) {
    CANCELED(R.string.quote_respond_gone_canceled_title, R.string.quote_respond_gone_removed_body),
    DELETED(R.string.quote_respond_gone_deleted_title, R.string.quote_respond_gone_removed_body),
    FORBIDDEN(R.string.quote_respond_gone_forbidden_title, R.string.quote_respond_gone_forbidden_body),
    ERROR(R.string.quote_respond_gone_error_title, R.string.quote_respond_gone_error_body, retryable = true),
}

/** 새 견적을 받을 수 없는 요청(서버 respond() 403 의 나머지) — 매칭 완료 / 기간 마감. */
enum class QuoteRespondClosed(@StringRes val title: Int, @StringRes val body: Int) {
    MATCHED(R.string.quote_respond_closed_matched_title, R.string.quote_respond_closed_matched_body),
    EXPIRED(R.string.quote_respond_closed_expired_title, R.string.quote_respond_closed_expired_body),
}

/** 내가 보낸 견적서 상태 배지 — 웹 QUOTE_RESPONSE_STATUS_PRO(라벨 · .level 색). */
enum class MyResponseBadge(@StringRes val label: Int, val level: com.muyeon.app.ui.theme.CategoryColors.Level) {
    SENT(R.string.quote_respond_status_sent, com.muyeon.app.ui.theme.CategoryColors.Level.Middle),
    ACCEPTED(R.string.quote_respond_status_accepted, com.muyeon.app.ui.theme.CategoryColors.Level.Row),
    REJECTED(R.string.quote_respond_status_rejected, com.muyeon.app.ui.theme.CategoryColors.Level.VeryHigh),
    WITHDRAWN(R.string.quote_owner_withdrawn_plain, com.muyeon.app.ui.theme.CategoryColors.Level.End),
    OTHER(R.string.quote_respond_status_default, com.muyeon.app.ui.theme.CategoryColors.Level.Middle);

    companion object {
        fun of(status: String?) = when (status) { "SENT" -> SENT; "ACCEPTED" -> ACCEPTED; "REJECTED" -> REJECTED; "WITHDRAWN" -> WITHDRAWN; else -> OTHER }
    }
}
