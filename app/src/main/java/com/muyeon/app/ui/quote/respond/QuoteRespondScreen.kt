package com.muyeon.app.ui.quote.respond

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.muyeon.app.R
import com.muyeon.app.ui.common.MuyeonDialog
import com.muyeon.app.ui.common.MuyeonDialogAction
import com.muyeon.app.ui.common.MuyeonDialogKind
import com.muyeon.app.ui.components.EmptyView
import com.muyeon.app.ui.components.MainTopBar
import com.muyeon.app.ui.components.SheetAction
import com.muyeon.app.ui.format.KstDates
import com.muyeon.app.ui.quote.QuoteFull
import com.muyeon.app.ui.quote.QuoteRespondSheet
import com.muyeon.app.ui.quote.QuoteUi
import com.muyeon.app.ui.quote.owner.QuoteResponseDraft
import com.muyeon.app.ui.quote.owner.QuoteResponseOwnerActions
import com.muyeon.app.ui.quote.owner.canChangeResponse
import com.muyeon.app.ui.quote.respond.components.QuotePublicCta
import com.muyeon.app.ui.quote.respond.components.QuoteRequestDetailSection
import com.muyeon.app.ui.quote.respond.components.QuoteRequestSummaryCard
import com.muyeon.app.ui.quote.respond.components.QuoteRespondButtons
import com.muyeon.app.ui.quote.respond.components.QuoteSentResponseCard
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.Dimensions
import kotlinx.coroutines.launch

/** 화면 밖으로 나가는 이동 — 부르는 쪽(QuoteBrowseActivity)이 연다. */
class QuoteRespondActions(
    val onBack: () -> Unit,
    /** 채팅방 — replace = 보내자마자 그 방으로(웹 openChatRoom replace) → 이 화면은 닫는다. */
    val onOpenChat: (roomId: Int, replace: Boolean) -> Unit,
    /** 웹 navigate("/availableQuotes") — 수강생 찾기(견적 모아보기). */
    val onToList: () -> Unit,
    /** 웹 navigate("/home"). */
    val onHome: () -> Unit,
    /** 비회원 [가입하고 견적 보내기] — 앱 로그인 화면(돌아오면 이 요청 + 작성 시트). */
    val onLogin: () -> Unit,
    /** 이미 가진 강사·학원 유형으로 전환. */
    val onSwitch: (type: String) -> Unit,
    /** 일반회원 — 강사·학원 유형 추가(웹 goRoleVerification). */
    val onAddRole: (role: String) -> Unit,
    val onOpenPhoto: (urls: List<String>, index: Int) -> Unit,
)

/**
 * 견적 응답(강사·학원이 레슨 요청 하나를 보고 견적을 보낸다) — 웹 /availableQuotes/:id(pages/muyeon/my/QuoteRespond.js) 1:1.
 *  강사·학원 활동 중: 요청 상세 → [견적 보내기] → 기존 견적 작성 시트(QuoteRespondSheet, 모아보기와 같은 부품) / 이미 보냄 → 내가 보낸 견적서 / 마감 → 안내.
 *  그 밖(비회원·일반회원): 공개 상세 + 가입·전환·유형 추가 유도.
 */
