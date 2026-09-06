package com.muyeon.app.ui.resume

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors
import com.muyeon.app.ui.quote.QuoteAvatar
import com.muyeon.app.ui.quote.QuoteDialog
import com.muyeon.app.ui.quote.QuoteNavBar
import com.muyeon.app.ui.quote.QuoteUi
import kotlinx.coroutines.launch

/**
 * 지원자 이력서 열람(원장) — iOS `ApplicantResumeView.swift` 이식.
 *  지원 메타 + 이력서 원본(마스크 없음) + 합불 처리 + 1:1 채팅.
 *
 * ⚠️ 표시 데이터는 **공개 프로필 위에 첨부 이력서를 덮어 병합**한 것(웹과 동일 규칙, Applicant.mergedData).
 *   ★ 연락처·이메일은 노출하지 않는다. 공개 프로필에서만 서버 허용 시 보여준다(iOS 와 동일).
 */
@Composable
fun ApplicantResumeScreen(
    api: ResumeApi,
    jobId: Int,
    applicationId: Int,
    onClose: () -> Unit,
    onOpenChat: (Int) -> Unit,
) {
    var applicant by remember { mutableStateOf<Applicant?>(null) }
    var loading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf<String?>(null) }   // "ACCEPTED" | "REJECTED"
    var toast by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun load() {
        api.applicant(jobId, applicationId).onSuccess { applicant = it }
        loading = false
    }

    LaunchedEffect(jobId, applicationId) { load() }

    Column(Modifier.fillMaxSize().background(MuyeonColors.surface)) {
        QuoteNavBar(title = "지원자 이력서", onBack = onClose)

        if (loading) {
            Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                CircularProgressIndicator(color = MuyeonColors.primary)
            }
            return@Column
        }
        val a = applicant
        if (a == null) {
            Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                Text("지원 정보를 불러오지 못했어요.", fontFamily = customFontFamily, fontSize = 14.sp, color = MuyeonColors.textSub)
            }
            return@Column
        }

        val d = a.mergedData

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            // 지원 메타
            Row(
                Modifier.padding(top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                QuoteAvatar(d.image ?: d.basic?.photo, a.applicantName ?: "지원자", 52.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        a.applicantName ?: d.basic?.name ?: "지원자",
                        fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 17.sp,
                        lineHeight = 20.sp, color = MuyeonColors.textHead,
                    )
                    Text(
                        listOfNotNull(
                            applicantStatusLabel(a.status),
                            QuoteUi.relativeTime(a.appliedAt).ifEmpty { null },
                        ).joinToString(" · "),
                        fontFamily = customFontFamily, fontSize = 13.sp, lineHeight = 16.sp, color = MuyeonColors.textSub,
                    )
                }
            }

            // ★ 지원자 이력서에는 연락처·이메일을 노출하지 않는다(iOS fa61d9a 와 동일).
            //   연락처는 공개 프로필에서만, 서버가 허용한 경우(contactVisible/contactPhone)에 보여준다.
            //   종전에는 phoneUnlocked 로 분기했는데 백엔드에 그런 필드가 없어 안내문만 뜨고 있었다.

            a.resumeTitle?.takeIf { it.isNotEmpty() }?.let {
                Text(
                    "첨부 이력서 · $it",
                    fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                    lineHeight = 16.sp, color = MuyeonColors.primary,
                    modifier = Modifier.padding(top = 14.dp),
                )
            }

            ApplicantSection("한 줄 소개", d.oneLiner)
            ApplicantSection("소개글", d.intro)
            ApplicantChips("전공·장르", (d.fields ?: d.genres)?.map { ResumeOptions.fieldLabel(it) })
            ApplicantSection("활동 지역", d.activeRegion)
            ApplicantSection("희망 직종", d.desired?.job)
            ApplicantSection("희망 지역", d.desired?.region ?: d.desiredRegion)
            ApplicantSection("희망 급여", ResumeOptions.salaryLabel(d.desired?.salary).ifEmpty { null })
            ApplicantSection("경력", ResumeOptions.careerLabel(d.career).ifEmpty { null })

            d.careers?.takeIf { it.isNotEmpty() }?.let { cs ->
                ApplicantLines("경력 상세", cs.map { c ->
                    listOf(c.academy, c.position, c.classes, c.period).filter { it.isNotEmpty() }.joinToString(" · ")
                })
            }
            d.seededEducations().takeIf { it.isNotEmpty() }?.let { edus ->
                ApplicantLines("학력", edus.map { e ->
                    listOf(e.school, e.major, e.period).filter { it.isNotEmpty() }.joinToString(" · ")
                })
            }
            d.certificates?.takeIf { it.isNotEmpty() }?.let {
                ApplicantLines("자격증", it.split("\n").map { s -> s.trim() }.filter { s -> s.isNotEmpty() })
            }
            d.performances?.takeIf { it.isNotEmpty() }?.let { ps ->
                ApplicantLines("공연 이력", ps.map { p ->
                    listOf(p.year, p.title, p.role).filter { it.isNotEmpty() }.joinToString(" · ")
                })
            }
            d.awards?.takeIf { it.isNotEmpty() }?.let {
                ApplicantLines("수상 이력", it.split("\n").map { s -> s.trim() }.filter { s -> s.isNotEmpty() })
            }

            Spacer(Modifier.height(20.dp))
        }

        // 합불 + 채팅
        Column(Modifier.fillMaxWidth().background(MuyeonColors.surface)) {
            HorizontalDivider(color = MuyeonColors.border)
            Row(
                Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ActionButton("1:1 채팅", filled = false, enabled = !busy, modifier = Modifier.weight(1f)) {
                    busy = true
                    scope.launch {
                        api.directRoom(a.applicantId)
                            .onSuccess { rid -> if (rid > 0) onOpenChat(rid) else toast = "채팅방을 여는 데 실패했어요." }
                            .onFailure { toast = it.message }
                        busy = false
                    }
                }
                if (a.status == "APPLIED" || a.status == null) {
                    ActionButton("불합격", filled = false, enabled = !busy, modifier = Modifier.weight(1f)) { confirm = "REJECTED" }
                    ActionButton("합격", filled = true, enabled = !busy, modifier = Modifier.weight(1f)) { confirm = "ACCEPTED" }
                }
            }
        }
    }

    confirm?.let { status ->
        val accept = status == "ACCEPTED"
        QuoteDialog(
            title = if (accept) "합격 처리할까요?" else "불합격 처리할까요?",
            message = if (accept) "지원자에게 합격 알림이 전송돼요." else "지원자에게 결과가 전달돼요. 되돌릴 수 없어요.",
            confirmText = if (accept) "합격" else "불합격",
            onConfirm = {
                confirm = null
                busy = true
                scope.launch {
                    api.decide(applicationId, status)
                        .onSuccess { load(); toast = if (accept) "합격 처리했어요." else "불합격 처리했어요." }
                        .onFailure { toast = it.message }
                    busy = false
                }
            },
            onDismiss = { confirm = null },
        )
    }
    toast?.let { msg ->
        QuoteDialog("알림", msg, "확인", onConfirm = { toast = null }, onDismiss = { toast = null })
    }
}

