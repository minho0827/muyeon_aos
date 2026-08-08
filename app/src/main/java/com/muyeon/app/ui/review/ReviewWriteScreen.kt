package com.muyeon.app.ui.review

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors
import com.muyeon.app.ui.quote.QuoteAvatar
import com.muyeon.app.ui.quote.QuoteDialog
import com.muyeon.app.ui.quote.QuoteNavBar
import com.muyeon.app.ui.quote.QuoteUi
import com.muyeon.app.ui.resume.ResumeApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 리뷰 작성 — iOS `ReviewWriteView.swift` + `+Sections.swift` 1:1.
 *  헤더(강사·레슨일시) + 3축 별점 + 레슨형태 + 목적 + 수강기간 + 좋았던 점 태그 +
 *  사진 첨부(최대 20) + 본문 + 재수강 의향. 하단 CTA는 필수 미충족 시 안내 문구로 바뀐다.
 *  필수: 3축 별점 전부 + 레슨 형태 + 목적.
 */
@Composable
fun ReviewWriteScreen(
    api: ReviewApi,
    resumeApi: ResumeApi,
    teacherId: Int,
    teacherName: String,
    teacherImage: String?,
    lessonDateLine: String?,        // "2026.07.18 (토) · 오후 8:00"
    prefillLessonType: String?,
    onClose: () -> Unit,
    onDone: () -> Unit,
) {
    var ratings by remember { mutableStateOf(mapOf<String, Int>()) }
    var lessonType by remember { mutableStateOf(prefillLessonType) }
    var purpose by remember { mutableStateOf<String?>(null) }
    var durationBucket by remember { mutableStateOf<String?>(null) }
    var tags by remember { mutableStateOf(setOf<String>()) }
    var images by remember { mutableStateOf(listOf<String>()) }
    var content by remember { mutableStateOf("") }
    var revisit by remember { mutableStateOf<Boolean?>(null) }
    var uploading by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var loadedName by remember { mutableStateOf<String?>(null) }
    var loadedImage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 강사 이름/이미지 미전달 시 공개 프로필에서 보강
    LaunchedEffect(teacherId) {
        if (teacherName.isNotEmpty() && teacherImage != null) return@LaunchedEffect
        resumeApi.publicProfile(teacherId, src = "chat").getOrNull()?.let { p ->
            if (teacherName.isEmpty()) loadedName = p.name
            if (teacherImage == null) loadedImage = p.image
        }
    }

    val displayName = teacherName.ifEmpty { loadedName ?: "강사" }
    val displayImage = teacherImage ?: loadedImage

    val allAxesRated = ReviewOptions.ratingAxes.all { ratings[it.first] != null }
    val canSubmit = allAxesRated && lessonType != null && purpose != null
    val ctaText = when {
        !allAxesRated -> "별점을 매겨주세요"
        lessonType == null -> "레슨 형태를 선택해주세요"
        purpose == null -> "레슨 목적을 선택해주세요"
        else -> "리뷰 등록하기"
    }
    // 전체 별점 = 3축 평균(반올림)
    val overall = if (allAxesRated) {
        val vals = ReviewOptions.ratingAxes.mapNotNull { ratings[it.first] }
        Math.round(vals.sum().toDouble() / vals.size).toInt()
    } else 0

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = 20),
    ) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        uploading = true
        scope.launch {
            uris.forEach { uri ->
                if (images.size >= 20) return@forEach
                val bytes = withContext(Dispatchers.IO) {
                    runCatching { context.contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()
                }
                bytes?.let { api.uploadImage(it).getOrNull()?.let { url -> images = images + url } }
            }
            uploading = false
        }
    }

    Column(Modifier.fillMaxSize().background(MuyeonColors.surface)) {
        QuoteNavBar(title = "리뷰 작성", onClose = onClose)

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            // 헤더 — 강사 + 레슨 일시
            Row(
                Modifier.padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                QuoteAvatar(displayImage, displayName, 52.dp)
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        displayName,
                        fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 17.sp,
                        lineHeight = 20.sp, color = MuyeonColors.textHead,
                    )
                    lessonDateLine?.let {
                        Text(
                            it,
                            fontFamily = customFontFamily, fontSize = 13.sp, lineHeight = 16.sp,
                            color = MuyeonColors.textSub,
                        )
                    }
                }
            }

            // 3축 별점
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionTitle("별점을 매겨주세요", required = true)
                if (overall > 0) {
                    Text(
                        ReviewOptions.starLabels[overall - 1],
                        fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                        lineHeight = 18.sp, color = MuyeonColors.primary,
                    )
                }
                ReviewOptions.ratingAxes.forEach { (key, label) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            label,
                            fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp,
                            lineHeight = 17.sp, color = MuyeonColors.textHead, modifier = Modifier.width(84.dp),
                        )
                        StarRow(ratings[key] ?: 0) { ratings = ratings + (key to it) }
                    }
                }
            }

            ChipSection("레슨 형태", ReviewOptions.lessonTypes, lessonType, required = true) { lessonType = it }
            ChipSection("레슨 목적", ReviewOptions.purposes, purpose, required = true) { purpose = it }
            ChipSection("수강 기간", ReviewOptions.durations, durationBucket) { durationBucket = it }

            // 좋았던 점(다중)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle("이런 점이 좋았어요")
                FlowChips(ReviewOptions.tags, tags) { v ->
                    tags = if (tags.contains(v)) tags - v else tags + v
                }
            }

            // 사진 첨부
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle("사진 첨부 (${images.size}/20)")
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        Modifier.size(72.dp).clip(RoundedCornerShape(10.dp))
                            .border(1.dp, MuyeonColors.border, RoundedCornerShape(10.dp))
                            .clickable(enabled = !uploading && images.size < 20) {
                                picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.AddPhotoAlternate, "사진 추가", tint = MuyeonColors.secondary, modifier = Modifier.size(22.dp))
                    }
                    images.forEach { url ->
                        Box {
                            AsyncImage(
                                QuoteUi.imageUrl(url), null, contentScale = ContentScale.Crop,
                                modifier = Modifier.size(72.dp).clip(RoundedCornerShape(10.dp)),
                            )
                            Icon(
                                Icons.Filled.Close, "삭제", tint = Color.White,
                                modifier = Modifier.align(Alignment.TopEnd).padding(2.dp).size(16.dp)
                                    .clip(RoundedCornerShape(50)).background(Color.Black.copy(alpha = 0.5f))
                                    .clickable { images = images - url },
                            )
                        }
                    }
                }
            }

            // 본문
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle("어떤 점이 좋았는지 알려주세요")
                OutlinedTextField(
                    value = content, onValueChange = { content = it },
                    placeholder = {
                        Text(
                            "다른 회원에게 도움이 되는 후기를 남겨주세요.",
                            fontFamily = customFontFamily, fontSize = 14.sp,
                        )
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                )
            }

            // 재수강 의향
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle("이 강사에게 다시 배우고 싶나요?")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(true to "네, 추천해요", false to "아니요").forEach { (v, label) ->
                        val on = revisit == v
                        Text(
                            label,
                            fontFamily = customFontFamily,
                            fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 14.sp, lineHeight = 17.sp,
                            color = if (on) Color.White else MuyeonColors.textSub,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (on) MuyeonColors.primary else Color(0xFFF2F2F7))
                                .clickable { revisit = v }
                                .padding(vertical = 11.dp),
                        )
                    }
                }
            }

            Text(
                "작성한 리뷰는 공개되며, 허위·비방 내용은 운영정책에 따라 삭제될 수 있어요.",
                fontFamily = customFontFamily, fontSize = 12.sp, lineHeight = 17.sp,
                color = MuyeonColors.secondary,
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }

        Text(
            if (saving) "등록 중…" else ctaText,
            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp,
            lineHeight = 19.sp, color = Color.White, textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(horizontal = 20.dp).padding(top = 8.dp, bottom = 12.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(if (canSubmit && !saving) MuyeonColors.primary else Color.Gray.copy(alpha = 0.4f))
                .clickable(enabled = canSubmit && !saving) {
                    saving = true
                    scope.launch {
                        api.submit(
                            ReviewSubmit(
                                teacherId = teacherId, ratings = ratings, revisit = revisit,
                                content = content.trim().ifEmpty { null },
                                lessonType = lessonType, purpose = purpose, durationBucket = durationBucket,
                                tags = tags.toList(), images = images,
                            ),
                        ).onSuccess { onDone() }.onFailure { errorMessage = it.message }
                        saving = false
                    }
                }
                .padding(vertical = 16.dp),
        )
    }

    errorMessage?.let { msg ->
        QuoteDialog("오류", msg, "확인", onConfirm = { errorMessage = null }, onDismiss = { errorMessage = null })
    }
}

