package com.muyeon.app.ui.review

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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
import com.muyeon.app.ui.quote.QuoteEmptyState
import com.muyeon.app.ui.quote.QuoteNavBar
import com.muyeon.app.ui.quote.QuoteUi
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * 강사 후기 목록 — iOS `ReviewListView.swift` + `+Components.swift` 1:1.
 *  요약(평균·분포·3축 평균·태그 집계) + 정렬/필터(형태·목적·사진만) + 후기 카드(도움돼요).
 */
@Composable
fun ReviewListScreen(
    api: ReviewApi,
    teacherId: Int,
    onClose: () -> Unit,
    onWrite: () -> Unit,
) {
    var data by remember { mutableStateOf<ReviewList?>(null) }
    var loading by remember { mutableStateOf(true) }
    var sort by remember { mutableStateOf("best") }
    var lessonType by remember { mutableStateOf<String?>(null) }
    var purpose by remember { mutableStateOf<String?>(null) }
    var photoOnly by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    suspend fun load() {
        loading = data == null
        api.list(teacherId, sort, lessonType, purpose, photoOnly).onSuccess { data = it }
        loading = false
    }

    LaunchedEffect(sort, lessonType, purpose, photoOnly) { load() }

    Column(Modifier.fillMaxSize().background(MuyeonColors.surface)) {
        QuoteNavBar(title = "후기", onBack = onClose)

        when {
            loading -> Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                CircularProgressIndicator(color = MuyeonColors.primary)
            }
            else -> {
                val d = data
                LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 24.dp)) {
                    item { SummarySection(d) }
                    item {
                        FilterBar(
                            sort = sort, lessonType = lessonType, purpose = purpose, photoOnly = photoOnly,
                            onSort = { sort = it }, onLessonType = { lessonType = it },
                            onPurpose = { purpose = it }, onPhotoOnly = { photoOnly = it },
                        )
                    }
                    val items = d?.items ?: emptyList()
                    if (items.isEmpty()) {
                        item {
                            QuoteEmptyState(
                                Icons.Outlined.RateReview, "아직 후기가 없어요",
                                "레슨을 받은 회원이 후기를 남기면 여기에 보여요.",
                                Modifier.height(200.dp).wrapContentHeight(Alignment.CenterVertically),
                            )
                        }
                    } else {
                        items(items, key = { it.id }) { item ->
                            ReviewCard(item) { rid ->
                                scope.launch { api.toggleHelpful(rid); load() }
                            }
                        }
                    }
                }
                if (d?.canWrite == true) {
                    Text(
                        "후기 쓰기",
                        fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                        lineHeight = 19.sp, color = Color.White,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MuyeonColors.primary)
                            .clickable(onClick = onWrite)
                            .padding(vertical = 15.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SummarySection(d: ReviewList?) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                String.format(Locale.KOREA, "%.1f", d?.avg ?: 0.0),
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 34.sp,
                lineHeight = 40.sp, color = MuyeonColors.textHead,
            )
            Text(
                "후기 ${d?.count ?: 0}개",
                fontFamily = customFontFamily, fontSize = 13.sp, lineHeight = 16.sp,
                color = MuyeonColors.textSub, modifier = Modifier.padding(bottom = 6.dp),
            )
        }
        // 별점 분포 미니 막대(5→1)
        val dist = d?.distribution ?: emptyMap()
        val total = dist.values.sum().coerceAtLeast(1)
        (5 downTo 1).forEach { star ->
            val n = dist[star.toString()] ?: 0
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "$star",
                    fontFamily = customFontFamily, fontSize = 11.sp, lineHeight = 13.sp,
                    color = MuyeonColors.textSub, modifier = Modifier.width(10.dp),
                )
                Box(Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(50)).background(Color(0xFFF2F2F7))) {
                    Box(
                        Modifier.fillMaxHeight().fillMaxWidth(n.toFloat() / total)
                            .clip(RoundedCornerShape(50)).background(MuyeonColors.primary),
                    )
                }
                Text(
                    "$n",
                    fontFamily = customFontFamily, fontSize = 11.sp, lineHeight = 13.sp,
                    color = MuyeonColors.secondary, modifier = Modifier.width(24.dp),
                )
            }
        }
        // 3축 평균
        val axes = d?.axisAverages ?: emptyMap()
        if (axes.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ReviewOptions.ratingAxes.forEach { (key, label) ->
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(Color(0xFFF7F7F7)).padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            label,
                            fontFamily = customFontFamily, fontSize = 11.sp, lineHeight = 13.sp,
                            color = MuyeonColors.textSub,
                        )
                        Text(
                            String.format(Locale.KOREA, "%.1f", axes[key] ?: 0.0),
                            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                            lineHeight = 18.sp, color = MuyeonColors.textHead,
                        )
                    }
                }
            }
        }
        // '이런 점이 좋았어요' 집계 상위
        val tagCounts = (d?.tagCounts ?: emptyMap()).entries.sortedByDescending { it.value }.take(4)
        if (tagCounts.isNotEmpty()) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                tagCounts.forEach { (tag, n) ->
                    Text(
                        "${ReviewOptions.tagLabel(tag)} $n",
                        fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp,
                        lineHeight = 14.sp, color = MuyeonColors.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(MuyeonColors.primary.copy(alpha = 0.08f))
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterBar(
    sort: String,
    lessonType: String?,
    purpose: String?,
    photoOnly: Boolean,
    onSort: (String) -> Unit,
    onLessonType: (String?) -> Unit,
    onPurpose: (String?) -> Unit,
    onPhotoOnly: (Boolean) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            listOf("best" to "추천순", "recent" to "최신순", "high" to "별점 높은순", "low" to "별점 낮은순")
                .forEach { (v, label) -> FilterChip(label, sort == v) { onSort(v) } }
        }
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            FilterChip("사진 후기", photoOnly) { onPhotoOnly(!photoOnly) }
            ReviewOptions.lessonTypes.forEach { (v, label) ->
                FilterChip(label, lessonType == v) { onLessonType(if (lessonType == v) null else v) }
            }
            ReviewOptions.purposes.forEach { (v, label) ->
                FilterChip(label, purpose == v) { onPurpose(if (purpose == v) null else v) }
            }
        }
    }
}

