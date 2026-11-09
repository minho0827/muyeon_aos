package com.muyeon.app.ui.castingstage

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.ui.castingstage.components.*
import com.muyeon.app.ui.castingstage.data.CastingStageApi
import com.muyeon.app.ui.castingstage.model.*
import com.muyeon.app.ui.common.MuyeonColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 캐스팅·무대 본문 상태 — 지금 보는 섹션([current])·섹션 목록([cats])·섹션으로 이동([jump]).
 *   본문 밖 머리(통합 탭 [공연][캐스팅] 줄)가 이걸로 섹션을 보이고 옮긴다.
 */
@Stable
class CastingStageBodyState internal constructor(
    val listState: LazyListState,
    private val scope: CoroutineScope,
    private val passLine: Int,
) {
    /** 캐스팅(서버 목록에 있을 때) + 소식이 있는 섹션(본문이 채운다). 서버 순서·이름. */
    var cats by mutableStateOf(CastingStageSession.categories.filter { it.isCasting })
        internal set
    internal var keys by mutableStateOf(listOf<String>())
    private var jumpingTo by mutableStateOf<String?>(null)

    /** 지금 보는 섹션 — 위 줄 바로 아래를 지난 섹션 중 가장 아래 것(iOS 와 같은 규칙). */
    val current: String by derivedStateOf {
        jumpingTo ?: listState.layoutInfo.visibleItemsInfo
            .filter { (it.key as? String)?.startsWith("section.") == true && it.offset <= passLine }
            .maxByOrNull { it.offset }?.let { (it.key as String).removePrefix("section.") }
        ?: cats.firstOrNull()?.code ?: StageCategory.CASTING_CODE
    }

    fun jump(code: String) {
        scope.launch {
            jumpingTo = code
            listState.animateScrollToItem(keys.indexOf("section.$code").coerceAtLeast(0))
            delay(150)
            jumpingTo = null
        }
    }

    fun scrollToTop() { scope.launch { listState.animateScrollToItem(0) } }
}

@Composable
fun rememberCastingStageBodyState(): CastingStageBodyState {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val passLine = with(LocalDensity.current) { (TAB_HEIGHT_DP + SECTION_TOP_DP + 24).dp.roundToPx() }
    return remember(listState, scope, passLine) { CastingStageBodyState(listState, scope, passLine) }
}

/**
 * 캐스팅·무대 본문(배너 · 캐스팅 섹션 · 소식 섹션 · 안내 · 맨 위로) — 예전 캐스팅·무대 화면과 통합 탭이 같이 쓴다.
 *
 * @param sectionNav true 면 배너 아래 바로가기 칩 + 붙는 탭(섹션이 둘 이상일 때). 머리가 섹션 줄을 맡으면 false.
 * @param bottomPadding 목록 맨 아래 여백(하단 탭이면 탭바 높이)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CastingStageBody(
    api: CastingStageApi,
    state: CastingStageBodyState,
    sectionNav: Boolean,
    bottomPadding: Dp,
    onOpenCasting: (Int) -> Unit,
    onOpenNews: (StageNewsItem, StageCategory) -> Unit,
    onCastingList: () -> Unit,
    onNewsList: (Int, String) -> Unit,
    onIssues: () -> Unit,
    /** 이번 호 카드 → 그 호 소식 화면(11-09: 스크롤 이동이 아니라 글로 — 사용자 결정). */
    onOpenIssue: (Int) -> Unit,
    modifier: Modifier = Modifier,
    /** 바뀌면 다시 불러온다 — 공고를 새로 올린 뒤. */
    reloadKey: Int = 0,
) {
    var issue by remember { mutableStateOf<StageNewsIssue?>(null) }
    var castings by remember { mutableStateOf<List<StageCasting>>(emptyList()) }
    var genre by remember { mutableStateOf("전체") }
    var loading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val listState = state.listState

    LaunchedEffect(reloadKey) {
        val i = async { runCatching { api.latest() } }
        val c = async { runCatching { api.castings(null) } }
        // ★ 11-09 분류는 서버가 준다 — 실패·404 면 기본 목록(국내소식·콩쿠르·대회 뺌).
        val k = async { runCatching { api.categories() } }
        CastingStageSession.categories = StageCategory.resolve(k.await().getOrNull())
        val ri = i.await()
        val rc = c.await()
        issue = ri.getOrNull()
        castings = rc.getOrNull().orEmpty()
        failed = ri.isFailure && rc.isFailure
        loading = false
    }

    val categories = CastingStageSession.categories
    // 캐스팅 칸은 서버 목록에 있을 때만 · 소식 칸은 목록에 있고 글이 있을 때만(서버 순서).
    val castingCat = categories.firstOrNull { it.isCasting }
    val newsSections = remember(issue, categories) { StageCategory.newsSections(categories, issue?.sections) }
    val cats = listOfNotNull(castingCat) + newsSections.map { it.first }
    fun catOf(n: StageNewsItem): StageCategory = newsSections.firstOrNull { (_, list) -> list.any { it.id == n.id } }?.first
        ?: StageCategory("NEWS", "무대 소식", StageCategory.KIND_NEWS)

    // 배너·바로가기 칩을 지나 내려가면(첫 항목이 배너가 아니고 조금 더 내려감) 맨 위로 버튼.
    val showTop by remember {
        derivedStateOf { listState.firstVisibleItemIndex >= 2 || (listState.firstVisibleItemIndex == 1 && listState.firstVisibleItemScrollOffset > 300) }
    }

    // 섹션이 하나뿐이면(호가 아직 없음 등) 바로가기 칩·붙는 탭은 숨긴다 — 자리만 차지한다.
    val multi = cats.size > 1 && sectionNav
    val keys = buildList {
        add("banner")
        if (multi) { add("icons"); add("tabs") }
        castingCat?.let { add("section.${it.code}") }
        newsSections.forEach { add("section.${it.first.code}") }
        add("footer")
    }
    SideEffect {
        if (state.cats != cats) state.cats = cats
        if (state.keys != keys) state.keys = keys
    }
    Box(modifier.fillMaxSize()) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = bottomPadding)) {
            // ★ 11-09 맨 위 배너는 뺐다(사용자 결정) — 분류 줄·캐스팅·소식 칸부터.
            if (multi) {
                item(key = "icons") { StageChipRow(cats, state::jump) }
                stickyHeader(key = "tabs") { StageTabBar(cats, state.current, state::jump) }
            }
            if (castingCat != null) item(key = "section.${castingCat.code}") {
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
                    StageMoreButton(castingCat.label) { onCastingList() }
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
                    .clickable { state.scrollToTop() }
                    .semantics { contentDescription = "맨 위로" },
                Alignment.Center,
            ) { Icon(Icons.Filled.KeyboardArrowUp, null, tint = MuyeonColors.textHead) }
        }
    }
}