private fun applicantStatusLabel(s: String?): String = when (s) {
    "ACCEPTED" -> "합격"
    "REJECTED" -> "불합격"
    "APPLIED", null -> "지원 완료"
    else -> s
}

@Composable
private fun ApplicantSection(title: String, value: String?) {
    if (value.isNullOrEmpty()) return
    Column(Modifier.padding(top = 18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 18.sp, color = MuyeonColors.textHead)
        Text(value, fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, color = MuyeonColors.body)
    }
}

@Composable
private fun ApplicantLines(title: String, lines: List<String>) {
    if (lines.isEmpty()) return
    Column(Modifier.padding(top = 18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 18.sp, color = MuyeonColors.textHead)
        lines.forEach {
            Text("· $it", fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, color = MuyeonColors.body)
        }
    }
}

@Composable
private fun ApplicantChips(title: String, values: List<String>?) {
    if (values.isNullOrEmpty()) return
    Column(Modifier.padding(top = 18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 18.sp, color = MuyeonColors.textHead)
        values.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach {
                    Text(
                        it,
                        fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 12.sp,
                        lineHeight = 15.sp, color = MuyeonColors.primary, maxLines = 1,
                        modifier = Modifier.clip(RoundedCornerShape(50))
                            .background(MuyeonColors.primary.copy(alpha = 0.10f))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoBox(title: String, value: String, muted: Boolean = false) {
    Column(
        Modifier.padding(top = 14.dp).fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)).background(Color(0xFFF7F7F7)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(title, fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 16.sp, color = MuyeonColors.textSub)
        Text(
            value,
            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 18.sp,
            color = if (muted) MuyeonColors.secondary else MuyeonColors.textHead,
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
        fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 17.sp,
        color = if (filled) Color.White else MuyeonColors.primary, textAlign = TextAlign.Center,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (filled) MuyeonColors.primary else MuyeonColors.primary.copy(alpha = 0.08f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 13.dp),
    )
}
