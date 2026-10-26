package com.muyeon.app.ui.castingstage.screens

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.ui.common.MuyeonColors
import com.muyeon.app.ui.castingstage.model.*
import com.muyeon.app.ui.castingstage.data.*
import com.muyeon.app.ui.castingstage.components.*

/** 전체 목록 — 캐스팅 공고·소식 칸·지난 호(iOS StageCastingListView·StageNewsListView·StageIssuesView). */

@Composable
fun StageCastingListScreen(api: CastingStageApi, onBack: () -> Unit, onOpen: (Int) -> Unit) {
    val genres = listOf("전체", "발레", "한국무용", "현대무용", "실용무용", "뮤지컬", "행사")
    var genre by remember { mutableStateOf("전체") }
    val items = remember { mutableStateListOf<StageCasting>() }
    var cursor by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var done by remember { mutableStateOf(false) }
    val state = rememberLazyListState()

    suspend fun more() {
        if (loading || done) return
        loading = true
        runCatching { api.castingsPage(genre.takeIf { it != "전체" }, cursor) }
            .onSuccess { p -> items.addAll(p.items.filter { n -> items.none { it.id == n.id } }); cursor = p.nextCursor; done = p.nextCursor == null }
            .onFailure { done = true }
        loading = false
    }
    LaunchedEffect(genre) { items.clear(); cursor = null; done = false; more() }
    val nearEnd by remember { derivedStateOf { (state.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) >= items.size - 3 } }
    LaunchedEffect(nearEnd) { if (nearEnd && items.isNotEmpty()) more() }

    Column(Modifier.fillMaxSize().background(Color.White).statusBarsPadding().navigationBarsPadding()) {
        StageListBar("캐스팅 공고", onBack)
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(genres) { g ->
                val on = g == genre
                Box(Modifier.height(36.dp).clip(CircleShape).background(if (on) MuyeonColors.textHead else Ink100).clickable { genre = g }.padding(horizontal = 16.dp), Alignment.Center) {
                    Text(g, style = Medium, fontSize = 14.sp, color = if (on) Color.White else MuyeonColors.textHead)
                }
            }
        }
        LazyColumn(state = state, modifier = Modifier.fillMaxSize()) {
            items(items, key = { it.id }) { c ->
                StageListRow(c.imageUrl, Icons.Filled.Star, Color(0xFFFFA262), Color(0xFFFFF3EA), StageDate.deadline(c.deadline), c.title,
                    listOfNotNull(c.team, c.genre).joinToString(" · "), placeholder = com.muyeon.app.R.drawable.stage_noimage_casting) { onOpen(c.id) }
            }
            item {
                Box(Modifier.fillMaxWidth().padding(32.dp), Alignment.Center) {
                    when {
                        loading -> CircularProgressIndicator(color = Brand)
                        items.isEmpty() -> Text("조건에 맞는 공고가 없어요.", style = Regular, fontSize = 14.sp, color = MuyeonColors.textSub)
                    }
                }
            }
        }
    }
}

/** 한 호의 소식 목록 — code 가 있으면 그 칸만, null 이면 모든 칸(지난 호 보기). */
@Composable
fun StageNewsListScreen(api: CastingStageApi, issueNo: Int, code: String?, onBack: () -> Unit, onOpen: (StageNewsItem, StageCategory) -> Unit) {
    var issue by remember { mutableStateOf<StageNewsIssue?>(null) }
    var failed by remember { mutableStateOf(false) }
    LaunchedEffect(issueNo) { runCatching { api.issue(issueNo) }.onSuccess { issue = it }.onFailure { failed = true } }
    val cats = StageCategory.NEWS.filter { code == null || it.code == code }
    Column(Modifier.fillMaxSize().background(Color.White).statusBarsPadding().navigationBarsPadding()) {
        StageListBar(code?.let { c -> StageCategory.NEWS.firstOrNull { it.code == c }?.label } ?: "무대 소식 #${issueNo.toString().padStart(3, '0')}", onBack)
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 40.dp)) {
            val i = issue
            if (i == null) {
                item { Box(Modifier.fillMaxWidth().padding(40.dp), Alignment.Center) {
                    if (failed) Text("소식을 불러오지 못했어요.", style = Regular, fontSize = 14.sp, color = MuyeonColors.textSub) else CircularProgressIndicator(color = Brand)
                } }
            } else {
                if (code == null) item {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text(i.headline, style = Bold, fontSize = 20.sp, color = MuyeonColors.textHead)
                        Text(StageDate.week(i.weekStart)?.range ?: "", style = Regular, fontSize = 12.sp, color = MuyeonColors.textSub)
                    }
                }
                cats.forEach { c ->
                    val list = i.sections[c.code].orEmpty()
                    if (list.isNotEmpty()) {
                        if (code == null) item { Text(c.label, style = Bold, fontSize = 18.sp, color = c.fg, modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 4.dp)) }
                        items(list, key = { "${c.code}.${it.id}" }) { n ->
                            StageListRow(n.imageUrl, c.icon, c.fg, c.bg, StageDate.short(n.eventDate)?.let { "일정 $it" }, n.title, n.sourceName) { onOpen(n, c) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StageIssuesScreen(api: CastingStageApi, onBack: () -> Unit, onOpen: (Int) -> Unit) {
    var rows by remember { mutableStateOf<List<StageIssueSummary>?>(null) }
    LaunchedEffect(Unit) { rows = api.issues() }
    Column(Modifier.fillMaxSize().background(Color.White).statusBarsPadding().navigationBarsPadding()) {
        StageListBar("지난 무대 소식", onBack)
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val list = rows
            if (list == null) item { Box(Modifier.fillMaxWidth().padding(40.dp), Alignment.Center) { CircularProgressIndicator(color = Brand) } }
            else if (list.isEmpty()) item { Text("아직 나온 무대 소식이 없어요.", style = Regular, fontSize = 14.sp, color = MuyeonColors.textSub, modifier = Modifier.padding(24.dp)) }
            else items(list, key = { it.issueNo }) { r ->
                val w = StageDate.week(r.weekStart)
                Row(Modifier.fillMaxWidth().height(118.dp).clip(RoundedCornerShape(16.dp)).border(1.dp, Color(0xFFFFE4D2), RoundedCornerShape(16.dp)).clickable { onOpen(r.issueNo) }) {
                    Column(Modifier.width(92.dp).fillMaxHeight().background(Color(0xFFF58232)), Arrangement.Center, Alignment.CenterHorizontally) {
                        Text("${w?.year ?: ""} ${w?.monthEn ?: ""}", style = Semi, fontSize = 10.sp, color = Color.White.copy(alpha = 0.85f))
                        Text("W${w?.week ?: 1}", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 30.sp, color = Color.White)
                        Text(w?.label ?: "", style = Semi, fontSize = 11.sp, color = Color.White)
                    }
                    Column(Modifier.weight(1f).padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("무대 소식 #${r.issueNo.toString().padStart(3, '0')}", style = Bold, fontSize = 11.sp, color = Brand)
                        Text(r.headline, style = Bold, fontSize = 16.sp, color = MuyeonColors.textHead, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(w?.range ?: "", style = Regular, fontSize = 12.sp, color = MuyeonColors.textSub)
                    }
                }
            }
        }
    }
}
