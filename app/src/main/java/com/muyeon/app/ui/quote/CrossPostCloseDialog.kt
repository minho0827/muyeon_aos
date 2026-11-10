package com.muyeon.app.ui.quote

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.muyeon.app.ui.common.MuyeonDialog
import com.muyeon.app.ui.common.MuyeonDialogAction
import com.muyeon.app.ui.common.MuyeonDialogKind
import kotlinx.coroutines.launch

/**
 * ★ 11-10 학원 요청 채택 직후 — '채용 공고도 마감할까요?' (웹 CrossPostCloseModal · iOS 와 같은 문구)
 *  채택 응답의 openCrossPosts(같이 올려 아직 모집 중인 공고)를 하나씩 묻는다.
 *  [계속 모집]은 그대로 둔다 — 강사를 여러 명 뽑는 학원도 있다.
 *  첫 질문 앞에 채택 완료 안내를 붙인다(채택 토스트와 겹쳐 뜨지 않게 이 창이 대신 알린다).
 *
 *  @param onMessage 마감 결과 한 줄(토스트) — 호출 화면의 토스트로 띄운다.
 *  @param onDone 다 물어본 뒤(마감·계속 무관) 한 번 부른다 — 화면이 채택 뒤 하던 일을 이어 간다(채팅방 열기 등).
 */
@Composable
fun CrossPostCloseDialog(
    api: QuoteApi,
    quoteId: Int,
    posts: List<QuoteCrossPost>,
    onMessage: (String) -> Unit,
    onDone: () -> Unit,
) {
    var index by remember(posts) { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val cur = posts.getOrNull(index) ?: return

    fun next() {
        if (index + 1 < posts.size) index += 1 else onDone()
    }

    val intro = if (index == 0) "채용을 확정했어요. " else ""
    MuyeonDialog(
        title = "채용 공고도 마감할까요?",
        message = "${intro}같이 올린 '${cur.title?.ifBlank { null } ?: "채용 공고"}'가 아직 모집 중이에요. 강사님을 더 뽑지 않는다면 마감해 주세요.",
        actions = listOf(
            MuyeonDialogAction(if (busy) "처리 중…" else "마감하기", MuyeonDialogKind.PRIMARY) {
                if (busy) return@MuyeonDialogAction
                busy = true
                scope.launch {
                    api.closeCrossPost(quoteId, cur.id)
                        .onSuccess { onMessage("채용 공고를 마감했어요.") }
                        .onFailure {
                            onMessage((it as? ApiMessageException)?.message?.ifBlank { null }
                                ?: "공고를 마감하지 못했어요. 내 공고에서 마감해 주세요.")
                        }
                    busy = false
                    next()
                }
            },
            MuyeonDialogAction("계속 모집", MuyeonDialogKind.CANCEL) { if (!busy) next() },
        ),
        // 반드시 둘 중 하나를 고르게 — 바깥 탭으로 닫히면 채택 뒤 흐름(채팅방 열기)이 멈춘다.
        force = true,
        dismissOnAction = false,
        onDismiss = {},
    )
}
