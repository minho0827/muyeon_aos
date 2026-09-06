package com.muyeon.app.ui.review

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors
import com.muyeon.app.ui.quote.QuoteAvatar
import com.muyeon.app.ui.quote.QuoteNavBar
import com.muyeon.app.ui.quote.QuoteUi
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * 리뷰 상세(숨고식) — iOS `Content/ReviewDetailView.swift` 1:1.
 *  작성자·별점·사진·본문·좋았던점 태그 + 고객이 받은 서비스 정보 + 강사 카드 +
 *  하단 '같은 조건으로 견적 요청하기'.
 *
 * ⚠️ 종전 AOS 는 둘러보기에서 리뷰 카드를 탭하면 곧장 강사 프로필로 보냈다.
 *   무슨 후기를 보고 눌렀는지 확인할 화면이 없어 '같은 조건으로 견적' 도 못 냈다.
 */
@Composable
fun ReviewDetailScreen(
    api: ReviewApi,
    reviewId: Int,
    onClose: () -> Unit,
    onSelectTeacher: (Int) -> Unit,
    onRequestQuote: (LessonQuotePrefill) -> Unit,
) {
    var review by remember { mutableStateOf<ReviewDetail?>(null) }
    var loading by remember { mutableStateOf(true) }
    var expanded by remember { mutableStateOf(false) }
    var requesting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(reviewId) {
        api.detail(reviewId).onSuccess { review = it }
        loading = false
    }

    Column(Modifier.fillMaxSize().background(MuyeonColors.surface)) {
        QuoteNavBar(title = "후기", onBack = onClose)

        val r = review
        when {
            loading -> Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                CircularProgressIndicator(color = MuyeonColors.primary)
            }
            r == null -> Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                Text(
                    "후기를 찾을 수 없어요",
                    fontFamily = customFontFamily, fontSize = 14.sp, color = MuyeonColors.textSub,
                )
            }
            else -> {
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    // 작성자 + 별점
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                r.reviewerName ?: "회원",
                                fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                                lineHeight = 18.sp, color = MuyeonColors.textHead, modifier = Modifier.weight(1f),
                            )
                            r.createdAt?.let {
                                Text(
                                    QuoteUi.relativeTime(it),
                                    fontFamily = customFontFamily, fontSize = 13.sp, lineHeight = 16.sp,
                                    color = MuyeonColors.textSub,
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            (1..5).forEach { i ->
                                Icon(
                                    if (i <= r.rating) Icons.Filled.Star else Icons.Outlined.StarOutline, null,
                                    tint = if (i <= r.rating) MuyeonColors.yellow else MuyeonColors.chevron,
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                        }
                    }

                    r.images?.takeIf { it.isNotEmpty() }?.let { imgs ->
                        Row(
                            Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            imgs.forEach { s ->
                                AsyncImage(
                                    QuoteUi.imageUrl(s), null, contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(width = 240.dp, height = 300.dp)
                                        .clip(RoundedCornerShape(12.dp)).background(MuyeonColors.groupedBg),
                                )
                            }
                        }
                    }

                    r.content?.takeIf { it.isNotEmpty() }?.let { c ->
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                c,
                                fontFamily = customFontFamily, fontSize = 15.sp, lineHeight = 23.sp,
                                color = MuyeonColors.textHead,
                                maxLines = if (expanded) Int.MAX_VALUE else 6,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (!expanded) {
                                Row(
                                    Modifier.clickable { expanded = true },
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        "더보기",
                                        fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp,
                                        lineHeight = 17.sp, color = MuyeonColors.textSub,
                                    )
                                    Icon(
                                        Icons.Filled.ExpandMore, null, tint = MuyeonColors.textSub,
                                        modifier = Modifier.size(13.dp),
                                    )
                                }
                            }
                        }
                    }

                    r.tags?.takeIf { it.isNotEmpty() }?.let { tags ->
                        // iOS 는 adaptive grid — 폭이 좁으면 줄바꿈되게 3개씩 끊는다.
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            tags.chunked(3).forEach { row ->
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    row.forEach { t ->
                                        Text(
                                            ReviewOptions.tagLabel(t),
                                            fontFamily = customFontFamily, fontSize = 13.sp, lineHeight = 16.sp,
                                            color = MuyeonColors.textHead,
                                            modifier = Modifier.clip(RoundedCornerShape(50))
                                                .background(MuyeonColors.groupedBg)
                                                .padding(horizontal = 12.dp, vertical = 7.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MuyeonColors.border)

                    // 고객이 받은 서비스 정보
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "고객이 받은 서비스 정보",
                            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                            lineHeight = 19.sp, color = MuyeonColors.textHead,
                        )
                        Column(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                .background(MuyeonColors.groupedBg).padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            // purposes 는 옵션 id("adult"…)라 장르 카테고리로 라벨을 되돌린다.
                            //  안 하면 "성인 발레" 대신 "adult" 가 그대로 보인다(iOS classLabel).
                            val purpose = r.service?.purposes?.takeIf { it.isNotEmpty() }
                                ?.joinToString(", ") { classLabel(it, r.service.genre) }
                            val genreFallback = r.service?.genre?.let { "$it 레슨" }
                            (purpose ?: genreFallback)?.let { InfoRow("이용 목적", it) }
                            r.service?.region?.let { InfoRow("지역", it) }
                            ReviewOptions.lessonTypeLabel(r.lessonType).takeIf { it.isNotEmpty() }
                                ?.let { InfoRow("진행 방식", it) }
                            ReviewOptions.durationLabel(r.durationBucket).takeIf { it.isNotEmpty() }
                                ?.let { InfoRow("수강 기간", it) }
                        }
                    }

                    // 서비스를 제공한 강사
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "서비스를 제공한 강사",
                            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                            lineHeight = 19.sp, color = MuyeonColors.textHead,
                        )
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                .background(MuyeonColors.groupedBg)
                                .clickable(enabled = r.teacher.id > 0) { onSelectTeacher(r.teacher.id) }
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            QuoteAvatar(r.teacher.image, r.teacher.name ?: "강사", 48.dp)
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    r.teacher.name ?: "강사",
                                    fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                                    lineHeight = 18.sp, color = MuyeonColors.textHead,
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        Icons.Filled.Star, null, tint = MuyeonColors.yellow,
                                        modifier = Modifier.size(12.dp),
                                    )
                                    Text(
                                        String.format(Locale.KOREA, "%.1f", r.teacher.avg ?: 0.0),
                                        fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp, lineHeight = 16.sp, color = MuyeonColors.textHead,
                                    )
                                    Text(
                                        "(${r.teacher.count ?: 0})",
                                        fontFamily = customFontFamily, fontSize = 13.sp, lineHeight = 16.sp,
                                        color = MuyeonColors.textSub,
                                    )
                                }
                            }
                            Text("›", fontFamily = customFontFamily, fontSize = 16.sp, color = MuyeonColors.chevron)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                // 하단 CTA — 프리필 대상이 없으면 누를 게 없다(iOS 와 같이 흐리게).
                HorizontalDivider(color = MuyeonColors.border)
                val enabled = !requesting && r.prefillLessonId != null
                Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(
                        if (requesting) "불러오는 중…" else "같은 조건으로 견적 요청하기",
                        fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                        lineHeight = 52.sp, color = Color.White,
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MuyeonColors.primary.copy(alpha = if (enabled) 1f else 0.5f))
                            .clickable(enabled = enabled) {
                                val lid = r.prefillLessonId ?: return@clickable
                                requesting = true
                                scope.launch {
                                    api.quotePrefill(lid).onSuccess(onRequestQuote)
                                    requesting = false
                                }
                            },
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoRow(key: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            key,
            fontFamily = customFontFamily, fontSize = 14.sp, lineHeight = 17.sp,
            color = MuyeonColors.textSub, modifier = Modifier.width(72.dp),
        )
        Text(
            value,
            fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 15.sp,
            lineHeight = 20.sp, color = MuyeonColors.textHead,
        )
    }
}

/** 수업 옵션 id → 라벨. 장르 카테고리에서 찾고, 없으면 id 를 그대로 쓴다(iOS QuoteOptionLabel.classLabel). */
private fun classLabel(id: String, genre: String?): String =
    com.muyeon.app.ui.quote.QuoteCategory.find(genre)
        ?.classOptions?.firstOrNull { it.id == id }?.label ?: id