@Composable
fun QuoteRespondScreen(state: QuoteRespondState, actions: QuoteRespondActions) {
    val context = LocalContext.current
    LaunchedEffect(state.toast) {
        state.toast?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show(); state.toast = null }
    }
    val phase = state.phase
    val reload = rememberLaunch { state.load() }
    val title = stringResource(if (phase is QuoteRespondPhase.Sent) R.string.quote_respond_title_sent else R.string.quote_respond_title)

    Column(Modifier.fillMaxSize().background(AppColors.Surface).navigationBarsPadding()) {
        MainTopBar(title = title, primary = false, onBack = actions.onBack)
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (phase) {
                QuoteRespondPhase.Loading, is QuoteRespondPhase.Gone -> Unit
                is QuoteRespondPhase.Public -> PublicBody(state, phase.data, actions)
                is QuoteRespondPhase.Done -> ResultBody(
                    stringResource(R.string.quote_respond_done_title),
                    stringResource(if (phase.detail.quote.guestRequest) R.string.quote_respond_done_guest else R.string.quote_respond_done_member),
                    actions,
                )
                is QuoteRespondPhase.Closed -> ResultBody(stringResource(phase.reason.title), stringResource(phase.reason.body), actions)
                is QuoteRespondPhase.Sent -> RespondPage {
                    Summary(phase.detail.quote)
                    Section {
                        Text(stringResource(R.string.quote_respond_section_sent), style = AppTypography.Section16, color = AppColors.TextHead)
                        QuoteSentResponseCard(phase.mine)
                        // ★ 11-10 보낸 상태 · 요청이 열려 있으면 [수정][철회] — 바뀌면 다시 읽는다(철회면 다시 보내기 화면으로).
                        if (canChangeResponse(phase.mine.status, phase.detail.quote.status == "OPEN" && !phase.detail.expired)) {
                            QuoteResponseOwnerActions(
                                quoteId = phase.detail.quote.id,
                                responseId = phase.mine.id,
                                categoryLabel = QuoteUi.categoryLabel(phase.detail.quote.categoryId),
                                sent = phase.mine.draft(),
                                onChanged = { reload() },
                            )
                        }
                    }
                    QuoteRequestDetailSection(phase.detail.quote, Modifier.padding(horizontal = Dimensions.gutter), onOpenPhoto = actions.onOpenPhoto)
                    QuoteRespondButtons(
                        listOfNotNull(
                            phase.mine.chatRoomId?.let { rid -> SheetAction(stringResource(R.string.quote_respond_open_chat), true) { actions.onOpenChat(rid, false) } },
                            SheetAction(stringResource(R.string.quote_respond_to_list), false, actions.onToList),
                        ),
                        Modifier.padding(horizontal = Dimensions.gutter),
                    )
                }
                is QuoteRespondPhase.Request -> RespondPage {
                    Summary(phase.detail.quote)
                    phase.withdrawn?.let { WithdrawnSection(it) }
                    QuoteRequestDetailSection(phase.detail.quote, Modifier.padding(horizontal = Dimensions.gutter), onOpenPhoto = actions.onOpenPhoto)
                    // ★ 2026-11-14 다이렉트(나를 지정한) 요청이면 [죄송해요, 다음에 뵐게요] — 사유를 골라 정중히 거절.
                    QuoteRespondButtons(
                        listOfNotNull(
                            if (phase.detail.quote.isDirect) SheetAction(stringResource(R.string.quote_decline_button), false) { state.declining = true } else null,
                            SheetAction(stringResource(R.string.quote_respond_send), true) { state.composing = true },
                        ),
                        Modifier.padding(horizontal = Dimensions.gutter),
                    )
                }
            }
        }
    }

    (phase as? QuoteRespondPhase.Gone)?.let { GoneDialog(it.reason, state, actions) }

    val request = (phase as? QuoteRespondPhase.Request)?.detail
    if (state.declining && request != null) {
        val done = stringResource(R.string.quote_decline_done)
        val failed = stringResource(R.string.quote_decline_failed)
        val scope = androidx.compose.runtime.rememberCoroutineScope()
        com.muyeon.app.ui.quote.respond.decline.QuoteDeclineSheet(
            academy = request.quote.isAcademyRequest, busy = state.busy,
            onSubmit = { r, detail ->
                // 닫힐 때 QuoteBrowseActivity 기본 결과(QUOTES)로 목록이 다시 읽힌다.
                scope.launch { state.decline(r, detail, done, failed) }
            },
            onDismiss = { state.declining = false },
        )
    }
    if (state.composing && request != null) {
        val fallback = stringResource(R.string.quote_respond_send_failed)
        QuoteRespondSheet(
            quote = request.quote,
            attachmentRole = if (com.muyeon.app.webview.ActiveRole.isAcademy(context)) "ACADEMY" else "TEACHER",
            onDismiss = { state.composing = false },
            onSend = { amount, deposit, message, attachmentType ->
                state.send(amount, deposit, message, attachmentType, fallback) { rid -> actions.onOpenChat(rid, true) }
            },
            initialMessage = state.autoMessage,
        )
    }
}

/** 전에 보냈다가 철회한 제안 — 카드 + '다시 보낼 수 있어요'. */
@Composable
private fun WithdrawnSection(withdrawn: MyQuoteResponse) = Section {
    Text(stringResource(R.string.quote_owner_resend_title), style = AppTypography.Section16, color = AppColors.TextHead)
    QuoteSentResponseCard(withdrawn)
    Text(stringResource(R.string.quote_owner_resend_note), style = AppTypography.Sub13, color = AppColors.TextSub)
}

/** 고치기 시트를 보낸 값으로 채운다. */
private fun MyQuoteResponse.draft() = QuoteResponseDraft.of(priceAmount, priceUnit, paymentMode, depositAmount, message)

