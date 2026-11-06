package com.muyeon.app.ui.quote

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.auth0.jwt.JWT
import com.muyeon.app.ui.resume.PublicProfile
import com.muyeon.app.ui.resume.ResumeApi
import com.muyeon.app.utils.TokenManager

/**
 * ★ 2026-11-06 자동응답 미리보기 — "회원에게는 이렇게 보여요"(웹 AutoQuotePreviewSheet 와 같다).
 *  '다른 강사님들은 이렇게 보내요' 시트와 같은 채팅 모양(QuoteChatThread = 사진 + 견적 카드 + 말풍선)에
 *  내 공개 프로필(사진·이름·지역·경력·장르)과 이 메시지·금액을 넣어 보여 준다. 보기만 한다.
 *  금액은 강사가 쓴 문구 그대로 — 비어 있으면 카드가 '협의 가능'.
 */
@Composable
fun AutoQuotePreviewSheet(value: AutoQuoteEditorState, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val token = remember { TokenManager.getAccessToken(context) }
    // 메시지 정렬과 같은 기준 — JWT sub(ChatRoomViewModel.currentUserId).
    val myId = remember(token) {
        runCatching { token?.takeIf { it.isNotBlank() }?.let { JWT.decode(it).subject?.toIntOrNull() } }.getOrNull() ?: 0
    }
    var me by remember { mutableStateOf<PublicProfile?>(null) }
    LaunchedEffect(myId) {
        if (myId > 0) me = ResumeApi(token).publicProfile(myId, src = "quote").getOrNull() // 'quote' = 열람 알림 없음
    }

    val name = me?.name?.takeIf { it.isNotBlank() } ?: "나"
    val image = me?.image?.takeIf { it.isNotBlank() }
    QuoteThreadSheet(
        title = "미리보기",
        sub = "새 레슨 요청이 오면 회원 채팅방에 이렇게 보내져요.",
        onDismiss = onDismiss,
    ) {
        QuoteChatThread(
            name = name,
            image = image,
            cardJson = QuoteMessageExamples.previewCardJson(
                userId = myId, name = name, image = image,
                service = me?.genres?.firstOrNull(),
                region = me?.activeRegion,
                career = me?.career,
                price = value.price,
            ),
            text = value.message.trim().ifEmpty { "메시지를 입력하면 여기에 보여요." },
            onOpenProfile = {},
        )
    }
}
