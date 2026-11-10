package com.muyeon.app.ui.jobposting

import com.muyeon.app.util.optStringOrNull

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.muyeon.app.R
import com.muyeon.app.ui.components.HeartButton
import com.muyeon.app.ui.components.MainTopBar
import com.muyeon.app.ui.resume.ResumeApi
import com.muyeon.app.ui.resume.ResumeListItem
import com.muyeon.app.ui.resume.ResumeMode
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.utils.TokenManager
import com.muyeon.app.webview.ActiveRole
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * ★ 2026-11-08 공고 상세(보는 사람) — 채용(JOB)·대타(SUB). 웹 JobDetail·SubDetail 의 본문·지원하기.
 *   본문은 내 공고 관리 상세와 같은 부품(DetailHeader · JobPostingDetailContent · SubBody)을 쓴다.
 *   하단: [찜] [지원하기] → 이력서 고르기 → 지원(POST /{kind}/:id/apply). 이미 지원했으면 '지원한 공고예요'.
 *   ★ 11-10 내 공고(authorId == 내 id)면 하단은 [PostingOwnerBar](삭제·마감·수정) — 둘 중 하나를 고르기만 한다.
 */
@Composable
fun PostingViewScreen(
    kind: String,
    id: Int,
    liked: Boolean,
    onToggleLike: () -> Unit,
    onBack: () -> Unit,
    onLogin: () -> Unit,
    onWriteResume: () -> Unit,
    /** 작성자 [수정] — 대타는 대타 수정 폼, 채용은 채용 위저드(부모가 고른다). */
    onEdit: () -> Unit = {},
    /** 작성자 삭제 뒤 — 부모가 목록 결과 키를 싣고 닫는다. */
    onDeleted: () -> Unit = onBack,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val token = remember { TokenManager.getAccessToken(context) }
    val api = remember { JobPostingApi(token) }
    var detail by remember { mutableStateOf<JSONObject?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var applied by remember { mutableStateOf(false) }
    var resumes by remember { mutableStateOf<List<ResumeListItem>?>(null) }
    var picking by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var cancelOpen by remember { mutableStateOf(false) }
    val loggedIn = !token.isNullOrEmpty()

    LaunchedEffect(kind, id) {
        api.loadPosting(kind, id).onSuccess { detail = it }.onFailure { error = it.message ?: context.getString(R.string.posting_load_failed) }
        if (loggedIn) {
            applied = api.applied(kind, id)
            if (kind == "JOB") api.recordView(id)
        }
    }
    suspend fun reload() { api.loadPosting(kind, id).onSuccess { detail = it } }
    // 수정 화면(채용 위저드는 다른 Activity)에서 돌아오면 다시 읽는다 — 처음 한 번은 위 LaunchedEffect 가 읽었다.
    var resumedOnce by remember { mutableStateOf(false) }
    androidx.lifecycle.compose.LifecycleResumeEffect(kind, id) {
        if (resumedOnce) scope.launch { reload() } else resumedOnce = true
        onPauseOrDispose { }
    }
    // 작성자 동작 — 내 공고 관리와 같은 마감·삭제(확인창·서버 문구 그대로).
    val myId = remember { com.muyeon.app.ui.common.NativeHttp(context).myUserId() }
    val ownerActions = rememberPostingActions(api) { _, change ->
        if (change == PostingChange.DELETED) {
            Toast.makeText(context, R.string.owner_posting_deleted, Toast.LENGTH_SHORT).show()
            onDeleted()
        } else scope.launch { reload() }
    }

    Column(Modifier.fillMaxSize().background(AppColors.Surface)) {
        MainTopBar(stringResource(if (kind == "SUB") R.string.tab_subs else R.string.recruit_job_detail), primary = false, onBack = onBack)
        val d = detail
        when {
            d == null && error == null -> Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) { CircularProgressIndicator(color = AppColors.Primary) }
            d == null -> Box(Modifier.weight(1f).fillMaxWidth().padding(20.dp), Alignment.Center) {
                Text(error.orEmpty(), style = AppTypography.Body14, color = AppColors.TextSub, textAlign = TextAlign.Center)
            }
            else -> {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    DetailHeader(kind, d, null, d.optStringOrNull("status"))
                    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        HorizontalDivider(Modifier.padding(vertical = 6.dp), color = AppColors.Divider)
                        if (kind == "SUB") SubBody(d) else JobPostingDetailContent(JobForm.from(d), showPlaceholders = false)
                    }
                }
                val open = d.optString("status").let { it.isEmpty() || it == "OPEN" }
                // 하단 바 — 작성자/지원자 둘 중 하나를 고르기만 한다.
                val isOwner = myId != null && d.optInt("authorId", -1) == myId
                if (isOwner) PostingOwnerBar(
                    open = open, busy = ownerActions.busy,
                    onDelete = { ownerActions.requestDelete(PostingRef(kind, id)) },
                    onClose = { ownerActions.requestClose(PostingRef(kind, id)) },
                    onEdit = onEdit,
                )
                else PostingApplicantBar(
                    kind = kind, d = d, open = open, liked = liked, applied = applied, busy = busy,
                    onToggleLike = onToggleLike,
                    onCancel = { cancelOpen = true },
                    onApply = {
                        if (!loggedIn) { onLogin(); return@PostingApplicantBar }
                        busy = true
                        scope.launch {
                            val mode = if (ActiveRole.current(context) == "DANCER") ResumeMode.DANCER else ResumeMode.TEACHER
                            resumes = ResumeApi(token).list(mode).getOrNull().orEmpty()
                            busy = false; picking = true
                        }
                    },
                )
            }
        }
    }

    ownerActions.Dialogs()

    if (cancelOpen) {
        val appId = detail?.optJSONObject("myOffer")?.optInt("applicationId", 0) ?: 0
        SubCancelDialog(api, appId, onDismiss = { cancelOpen = false }) { msg ->
            cancelOpen = false
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            scope.launch { api.loadPosting(kind, id).onSuccess { detail = it } }
        }
    }

    if (picking) ApplySheet(
        resumes.orEmpty(), busy,
        onDismiss = { picking = false },
        onWriteResume = { picking = false; onWriteResume() },
        onApply = { resumeId ->
            busy = true
            scope.launch {
                api.apply(kind, id, resumeId)
                    .onSuccess { applied = true; Toast.makeText(context, R.string.posting_apply_done, Toast.LENGTH_SHORT).show() }
                    .onFailure { Toast.makeText(context, it.message ?: context.getString(R.string.posting_apply_failed), Toast.LENGTH_LONG).show() }
                busy = false; picking = false
            }
        },
    )
}