/** 공개 상세 — 요약(요청일 · 받은 견적 수) · 요청 내용(사진은 개수만) · 유도 박스 · 가입/전환/유형 추가 · 둘러보기. */
@Composable
private fun PublicBody(state: QuoteRespondState, data: PublicQuoteDetail, actions: QuoteRespondActions) = RespondPage {
    val q = data.quote
    val date = KstDates.dotTime(q.createdAt)
    val meta = if (data.responseCount > 0) stringResource(R.string.quote_respond_public_meta, date, data.responseCount)
    else stringResource(R.string.quote_respond_public_meta_none, date)
    Summary(q, meta = meta, guest = false)
    QuoteRequestDetailSection(
        q, Modifier.padding(horizontal = Dimensions.gutter), showPhotos = false,
        photoNote = data.photoCount.takeIf { it > 0 }?.let { stringResource(R.string.quote_respond_public_photo_note, it) },
    )
    val closedTitle = when (q.status) {
        "MATCHED" -> R.string.quote_respond_public_closed_matched
        "CANCELED" -> R.string.quote_respond_public_closed_canceled
        else -> R.string.quote_respond_public_closed_expired
    }
    QuotePublicCta(
        title = stringResource(if (data.closed) closedTitle else R.string.quote_respond_public_open_title),
        body = stringResource(if (data.closed) R.string.quote_respond_public_closed_body else R.string.quote_respond_public_open_body),
        modifier = Modifier.padding(horizontal = Dimensions.gutter),
    )
    val switchTo = state.switchTo
    val main = when {
        !state.isLoggedIn -> listOf(SheetAction(stringResource(if (data.closed) R.string.quote_respond_public_join else R.string.quote_respond_public_join_send), true, actions.onLogin))
        switchTo != null -> {
            val role = stringResource(if (switchTo == "ACADEMY") R.string.quote_respond_role_academy else R.string.quote_respond_role_teacher)
            listOf(SheetAction(stringResource(R.string.quote_respond_public_switch, role), true) { if (!state.busy) actions.onSwitch(switchTo) })
        }
        else -> listOf(
            SheetAction(stringResource(R.string.quote_respond_public_add_teacher), true) { actions.onAddRole("TEACHER") },
            SheetAction(stringResource(R.string.quote_respond_public_add_academy), false) { actions.onAddRole("ACADEMY") },
        )
    }
    QuoteRespondButtons(
        main + SheetAction(stringResource(R.string.quote_respond_public_browse), false, actions.onHome),
        Modifier.padding(horizontal = Dimensions.gutter).padding(bottom = Dimensions.spacingXXL),
        vertical = true,
    )
}

/** 보낸 뒤 · 마감 — 웹 EmptyView + [목록으로]. */
@Composable
private fun ResultBody(title: String, content: String, actions: QuoteRespondActions) = Column(Modifier.fillMaxSize()) {
    EmptyView(title, content = content)
    QuoteRespondButtons(
        listOf(SheetAction(stringResource(R.string.quote_respond_to_list), false, actions.onToList)),
        Modifier.padding(horizontal = Dimensions.gutter),
    )
}

/** 상세를 열 수 없음 — 사유별 팝업. 일시 장애면 [다시 시도] / [목록으로], 아니면 [확인] 뒤 목록(비회원은 홈). */
@Composable
private fun GoneDialog(reason: QuoteRespondGone, state: QuoteRespondState, actions: QuoteRespondActions) {
    val close = if (state.provider) actions.onToList else actions.onHome
    val retry = rememberLaunch { state.load() }
    MuyeonDialog(
        title = stringResource(reason.title),
        message = stringResource(reason.body),
        actions = if (reason.retryable) listOf(
            MuyeonDialogAction(stringResource(R.string.quote_respond_retry), MuyeonDialogKind.PRIMARY, retry),
            MuyeonDialogAction(stringResource(R.string.quote_respond_to_list), MuyeonDialogKind.CANCEL, close),
        ) else listOf(MuyeonDialogAction(stringResource(R.string.quote_respond_ok), MuyeonDialogKind.PRIMARY, close)),
        dismissOnAction = false,
        onDismiss = close,
    )
}

/** 웹 #quote_respond .content-wrap — 위 24 · 아래 48 · 묶음 사이 24. */
@Composable
private fun RespondPage(content: @Composable ColumnScope.() -> Unit) = Column(
    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = Dimensions.spacingXXL, bottom = PAGE_BOTTOM),
    verticalArrangement = Arrangement.spacedBy(Dimensions.spacingXXL),
    content = content,
)

/** 웹 .form-list.prl-20 — 좌우 20 묶음. */
@Composable
private fun Section(content: @Composable ColumnScope.() -> Unit) = Column(
    Modifier.fillMaxWidth().padding(horizontal = Dimensions.gutter),
    verticalArrangement = Arrangement.spacedBy(Dimensions.spacingM),
    content = content,
)

@Composable
private fun Summary(q: QuoteFull, meta: String? = null, guest: Boolean = q.guestRequest) = QuoteRequestSummaryCard(
    categoryLabel = QuoteUi.categoryLabel(q.categoryId),
    region = q.region,
    meta = meta,
    guestRequest = guest,
    requesterBadge = q.requesterBadge,
    academyName = q.academyName,
    modifier = Modifier.padding(horizontal = Dimensions.gutter),
)

@Composable
private fun rememberLaunch(block: suspend () -> Unit): () -> Unit {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    return { scope.launch { block() }.let { } }
}

private val PAGE_BOTTOM = 48.dp
