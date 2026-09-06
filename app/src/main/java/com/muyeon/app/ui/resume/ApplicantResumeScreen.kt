package com.muyeon.app.ui.resume

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors
import com.muyeon.app.ui.quote.QuoteDialog
import com.muyeon.app.ui.quote.QuoteNavBar
import com.muyeon.app.ui.quote.QuoteUi
import com.muyeon.app.ui.review.ReviewApi
import kotlinx.coroutines.launch

/**
 * 지원자 이력서 열람(원장) — iOS `ApplicantResumeView.swift` 1:1.
 *  지원 메타 + 이력서 원본(마스크 없음) + [채팅하기]/[확정] + 확정 후 일괄 안내 유도.
 *
 * ⚠️ 표시 데이터는 **공개 프로필 위에 첨부 이력서를 덮어 병합**한 것(웹과 동일 규칙, Applicant.mergedData).
 *   ★ 연락처·이메일은 노출하지 않는다. 공개 프로필에서만 서버 허용 시 보여준다(iOS 와 동일).
 *   ★ 지원자가 프로필 노출 멤버십 보유(profileMembershipActive)면 공개 프로필 화면을 그대로 끼워 넣는다.
 */
@Composable
fun ApplicantResumeScreen(
    api: ResumeApi,
    reviewApi: ReviewApi,
    postingId: Int,
    applicationId: Int,
    kind: ApplicantPostingKind,
    onClose: () -> Unit,
    onOpenChat: (Int, String) -> Unit,   // (roomId, 상대 이름)
) {
    var applicant by remember { mutableStateOf<Applicant?>(null) }
    var loading by remember { mutableStateOf(true) }
    var deciding by remember { mutableStateOf(false) }
    var confirmedInCurrentView by remember { mutableStateOf(false) }
    var showConfirmApplicant by remember { mutableStateOf(false) }
    var decisionPrompt by remember { mutableStateOf<ApplicantDecisionPrompt?>(null) }
    var infoMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(postingId, applicationId, kind) {
        api.applicant(postingId, applicationId, kind)
            .onSuccess { applicant = it }
            .onFailure { errorMessage = it.message }
        loading = false
    }

    fun applicantName(): String =
        applicant?.applicantName ?: applicant?.mergedData?.basic?.name ?: "지원자"

    fun confirmApplicant() {
        if (deciding) return
        deciding = true
        scope.launch {
            api.decide(applicationId, "ACCEPTED", kind)
                .onSuccess { remaining ->
                    applicant = applicant?.copy(status = "ACCEPTED")
                    confirmedInCurrentView = true
                    decisionPrompt = when {
                        remaining <= 0 -> ApplicantDecisionPrompt.Confirmed(
                            if (kind == ApplicantPostingKind.SUB) "대타가 확정되었습니다." else "채용이 확정되었습니다.",
                        )
                        kind == ApplicantPostingKind.SUB -> ApplicantDecisionPrompt.SubConfirmed(remaining)
                        else -> ApplicantDecisionPrompt.JobConfirmed(applicantName(), remaining)
                    }
                }
                .onFailure { errorMessage = it.message }
            deciding = false
        }
    }

    fun finalizePosting() {
        if (deciding) return
        deciding = true
        scope.launch {
            api.finalizePosting(postingId, kind)
                .onSuccess { rejected ->
                    decisionPrompt = null
                    infoMessage = "남은 지원자 ${rejected}명에게 미선정 결과를 안내했어요."
                }
                .onFailure { errorMessage = it.message }
            deciding = false
        }
    }

    Column(Modifier.fillMaxSize().background(Color(0xFFF7F7F7))) {
        // 내비 — 확정한 뒤에는 우측에 확정 상태를 남긴다(iOS confirmedInCurrentView).
        QuoteNavBar(
            title = "지원자 이력서",
            onBack = onClose,
            trailing = if (!confirmedInCurrentView) null else {
                {
                    Text(
                        kind.confirmTitle,
                        fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                        lineHeight = 16.sp, color = MuyeonColors.textSub,
                        modifier = Modifier.padding(end = 16.dp),
                    )
                }
            },
        )

        if (loading) {
            Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                CircularProgressIndicator(color = MuyeonColors.primary)
            }
            return@Column
        }
        val a = applicant
        if (a == null) {
            Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                Text(
                    "지원자 정보를 불러오지 못했어요",
                    fontFamily = customFontFamily, fontSize = 14.sp, color = MuyeonColors.textSub,
                )
            }
            return@Column
        }

        if (a.profileMembershipActive == true) {
            // 프로필 노출 이용권 보유 — 공개 프로필 그대로(채용 시점, 자체 뒤로가기·CTA 없음)
            Box(Modifier.weight(1f)) {
                PublicProfileScreen(
                    api = api, reviewApi = reviewApi, userId = a.applicantId,
                    src = "application", recruitMode = true, embedded = true, hideCta = true,
                    initialProfile = PublicProfile.from(a),
                    onClose = onClose, onOpenChat = {}, onRequestQuote = {}, onOpenReviews = {},
                )
            }
        } else {
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            ) {
                ResumeBody(a)
            }
        }

        BottomBar(
            confirmTitle = kind.confirmTitle,
            enabled = !deciding,
            onChat = {
                deciding = true
                scope.launch {
                    api.directRoom(a.applicantId, kind.raw, applicationId)
                        .onSuccess { rid ->
                            applicant = applicant?.copy(status = "OFFERED")
                            if (rid > 0) onOpenChat(rid, a.applicantName ?: "지원자")
                            else errorMessage = "채팅방을 여는 데 실패했어요."
                        }
                        .onFailure { errorMessage = it.message }
                    deciding = false
                }
            },
            onConfirm = { showConfirmApplicant = true },
        )
    }

    if (showConfirmApplicant) {
        val name = applicantName()
        QuoteDialogTwo(
            title = if (kind == ApplicantPostingKind.SUB) "${name}님을 대타로 확정하시겠습니까?"
            else "${name}님을 채용하시겠습니까?",
            confirmText = kind.confirmTitle,
            dismissText = "계속 검토하기",
            onConfirm = { showConfirmApplicant = false; confirmApplicant() },
            onDismiss = { showConfirmApplicant = false },
        )
    }
    decisionPrompt?.let { p ->
        ApplicantDecisionPromptDialog(
            prompt = p,
            onPrompt = { decisionPrompt = it },
            onBulkNotify = { finalizePosting() },
        )
    }
    infoMessage?.let { msg ->
        QuoteDialog("안내", msg, "확인", onConfirm = { infoMessage = null }, onDismiss = { infoMessage = null })
    }
    errorMessage?.let { msg ->
        QuoteDialog("오류", msg, "확인", onConfirm = { errorMessage = null }, onDismiss = { errorMessage = null })
    }
}

