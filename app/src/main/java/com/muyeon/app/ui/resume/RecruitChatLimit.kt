package com.muyeon.app.ui.resume

import androidx.compose.runtime.Composable
import com.muyeon.app.ui.common.MuyeonDialog
import com.muyeon.app.ui.common.MuyeonDialogAction
import com.muyeon.app.ui.common.MuyeonDialogKind
import com.muyeon.app.ui.quote.intOrNull
import com.muyeon.app.ui.quote.stringOrNull
import org.json.JSONObject

/**
 * 학원원장·공연팀이 이번 달 새 강사·무용수에게 먼저 채팅할 수 있는 인원을 다 썼을 때(403 RECRUIT_CHAT_LIMIT).
 *  서버: POST /chat/rooms/direct → { code, message, data: { tier, limit, used } } (서버 공통 에러 모양).
 *  iOS `RecruitChatLimit.swift` 와 같은 문구·버튼([닫기] [멤버십 보기]).
 *  ★ 11-07 채팅방 소켓 send-message ack 도 같은 코드로 거절한다:
 *   { ok:false, code:"RECRUIT_CHAT_LIMIT", message, data:{ tier, limit, used } } → fromAck → 같은 팝업.
 *   이때는 서버 문구(message)를 그대로 보이고, 비어 있으면 기본 문구(웹 recruitChatLimit.js 와 같음).
 */
class RecruitChatLimitException(
    val limit: Int?,
    val used: Int?,
    val tier: String?,
    serverMessage: String?,
    /** 팝업 본문으로 쓸 서버 문구 — 소켓 ack 에서만 채운다(방 열기 403 은 기본 문구 그대로). */
    private val displayMessage: String? = null,
) : Exception(serverMessage ?: CODE) {

    val dialogTitle: String get() = "이번 달 채팅 가능 인원을 모두 썼어요"

    val dialogMessage: String
        get() = displayMessage?.takeIf { it.isNotBlank() } ?: limit?.takeIf { it > 0 }?.let {
            "무료로는 한 달에 새로운 강사·무용수 ${it}명까지 먼저 채팅할 수 있어요. 더 연락하려면 멤버십에 가입해 주세요."
        } ?: "멤버십에 가입하면 강사·무용수에게 먼저 채팅할 수 있어요."

    companion object {
        const val CODE = "RECRUIT_CHAT_LIMIT"

        fun from(message: String?, data: JSONObject?) = RecruitChatLimitException(
            limit = data?.intOrNull("limit"),
            used = data?.intOrNull("used"),
            tier = data?.stringOrNull("tier"),
            serverMessage = message,
        )

        /** 소켓 send-message 거절 ack → 채팅 한도. data 가 없으면 바깥 limit·used·tier 를 읽는다(웹과 같은 방식). */
        fun fromAck(message: String?, data: JSONObject?, ack: JSONObject? = null): RecruitChatLimitException {
            val d = data ?: ack
            return RecruitChatLimitException(
                limit = d?.intOrNull("limit") ?: ack?.intOrNull("limit"),
                used = d?.intOrNull("used") ?: ack?.intOrNull("used"),
                tier = d?.stringOrNull("tier") ?: ack?.stringOrNull("tier"),
                serverMessage = message,
                displayMessage = message,
            )
        }
    }
}

/** 채팅 한도 팝업 — 앱 공통 팝업(MuyeonDialog). [멤버십 보기] → onUpgrade. */
@Composable
internal fun RecruitChatLimitDialog(
    e: RecruitChatLimitException,
    onUpgrade: () -> Unit,
    onDismiss: () -> Unit,
) {
    MuyeonDialog(
        title = e.dialogTitle,
        message = e.dialogMessage,
        actions = listOf(
            MuyeonDialogAction("멤버십 보기", MuyeonDialogKind.PRIMARY, onUpgrade),
            MuyeonDialogAction("닫기", MuyeonDialogKind.CANCEL, onDismiss),
        ),
        dismissOnAction = false,
        onDismiss = onDismiss,
    )
}
