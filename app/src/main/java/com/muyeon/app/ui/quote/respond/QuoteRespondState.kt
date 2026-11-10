package com.muyeon.app.ui.quote.respond

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.muyeon.app.ui.common.ApiMessageError

/** 화면 단계 — 웹 QuoteRespond.js 의 분기 순서 그대로(gone → 공개 → 보냄 완료 → 이미 보냄 → 마감 → 요청 상세). */
sealed interface QuoteRespondPhase {
    data object Loading : QuoteRespondPhase
    data class Gone(val reason: QuoteRespondGone) : QuoteRespondPhase
    data class Public(val data: PublicQuoteDetail) : QuoteRespondPhase
    /** 보낸 뒤 — 채팅방이 없을 때(비회원 요청)만 남는다. 방이 있으면 바로 그 방으로 간다(웹 replace). */
    data class Done(val detail: QuoteRespondDetail) : QuoteRespondPhase
    data class Sent(val detail: QuoteRespondDetail, val mine: MyQuoteResponse) : QuoteRespondPhase
    data class Closed(val detail: QuoteRespondDetail, val reason: QuoteRespondClosed) : QuoteRespondPhase
    /** [withdrawn] = 전에 보냈다가 철회한 제안(있으면 위에 보여 주고 다시 보낼 수 있다). */
    data class Request(val detail: QuoteRespondDetail, val withdrawn: MyQuoteResponse? = null) : QuoteRespondPhase
}

/**
 * 견적 응답 화면 상태 — 기존 견적 화면(QuoteBrowseState)과 같은 꼴(Compose 상태 + suspend 함수).
 * @param isProvider 지금 견적을 보낼 수 있는가(로그인 + 강사·학원 활동 중) — 웹 `isLoggedIn && canReceiveQuoteAs(activeType)`.
 * @param autoCompose 가입·로그인하고 돌아왔다(웹 초안 quoteRespond:id) — 요청이 열려 있으면 작성 시트를 바로 연다.
 */
class QuoteRespondState(
    private val api: QuoteRespondApi,
    val quoteId: Int,
    private val isProvider: () -> Boolean,
    private var autoCompose: Boolean = false,
) {
    var phase by mutableStateOf<QuoteRespondPhase>(QuoteRespondPhase.Loading)
        private set
    var provider by mutableStateOf(false)
        private set
    /** 강사·학원 중 이미 가진 유형(일반 유형으로 활동 중) — '○○ 유형으로 전환하고 견적 보내기'. */
    var switchTo by mutableStateOf<String?>(null)
        private set
    /** 자동 인사 템플릿 — 작성 시트의 메시지 칸을 미리 채운다(웹 callGetMyChatAutoMessage). */
    var autoMessage by mutableStateOf("")
        private set
    var composing by mutableStateOf(false)
    var busy by mutableStateOf(false)
    var toast by mutableStateOf<String?>(null)

    val isLoggedIn: Boolean get() = api.isLoggedIn

    suspend fun load() {
        provider = isProvider()
        if (!provider) {
            switchTo = if (api.isLoggedIn) api.heldTypes().let { held -> listOf("TEACHER", "ACADEMY").firstOrNull { it in held } } else null
            api.getPublicQuote(quoteId)
                .onSuccess { d -> phase = d?.let { QuoteRespondPhase.Public(it) } ?: QuoteRespondPhase.Gone(QuoteRespondGone.DELETED) }
                .onFailure { phase = QuoteRespondPhase.Gone(QuoteRespondApi.classify(it, QuoteRespondGone.DELETED)) }
            return
        }
        api.getQuote(quoteId)
            .onSuccess { d -> phase = phaseOf(d) }
            .onFailure { phase = QuoteRespondPhase.Gone(QuoteRespondApi.classify(it, QuoteRespondGone.FORBIDDEN)) }
        if (autoMessage.isEmpty()) api.myAutoMessage()?.let { autoMessage = it }
        // 가입·전환하고 돌아왔다 — 하러 온 일(견적 보내기)을 바로 이어서 연다.
        if (autoCompose && phase is QuoteRespondPhase.Request) composing = true
        autoCompose = false
    }

    private fun phaseOf(d: QuoteRespondDetail?): QuoteRespondPhase {
        if (d == null) return QuoteRespondPhase.Gone(QuoteRespondGone.DELETED)
        if (d.quote.status == "CANCELED") return QuoteRespondPhase.Gone(QuoteRespondGone.CANCELED)
        // ★ 11-10 철회한 제안은 '보냄'이 아니다 — 열린 요청이면 다시 보내기로.
        val mine = d.myResponse
        if (mine != null && mine.status != com.muyeon.app.ui.quote.owner.WITHDRAWN) return QuoteRespondPhase.Sent(d, mine)
        val closed = when {
            d.quote.status == "MATCHED" -> QuoteRespondClosed.MATCHED
            d.expired || d.quote.status == "EXPIRED" || d.quote.status == "CLOSED" -> QuoteRespondClosed.EXPIRED
            else -> null
        }
        return closed?.let { QuoteRespondPhase.Closed(d, it) } ?: QuoteRespondPhase.Request(d, mine)
    }

    /**
     * 견적 보내기 — 성공이면 onRoom(방 id) 또는 완료 화면. 실패면 서버 안내(웹 alert)를 토스트로 두고 시트를 열어 둔다.
     * @return 성공 여부(시트가 버튼을 되살릴지).
     */
    suspend fun send(
        priceAmount: Int?, depositAmount: Int?, message: String, attachmentType: String,
        fallbackError: String, onRoom: (Int) -> Unit,
    ): Boolean {
        val detail = (phase as? QuoteRespondPhase.Request)?.detail ?: return false
        var ok = false
        api.sendResponse(quoteId, priceAmount, depositAmount, message, attachmentType)
            .onSuccess { roomId ->
                ok = true
                composing = false
                if (roomId != null) onRoom(roomId) else phase = QuoteRespondPhase.Done(detail)
            }
            .onFailure { toast = (it as? ApiMessageError)?.message?.ifBlank { null } ?: fallbackError }
        return ok
    }
}