/** 지원자 하단 바 — [찜] [지원하기 / 지원한 공고예요 / 지원 취소 …]. 대타는 내 지원 상태(myOffer, 웹 SubDetail 과 같은 분기). */
@Composable
private fun PostingApplicantBar(
    kind: String, d: JSONObject, open: Boolean, liked: Boolean, applied: Boolean, busy: Boolean,
    onToggleLike: () -> Unit, onCancel: () -> Unit, onApply: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().background(AppColors.Surface).navigationBarsPadding()) {
        HorizontalDivider(color = AppColors.Divider)
        Row(Modifier.padding(horizontal = 16.dp).padding(top = 8.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(52.dp).border(1.dp, AppColors.ChipBorder, RoundedCornerShape(12.dp)), Alignment.Center) {
                HeartButton(liked, onToggleLike, size = 24.dp)
            }
            // 확정 + 수업 전이면 [지원 취소].
            val offer = if (kind == "SUB") d.optJSONObject("myOffer") else null
            val offerStatus = offer?.optString("status").orEmpty()
            val confirmed = offerStatus in setOf("ACCEPTED", "HIRED")
            val canceledAfterConfirm = offerStatus == "CANCELED" && offer?.optString("canceledBy") in setOf("TEACHER", "POSTER")
            val classStarted = kind == "SUB" && subClassStarted(d)
            val label = when {
                confirmed && !classStarted -> R.string.posting_cancel_apply
                confirmed -> R.string.posting_confirmed
                canceledAfterConfirm -> R.string.posting_canceled
                applied -> R.string.posting_applied
                !open -> R.string.posting_closed
                else -> R.string.posting_apply
            }
            if (confirmed && !classStarted) JobButton(stringResource(label), filled = false, enabled = !busy, modifier = Modifier.weight(1f), onClick = onCancel)
            else JobButton(stringResource(label), filled = true, enabled = open && !applied && !confirmed && !canceledAfterConfirm && !busy, modifier = Modifier.weight(1f), onClick = onApply)
        }
    }
}

