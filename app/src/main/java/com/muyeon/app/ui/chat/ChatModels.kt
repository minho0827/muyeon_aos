package com.muyeon.app.ui.chat

import com.muyeon.app.ui.quote.boolOrNull
import com.muyeon.app.ui.quote.intOrNull
import com.muyeon.app.ui.quote.map
import com.muyeon.app.ui.quote.stringOrNull
import org.json.JSONObject

/**
 * 채팅 모델 — iOS `ChatModels.swift` 1:1 (muyeon-backend chat 응답 매칭).
 *  파싱은 기존 ui/quote 와 같은 org.json 수동 방식(필드 누락에도 크래시 없음).
 */

/** 채팅 상대(견적 상대·공간 소유자 등). */
data class ChatOpponent(val id: Int, val name: String?, val nickname: String?, val image: String?) {
    companion object {
        fun from(o: JSONObject?): ChatOpponent? = o?.let {
            ChatOpponent(it.optInt("id"), it.stringOrNull("name"), it.stringOrNull("nickname"), it.stringOrNull("image"))
        }
    }
}

/** 채팅방 목록 한 칸 (GET /chat/rooms · 소켓 chat-room-added payload 와 동일 shape). */
data class ChatRoomSummary(
    val roomId: Int,
    val type: String?,
    val spaceId: Int?,
    val lastMessage: String?,
    val lastMessageAt: String?,
    val unreadCount: Int,
    val muted: Boolean?,
    val quoteStatus: String?,     // 견적방이면 요청 상태(EXPIRED → '견적 마감')
    val kind: String?,            // quote | space | direct — 세그먼트 필터 기준
    val myQuoteRole: String?,     // 견적방에서 내 역할: customer | pro | null
    val opponent: ChatOpponent?,
) {
    val displayTitle: String get() = opponent?.nickname ?: opponent?.name ?: "채팅"
    val isQuoteExpired: Boolean get() = quoteStatus == "EXPIRED"

    /** 리스트 셀 역할 배지. null = 배지 없음. */
    val roleBadge: String?
        get() = when {
            kind == "quote" && myQuoteRole == "customer" -> "내 요청"
            kind == "quote" && myQuoteRole == "pro" -> "견적 응답"
            kind == "space" -> "공간 문의"
            else -> null
        }

    companion object {
        fun from(o: JSONObject) = ChatRoomSummary(
            roomId = o.optInt("roomId"),
            type = o.stringOrNull("type"),
            spaceId = o.intOrNull("spaceId"),
            lastMessage = o.stringOrNull("lastMessage"),
            lastMessageAt = o.stringOrNull("lastMessageAt"),
            unreadCount = o.optInt("unreadCount", 0),
            muted = o.boolOrNull("muted"),
            quoteStatus = o.stringOrNull("quoteStatus"),
            kind = o.stringOrNull("kind"),
            myQuoteRole = o.stringOrNull("myQuoteRole"),
            opponent = ChatOpponent.from(o.optJSONObject("opponent")),
        )
    }
}

/** 채팅 리스트 세그먼트 필터 — iOS ChatRoomFilter. */
enum class ChatRoomFilter(val title: String) {
    ALL("전체"),
    REQUESTED("내 견적요청"),
    RESPONDED("견적 응답"),
    INQUIRY("일반·문의");

    /**
     * 다이렉트 문의방(kind=direct)은 견적방이 아니라 기본 세그먼트에서 사라지던 문제 →
     *  양쪽(고객/강사) 기본 탭에 함께 노출(놓침 방지). iOS matches() 와 동일.
     */
    fun matches(room: ChatRoomSummary): Boolean = when (this) {
        ALL -> true
        REQUESTED -> (room.kind == "quote" && room.myQuoteRole == "customer") || room.kind == "direct"
        RESPONDED -> (room.kind == "quote" && room.myQuoteRole == "pro") || room.kind == "direct"
        INQUIRY -> room.kind != "quote"
    }

    companion object {
        /** 웹 브릿지(openChatList filter) 문자열 → 기본 세그먼트. */
        fun from(s: String?): ChatRoomFilter = when (s) {
            "requested" -> REQUESTED
            "responded" -> RESPONDED
            "inquiry" -> INQUIRY
            else -> ALL
        }
    }
}

