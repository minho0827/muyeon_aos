package com.muyeon.app.ui.quote

import android.content.Context
import com.muyeon.app.ui.chat.ChatActivity
import com.muyeon.app.utils.TokenManager

/**
 * ★ 2026-11-16 레슨 문의(이 사람에게만 보내는 지정 견적 요청) 흐름 — 강사 프로필(ResumeActivity)과 무용수 상세가 같이 쓴다.
 *   ★ 2026-11-16 지금 유형으로 막지 않는다(강사·원장도 수강생으로 요청할 수 있다).
 *   1) 이미 이 사람과 대화 중이면(GET /quotes/request-quota?teacherId= 의 chatRoomId) 위저드 대신 그 채팅방.
 *   2) 아니면 견적 위저드(targetTeacherId = 이 사람). 비회원은 위저드가 알아서 비회원 요청으로 받는다.
 *   확인에 실패하면 그대로 위저드 — 서버가 판정한다(409 QUOTE_CHAT_EXISTS).
 */
object LessonInquiry {
    suspend fun start(context: Context, targetUserId: Int, category: String?) {
        val token = TokenManager.getAccessToken(context)
        val roomId = if (token.isNullOrEmpty()) null else QuoteApi(token).requestQuota(targetUserId).getOrNull()?.chatRoomId
        if (roomId != null) ChatActivity.startRoom(context, roomId)
        else QuoteWizardActivity.start(context, category, targetUserId.toString())
    }

    /** 이미 요청했고 답이 온 상태 — 받은 견적 상세(iOS presentReceivedQuotes). */
    fun openReceived(context: Context, quoteId: Int) = QuoteHubActivity.start(context, isPro = false, quoteId = quoteId)

    /** 장르 → 견적 위저드 분류(첫 번째로 맞는 것). */
    fun categoryOf(genres: List<String>?): String? =
        genres?.firstNotNullOfOrNull { com.muyeon.domain.model.teacher.LessonGenres.QUOTE_CATEGORY[it] }
}
