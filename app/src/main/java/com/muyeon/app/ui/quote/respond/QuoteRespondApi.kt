package com.muyeon.app.ui.quote.respond

import android.content.Context
import com.muyeon.app.ui.common.ApiMessageError
import com.muyeon.app.ui.common.NativeHttp
import com.muyeon.app.ui.common.jInt
import com.muyeon.app.ui.common.jStr
import com.muyeon.app.ui.common.jStrings
import org.json.JSONObject

/**
 * 견적 응답 화면 서버 호출 — 웹 QuoteRespond.js 가 부르는 것 그대로.
 *  상태 코드(404 삭제 · 403 권한)로 사유를 나눠야 해서 [NativeHttp](ApiMessageError.status) 를 쓴다.
 *   - GET  /quotes/:id              (강사·학원 활동 중)  — quotes.controller findOne
 *   - GET  /public/quotes/:id       (비회원·일반회원)    — quotes.public.controller
 *   - GET  /chat/my/auto-message    (자동 인사 템플릿 — 메시지 미리 채우기) — chat-templates.controller
 *   - GET  /auth/me                 (보유 회원유형 — '강사 유형으로 전환하고 견적 보내기')
 *   - POST /quotes/:id/responses    (견적 보내기 → { roomId })
 *   - POST /quotes/:id/decline      (★ 11-14 다이렉트 요청 정중히 거절)
 */
class QuoteRespondApi(context: Context) {
    private val http = NativeHttp(context)

    val isLoggedIn: Boolean get() = http.isLoggedIn
    fun myUserId(): Int? = http.myUserId()

    suspend fun getQuote(id: Int): Result<QuoteRespondDetail?> =
        http.get("/quotes/$id").map { QuoteRespondDetail.from(JSONObject(it.ifBlank { "{}" }), http.myUserId()) }

    suspend fun getPublicQuote(id: Int): Result<PublicQuoteDetail?> =
        http.get("/public/quotes/$id").map { PublicQuoteDetail.from(JSONObject(it.ifBlank { "{}" })) }

    /** 내 자동 인사 템플릿 — 없거나 실패면 null(웹도 조용히 넘어간다). */
    suspend fun myAutoMessage(): String? =
        http.get("/chat/my/auto-message").getOrNull()?.let { runCatching { JSONObject(it).jStr("message") }.getOrNull() }

    /** 보유 회원유형(user.memberTypes) — 실패면 빈 목록. */
    suspend fun heldTypes(): List<String> =
        http.get("/auth/me").getOrNull()?.let { runCatching { JSONObject(it).jStrings("memberTypes") }.getOrNull() } ?: emptyList()

    /**
     * 견적 보내기 — 본문은 기존 QuoteApi.sendQuoteResponse(모아보기 시트)와 같다(회당 금액 · 예약금 · 메시지 · 프로필 첨부).
     *  성공이면 전송 즉시 생긴 고객 채팅방 id(비회원 요청이면 null).
     */
    suspend fun sendResponse(
        quoteId: Int, priceAmount: Int?, depositAmount: Int?, message: String, attachmentType: String,
    ): Result<Int?> {
        val body = JSONObject().put("message", message).put("attachmentType", attachmentType)
        if (priceAmount != null) body.put("priceAmount", priceAmount).put("priceUnit", "PER_SESSION")
        body.put("paymentMode", if (depositAmount == null) "NONE" else "DEPOSIT")
        body.put("depositAmount", depositAmount ?: 0)
        return http.call("/quotes/$quoteId/responses", "POST", body).map { text ->
            runCatching { JSONObject(text).jInt("roomId") }.getOrNull()?.takeIf { it > 0 }
        }
    }

    /** ★ 2026-11-14 다이렉트 요청 정중히 거절 — { reasonCode, reasonDetail? }(기타일 때만 글). */
    suspend fun decline(quoteId: Int, reason: com.muyeon.app.ui.quote.respond.decline.QuoteDeclineReason, detail: String): Result<Unit> {
        val body = JSONObject().put("reasonCode", reason.code)
        if (reason == com.muyeon.app.ui.quote.respond.decline.QuoteDeclineReason.OTHER) body.put("reasonDetail", detail.trim())
        return http.call("/quotes/$quoteId/decline", "POST", body).map { }
    }

    companion object {
        /** 웹 classifyLoadError — 404·403 만 '사라진 요청', 나머지(끊김·5xx)는 다시 시도. */
        fun classify(e: Throwable, forbidden: QuoteRespondGone): QuoteRespondGone = when ((e as? ApiMessageError)?.status) {
            404 -> QuoteRespondGone.DELETED
            403 -> forbidden
            else -> QuoteRespondGone.ERROR
        }
    }
}