/** 레슨 진행 타임라인 — 견적요청→견적도착→채택→일정확정→완료(서버 산출). */
data class ChatLessonProgress(
    val step: String,              // RESPONDED | ACCEPTED | SCHEDULED | DONE
    val requestedAt: String?,
    val firstResponseAt: String?,
    val responseCount: Int?,
    val acceptedAt: String?,
    val scheduledAt: String?,
    val scheduleStartAt: String?,
    val completedAt: String?,
    val lessonId: Int?,            // 강사 [일정 정하기]/상세 진입용
) {
    /** 스테퍼 채움 단계(0=요청,1=견적,2=채택,3=확정,4=완료). */
    val stepIndex: Int
        get() = when (step) {
            "DONE" -> 4
            "SCHEDULED" -> 3
            "ACCEPTED" -> 2
            else -> 1
        }

    companion object {
        fun from(o: JSONObject) = ChatLessonProgress(
            step = o.optString("step"),
            requestedAt = o.stringOrNull("requestedAt"),
            firstResponseAt = o.stringOrNull("firstResponseAt"),
            responseCount = o.intOrNull("responseCount"),
            acceptedAt = o.stringOrNull("acceptedAt"),
            scheduledAt = o.stringOrNull("scheduledAt"),
            scheduleStartAt = o.stringOrNull("scheduleStartAt"),
            completedAt = o.stringOrNull("completedAt"),
            lessonId = o.intOrNull("lessonId"),
        )
    }
}

/** 채팅방에 연결된 확정 레슨 일정 — 상단 배너. */
data class ChatLessonSchedule(
    val lessonId: Int,
    val startAt: String?,
    val place: String?,
    val categoryId: String?,
    val title: String?,
    val confirmed: Boolean,
    val source: String?,     // QUOTE | BOOKING | PROPOSAL
) {
    companion object {
        fun from(o: JSONObject?): ChatLessonSchedule? = o?.let {
            ChatLessonSchedule(
                it.optInt("lessonId"), it.stringOrNull("startAt"), it.stringOrNull("place"),
                it.stringOrNull("categoryId"), it.stringOrNull("title"),
                it.optBoolean("confirmed", false), it.stringOrNull("source"),
            )
        }
    }
}

/** 채팅방의 견적 컨텍스트 — 견적요청↔응답 연결 + 채택 상태. */
data class ChatQuoteContext(
    val quoteId: Int,
    val responseId: Int,
    val quoteStatus: String,     // OPEN | MATCHED
    val responseStatus: String,  // SENT | ACCEPTED | REJECTED
    val isTeacher: Boolean,      // 열람자가 이 방의 강사인지
    val matched: Boolean,
    val categoryId: String?,
    val priceAmount: Int?,
    val priceUnit: String?,
    val price: String?,
    val quoteCount: Int?,
) {
    /** "회당 60,000원" 등 금액 요약(구조화 금액 우선, 없으면 자유 메모). */
    val priceText: String?
        get() {
            if (priceAmount != null) {
                val won = String.format(java.util.Locale.KOREA, "%,d", priceAmount)
                val prefix = when (priceUnit) {
                    "PER_MONTH" -> "월 "
                    "TOTAL" -> ""
                    else -> "회당 "
                }
                return "$prefix${won}원"
            }
            return price?.ifEmpty { null }
        }

    companion object {
        fun from(o: JSONObject?): ChatQuoteContext? = o?.let {
            ChatQuoteContext(
                quoteId = it.optInt("quoteId"), responseId = it.optInt("responseId"),
                quoteStatus = it.optString("quoteStatus"), responseStatus = it.optString("responseStatus"),
                isTeacher = it.optBoolean("isTeacher", false), matched = it.optBoolean("matched", false),
                categoryId = it.stringOrNull("categoryId"), priceAmount = it.intOrNull("priceAmount"),
                priceUnit = it.stringOrNull("priceUnit"), price = it.stringOrNull("price"),
                quoteCount = it.intOrNull("quoteCount"),
            )
        }
    }
}

/**
 * 레슨 사이클 — 한 방(양방향 가능)의 개별 레슨(견적 방향)별 역할·진행상태.
 *  둘 다 강사인 두 사람이 서로에게 요청하면 사이클이 여러 개(반대 방향) 생긴다.
 */
