package com.muyeon.app.ui.lesson

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
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
import com.muyeon.app.ui.quote.QuoteNavBar
import com.muyeon.app.ui.quote.QuoteUi
import com.muyeon.app.utils.TokenManager

/**
 * 강사 콘텐츠 둘러보기 — iOS `LessonBrowseView.swift` 1:1.
 *  숨고식 혼합 피드(포트폴리오+리뷰) 2열 그리드 + 상단 드롭다운 필터 칩.
 *  카드 탭 → 포트폴리오 상세 / 리뷰는 강사 공개 프로필.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonBrowseScreen(
    api: LessonBrowseApi,
    onClose: () -> Unit,
    onSelectPortfolio: (Int) -> Unit,
    onSelectTeacher: (Int) -> Unit,
    onSelectReview: (Int) -> Unit,
) {
    var filter by remember { mutableStateOf(LessonBrowseFilter()) }
    var items by remember { mutableStateOf<List<BrowseFeedItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var facet by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(filter) {
        loading = true
        api.feed(filter).onSuccess { items = it }
        loading = false
    }

    Column(Modifier.fillMaxSize().background(MuyeonColors.surface)) {
        QuoteNavBar(title = "둘러보기", onBack = onClose)

        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FacetChip(filter.genre ?: "장르", filter.genre != null) { facet = "genre" }
            FacetChip(filter.sidoName ?: "지역", filter.sidoName != null) { facet = "region" }
            FacetChip(
                if (filter.ageGroups.isEmpty()) "고객 연령대" else "연령대 ${filter.ageGroups.size}",
                filter.ageGroups.isNotEmpty(),
            ) { facet = "age" }
        }

        when {
            loading -> Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                CircularProgressIndicator(color = MuyeonColors.primary)
            }
            items.isEmpty() -> Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                Text(
                    "조건에 맞는 콘텐츠가 없어요.",
                    fontFamily = customFontFamily, fontSize = 14.sp, color = MuyeonColors.textSub,
                )
            }
            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(items, key = { "${it.type}-${it.itemId}" }) { item ->
                    BrowseCard(item) {
                        // 리뷰 카드는 리뷰 상세로, 포트폴리오는 콘텐츠 상세로(iOS 라우팅과 동일).
                        if (item.isReview) onSelectReview(item.itemId)
                        else onSelectPortfolio(item.itemId)
                    }
                }
            }
        }
    }

    facet?.let { which ->
        ModalBottomSheet(onDismissRequest = { facet = null }) {
            Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                when (which) {
                    "genre" -> {
                        FacetRow("전체", filter.genre == null) { filter = filter.copy(genre = null); facet = null }
                        LessonBrowseOptions.genres.forEach { g ->
                            FacetRow(g, filter.genre == g) { filter = filter.copy(genre = g); facet = null }
                        }
                    }
                    "region" -> {
                        FacetRow("전체", filter.sidoName == null) {
                            filter = filter.copy(sidoName = null, sidoCode = null); facet = null
                        }
                        LessonBrowseOptions.sidos.forEach { (name, code) ->
                            FacetRow(name, filter.sidoName == name) {
                                filter = filter.copy(sidoName = name, sidoCode = code); facet = null
                            }
                        }
                    }
                    else -> {
                        // 연령대는 다중 선택 — 시트를 닫지 않는다.
                        LessonBrowseOptions.ages.forEach { (id, label) ->
                            val on = filter.ageGroups.contains(id)
                            FacetRow(label, on) {
                                filter = filter.copy(
                                    ageGroups = if (on) filter.ageGroups - id else filter.ageGroups + id,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FacetChip(label: String, active: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(50))
            .background(if (active) MuyeonColors.primary.copy(alpha = 0.10f) else Color(0xFFF2F2F7))
            .clickable(onClick = onClick)
            .padding(start = 12.dp, end = 8.dp, top = 7.dp, bottom = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
            lineHeight = 16.sp, color = if (active) MuyeonColors.primary else MuyeonColors.textSub,
            maxLines = 1,
        )
        Icon(
            Icons.Filled.ExpandMore, null,
            tint = if (active) MuyeonColors.primary else MuyeonColors.textSub,
            modifier = Modifier.size(14.dp),
        )
    }
}

@Composable
private fun FacetRow(label: String, on: Boolean, onClick: () -> Unit) {
    Text(
        label,
        fontFamily = customFontFamily,
        fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
        fontSize = 15.sp, lineHeight = 18.sp,
        color = if (on) MuyeonColors.primary else MuyeonColors.textHead,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 13.dp),
    )
}

@Composable
private fun BrowseCard(item: BrowseFeedItem, onClick: () -> Unit) {
    Column(
        Modifier.clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box {
            AsyncImage(
                QuoteUi.imageUrl(item.thumbnail), null, contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp)).background(Color(0xFFF2F2F7)),
            )
            if (item.isReview) {
                Row(
                    Modifier.align(Alignment.TopStart).padding(8.dp)
                        .clip(RoundedCornerShape(50)).background(Color.Black.copy(alpha = 0.55f))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Star, null, tint = MuyeonColors.yellow, modifier = Modifier.size(11.dp))
                    Text(
                        "${item.rating ?: 5}",
                        fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 11.sp,
                        lineHeight = 14.sp, color = Color.White,
                    )
                }
            }
        }
        Text(
            item.cardTitle,
            fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
            lineHeight = 19.sp, color = MuyeonColors.textHead,
            maxLines = 2, overflow = TextOverflow.Ellipsis,
        )
        Text(
            listOfNotNull(item.displayName, item.region).joinToString(" · "),
            fontFamily = customFontFamily, fontSize = 12.sp, lineHeight = 15.sp,
            color = MuyeonColors.textSub, maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}

/** 둘러보기 · 콘텐츠 상세 컨테이너. */
class LessonBrowseActivity : ComponentActivity() {

