package com.muyeon.app.ui.quote.owner

import android.content.Context
import com.muyeon.app.ui.common.NativeHttp
import org.json.JSONObject

/**
 * 보낸 제안 고치기·철회 — 서버 거절(400·404·409·500)은 [com.muyeon.app.ui.common.ApiMessageError] 문구 그대로 화면에 보인다.
 *   - PATCH /quotes/:quoteId/responses/:responseId          { 바뀐 칸만 } → 고친 제안(editedAt)
 *   - POST  /quotes/:quoteId/responses/:responseId/withdraw { reason? }  → status WITHDRAWN · withdrawnAt
 */
class QuoteResponseOwnerApi(context: Context) {
    private val http = NativeHttp(context)

    suspend fun edit(quoteId: Int, responseId: Int, patch: JSONObject): Result<QuoteResponseUpdate> =
        http.call("/quotes/$quoteId/responses/$responseId", "PATCH", patch).map(::parse)

    suspend fun withdraw(quoteId: Int, responseId: Int, reason: String?): Result<QuoteResponseUpdate> {
        val body = JSONObject().apply { reason?.trim()?.takeIf { it.isNotEmpty() }?.let { put("reason", it.take(REASON_MAX)) } }
        return http.call("/quotes/$quoteId/responses/$responseId/withdraw", "POST", body).map(::parse)
    }

    private fun parse(text: String) = QuoteResponseUpdate.from(runCatching { JSONObject(text) }.getOrDefault(JSONObject()))

    companion object { const val REASON_MAX = 200 }
}