data class ChatLessonCycle(
    val quoteId: Int,
    val responseId: Int,
    val categoryId: String?,
    val title: String?,
    val role: String,            // 열람자의 역할: TEACHER | MEMBER
    val teacherId: Int,
    val memberId: Int,
    val teacherName: String?,
    val memberName: String?,
    val matched: Boolean,
    val priceAmount: Int?,
    val priceUnit: String?,
    val price: String?,
    val progress: ChatLessonProgress,
    val lessonSchedule: ChatLessonSchedule?,
    val kind: String?,           // "PROPOSAL"=약속잡기 레슨(견적 없음)
) {
    val isTeacher: Boolean get() = role == "TEACHER"
    val isProposal: Boolean get() = kind == "PROPOSAL"

    /** 방향 배지 — "누가 가르치고 누가 배우나"를 명확히. */
    val directionLabel: String
        get() {
            val opponent = (if (isTeacher) memberName else teacherName) ?: "상대"
            return if (isTeacher) "내가 가르쳐요 · 수강생 ${opponent}님" else "내가 배워요 · 강사 ${opponent}님"
        }

    /** 내 액션(강사로서 일정 확정)이 필요한 사이클인지 — 요약바 '할 일' 표시용. */
    val needsMyAction: Boolean get() = isTeacher && progress.step == "ACCEPTED"

    val opponentName: String get() = (if (isTeacher) memberName else teacherName) ?: "상대"
    val opponentRoleLabel: String get() = if (isTeacher) "수강생" else "강사"

    /** 진행 카드가 기대하는 ChatQuoteContext 로 어댑트(카드 로직 재사용). */
    val asContext: ChatQuoteContext
        get() = ChatQuoteContext(
            quoteId = quoteId, responseId = responseId,
            quoteStatus = if (matched) "MATCHED" else "OPEN",
            responseStatus = if (matched) "ACCEPTED" else "SENT",
            isTeacher = isTeacher, matched = matched,
            categoryId = categoryId, priceAmount = priceAmount,
            priceUnit = priceUnit, price = price, quoteCount = progress.responseCount,
        )

    companion object {
        fun from(o: JSONObject) = ChatLessonCycle(
            quoteId = o.optInt("quoteId"), responseId = o.optInt("responseId"),
            categoryId = o.stringOrNull("categoryId"), title = o.stringOrNull("title"),
            role = o.optString("role"), teacherId = o.optInt("teacherId"), memberId = o.optInt("memberId"),
            teacherName = o.stringOrNull("teacherName"), memberName = o.stringOrNull("memberName"),
            matched = o.optBoolean("matched", false),
            priceAmount = o.intOrNull("priceAmount"), priceUnit = o.stringOrNull("priceUnit"),
            price = o.stringOrNull("price"),
            progress = ChatLessonProgress.from(o.optJSONObject("progress") ?: JSONObject()),
            lessonSchedule = ChatLessonSchedule.from(o.optJSONObject("lessonSchedule")),
            kind = o.stringOrNull("kind"),
        )
    }
}

/** 채팅방 상세 (GET /chat/rooms/:id). */
data class ChatRoomDetail(
    val roomId: Int,
    val type: String?,
    val spaceId: Int?,
    val opponent: ChatOpponent?,
    val opponentLastReadAt: String?,   // 상대 마지막 읽음시각(읽음표시 초기상태)
    val muted: Boolean?,
    val quoteContext: ChatQuoteContext?,
    val lessonSchedule: ChatLessonSchedule?,
    val progress: ChatLessonProgress?,
    val lessonCycles: List<ChatLessonCycle>?,   // 있으면 이걸 우선 렌더(양방향)
) {
    companion object {
        fun from(o: JSONObject) = ChatRoomDetail(
            roomId = o.optInt("roomId"),
            type = o.stringOrNull("type"),
            spaceId = o.intOrNull("spaceId"),
            opponent = ChatOpponent.from(o.optJSONObject("opponent")),
            opponentLastReadAt = o.stringOrNull("opponentLastReadAt"),
            muted = o.boolOrNull("muted"),
            quoteContext = ChatQuoteContext.from(o.optJSONObject("quoteContext")),
            lessonSchedule = ChatLessonSchedule.from(o.optJSONObject("lessonSchedule")),
            progress = o.optJSONObject("progress")?.let { ChatLessonProgress.from(it) },
            lessonCycles = o.optJSONArray("lessonCycles")?.map { ChatLessonCycle.from(it) },
        )
    }
}

