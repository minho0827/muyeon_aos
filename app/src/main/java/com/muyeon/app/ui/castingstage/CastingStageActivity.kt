package com.muyeon.app.ui.castingstage

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.muyeon.app.result.ResultActivity
import com.muyeon.app.result.launchScreen
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors
import com.muyeon.app.utils.TokenManager
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * '캐스팅·무대' 네이티브 화면(10-26) — 웹 하단 탭 '캐스팅·무대'(openCastingStage). iOS CastingStageView 와 같은 구성.
 *  위에서 아래로: 상단바 → 자동 넘김 배너 → 아이콘 칸 11개 → 위에 붙는 글자 탭 → 섹션(가로 카드 + 전체보기) → 맨 위로.
 *  캐스팅·소식 상세, 전체보기 목록, 지난 호는 웹으로 넘기지 않고 화면 안 내비게이션(NavHost)으로 연다(10-26, CastingStageDetail.kt).
 */
class CastingStageActivity : ResultActivity() {
    companion object {
        fun start(context: Context) = context.launchScreen(Intent(context, CastingStageActivity::class.java))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val api = remember { CastingStageApi(TokenManager.getAccessToken(this)) }
            val nav = rememberNavController()
            com.muyeon.app.analytics.TrackNavigation("CastingStage", nav)
            // 소식 상세는 목록에서 받은 값을 그대로 넘긴다(소식 하나만 주는 API 가 없다).
            val newsCache = remember { mutableMapOf<Int, Pair<StageNewsItem, StageCategory>>() }
            val openUrl: (String) -> Unit = { url -> runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } }
            fun openNews(item: StageNewsItem, cat: StageCategory) { newsCache[item.id] = item to cat; nav.navigate("news/${item.id}") }
            fun back() { if (!nav.popBackStack()) finish() }
            NavHost(nav, startDestination = "home") {
                composable("home") {
                    CastingStageScreen(
                        api = api,
                        onClose = { finish() },
                        onOpenCasting = { nav.navigate("casting/$it") },
                        onOpenNews = ::openNews,
                        onCastingList = { nav.navigate("castingList") },
                        onNewsList = { no, code -> nav.navigate("newsList/$no/$code") },
                        onIssues = { nav.navigate("issues") },
                    )
                }
                composable("casting/{id}", listOf(navArgument("id") { type = NavType.IntType })) { e ->
                    StageCastingDetailScreen(api, e.arguments?.getInt("id") ?: 0, onBack = ::back, onOpenUrl = openUrl,
                        onWriteResume = { com.muyeon.app.ui.resume.ResumeActivity.startEdit(this@CastingStageActivity, null, "dancer") })
                }
                composable("news/{id}", listOf(navArgument("id") { type = NavType.IntType })) { e ->
                    val hit = newsCache[e.arguments?.getInt("id") ?: 0]
                    if (hit != null) StageNewsDetailScreen(hit.first, hit.second, onBack = ::back, onOpenUrl = openUrl) else back()
                }
                composable("castingList") { StageCastingListScreen(api, onBack = ::back, onOpen = { nav.navigate("casting/$it") }) }
                composable("newsList/{no}/{code}", listOf(navArgument("no") { type = NavType.IntType })) { e ->
                    StageNewsListScreen(api, e.arguments?.getInt("no") ?: 0, e.arguments?.getString("code"), onBack = ::back, onOpen = ::openNews)
                }
                composable("issues") { StageIssuesScreen(api, onBack = ::back, onOpen = { nav.navigate("issue/$it") }) }
                composable("issue/{no}", listOf(navArgument("no") { type = NavType.IntType })) { e ->
                    StageNewsListScreen(api, e.arguments?.getInt("no") ?: 0, null, onBack = ::back, onOpen = ::openNews)
                }
            }
        }
    }
}