@Composable
private fun FilterChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text,
        fontFamily = customFontFamily,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        fontSize = 13.sp, lineHeight = 16.sp,
        color = if (selected) Color.White else MuyeonColors.textSub,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) MuyeonColors.primary else Color(0xFFF2F2F7))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
    )
}

@Composable
private fun ReviewCard(item: ReviewList.Item, onHelpful: (Int) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFFAFAFA))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Row {
                (1..5).forEach { i ->
                    Icon(
                        Icons.Filled.Star, null,
                        tint = if (i <= (item.rating ?: 0)) MuyeonColors.primary else Color(0xFFE5E5EA),
                        modifier = Modifier.size(13.dp),
                    )
                }
            }
            Text(
                item.reviewerName ?: "회원",
                fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                lineHeight = 16.sp, color = MuyeonColors.textHead,
            )
            Spacer(Modifier.weight(1f))
            Text(
                QuoteUi.relativeTime(item.createdAt),
                fontFamily = customFontFamily, fontSize = 11.sp, lineHeight = 13.sp, color = MuyeonColors.secondary,
            )
        }

        val meta = listOfNotNull(
            ReviewOptions.lessonTypeLabel(item.lessonType).ifEmpty { null },
            ReviewOptions.purposeLabel(item.purpose).ifEmpty { null },
            ReviewOptions.durationLabel(item.durationBucket).ifEmpty { null },
        ).joinToString(" · ")
        if (meta.isNotEmpty()) {
            Text(meta, fontFamily = customFontFamily, fontSize = 12.sp, lineHeight = 14.sp, color = MuyeonColors.textSub)
        }

        item.content?.takeIf { it.isNotBlank() }?.let {
            Text(it, fontFamily = customFontFamily, fontSize = 14.sp, lineHeight = 20.sp, color = MuyeonColors.body)
        }

        item.images?.takeIf { it.isNotEmpty() }?.let { imgs ->
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                imgs.forEach { url ->
                    AsyncImage(
                        QuoteUi.imageUrl(url), null, contentScale = ContentScale.Crop,
                        modifier = Modifier.size(84.dp).clip(RoundedCornerShape(8.dp)),
                    )
                }
            }
        }

        item.tags?.takeIf { it.isNotEmpty() }?.let { tags ->
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                tags.forEach { t ->
                    Text(
                        ReviewOptions.tagLabel(t),
                        fontFamily = customFontFamily, fontSize = 11.sp, lineHeight = 13.sp, color = MuyeonColors.textSub,
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(Color(0xFFF2F2F7))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }
        }

        // 도움돼요 토글
        Row(
            Modifier.clickable { onHelpful(item.id) }.padding(top = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.ThumbUp, null,
                tint = if (item.helpfulByMe == true) MuyeonColors.primary else MuyeonColors.secondary,
                modifier = Modifier.size(13.dp),
            )
            Text(
                "도움돼요 ${item.helpfulCount ?: 0}",
                fontFamily = customFontFamily, fontSize = 12.sp, lineHeight = 14.sp,
                color = if (item.helpfulByMe == true) MuyeonColors.primary else MuyeonColors.secondary,
            )
        }
    }
}