/** 메시지 발신자(소켓 new-message 에 포함. REST 메시지엔 senderId 만). */
data class ChatSender(val id: Int, val name: String?, val nickname: String?) {
    val displayName: String get() = nickname ?: name ?: "상대"

    companion object {
        fun from(o: JSONObject?): ChatSender? = o?.let {
            ChatSender(it.optInt("id"), it.stringOrNull("name"), it.stringOrNull("nickname"))
        }
    }
}

/** 이모지 반응 집계(emoji별 개수 + 내가 눌렀는지). */
data class ChatReaction(val emoji: String, val count: Int, val mine: Boolean) {
    companion object {
        fun from(o: JSONObject) = ChatReaction(o.optString("emoji"), o.optInt("count"), o.optBoolean("mine", false))
    }
}

/** 답장 미리보기(인용 버블). */
data class ChatReplyPreview(val id: Int, val content: String?, val senderName: String?) {
    companion object {
        fun from(o: JSONObject?): ChatReplyPreview? = o?.let {
            ChatReplyPreview(it.optInt("id"), it.stringOrNull("content"), it.stringOrNull("senderName"))
        }
    }
}

/** 채팅 메시지. REST/소켓 공통 — sender 는 소켓에서만 온다. */
data class ChatMessage(
    val id: Int,
    val roomId: Int,
    val senderId: Int,
    val type: String,
    val content: String,
    val imageUrl: String?,
    val replyToId: Int?,
    val editedAt: String?,
    val deletedAt: String?,
    val replyTo: ChatReplyPreview?,
    val createdAt: String?,
    val sender: ChatSender?,
    val surveyDone: Boolean?,
    val surveySeq: Int?,
    val surveySentAt: String?,
    val surveyRevision: Int?,
    val surveyRespondedAt: String?,
    val surveyUpdatedAt: String?,
    val reactions: List<ChatReaction>?,
) {
    val isDeleted: Boolean get() = deletedAt != null
    val isEdited: Boolean get() = editedAt != null

    /** 여러 장 이미지는 콤마 joined 문자열로 온다(iOS sendImage 규약). */
    val imageUrls: List<String>
        get() = imageUrl?.split(",")?.mapNotNull { it.trim().ifEmpty { null } } ?: emptyList()

    companion object {
        fun from(o: JSONObject) = ChatMessage(
            id = o.optInt("id"),
            roomId = o.optInt("roomId"),
            senderId = o.optInt("senderId"),
            type = o.optString("type").ifEmpty { "TEXT" },
            content = o.optString("content"),
            imageUrl = o.stringOrNull("imageUrl"),
            replyToId = o.intOrNull("replyToId"),
            editedAt = o.stringOrNull("editedAt"),
            deletedAt = o.stringOrNull("deletedAt"),
            replyTo = ChatReplyPreview.from(o.optJSONObject("replyTo")),
            createdAt = o.stringOrNull("createdAt"),
            sender = ChatSender.from(o.optJSONObject("sender")),
            surveyDone = o.boolOrNull("surveyDone"),
            surveySeq = o.intOrNull("surveySeq"),
            surveySentAt = o.stringOrNull("surveySentAt"),
            surveyRevision = o.intOrNull("surveyRevision"),
            surveyRespondedAt = o.stringOrNull("surveyRespondedAt"),
            surveyUpdatedAt = o.stringOrNull("surveyUpdatedAt"),
            reactions = o.optJSONArray("reactions")?.map { ChatReaction.from(it) },
        )
    }
}

/** GET /chat/rooms/:id/messages */
data class ChatMessagesResponse(val total: Int, val page: Int, val limit: Int, val messages: List<ChatMessage>) {
    companion object {
        fun from(o: JSONObject) = ChatMessagesResponse(
            total = o.optInt("total"), page = o.optInt("page"), limit = o.optInt("limit"),
            messages = o.optJSONArray("messages")?.map { ChatMessage.from(it) } ?: emptyList(),
        )
    }
}

/** 빠른 답변 칩(백엔드 chat_quick_replies). */
data class ChatQuickReply(val id: Int, val text: String, val icon: String?) {
    companion object {
        fun from(o: JSONObject) = ChatQuickReply(o.optInt("id"), o.optString("text"), o.stringOrNull("icon"))
    }
}