/** 이력서 본문 카드 — iOS `ApplicantResumeView.resumeBody` 1:1(흰 카드 + 12dp 라운드). */
@Composable
private fun ResumeBody(a: Applicant) {
    val d = a.mergedData
    Column(
        Modifier.padding(vertical = 16.dp).fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)).background(MuyeonColors.surface)
            .padding(horizontal = 16.dp),
    ) {
        // 기본 정보
        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            QuoteUi.imageUrl(d.image ?: d.basic?.photo)?.let {
                AsyncImage(
                    it, null, contentScale = ContentScale.Crop,
                    modifier = Modifier.size(88.dp).clip(RoundedCornerShape(12.dp)),
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    a.applicantName ?: d.basic?.name ?: "지원자",
                    fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp,
                    lineHeight = 24.sp, color = MuyeonColors.textHead,
                )
                Kv("생년월일", d.basic?.birth)
            }
        }

        d.oneLiner?.takeIf { it.isNotEmpty() }?.let {
            Text(
                it,
                fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp,
                lineHeight = 20.sp, color = MuyeonColors.textSub,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
        d.genres?.takeIf { it.isNotEmpty() }?.let { gs ->
            Section("전공") { Plain(gs.joinToString(", ") { ResumeOptions.fieldLabel(it) }) }
        }
        d.fields?.takeIf { it.isNotEmpty() }?.let { fs ->
            Section("지도 분야") { Plain(fs.joinToString(", ") { ResumeOptions.fieldLabel(it) }) }
        }
        d.activeRegion?.takeIf { it.isNotEmpty() }?.let { Section("활동 지역") { Plain(it) } }
        d.seededEducations().takeIf { it.isNotEmpty() }?.let { edus ->
            Section("학력") {
                Lines(edus.map { listOf(it.school, it.period).filter { s -> s.isNotEmpty() }.joinToString(" · ") })
            }
        }
        d.careers?.takeIf { it.isNotEmpty() }?.let { cs ->
            val bucket = d.career?.let { " (${ResumeOptions.careerLabel(it)})" }.orEmpty()
            Section("경력$bucket") {
                Lines(
                    cs.map { c ->
                        listOf(
                            listOf(c.academy, c.position).filter { it.isNotEmpty() }.joinToString(" "),
                            c.period,
                        ).filter { it.isNotEmpty() }.joinToString(" · ")
                    },
                )
            }
        }
        d.performances?.takeIf { it.isNotEmpty() }?.let { ps ->
            Section("공연 이력") {
                Lines(ps.map { listOf("${it.year} ${it.title}".trim(), it.role).filter { s -> s.isNotEmpty() }.joinToString(" — ") })
            }
        }
        d.certificates?.takeIf { it.isNotEmpty() }?.let {
            Section("자격증") { Lines(it.split("\n")) }
        }
        d.awards?.takeIf { it.isNotEmpty() }?.let {
            Section("수상 이력") { Lines(it.split("\n")) }
        }
        d.desired?.takeIf { ds -> listOf(ds.job, ds.region, ds.salary).any { !it.isNullOrEmpty() } }?.let { ds ->
            Section("희망 근무 조건") {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Kv("희망 직종", ds.job)
                    Kv("희망 지역", ds.region)
                    Kv("희망 급여", ds.salary?.let { ResumeOptions.salaryLabel(it) })
                }
            }
        }
        d.intro?.takeIf { it.isNotEmpty() }?.let { Section("자기소개") { Plain(it) } }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun BottomBar(
    confirmTitle: String,
    enabled: Boolean,
    onChat: () -> Unit,
    onConfirm: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().background(MuyeonColors.surface)) {
        HorizontalDivider(color = MuyeonColors.border)
        Row(
            Modifier.padding(horizontal = 16.dp).padding(top = 8.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ActionButton("채팅하기", filled = false, enabled = enabled, modifier = Modifier.weight(1f), onClick = onChat)
            ActionButton(confirmTitle, filled = true, enabled = enabled, modifier = Modifier.weight(1f), onClick = onConfirm)
        }
    }
}

/** 확인/취소 문구를 모두 지정하는 다이얼로그 — QuoteDialog 는 취소 문구가 고정이라 별도로 둔다. */
@Composable
private fun QuoteDialogTwo(
    title: String,
    confirmText: String,
    dismissText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { com.muyeon.app.ui.quote.DialogTitle(title) },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onConfirm) {
                com.muyeon.app.ui.quote.DialogAction(confirmText)
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                com.muyeon.app.ui.quote.DialogAction(dismissText)
            }
        },
    )
}

