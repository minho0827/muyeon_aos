package com.muyeon.app.ui.quote

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.muyeon.app.ui.chat.ChatActivity
import com.muyeon.app.ui.common.MuyeonDialog
import com.muyeon.app.ui.common.MuyeonDialogAction
import com.muyeon.app.ui.common.MuyeonDialogKind
import com.muyeon.app.utils.TokenManager
import com.muyeon.app.webview.ActiveRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * ★ 2026-11-16 레슨 문의(이 사람에게만 보내는 지정 견적 요청) 흐름 — 강사 프로필(ResumeActivity)과 무용수 상세가 같이 쓴다.
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

/**
 * 무용수 활동유형 → 레슨 문의 차단 안내(고객측). 일반 전환에 성공하면 [onSwitched] 로 이어서 요청한다
 *  (iOS RoleGate.dancerLessonCustomerGate). 강사 프로필·무용수 상세가 같이 쓴다.
 *  @param scope 화면의 범위 — 창이 닫혀도 전환 요청이 끊기지 않게 부르는 쪽 것을 쓴다.
 */
@Composable
fun DancerCustomerGateDialog(scope: CoroutineScope, onSwitched: () -> Unit, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    MuyeonDialog(
        title = "회원유형 안내",
        message = "무용수 유형에서는 레슨을 요청하거나 예약할 수 없어요. 일반 유형으로 전환하면 계속할 수 있어요.",
        actions = listOf(
            MuyeonDialogAction("${ActiveRole.label("GENERAL")} 유형으로 전환하고 계속", MuyeonDialogKind.PRIMARY) {
                onDismiss()
                scope.launch {
                    // ★ 2026-11-16 셸 단일 전환 함수로(탭바·MY·글쓰기 메뉴가 같이 바뀐다).
                    if (com.muyeon.app.ui.common.ShellTypeSwitch.switchTo(ctx, "GENERAL")) {
                        onSwitched()
                    }
                }
            },
            MuyeonDialogAction("취소", MuyeonDialogKind.CANCEL) { onDismiss() },
        ),
        onDismiss = onDismiss,
    )
}
