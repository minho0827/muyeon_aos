package com.muyeon.app.ui.jobposting

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors
import com.muyeon.app.ui.quote.QuoteUi
import com.muyeon.app.ui.resume.ResumeOptions

/**
 * 공고 미리보기 — iOS `JobPostingPreviewView.swift` 1:1.
 *  등록 전 실제 노출 화면을 강사 시점으로 렌더(읽기 전용).
 */
@Composable
fun JobPostingPreviewScreen(form: JobForm, onClose: () -> Unit) {
    // 대표 이미지가 없으면 상세 이미지 첫 장(웹 ImageCarousel 과 동일 폴백)
    val hero = form.imageUrl?.takeIf { it.isNotEmpty() } ?: form.images?.firstOrNull()
    val chips = buildList {
        form.genre?.takeIf { it.isNotEmpty() }?.let { add(it) }
        addAll((form.fields ?: emptyList()).map { ResumeOptions.fieldLabel(it) })
    }
    val deadlineText = form.deadline?.takeIf { it.isNotEmpty() && it != "-" }
        ?.take(10)?.replace("-", ".") ?: "미정"
    // 급여 구간 + 부가설명(pay) — 상세와 동일하게 "3만~4만원 (경력별 협의)" 형태
    val salaryText = run {
        val range = JobFormOptions.salaryLabel(form.salary)
        val note = form.pay.orEmpty()
        when {
            range.isNotEmpty() && note.isNotEmpty() -> "$range ($note)"
            range.isEmpty() -> note
            else -> range
        }
    }
    val careerText = listOf(JobFormOptions.careerLevelsLabel(form.careerLevels), form.careerText.orEmpty())
        .filter { it.isNotEmpty() }.joinToString(" · ")
    // 원하는 강사 조건 — 상세의 prefList 와 동일 구성
    val preferenceLines = buildList {
        val p = form.pref
        if (p.artHigh == true) add("예고 출신 우대")
        if (p.university == true) {
            val name = p.universityName?.takeIf { it.isNotEmpty() }?.let { " ($it)" }.orEmpty()
            add("대학 졸업 우대$name")
        }
        if (p.company == true) add("무용단 출신 우대")
        if (p.certRequired == true) add("자격증 필수")
        if (p.videoRequired == true) add("영상 포트폴리오 필수")
        p.note?.takeIf { it.isNotEmpty() }?.let { add(it) }
    }

    Column(Modifier.fillMaxSize().background(MuyeonColors.surface)) {
        Box(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "미리보기",
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp,
                lineHeight = 22.sp, color = MuyeonColors.textHead,
            )
            Icon(
                Icons.Filled.Close, "닫기", tint = MuyeonColors.body,
                modifier = Modifier.align(Alignment.CenterEnd).size(18.dp).clickable(onClick = onClose),
            )
        }

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            hero?.let {
                AsyncImage(
                    QuoteUi.imageUrl(it), null, contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(220.dp).background(Color(0xFFF7F7F7)),
                )
            }
            Column(
                Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // 분야가 많으면 한 줄로 밀리므로 등록폼과 동일하게 줄바꿈시킨다.
                if (chips.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        chips.chunked(3).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                row.forEach { label ->
                                    Text(
                                        label,
                                        fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp, lineHeight = 15.sp, color = MuyeonColors.primary,
                                        maxLines = 1,
                                        modifier = Modifier.clip(RoundedCornerShape(50))
                                            .background(MuyeonColors.primary.copy(alpha = 0.10f))
                                            .padding(horizontal = 10.dp, vertical = 5.dp),
                                    )
                                }
                            }
                        }
                    }
                }
                Text(
                    form.title.ifEmpty { "공고 제목" },
                    fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 22.sp,
                    lineHeight = 27.sp, color = MuyeonColors.textHead,
                )
                form.academy?.takeIf { it.isNotEmpty() }?.let {
                    Text(
                        it,
                        fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp,
                        lineHeight = 17.sp, color = MuyeonColors.textSub,
                    )
                }
                HorizontalDivider(Modifier.padding(vertical = 6.dp), color = MuyeonColors.border)

                // 공고 상세(JobDetail)에 노출되는 항목과 1:1 — 빠짐 없이 전부 표시.
                PreviewRow("장르", form.genre)
                PreviewRow("모집 분야", (form.fields ?: emptyList()).joinToString(", ") { ResumeOptions.fieldLabel(it) })
                PreviewRow("근무 지역", listOfNotNull(form.region, form.address).filter { it.isNotEmpty() }.joinToString(" "))
                PreviewRow("가까운 지하철역", form.subway)
                PreviewRow("근무 요일", form.days)
                PreviewRow("근무 시간", form.time)
                PreviewRow("고용 형태", JobFormOptions.employmentLabel(form.employment))
                PreviewRow("수업 대상", ResumeOptions.classTargets.firstOrNull { it.first == form.target }?.second)
                PreviewRow("모집 인원", form.headcount?.let { "${it}명" })
                PreviewRow("지원 마감일", deadlineText)
                PreviewRow("급여", salaryText)
                PreviewRow("허용 경력", careerText)

                if (preferenceLines.isNotEmpty()) {
                    HorizontalDivider(Modifier.padding(vertical = 6.dp), color = MuyeonColors.border)
                    PreviewHead("원하는 강사 조건")
                    PreviewBody(preferenceLines.joinToString("\n"))
                }
                form.description?.takeIf { it.isNotEmpty() }?.let {
                    HorizontalDivider(Modifier.padding(vertical = 6.dp), color = MuyeonColors.border)
                    PreviewHead("상세 설명")
                    PreviewBody(it)
                }
                // 실제 상세는 캐러셀, 미리보기는 세로 스택으로 전부 확인.
                form.images?.takeIf { it.isNotEmpty() }?.let { images ->
                    HorizontalDivider(Modifier.padding(vertical = 6.dp), color = MuyeonColors.border)
                    PreviewHead("상세 이미지")
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        images.forEach { img ->
                            AsyncImage(
                                QuoteUi.imageUrl(img), null, contentScale = ContentScale.FillWidth,
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFF7F7F7)),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewRow(label: String, value: String?) {
    if (value.isNullOrEmpty()) return
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(
            label,
            fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp,
            lineHeight = 16.sp, color = MuyeonColors.textSub, modifier = Modifier.width(74.dp),
        )
        Text(
            value,
            fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp,
            lineHeight = 17.sp, color = MuyeonColors.textHead,
        )
    }
}

@Composable
private fun PreviewHead(text: String) = Text(
    text,
    fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp,
    lineHeight = 18.sp, color = MuyeonColors.textHead,
)

@Composable
private fun PreviewBody(text: String) = Text(
    text,
    fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp,
    lineHeight = 22.sp, color = MuyeonColors.body,
)
