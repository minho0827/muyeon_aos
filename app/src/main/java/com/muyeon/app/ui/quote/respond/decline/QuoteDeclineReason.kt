package com.muyeon.app.ui.quote.respond.decline

import androidx.annotation.StringRes
import com.muyeon.app.R

/**
 * ★ 2026-11-14 다이렉트 레슨 요청 거절 사유(iOS QuoteDeclineReason 1:1) — 코드는 서버 허용 목록(quote-decline-reasons.ts),
 *  글자는 여기. 고른 사유는 요청자(원장·회원)에게 정중한 문장으로 전해진다. 순서 = 실제로 많이 안 맞는 순.
 */
enum class QuoteDeclineReason(val code: String, @StringRes private val label: Int, @StringRes val hint: Int? = null) {
    CLASS_MISMATCH("CLASS_MISMATCH", R.string.quote_decline_reason_class),
    SCHEDULE("SCHEDULE", R.string.quote_decline_reason_schedule),
    REGION("REGION", R.string.quote_decline_reason_region),
    PRICE("PRICE", R.string.quote_decline_reason_price),
    FULL("FULL", R.string.quote_decline_reason_full),
    FORMAT("FORMAT", R.string.quote_decline_reason_format),
    OTHER("OTHER", R.string.quote_decline_reason_other);

    /** 학원 요청(구인)이면 '수업료' 대신 '급여'. */
    @StringRes fun label(academy: Boolean): Int = if (this == PRICE && academy) R.string.quote_decline_reason_pay else label

    companion object { const val DETAIL_MAX = 200 }
}
