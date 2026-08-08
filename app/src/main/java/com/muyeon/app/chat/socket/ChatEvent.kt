package com.muyeon.app.chat.socket

import com.muyeon.app.ui.chat.ChatMessage
import com.muyeon.app.ui.chat.ChatRoomSummary

/**
 * muyeon-backend `chat.gateway.ts` 가 실제로 emit 하는 이벤트만 sealed class 로.
 *
 *  ⚠️ PaceERA 의 ChatEvent 를 그대로 베끼면 안 된다 — 무용연 게이트웨이에는
 *   force-logout · user-online/offline · participant-left · chat-room-updated/removed 가 **없다**.
 *   (서버에 없는 이벤트를 구독하면 영원히 안 오는 코드가 남는다.)
 *
 *  서버 → 클라 8종:
 *   new-message · message-updated · message-deleted · messages-read
 *   · message-reaction · user-typing · room-updated · chat-room-added
 */
sealed class ChatEvent {

    /** 새 메시지 도착(room:{id} 브로드캐스트). 방 화면이 리스트 끝에 append. */
    data class NewMessage(val roomId: Int, val message: ChatMessage) : ChatEvent()

    /** 메시지 수정됨(작성자 edit-message). 같은 id 를 교체. */
    data class MessageUpdated(val roomId: Int, val message: ChatMessage) : ChatEvent()

    /** 메시지 삭제됨(soft). 같은 id 를 교체 → '삭제된 메시지'로 렌더. */
    data class MessageDeleted(val roomId: Int, val message: ChatMessage) : ChatEvent()

    /** 상대가 읽음 처리. 내 말풍선의 안읽음(1) 제거 기준 시각. */
    data class MessagesRead(val roomId: Int, val userId: Int, val readAt: String) : ChatEvent()

    /**
     * 이모지 반응 토글 — payload 에 집계가 없다(뷰어별 mine 이 달라서).
     *  서버 규약대로 **해당 메시지를 재조회**해야 정확하다.
     */
    data class MessageReaction(val roomId: Int, val messageId: Int) : ChatEvent()

    /** 상대 입력중 표시. 서버가 sender 를 제외하고 relay 하므로 내 것은 안 온다. */
    data class Typing(val roomId: Int, val userId: Int, val isTyping: Boolean) : ChatEvent()

    /** 방 메타 변경(user:{id} 로 옴) — 요약 없이 오는 갱신. 목록 전체 재조회 안전망. */
    data class RoomUpdated(val roomId: Int) : ChatEvent()

    /**
     * 방 요약을 통째로 실은 증분 갱신(user:{id}). 목록에 upsert 하면 재조회가 필요 없다.
     *  이름은 'added' 지만 서버는 **신규/갱신 양쪽에 같은 이벤트**를 쓴다
     *  (send-message·mark-read·join-room 모두 이걸 쏜다).
     */
    data class ChatRoomAdded(val room: ChatRoomSummary) : ChatEvent()
}
