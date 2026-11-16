package com.muyeon.app.ui.chat

import com.muyeon.app.ui.quote.boolOrNull
import com.muyeon.app.ui.quote.intOrNull
import com.muyeon.app.ui.quote.stringOrNull
import org.json.JSONObject

/**
 * ★ 2026-11-16 채팅방에 걸린 대타 건 한 개(GET /chat/rooms/:id 의 subContexts[], 지원 번호 단위).
 *  한 방(두 사람당 하나)에 대타 건이 여러 개 걸릴 수 있다. 레슨 사이클과 함께 내려올 수도 있다.
 *  서버와 같이 만드는 중이라 빠진 값은 기본값으로 읽는다(크래시 없음).
 */
data class ChatSubContext(
    val applicationId: Int,
    val postingId: Int,
    val title: String?,
    val classDate: String?,          // "2026-11-20" (ISO 날짜 또는 날짜+시각)
    val classTime: String?,          // "19:00~20:00"
    val region: String?,
    val pay: Int?,                   // 회당 금액(원)
    val postingStatus: String?,      // OPEN | CLOSED | HOLD | ARCHIVED …
    val applicationStatus: String?,  // APPLIED | REVIEWING | OFFERED | ACCEPTED | REJECTED | CANCELED
    val myRole: String?,             // author(공고 올린 분) | applicant(지원자)
    val canConfirm: Boolean,
    val canWithdraw: Boolean,
    val canCancelConfirmed: Boolean,
    val confirmedAt: String?,
    /** 끝난 이유(서버 resultReason): OTHER_CONFIRMED | CLOSED | WITHDRAWN | CANCELED | REJECTED | null. */
    val resultReason: String? = null,
) {
    val isAuthor: Boolean get() = myRole == "author"

    companion object {
        /** 지원 번호가 없으면 버튼을 걸 수 없어 버린다(null). */
        fun from(o: JSONObject): ChatSubContext? {
            val appId = o.intOrNull("applicationId")?.takeIf { it > 0 } ?: return null
            return ChatSubContext(
                applicationId = appId,
                postingId = o.intOrNull("postingId") ?: 0,
                title = o.stringOrNull("title")?.takeIf { it.isNotBlank() },
                classDate = o.stringOrNull("classDate")?.takeIf { it.isNotBlank() },
                classTime = o.stringOrNull("classTime")?.takeIf { it.isNotBlank() },
                region = o.stringOrNull("region")?.takeIf { it.isNotBlank() },
                pay = o.intOrNull("pay")?.takeIf { it > 0 },
                postingStatus = o.stringOrNull("postingStatus"),
                applicationStatus = o.stringOrNull("applicationStatus"),
                myRole = o.stringOrNull("myRole"),
                canConfirm = o.boolOrNull("canConfirm") == true,
                canWithdraw = o.boolOrNull("canWithdraw") == true,
                canCancelConfirmed = o.boolOrNull("canCancelConfirmed") == true,
                confirmedAt = o.stringOrNull("confirmedAt")?.takeIf { it.isNotBlank() },
                resultReason = o.stringOrNull("resultReason")?.takeIf { it.isNotBlank() },
            )
        }

        fun listFrom(o: JSONObject): List<ChatSubContext> {
            val arr = o.optJSONArray("subContexts") ?: return emptyList()
            return (0 until arr.length()).mapNotNull { i -> arr.optJSONObject(i)?.let(::from) }
        }
    }
}
