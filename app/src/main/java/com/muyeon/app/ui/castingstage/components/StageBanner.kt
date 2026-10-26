package com.muyeon.app.ui.castingstage.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.muyeon.app.ui.common.MuyeonColors
import kotlinx.coroutines.delay
import com.muyeon.app.ui.castingstage.model.*
import com.muyeon.app.ui.castingstage.data.*
import com.muyeon.app.ui.castingstage.screens.*

/** 자동 넘김 배너(iOS StageBanner). */

/** 배너 — 이번 호 카드 + 사진 있는 캐스팅·소식. 4초마다 넘김, ⏸ 멈춤, '03 / 06'. */
@Composable
internal fun StageBanner(
    issue: StageNewsIssue?, castings: List<StageCasting>, loading: Boolean,
    onTapIssue: () -> Unit, onTapCasting: (StageCasting) -> Unit, onTapNews: (StageNewsItem) -> Unit,
) {
    val slides = remember(issue, castings) {
        buildList<Any> {
            issue?.let { add(it) }
            addAll(castings.filter { !it.imageUrl.isNullOrBlank() }.take(3))
            issue?.let { i ->
                StageCategory.NEWS.forEach { c ->
                    i.sections[c.code].orEmpty().filter { !it.imageUrl.isNullOrBlank() }.forEach { if (size < 8) add(it to c) }
                }
            }
        }
    }
    if (slides.isEmpty()) {
        // 불러오는 동안 자리를 먼저 잡는다 — 목록은 맨 위 항목 기준으로 자리를 지켜서, 0 높이였다가 커지면
        //  배너가 아이콘 칸 위로 숨어 버린다(10-26 실기기).
        if (loading) Box(Modifier.fillMaxWidth().height(210.dp).padding(start = 16.dp, end = 16.dp, top = 8.dp).clip(RoundedCornerShape(18.dp)).background(Ink100))
        return
    }
    val pager = rememberPagerState { slides.size }
    var paused by remember { mutableStateOf(false) }
    LaunchedEffect(paused, slides.size) {
        while (!paused && slides.size > 1) {
            delay(4000)
            pager.animateScrollToPage((pager.currentPage + 1) % slides.size)
        }
    }
    Box(Modifier.fillMaxWidth().height(210.dp).padding(top = 8.dp)) {
        HorizontalPager(pager, contentPadding = PaddingValues(horizontal = 16.dp), pageSpacing = 10.dp) { page ->
            when (val s = slides[page]) {
                is StageNewsIssue -> StageIssueSlide(s, onTapIssue)
                is StageCasting -> StagePhotoSlide(s.imageUrl, StageDate.deadline(s.deadline) ?: "캐스팅", s.title, s.team) { onTapCasting(s) }
                is Pair<*, *> -> {
                    val n = s.first as StageNewsItem
                    val c = s.second as StageCategory
                    StagePhotoSlide(n.imageUrl, c.label, n.title, n.sourceName) { onTapNews(n) }
                }
            }
        }
        Row(
            Modifier.align(Alignment.BottomEnd).padding(end = 30.dp, bottom = 14.dp).semantics { contentDescription = "stage.banner.counter" },
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(Modifier.size(30.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.45f)).clickable { paused = !paused }, Alignment.Center) {
                Icon(if (paused) Icons.Filled.PlayArrow else Icons.Filled.Pause, if (paused) "배너 넘기기 다시 시작" else "배너 넘기기 멈춤",
                    tint = Color.White, modifier = Modifier.size(16.dp))
            }
            Box(Modifier.height(30.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.45f)).padding(horizontal = 12.dp), Alignment.Center) {
                Text(buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append((pager.currentPage + 1).toString().padStart(2, '0')) }
                    append(" / ${slides.size.toString().padStart(2, '0')}")
                }, style = Regular, fontSize = 12.sp, color = Color.White)
            }
        }
    }
}

@Composable
internal fun StageIssueSlide(issue: StageNewsIssue, onClick: () -> Unit) {
    val w = StageDate.week(issue.weekStart)
    Row(
        Modifier.fillMaxSize().clip(RoundedCornerShape(18.dp)).border(1.dp, Color(0xFFFFE4D2), RoundedCornerShape(18.dp))
            .background(Color.White).clickable(onClick = onClick).semantics { contentDescription = "stage.banner.issue" },
    ) {
        Column(Modifier.width(104.dp).fillMaxHeight().background(Color(0xFFF58232)), Arrangement.Center, Alignment.CenterHorizontally) {
            Text("${w?.year ?: ""} ${w?.monthEn ?: ""}", style = Semi, fontSize = 11.sp, color = Color.White.copy(alpha = 0.85f))
            Text("W${w?.week ?: 1}", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 40.sp, color = Color.White)
            Text(w?.label ?: "", style = Semi, fontSize = 12.sp, color = Color.White)
        }
        Column(Modifier.weight(1f).fillMaxHeight().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)) {
            Text("무대 소식 #${issue.issueNo.toString().padStart(3, '0')}", style = Bold, fontSize = 12.sp, color = Color(0xFFF58232), letterSpacing = 1.sp)
            Text(issue.headline, style = Bold, fontSize = 18.sp, color = MuyeonColors.textHead, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Text(w?.range ?: "", style = Regular, fontSize = 12.sp, color = MuyeonColors.textSub)
        }
    }
}

@Composable
internal fun StagePhotoSlide(url: String?, kicker: String, title: String, sub: String?, onClick: () -> Unit) {
    Box(Modifier.fillMaxSize().clip(RoundedCornerShape(18.dp)).background(Color(0xFF3A2A20)).clickable(onClick = onClick)) {
        stageImageUrl(url)?.let { AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.5f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.7f))))
        Column(Modifier.align(Alignment.BottomStart).padding(start = 18.dp, end = 60.dp, bottom = 50.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(kicker, style = Medium, fontSize = 13.sp, color = Color.White.copy(alpha = 0.9f))
            Text(title, style = Bold, fontSize = 20.sp, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
            sub?.let { Text(it, style = Regular, fontSize = 12.sp, color = Color.White.copy(alpha = 0.85f), maxLines = 1) }
        }
    }
}
