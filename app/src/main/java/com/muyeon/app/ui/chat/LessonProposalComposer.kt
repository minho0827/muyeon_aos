package com.muyeon.app.ui.chat

import com.muyeon.app.webview.withActiveType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.muyeon.app.ui.chat.proposal.MemberProposalForm
import com.muyeon.app.ui.chat.proposal.ProposalFormState
import com.muyeon.app.ui.chat.proposal.ProposalSheetScaffold
import com.muyeon.app.ui.chat.proposal.TeacherProposalForm
import com.muyeon.app.BuildConfig
import com.muyeon.app.ui.lesson.LessonRefundPolicyRepo
import com.muyeon.app.ui.lesson.UserCalendarApi
import com.muyeon.app.ui.quote.QuoteDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** 겹침(409) — "그래도 보낼까요?" 경고용. */
internal class ProposalConflict(message: String) : Exception(message)

/**
 * 레슨 약속 제안 — POST /lesson-proposals. iOS `LessonProposalService.create` 1:1.
 *  겹침이면 409 → 경고 후 force 재시도.
 */
class LessonProposalApi(private val token: String?) {

    private val client = OkHttpClient()
    private val json = "application/json; charset=utf-8".toMediaType()

    suspend fun create(
        roomId: Int,
        startAtMillis: Long,
        durationMin: Int,
        place: String?,
        memo: String,
        calendarId: Int?,
        force: Boolean,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val body = JSONObject()
                .put("roomId", roomId)
                .put("startAt", ISO.format(Date(startAtMillis)))
                .put("durationMin", durationMin)
                .put("force", force)
            place?.takeIf { it.isNotBlank() }?.let { body.put("place", it) }
            memo.trim().takeIf { it.isNotEmpty() }?.let { body.put("memo", it) }
            calendarId?.takeIf { it > 0 }?.let { body.put("calendarId", it) }

            val req = Request.Builder()
                .url(BuildConfig.API_BASE_URL + "/api/lesson-proposals")
                .post(body.toString().toRequestBody(json))
                .apply { if (!token.isNullOrEmpty()) addHeader("Authorization", "Bearer $token") }.withActiveType()
                .build()
            client.newCall(req).execute().use { res ->
                val text = res.body?.string().orEmpty()
                val msg = runCatching { JSONObject(text).optString("message") }.getOrNull()
                // 409 는 실패가 아니라 "겹치는데 강행할래?" 물음이다 — 따로 구분한다.
                if (res.code == 409) throw ProposalConflict(msg?.ifEmpty { null } ?: "그 시간엔 이미 일정이 있어요.")
                if (!res.isSuccessful) error(msg?.ifEmpty { null } ?: "제안을 보내지 못했어요. 다시 시도해 주세요.")
            }
        }
    }

    companion object {
        private val ISO = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }
}

/**
 * 레슨 약속 제안 작성 — iOS `LessonProposalComposer.swift` 1:1. 채택된 견적의 회원·강사 모두 날짜를 제안할 수 있다.
 *  ★ 2026-11-14 한 함수(485줄)에서 강사·회원 칸을 if 로 끼우고 안드로이드 기본 보라 부품을 쓰던 것을
 *   proposal/ 폴더의 부품으로 나눴다: 틀(ProposalSheetScaffold) + 역할별 본문(Teacher/MemberProposalForm)
 *   + 상태·보내기(ProposalFormState). 여기는 조립만 한다. 보내는 내용·호출부는 예전과 같다.
 */
@Composable
fun LessonProposalComposer(
    api: LessonProposalApi,
    calendarApi: UserCalendarApi,
    roomId: Int,
    isTeacher: Boolean,
    totalPrice: Int,
    depositAmount: Int,
    /** 캘린더에서 열 때 편집기에 이미 적어둔 값을 그대로 싣는다(iOS 는 편집기가 제안 모드로 바뀐다). */
    initialStartAt: Long? = null,
    initialDurationMin: Int? = null,
    initialPlace: String = "",
    initialMemo: String = "",
    initialCalendarId: Int? = null,
    onSent: () -> Unit,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val state = remember {
        ProposalFormState(
            api, roomId, isTeacher, totalPrice, depositAmount,
            initialStartAt, initialDurationMin, initialPlace, initialMemo, initialCalendarId,
            LessonRefundPolicyRepo.current(),
        )
    }
    LaunchedEffect(Unit) { state.refundPolicy = LessonRefundPolicyRepo.get() }
    LaunchedEffect(Unit) { state.calendars = calendarApi.list().getOrDefault(emptyList()) }

    fun send(force: Boolean) {
        scope.launch { if (state.submit(force)) onSent() }
    }

    ProposalSheetScaffold(state.sending, state.canSend, onCancel = onDismiss, onSend = { send(force = false) }) {
        // 역할마다 다른 본문 부품 — 칸을 if 로 섞지 않는다.
        if (isTeacher) TeacherProposalForm(state) else MemberProposalForm(state)
    }

    state.conflictText?.let { msg ->
        QuoteDialog(
            "이미 일정이 있어요", "$msg\n같은 시간에 제안을 보낼까요?", "그래도 보내기",
            onConfirm = { state.conflictText = null; send(force = true) },
            onDismiss = { state.conflictText = null },
        )
    }
}
