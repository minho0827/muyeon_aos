package com.muyeon.app.ui.chat.dealbar

/**
 * ★ 2026-11-14 대화 시작 전 채팅방 가운데 안내 문구(당근 '대화는 당근 채팅방에서…', iOS ChatNoticeCopy 1:1).
 *  짝(pair)마다 다르다. 연락처 문구는 실제 규칙(서버 common/contact-disclosure: 채택하면 서로 공개)과 같아야 한다.
 */
object ChatNoticeCopy {
    fun of(pair: String?, myRole: String?): String = when (pair) {
        "MEMBER_TEACHER" -> if (myRole == "pro") TEACHER_SIDE else MEMBER_SIDE
        "PROPOSAL" -> PROPOSAL
        "ACADEMY_TEACHER" -> ACADEMY
        "RECRUIT" -> RECRUIT
        else -> GENERAL
    }

    private const val MEMBER_SIDE =
        "대화는 무용연 채팅방에서 하는 것이 가장 안전해요.\n" +
            "강사님을 채택하면 서로 연락처가 공개돼요.\n" +
            "레슨 일정과 예약금은 무용연에서 정해야 환불·후기·분쟁 지원을 받을 수 있어요."

    private const val TEACHER_SIDE =
        "대화는 무용연 채팅방에서 하는 것이 가장 안전해요.\n" +
            "회원님이 채택하면 서로 연락처가 공개돼요.\n" +
            "레슨 일정은 [약속잡기]로 정해 두면 일정·후기가 함께 남아요."

    private const val PROPOSAL =
        "레슨 날짜는 [약속잡기]로 정해 보세요.\n" +
            "약속이 확정되면 서로 연락처가 공개돼요."

    private const val ACADEMY =
        "근무 요일·시간과 수업 내용을 채팅으로 먼저 확인해 보세요.\n" +
            "함께하기로 하면 서로 연락처가 공개돼요."

    private const val RECRUIT =
        "지원 내용과 근무 조건을 채팅으로 확인해 보세요.\n" +
            "상대가 외부 메신저나 송금을 먼저 요구하면 주의하세요!"

    private const val GENERAL =
        "대화는 무용연 채팅방에서 하는 것이 가장 안전해요.\n" +
            "상대가 외부 메신저로 유도하는 경우 피해가 있을 수 있으니 주의하세요!"
}
