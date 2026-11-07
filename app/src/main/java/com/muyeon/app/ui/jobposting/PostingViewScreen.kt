package com.muyeon.app.ui.jobposting

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
 *   ⚠️ 긴급 대타 수락·지원 취소·지원자 보기(작성자) 같은 흐름은 아직 웹 상세 — 메뉴 '웹에서 보기'로 연다.
 */
@Composable
fun PostingViewScreen(
    kind: String,
    id: Int,
    liked: Boolean,
    onToggleLike: () -> Unit,
    onBack: () -> Unit,
    onLogin: () -> Unit,
    onOpenWeb: (String) -> Unit,
    onWriteResume: () -> Unit,
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
    val loggedIn = !token.isNullOrEmpty()
    val webPath = "/${PostingKind.seg(kind)}/$id"

    LaunchedEffect(kind, id) {
        api.loadPosting(kind, id).onSuccess { detail = it }.onFailure { error = it.message ?: context.getString(R.string.posting_load_failed) }
        if (loggedIn) {
            applied = api.applied(kind, id)
            if (kind == "JOB") api.recordView(id)
        }
    }

    Column(Modifier.fillMaxSize().background(AppColors.Surface)) {
        MainTopBar(stringResource(if (kind == "SUB") R.string.tab_subs else R.string.recruit_job_detail), primary = false, onBack = onBack) {
            Text(stringResource(R.string.posting_open_web), style = AppTypography.Body14SemiBold, color = AppColors.TextSub,
                modifier = Modifier.clickable { onOpenWeb(webPath) }.padding(horizontal = 12.dp, vertical = 8.dp))
        }
        val d = detail
        when {
            d == null && error == null -> Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) { CircularProgressIndicator(color = AppColors.Primary) }
            d == null -> Box(Modifier.weight(1f).fillMaxWidth().padding(20.dp), Alignment.Center) {
                Text(error.orEmpty(), style = AppTypography.Body14, color = AppColors.TextSub, textAlign = TextAlign.Center)
            }
            else -> {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    DetailHeader(kind, d, null, d.optString("status").ifEmpty { null })
                    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        HorizontalDivider(Modifier.padding(vertical = 6.dp), color = AppColors.Divider)
                        if (kind == "SUB") SubBody(d) else JobPostingDetailContent(JobForm.from(d), showPlaceholders = false)
                    }
                }
                val open = d.optString("status").let { it.isEmpty() || it == "OPEN" }
                Column(Modifier.fillMaxWidth().background(AppColors.Surface).navigationBarsPadding()) {
                    HorizontalDivider(color = AppColors.Divider)
                    Row(Modifier.padding(horizontal = 16.dp).padding(top = 8.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.size(52.dp).border(1.dp, AppColors.ChipBorder, RoundedCornerShape(12.dp)), Alignment.Center) {
                            HeartButton(liked, onToggleLike, size = 24.dp)
                        }
                        val label = when {
                            applied -> R.string.posting_applied
                            !open -> R.string.posting_closed
                            else -> R.string.posting_apply
                        }
                        JobButton(stringResource(label), filled = true, enabled = open && !applied && !busy, modifier = Modifier.weight(1f)) {
                            if (!loggedIn) { onLogin(); return@JobButton }
                            busy = true
                            scope.launch {
                                val mode = if (ActiveRole.current(context) == "DANCER") ResumeMode.DANCER else ResumeMode.TEACHER
                                resumes = ResumeApi(token).list(mode).getOrNull().orEmpty()
                                busy = false; picking = true
                            }
                        }
                    }
                }
            }
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
