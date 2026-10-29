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
 */
class RecruitChatLimitException(
    val limit: Int?,
    val used: Int?,
    val tier: String?,
    serverMessage: String?,
) : Exception(serverMessage ?: CODE) {

    val dialogTitle: String get() = "이번 달 채팅 가능 인원을 모두 썼어요"

    val dialogMessage: String
        get() = limit?.takeIf { it > 0 }?.let {
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