    companion object {
        private const val EXTRA_ROUTE = "route"
        private const val EXTRA_ID = "id"

        fun startBrowse(context: Context) = context.go(intent(context, "browse"))

        fun startDetail(context: Context, lessonProductId: Int) =
            context.go(intent(context, "detail").putExtra(EXTRA_ID, lessonProductId))

        /** 리뷰 카드 → 리뷰 상세. */
        fun startReview(context: Context, reviewId: Int) =
            context.go(intent(context, "review").putExtra(EXTRA_ID, reviewId))

        private fun intent(context: Context, route: String) =
            Intent(context, LessonBrowseActivity::class.java).putExtra(EXTRA_ROUTE, route)

        private fun Context.go(i: Intent) {
            if (this !is Activity) i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(i)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val route = intent.getStringExtra(EXTRA_ROUTE) ?: "browse"
        val id = intent.getIntExtra(EXTRA_ID, 0)

        setContent {
            val api = remember { LessonBrowseApi(TokenManager.getAccessToken(this)) }
            if (route == "review" && id > 0) {
                val reviewApi = remember { com.muyeon.app.ui.review.ReviewApi(TokenManager.getAccessToken(this)) }
                com.muyeon.app.ui.review.ReviewDetailScreen(
                    api = reviewApi, reviewId = id,
                    onClose = { finish() },
                    onSelectTeacher = { tid ->
                        com.muyeon.app.ui.resume.ResumeActivity.startProfile(this, tid, "review")
                    },
                    onRequestQuote = { p ->
                        com.muyeon.app.ui.quote.QuoteWizardActivity.start(
                            this, p.categoryId, p.targetTeacherId.takeIf { it > 0 }?.toString(),
                            p.prefillJson, p.region, p.regionCode,
                        )
                    },
                )
            } else if (route == "detail" && id > 0) {
                LessonContentDetailScreen(
                    api = api, lessonProductId = id,
                    onClose = { finish() },
                    onSelectLesson = { lid -> startDetail(this, lid) },
                    onSelectTeacher = { tid ->
                        com.muyeon.app.ui.resume.ResumeActivity.startProfile(this, tid, "lessonContent")
                    },
                )
            } else {
                LessonBrowseScreen(
                    api = api,
                    onClose = { finish() },
                    onSelectPortfolio = { pid -> startDetail(this, pid) },
                    onSelectTeacher = { tid ->
                        com.muyeon.app.ui.resume.ResumeActivity.startProfile(this, tid, "browse")
                    },
                    onSelectReview = { rid -> startReview(this, rid) },
                )
            }
        }
    }
}