// MARK: 컴포넌트 — iOS section/plain/list/kv 대응

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HorizontalDivider(color = Color(0xFFF7F7F7), modifier = Modifier.padding(vertical = 12.dp))
        Text(
            title,
            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp,
            lineHeight = 18.sp, color = MuyeonColors.textHead,
        )
        content()
    }
}

@Composable
private fun Plain(text: String) = Text(
    text,
    fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp,
    lineHeight = 20.sp, color = MuyeonColors.body,
)

@Composable
private fun Lines(lines: List<String>) {
    val items = lines.map { it.trim() }.filter { it.isNotEmpty() }
    if (items.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) { items.forEach { Plain(it) } }
}

@Composable
private fun Kv(key: String, value: String?) {
    if (value.isNullOrEmpty()) return
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            key,
            fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp,
            lineHeight = 16.sp, color = MuyeonColors.textSub,
        )
        Text(
            value,
            fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp,
            lineHeight = 16.sp, color = MuyeonColors.textHead,
        )
    }
}

@Composable
private fun ActionButton(
    text: String,
    filled: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Text(
        text,
        fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 18.sp,
        color = if (filled) Color.White else MuyeonColors.textHead, textAlign = TextAlign.Center,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (filled) MuyeonColors.primary else MuyeonColors.surface)
            // 아웃라인 버튼 테두리 — iOS `.overlay(RoundedRectangle.stroke(EAEAEA))`
            .then(if (filled) Modifier else Modifier.border(1.dp, MuyeonColors.border, RoundedCornerShape(12.dp)))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 14.dp),
    )
}