@Composable
private fun SectionTitle(text: String, required: Boolean = false) {
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            text,
            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp,
            lineHeight = 19.sp, color = MuyeonColors.textHead,
        )
        if (required) {
            Text("*", fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MuyeonColors.primary)
        }
    }
}

@Composable
private fun StarRow(value: Int, onPick: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        (1..5).forEach { i ->
            Icon(
                Icons.Filled.Star, "$i 점",
                tint = if (i <= value) MuyeonColors.primary else Color(0xFFE5E5EA),
                modifier = Modifier.size(28.dp).clickable { onPick(i) }.padding(2.dp),
            )
        }
    }
}

@Composable
private fun ChipSection(
    title: String,
    options: List<Pair<String, String>>,
    selected: String?,
    required: Boolean = false,
    onSelect: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle(title, required)
        FlowChips(options, selected?.let { setOf(it) } ?: emptySet(), onSelect)
    }
}

/** 줄바꿈 칩 — Compose FlowRow(실험 API) 대신 chunked Row 로 안정 구현. */
@Composable
private fun FlowChips(options: List<Pair<String, String>>, selected: Set<String>, onToggle: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (value, label) ->
                    val on = selected.contains(value)
                    Text(
                        label,
                        fontFamily = customFontFamily,
                        fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 13.sp, lineHeight = 16.sp,
                        color = if (on) Color.White else MuyeonColors.textSub,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                            .clip(RoundedCornerShape(50))
                            .background(if (on) MuyeonColors.primary else Color(0xFFF2F2F7))
                            .clickable { onToggle(value) }
                            .padding(vertical = 9.dp, horizontal = 4.dp),
                    )
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
