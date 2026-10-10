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
import androidx.compose.ui.text.style.TextAlign
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
            } ?: PreviewPlaceholder("대표 이미지가 표시됩니다", Modifier.fillMaxWidth().height(220.dp), square = true)
            Column(
                Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // 분야가 많으면 한 줄로 밀리므로 등록폼과 동일하게 줄바꿈시킨다.
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        (chips.ifEmpty { listOf("장르·모집 분야가 표시됩니다") }).chunked(3).forEach { row ->
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
                Text(
                    form.title.ifEmpty { "공고 제목" },
                    fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 22.sp,
                    lineHeight = 27.sp, color = MuyeonColors.textHead,
                )
                Text(
                        form.academy?.takeIf { it.isNotEmpty() } ?: "학원명이 표시됩니다",
                        fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp,
                        lineHeight = 17.sp, color = MuyeonColors.textSub,
                    )
                HorizontalDivider(Modifier.padding(vertical = 6.dp), color = MuyeonColors.border)
                JobPostingDetailContent(form, showPlaceholders = true)
            }
        }
    }
}

/**
 * 채용공고 본문(모집 정보 → 원하는 강사 조건 → 상세 설명 → 상세 이미지) — 미리보기와 내 공고 상세가 공유.
 *  ★ 2026-10-04 내 공고 관리 네이티브 상세(PostingDetailScreen)용으로 미리보기에서 떼어냈다.
 *   showPlaceholders = true(미리보기): 빈 값 자리에 '…가 표시됩니다' 안내를 그린다.
 *   showPlaceholders = false(상세): 빈 줄·빈 섹션은 아예 그리지 않는다(웹 상세 Field 와 동일).
 */
@Composable
internal fun JobPostingDetailContent(form: JobForm, showPlaceholders: Boolean) {
    // '-' = 상시 모집(작성자가 선택한 값), 값 없음(레거시 공고) = 마감일 미정. 웹 jobDeadlineText 와 같은 규칙.
    //  서버는 둘 다 무기한으로 본다. 작성 미리보기에서는 빈 값이면 입력 안내(placeholder)를 보여 준다.
    val deadlineText = when (val d = form.deadline?.trim()) {
        null, "" -> if (showPlaceholders) null else "마감일 미정"
        "-" -> "상시 모집"
        else -> d.take(10).replace("-", ".")
    }
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
    val description = form.description?.takeIf { it.isNotEmpty() }
    val images = form.images?.takeIf { it.isNotEmpty() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // 공고 상세(JobDetail)에 노출되는 항목과 1:1 — 빠짐 없이 전부 표시.
        PostingHead("모집 정보")
        PostingRow("장르", form.genre, showPlaceholders)
        PostingRow("모집 분야", (form.fields ?: emptyList()).joinToString(", ") { ResumeOptions.fieldLabel(it) }, showPlaceholders)
        PostingRow("근무 지역", listOfNotNull(form.region, form.address).filter { it.isNotEmpty() }.joinToString(" "), showPlaceholders)
        PostingRow("가까운 지하철역", form.subway, showPlaceholders)
        PostingRow("근무 요일", form.days, showPlaceholders)
        PostingRow("근무 시간", form.time, showPlaceholders)
        PostingRow("고용 형태", JobFormOptions.employmentLabel(form.employment), showPlaceholders)
        PostingRow("수업 대상", ResumeOptions.classTargets.firstOrNull { it.first == form.target }?.second, showPlaceholders)
        PostingRow("모집 인원", form.headcount?.let { "${it}명" }, showPlaceholders)
        PostingRow("지원 마감일", deadlineText, showPlaceholders)
        PostingRow("지원 방법", form.applyMethod?.trim(), showPlaceholders)
        PostingRow("급여", salaryText, showPlaceholders)
        PostingRow("허용 경력", careerText, showPlaceholders)

        if (showPlaceholders || preferenceLines.isNotEmpty()) {
            HorizontalDivider(Modifier.padding(vertical = 6.dp), color = MuyeonColors.border)
            PostingHead("원하는 강사 조건")
            if (preferenceLines.isEmpty()) {
                PreviewPlaceholder("선택한 우대 조건이 표시됩니다")
            } else {
                PostingBody(preferenceLines.joinToString("\n"))
            }
        }
        if (showPlaceholders || description != null) {
            HorizontalDivider(Modifier.padding(vertical = 6.dp), color = MuyeonColors.border)
            PostingHead("상세 설명")
            description?.let { PostingBody(it) } ?: PreviewPlaceholder("공고의 상세 설명이 표시됩니다")
        }
        // 실제 상세(웹)는 캐러셀, 앱은 세로 스택으로 전부 확인.
        if (showPlaceholders || images != null) {
            HorizontalDivider(Modifier.padding(vertical = 6.dp), color = MuyeonColors.border)
            PostingHead("상세 이미지")
            images?.let { PostingImageStack(it) }
                ?: PreviewPlaceholder("등록한 상세 이미지가 표시됩니다", Modifier.fillMaxWidth().height(160.dp))
        }
    }
}

/** 상세 이미지 세로 스택 — 채용·대타·공연 상세 공용. */
@Composable
internal fun PostingImageStack(images: List<String>) {
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

/** 라벨-값 한 줄. placeholders=false 면 값이 비었을 때 줄을 그리지 않는다. */
@Composable
internal fun PostingRow(label: String, value: String?, placeholders: Boolean = false) {
    if (value.isNullOrEmpty() && !placeholders) return
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            label,
            fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp,
            lineHeight = 16.sp, color = MuyeonColors.textSub, modifier = Modifier.width(74.dp),
        )
        if (value.isNullOrEmpty()) {
            PreviewPlaceholder("입력한 $label 정보가 표시됩니다", Modifier.weight(1f))
        } else {
            Text(
                value,
                fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp,
                lineHeight = 17.sp, color = MuyeonColors.textHead, modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun PreviewPlaceholder(
    text: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    square: Boolean = false,
) = Box(
    modifier.clip(if (square) RoundedCornerShape(0.dp) else RoundedCornerShape(8.dp))
        .background(MuyeonColors.tileIdle).padding(horizontal = 12.dp, vertical = 12.dp),
    contentAlignment = Alignment.Center,
) {
    Text(
        text,
        fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp,
        lineHeight = 17.sp, color = MuyeonColors.textSub, textAlign = TextAlign.Center,
    )
}

@Composable
internal fun PostingHead(text: String) = Text(
    text,
    fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp,
    lineHeight = 18.sp, color = MuyeonColors.textHead,
)

@Composable
internal fun PostingBody(text: String) = Text(
    text,
    fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp,
    lineHeight = 22.sp, color = MuyeonColors.body,
)