private val Bold = TextStyle(fontFamily = customFontFamily, fontWeight = FontWeight.Bold)
private val Semi = TextStyle(fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold)
private val Medium = TextStyle(fontFamily = customFontFamily, fontWeight = FontWeight.Medium)
private val Regular = TextStyle(fontFamily = customFontFamily, fontWeight = FontWeight.Normal)
private val Blue = Color(0xFF3D5AFE)
private val Ink100 = Color(0xFFF8F8F8)
private val Ink300 = Color(0xFFDADADA)
private const val TAB_HEIGHT_DP = 46
private const val SECTION_TOP_DP = 56
private val GENRES = listOf("전체", "발레", "한국무용", "현대무용", "실용무용", "뮤지컬", "행사")

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
) {
    var issue by remember { mutableStateOf<StageNewsIssue?>(null) }
    var castings by remember { mutableStateOf<List<StageCasting>>(emptyList()) }
    var genre by remember { mutableStateOf("전체") }
    var loading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
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
                    item(key = "icons") { IconGrid(cats, ::jump) }
                    stickyHeader(key = "tabs") { TabBar(cats, current, ::jump) }
                }
                item(key = "section.CASTING") {
                    SectionBox("지금 열린 캐스팅") {
                        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(GENRES) { g ->
                                val on = g == genre
                                Box(
                                    Modifier.height(36.dp).clip(CircleShape).background(if (on) MuyeonColors.textHead else Ink100)
                                        .clickable {
                                            genre = g
                                            scope.launch { castings = runCatching { api.castings(g.takeIf { it != "전체" }) }.getOrDefault(emptyList()) }
                                        }.padding(horizontal = 16.dp),
                                    Alignment.Center,
                                ) { Text(g, style = Medium, fontSize = 14.sp, color = if (on) Color.White else MuyeonColors.textHead) }
                            }
                        }
                        if (castings.isEmpty()) {
                            Box(Modifier.fillMaxWidth().heightIn(min = 120.dp), Alignment.Center) {
                                Text(if (loading) "불러오는 중…" else "지금 열린 캐스팅 공고가 없어요.", style = Regular, fontSize = 14.sp, color = MuyeonColors.textSub)
                            }
                        } else {
                            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(castings, key = { it.id }) { c -> CastingCard(c) { onOpenCasting(c.id) } }
                            }
                        }
                        MoreButton("캐스팅 공고") { onCastingList() }
                    }
                }
                newsSections.forEach { (cat, items) ->
                    item(key = "section.${cat.code}") {
                        SectionBox(cat.label) {
                            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(items, key = { it.id }) { n -> NewsCard(n, cat) { onOpenNews(n, cat) } }
                            }
                            issue?.let { MoreButton(cat.label) { onNewsList(it.issueNo, cat.code) } }
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
                    if (issue != null) MoreButton("지난 무대 소식") { onIssues() }
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

@Composable
private fun SectionBox(title: String, content: @Composable () -> Unit) {
    // 위 여백 56 = 붙는 탭(46dp)에 제목이 가리지 않게 한 몫(iOS 와 같다).
    Column(Modifier.fillMaxWidth().padding(top = SECTION_TOP_DP.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(title, style = Bold, fontSize = 22.sp, color = MuyeonColors.textHead, modifier = Modifier.padding(horizontal = 16.dp))
        content()
    }
}

@Composable
private fun IconGrid(cats: List<StageCategory>, onJump: (String) -> Unit) {
    // 가로 줄 먼저 채운다(윗줄 1~6, 아랫줄 7~11) — LazyHorizontalGrid 는 세로 먼저 채워 읽는 순서가 어색했다(10-26 iOS 확인).
    val half = (cats.size + 1) / 2
    Row(Modifier.fillMaxWidth().padding(top = 20.dp).horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf(cats.take(half), cats.drop(half)).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { row.forEach { IconButton(it, onJump) } }
            }
        }
    }
}

@Composable
private fun IconButton(c: StageCategory, onJump: (String) -> Unit) {
    Column(
        Modifier.width(76.dp).clickable { onJump(c.code) }.semantics { contentDescription = "stage.icon.${c.code}" },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(60.dp).clip(RoundedCornerShape(18.dp)).background(c.bg), Alignment.Center) {
            Icon(c.icon, null, tint = c.fg, modifier = Modifier.size(28.dp))
        }
        Text(c.label, style = Medium, fontSize = 13.sp, color = MuyeonColors.textHead, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun TabBar(cats: List<StageCategory>, current: String, onJump: (String) -> Unit) {
    val rowState = rememberLazyListState()
    LaunchedEffect(current) {
        val i = cats.indexOfFirst { it.code == current }
        if (i >= 0) rowState.animateScrollToItem(maxOf(0, i - 1))
    }
    Column(Modifier.fillMaxWidth().height(TAB_HEIGHT_DP.dp).background(Color.White)) {
        LazyRow(state = rowState, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(22.dp)) {
            items(cats, key = { it.code }) { c ->
                val on = c.code == current
                Column(Modifier.fillMaxHeight().clickable { onJump(c.code) }.semantics { contentDescription = "stage.tab.${c.code}${if (on) ".selected" else ""}" }) {
                    Box(Modifier.weight(1f), Alignment.Center) {
                        Text(c.label, style = if (on) Bold else Regular, fontSize = 16.sp,
                            color = if (on) MuyeonColors.textHead else MuyeonColors.textSub)
                    }
                    Box(Modifier.fillMaxWidth().height(3.dp).background(if (on) MuyeonColors.textHead else Color.Transparent))
                }
            }
        }
        HorizontalDivider(color = MuyeonColors.border)
    }
}

@Composable
private fun CastingCard(c: StageCasting, onClick: () -> Unit) {
    Column(Modifier.width(150.dp).clickable(onClick = onClick), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            Modifier.width(150.dp).height(200.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFFF4F1F8))
                .border(0.5.dp, MuyeonColors.border, RoundedCornerShape(14.dp)),
            Alignment.Center,
        ) {
            val url = stageImageUrl(c.imageUrl)
            if (url != null) AsyncImage(url, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else Icon(Icons.Filled.Star, null, tint = Color(0xFF9D8FBB), modifier = Modifier.size(36.dp))
        }
        StageDate.deadline(c.deadline)?.let { Text(it, style = Bold, fontSize = 15.sp, color = Blue, modifier = Modifier.padding(top = 4.dp)) }
        Text(c.title, style = Semi, fontSize = 15.sp, color = MuyeonColors.textHead, maxLines = 2, overflow = TextOverflow.Ellipsis)
        c.team?.let { Text(it, style = Regular, fontSize = 12.sp, color = MuyeonColors.textSub, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        c.genre?.let {
            Text(it, style = Medium, fontSize = 12.sp, color = MuyeonColors.textSub,
                modifier = Modifier.border(1.dp, Ink300, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 3.dp))
        }
    }
}

@Composable
private fun NewsCard(n: StageNewsItem, cat: StageCategory, onClick: () -> Unit) {
    Column(Modifier.width(230.dp).clickable(onClick = onClick), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.width(230.dp).height(140.dp).clip(RoundedCornerShape(14.dp)).background(cat.bg), Alignment.Center) {
            Icon(cat.icon, null, tint = cat.fg, modifier = Modifier.size(34.dp))
            stageImageUrl(n.imageUrl)?.let { AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
        }
        val when_ = StageDate.short(n.eventDate)
        if (when_ != null) Text("일정 $when_", style = Bold, fontSize = 14.sp, color = Blue, modifier = Modifier.padding(top = 4.dp))
        else StageDate.short(n.publishedDate)?.let { Text("$it 게시", style = Regular, fontSize = 12.sp, color = MuyeonColors.textSub, modifier = Modifier.padding(top = 4.dp)) }
        Text(n.title, style = Semi, fontSize = 15.sp, color = MuyeonColors.textHead, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(n.summary, style = Regular, fontSize = 12.sp, color = MuyeonColors.textSub, maxLines = 2, overflow = TextOverflow.Ellipsis)
        n.sourceName?.let { Text("$it · 원문 보기 ›", style = Regular, fontSize = 11.sp, color = MuyeonColors.textSub, maxLines = 1) }
    }
}

@Composable
private fun MoreButton(bold: String, onClick: () -> Unit) {
    Box(
        Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp).fillMaxWidth().heightIn(min = 50.dp)
            .clip(RoundedCornerShape(14.dp)).border(1.dp, Ink300, RoundedCornerShape(14.dp)).clickable(onClick = onClick),
        Alignment.Center,
    ) {
        Text(buildAnnotatedString {
            withStyle(SpanStyle(fontFamily = customFontFamily, fontWeight = FontWeight.Bold)) { append(bold) }
            withStyle(SpanStyle(fontFamily = customFontFamily, fontWeight = FontWeight.Normal)) { append(" 전체보기") }
        }, fontSize = 15.sp, color = MuyeonColors.textHead)
    }
}

/** 배너 — 이번 호 카드 + 사진 있는 캐스팅·소식. 4초마다 넘김, ⏸ 멈춤, '03 / 06'. */
@Composable
private fun StageBanner(
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
                is StageNewsIssue -> IssueSlide(s, onTapIssue)
                is StageCasting -> PhotoSlide(s.imageUrl, StageDate.deadline(s.deadline) ?: "캐스팅", s.title, s.team) { onTapCasting(s) }
                is Pair<*, *> -> {
                    val n = s.first as StageNewsItem
                    val c = s.second as StageCategory
                    PhotoSlide(n.imageUrl, c.label, n.title, n.sourceName) { onTapNews(n) }
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
private fun IssueSlide(issue: StageNewsIssue, onClick: () -> Unit) {
    val w = StageDate.week(issue.weekStart)
    Row(
        Modifier.fillMaxSize().clip(RoundedCornerShape(18.dp)).border(1.dp, Color(0xFFE4DEF2), RoundedCornerShape(18.dp))
            .background(Color.White).clickable(onClick = onClick).semantics { contentDescription = "stage.banner.issue" },
    ) {
        Column(Modifier.width(104.dp).fillMaxHeight().background(Color(0xFF4A2C6B)), Arrangement.Center, Alignment.CenterHorizontally) {
            Text("${w?.year ?: ""} ${w?.monthEn ?: ""}", style = Semi, fontSize = 11.sp, color = Color.White.copy(alpha = 0.85f))
            Text("W${w?.week ?: 1}", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 40.sp, color = Color.White)
            Text(w?.label ?: "", style = Semi, fontSize = 12.sp, color = Color.White)
        }
        Column(Modifier.weight(1f).fillMaxHeight().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)) {
            Text("무대 소식 #${issue.issueNo.toString().padStart(3, '0')}", style = Bold, fontSize = 12.sp, color = Color(0xFF6B4EE6), letterSpacing = 1.sp)
            Text(issue.headline, style = Bold, fontSize = 18.sp, color = MuyeonColors.textHead, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Text(w?.range ?: "", style = Regular, fontSize = 12.sp, color = MuyeonColors.textSub)
        }
    }
}

@Composable
private fun PhotoSlide(url: String?, kicker: String, title: String, sub: String?, onClick: () -> Unit) {
    Box(Modifier.fillMaxSize().clip(RoundedCornerShape(18.dp)).background(Color(0xFF2A2340)).clickable(onClick = onClick)) {
        stageImageUrl(url)?.let { AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.5f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.7f))))
        Column(Modifier.align(Alignment.BottomStart).padding(start = 18.dp, end = 60.dp, bottom = 50.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(kicker, style = Medium, fontSize = 13.sp, color = Color.White.copy(alpha = 0.9f))
            Text(title, style = Bold, fontSize = 20.sp, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
            sub?.let { Text(it, style = Regular, fontSize = 12.sp, color = Color.White.copy(alpha = 0.85f), maxLines = 1) }
        }
    }
}