/** 이력서 고르기 시트 — 웹 지원 확인 팝업(ResumePicker). 기본 이력서가 먼저 골라져 있다. 이력서가 없으면 작성하기. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun ApplySheet(resumes: List<ResumeListItem>, busy: Boolean, onDismiss: () -> Unit, onWriteResume: () -> Unit, onApply: (Int?) -> Unit) {
    var picked by remember(resumes) { mutableStateOf((resumes.firstOrNull { it.isDefault } ?: resumes.firstOrNull())?.id) }
    androidx.compose.material3.ModalBottomSheet(onDismiss, containerColor = AppColors.Surface) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(if (resumes.isEmpty()) R.string.posting_no_resume else R.string.posting_apply_q), style = AppTypography.Title18, color = AppColors.TextHead)
            if (resumes.isNotEmpty()) Text(stringResource(R.string.posting_apply_desc), style = AppTypography.Body14, color = AppColors.TextSub)
            resumes.forEach { r ->
                val on = r.id == picked
                Row(
                    Modifier.fillMaxWidth().border(1.dp, if (on) AppColors.TextHead else AppColors.ChipBorder, RoundedCornerShape(12.dp))
                        .clickable { picked = r.id }.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(r.title.ifBlank { stringResource(R.string.posting_resume_untitled) }, style = AppTypography.Body15Medium, color = AppColors.TextHead, modifier = Modifier.weight(1f))
                    if (r.isDefault) Text(stringResource(R.string.posting_resume_default), style = AppTypography.Caption12, color = AppColors.TextHint)
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                JobButton(stringResource(R.string.common_cancel), filled = false, enabled = !busy, modifier = Modifier.weight(1f)) { onDismiss() }
                if (resumes.isEmpty()) JobButton(stringResource(R.string.posting_write_resume), filled = true, enabled = true, modifier = Modifier.weight(1f)) { onWriteResume() }
                else JobButton(stringResource(R.string.posting_apply), filled = true, enabled = !busy && picked != null, modifier = Modifier.weight(1f)) { onApply(picked) }
            }
        }
    }
}

/** 대타 수업 시작이 지났나(classDate + classTime 시작, KST). 모르면 false. */
private fun subClassStarted(d: JSONObject): Boolean {
    val date = d.optString("classDate").takeIf { it.length >= 10 } ?: return false
    val start = d.optString("classTime").substringBefore("~").trim().takeIf { it.matches(Regex("\\d{1,2}:\\d{2}")) } ?: "00:00"
    return runCatching {
        val at = java.time.LocalDateTime.parse("${date.take(10)}T${start.padStart(5, '0')}")
        at.isBefore(java.time.LocalDateTime.now(java.time.ZoneId.of("Asia/Seoul")))
    }.getOrDefault(false)
}

/** 확정된 대타 취소 — 쌓일 벌점 안내 · 사유(선택) · [닫기][취소하기]. 웹 SubCancelModal(강사 쪽)과 같은 문구. */
@Composable
private fun SubCancelDialog(api: JobPostingApi, applicationId: Int, onDismiss: () -> Unit, onDone: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var points by remember { mutableStateOf<Int?>(null) }
    var reason by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    LaunchedEffect(applicationId) { points = api.subCancelPreview(applicationId) }
    androidx.compose.ui.window.Dialog(onDismiss) {
        Column(Modifier.fillMaxWidth().background(AppColors.Surface, RoundedCornerShape(16.dp)).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.posting_cancel_title), style = AppTypography.Title18, color = AppColors.TextHead)
            Text(points?.let { stringResource(R.string.posting_cancel_points, it) } ?: stringResource(R.string.posting_cancel_body),
                style = AppTypography.Body14, color = AppColors.TextBody)
            Box(Modifier.fillMaxWidth().border(1.dp, AppColors.ChipBorder, RoundedCornerShape(12.dp)).padding(14.dp)) {
                if (reason.isEmpty()) Text(stringResource(R.string.posting_cancel_reason), style = AppTypography.Body14, color = AppColors.TextHint)
                androidx.compose.foundation.text.BasicTextField(reason, { reason = it }, textStyle = AppTypography.Body14.copy(color = AppColors.TextHead), modifier = Modifier.fillMaxWidth())
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                JobButton(stringResource(R.string.common_close), filled = false, enabled = !busy, modifier = Modifier.weight(1f)) { onDismiss() }
                JobButton(stringResource(R.string.posting_cancel_confirm), filled = true, enabled = !busy && applicationId > 0, modifier = Modifier.weight(1f)) {
                    busy = true
                    scope.launch {
                        api.cancelSubApplication(applicationId, reason)
                            .onSuccess { r ->
                                val p = r.optInt("points", 0)
                                onDone(if (p > 0) context.getString(R.string.posting_cancel_done_points, p) else context.getString(R.string.posting_cancel_done))
                            }
                            .onFailure { Toast.makeText(context, it.message ?: context.getString(R.string.posting_apply_failed), Toast.LENGTH_LONG).show(); busy = false }
                    }
                }
            }
        }
    }
}
