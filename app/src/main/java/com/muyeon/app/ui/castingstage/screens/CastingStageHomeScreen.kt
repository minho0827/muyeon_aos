package com.muyeon.app.ui.castingstage.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.ui.common.MuyeonColors
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.muyeon.app.ui.castingstage.model.*
import com.muyeon.app.ui.castingstage.data.*
import com.muyeon.app.ui.castingstage.components.*

/** 캐스팅·무대 첫 화면(iOS CastingStageHomeView). */

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CastingStageScreen(
    api: CastingStageApi,
    onClose: () -> Unit,
    onOpenCasting: (Int) -> Unit,
    onOpenNews: (StageNewsItem, StageCategory) -> Unit,
    onCastingList: () -> Unit,
    onNewsList: (Int, String) -> Unit,
    onIssues: () -> Unit,
    /** 공연팀으로 활동 중일 때만 넘긴다(웹 CastingStage 헤더 ＋와 같은 규칙). null 이면 숨김. */
    onCreate: (() -> Unit)? = null,
    /** 바뀌면 다시 불러온다 — 공고를 새로 올린 뒤. */
    reloadKey: Int = 0,
) {
    var issue by remember { mutableStateOf<StageNewsIssue?>(null) }
    var castings by remember { mutableStateOf<List<StageCasting>>(emptyList()) }
    var genre by remember { mutableStateOf("전체") }
    var loading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(reloadKey) {
        val i = async { runCatching { api.latest() } }
        val c = async { runCatching { api.castings(null) } }
        val ri = i.await()
        val rc = c.await()
        issue = ri.getOrNull()
        castings = rc.getOrNull().orEmpty()
        failed = ri.isFailure && rc.isFailure
        loading = false
    }

    val newsSections = remember(issue) {
        StageCategory.NEWS.mapNotNull { c -> issue?.sections?.get(c.code)?.takeIf { it.isNotEmpty() }?.let { c to it } }
    }
    val cats = listOf(StageCategory.CASTING) + newsSections.map { it.first }
    fun catOf(n: StageNewsItem): StageCategory = newsSections.firstOrNull { (_, list) -> list.any { it.id == n.id } }?.first ?: StageCategory.NEWS.first()

    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val passLine = with(density) { (TAB_HEIGHT_DP + SECTION_TOP_DP + 24).dp.roundToPx() }
    var jumpingTo by remember { mutableStateOf<String?>(null) }
    // 지금 보는 섹션 — 붙는 탭 바로 아래를 지난 섹션 중 가장 아래 것(iOS 와 같은 규칙).
    val current by remember {
        derivedStateOf {
            jumpingTo ?: listState.layoutInfo.visibleItemsInfo
                .filter { (it.key as? String)?.startsWith("section.") == true && it.offset <= passLine }
                .maxByOrNull { it.offset }?.let { (it.key as String).removePrefix("section.") }
            ?: StageCategory.CASTING.code
        }
    }
    // 배너·아이콘 칸을 지나 내려가면(첫 항목이 배너가 아니고 조금 더 내려감) 맨 위로 버튼.
    val showTop by remember {
        derivedStateOf { listState.firstVisibleItemIndex >= 2 || (listState.firstVisibleItemIndex == 1 && listState.firstVisibleItemScrollOffset > 300) }
    }

    // 소식 칸이 없으면(호가 아직 없음) 아이콘 칸·붙는 탭은 숨긴다 — 캐스팅 하나뿐이라 자리만 차지한다.
    val multi = cats.size > 1
    val keys = buildList {
        add("banner")
        if (multi) { add("icons"); add("tabs") }
        add("section.CASTING")
        newsSections.forEach { add("section.${it.first.code}") }
        add("footer")
    }
    fun indexOf(code: String): Int = keys.indexOf("section.$code").coerceAtLeast(0)

    fun jump(code: String) {
        scope.launch {
            jumpingTo = code
            listState.animateScrollToItem(indexOf(code))
            delay(150)
            jumpingTo = null
        }
    }

    Column(Modifier.fillMaxSize().background(Color.White).statusBarsPadding().navigationBarsPadding()) {
        // 상단바
        Row(Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clickable(onClick = onClose).semantics { contentDescription = "뒤로" }, Alignment.Center) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MuyeonColors.textHead)
            }
            Text("캐스팅·무대", style = Bold, fontSize = 20.sp, color = MuyeonColors.textHead)
            Spacer(Modifier.weight(1f))
            if (onCreate != null) {
                Box(Modifier.size(44.dp).clickable(onClick = onCreate).semantics { contentDescription = "공연 공고 등록" }, Alignment.Center) {
                    Icon(Icons.Filled.Add, null, tint = MuyeonColors.textHead)
                }
            }
            Box(Modifier.size(44.dp).clickable { onCastingList() }.semantics { contentDescription = "캐스팅 공고 전체 보기" }, Alignment.Center) {
                Icon(Icons.Filled.Search, null, tint = MuyeonColors.textHead)
            }
        }
        Box(Modifier.fillMaxSize()) {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 80.dp)) {
                item(key = "banner") {
                    StageBanner(issue, castings, loading, onTapIssue = { jump(newsSections.firstOrNull()?.first?.code ?: "CASTING") },
                        onTapCasting = { onOpenCasting(it.id) }, onTapNews = { n -> onOpenNews(n, catOf(n)) })
                }
                if (multi) {
                    item(key = "icons") { StageIconGrid(cats, ::jump) }
                    stickyHeader(key = "tabs") { StageTabBar(cats, current, ::jump) }
                }
                item(key = "section.CASTING") {
                    StageSectionBox("지금 열린 캐스팅") {
                        // 10-27 공용 캡슐 탭(ui/common/MuyeonListKit) — 섹션 안이라 구분선 없음.
                        com.muyeon.app.ui.common.MuyeonPillTabs(GENRES.map { com.muyeon.app.ui.common.MuyeonPillTab(it, it) }, genre, { g ->
                            genre = g
                            scope.launch { castings = runCatching { api.castings(g.takeIf { it != "전체" }) }.getOrDefault(emptyList()) }
                        }, tagPrefix = "stage.genre", showDivider = false, compact = true)
                        if (castings.isEmpty()) {
                            Box(Modifier.fillMaxWidth().heightIn(min = 120.dp), Alignment.Center) {
                                Text(if (loading) "불러오는 중…" else "지금 열린 캐스팅 공고가 없어요.", style = Regular, fontSize = 14.sp, color = MuyeonColors.textSub)
                            }
                        } else {
                            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(castings, key = { it.id }) { c -> StageCastingCard(c) { onOpenCasting(c.id) } }
                            }
                        }
                        StageMoreButton("캐스팅 공고") { onCastingList() }
                    }
                }
                newsSections.forEach { (cat, items) ->
                    item(key = "section.${cat.code}") {
                        StageSectionBox(cat.label) {
                            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(items, key = { it.id }) { n -> StageNewsCard(n, cat) { onOpenNews(n, cat) } }
                            }
                            issue?.let { StageMoreButton(cat.label) { onNewsList(it.issueNo, cat.code) } }
                        }
                    }
                }
                item(key = "footer") {
                    Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 36.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        when {
                            issue == null && !loading -> Text(
                                if (failed) "무대 소식을 불러오지 못했어요. 잠시 뒤 다시 열어 주세요." else "무대 소식은 매주 월요일 아침에 새로 나와요.",
                                style = Regular, fontSize = 14.sp, color = MuyeonColors.textSub, modifier = Modifier.fillMaxWidth(),
                            )
                            issue != null -> {
                                Text("무용연이 웹에서 모아 원문을 확인한 소식이에요. 일정과 조건은 바뀔 수 있으니 원문을 꼭 확인해 주세요.",
                                    style = Regular, fontSize = 12.sp, color = MuyeonColors.textSub)
                            }
                        }
                    }
                    if (issue != null) StageMoreButton("지난 무대 소식") { onIssues() }
                }
            }
            if (showTop) {
                Box(
                    // AI 도우미 떠 있는 버튼(오른쪽 아래)과 겹치지 않게 그 위에 둔다.
                    Modifier.align(Alignment.BottomEnd).padding(end = 22.dp, bottom = 96.dp).size(48.dp).shadow(6.dp, CircleShape).clip(CircleShape).background(Color.White)
                        .border(1.dp, MuyeonColors.border, CircleShape)
                        .clickable { scope.launch { listState.animateScrollToItem(0) } }
                        .semantics { contentDescription = "맨 위로" },
                    Alignment.Center,
                ) { Icon(Icons.Filled.KeyboardArrowUp, null, tint = MuyeonColors.textHead) }
            }
        }
    }
}
