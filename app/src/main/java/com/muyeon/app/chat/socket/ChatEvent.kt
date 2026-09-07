package com.muyeon.app.chat.socket

import com.muyeon.app.ui.chat.ChatMessage
import com.muyeon.app.ui.chat.ChatRoomSummary

/**
 * muyeon-backend `chat.gateway.ts` 가 실제로 emit 하는 이벤트만 sealed class 로.
 *
 *  ⚠️ PaceERA 의 ChatEvent 를 그대로 베끼면 안 된다 — 무용연 게이트웨이에는
 *   force-logout · user-online/offline · participant-left 가 **없다**.
 *   (서버에 없는 이벤트를 구독하면 영원히 안 오는 코드가 남는다.)
 *
 *  ⚠️ 단 chat-room-removed 는 **있다**. 게이트웨이가 아니라 chat.controller 의
 *   방 나가기(DELETE /rooms/:id/leave)에서 emitToUser 로 보낸다. 종전 주석이
 *   '없다'고 단정하는 바람에 구독이 빠져, 웹이나 다른 기기에서 방을 나가도
 *   앱 목록에는 그 방이 계속 남아 있었다.
 *
 *  서버 → 클라 9종:
 *   new-message · message-updated · message-deleted · messages-read
 *   · message-reaction · user-typing · room-updated · chat-room-added · chat-room-removed
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

    /** 방에서 나감(다른 기기·웹 포함) — 목록에서 제거. chat.controller 의 leave 에서 온다. */
    data class ChatRoomRemoved(val roomId: Int) : ChatEvent()

    /**
     * 소켓이 (재)연결됨. 끊겨 있던 동안 온 이벤트는 replay=0 이라 통째로 유실되므로
     *  목록·뱃지를 서버 기준으로 다시 맞추라는 신호다.
     *  ⚠️ 서버가 보내는 이벤트가 아니라 **클라이언트가 스스로 만드는** 이벤트다.
     */
    data object Reconnected : ChatEvent()

    /**
     * 서버 알림(인증 승인 등) — 플로팅 책갈피/뱃지가 즉시 재조회하도록 흘린다.
     *  iOS FloatingSocketManager 의 'notification' 핸들러 대응. payload 는 쓰지 않는다.
     */
    data object ServerNotification : ChatEvent()
}
