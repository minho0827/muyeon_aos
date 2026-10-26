package com.muyeon.app.ui.castingstage

import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.muyeon.app.BuildConfig
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors
import com.muyeon.app.webview.ActiveRole
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/*
 * '캐스팅·무대' 네이티브 상세·목록(10-26) — 웹으로 넘기지 않고 화면 안 내비게이션으로 연다. iOS CastingStageDetailViews 와 같은 구성.
 *  상세는 NOL 티켓 상품 상세를 따른다: 큰 사진(1 / N) → 제목·부제 → 바로가기 칩 → 정보 줄 → 붙는 탭 + 섹션 → 아래 고정 버튼.
 *  긴 글 접기는 PACERA 모임 상세(ClubAboutBlock)와 같은 방식 — 3줄 + [더보기/접기] SemiBold.
 */

private val Bold = TextStyle(fontFamily = customFontFamily, fontWeight = FontWeight.Bold)
private val Semi = TextStyle(fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold)
private val Medium = TextStyle(fontFamily = customFontFamily, fontWeight = FontWeight.Medium)
private val Regular = TextStyle(fontFamily = customFontFamily, fontWeight = FontWeight.Normal)
private val Purple = Color(0xFF6B4EE6)
private val Blue = Color(0xFF3D5AFE)
private val Ink100 = Color(0xFFF8F8F8)
private val Ink200 = Color(0xFFEAEAEA)
private val Ink300 = Color(0xFFDADADA)
private const val TAB_DP = 46
private const val SECTION_TOP_DP = 56

data class StageDetailSection(val id: String, val title: String, val content: @Composable () -> Unit)

/** 3줄 접기 + [더보기/접기] — PACERA ClubAboutBlock 과 같은 방식. 잘리지 않는 짧은 글엔 버튼을 숨긴다. */
@Composable
fun StageExpandableText(text: String, lines: Int = 3) {
    var expanded by remember { mutableStateOf(false) }
    var overflow by remember { mutableStateOf(false) }
    Column {
        Text(
            text, style = Regular, fontSize = 15.sp, lineHeight = 24.sp, color = MuyeonColors.textHead,
            maxLines = if (expanded) Int.MAX_VALUE else lines, overflow = TextOverflow.Ellipsis,
            onTextLayout = { if (!expanded) overflow = it.hasVisualOverflow },
        )
        if (overflow || expanded) {
            Row(
                Modifier.padding(top = 12.dp).fillMaxWidth().heightIn(min = 44.dp).clip(RoundedCornerShape(12.dp))
                    .border(1.dp, Ink300, RoundedCornerShape(12.dp)).clickable { expanded = !expanded }
                    .semantics { contentDescription = "stage.expand" },
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(if (expanded) "접기" else "더보기", style = Semi, fontSize = 14.sp, color = MuyeonColors.textSub)
                Icon(if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, null, tint = MuyeonColors.textSub, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun InfoRows(rows: List<Pair<String, String>>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        rows.forEach { (k, v) ->
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(k, style = Regular, fontSize = 15.sp, color = MuyeonColors.textSub, modifier = Modifier.width(64.dp))
                Text(v, style = Regular, fontSize = 15.sp, color = MuyeonColors.textHead)
            }
        }
    }
}

@Composable
fun StageBottomButton(title: String, enabled: Boolean = true, busy: Boolean = false, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(16.dp)).background(if (enabled) Purple else Ink200)
            .clickable(enabled = enabled && !busy, onClick = onClick).semantics { contentDescription = "stage.detail.cta" },
        Alignment.Center,
    ) {
        if (busy) CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
        else Text(title, style = Bold, fontSize = 17.sp, color = if (enabled) Color.White else MuyeonColors.textSub)
    }
}

@Composable
private fun HeroGallery(urls: List<String>, icon: ImageVector, tint: Color, bg: Color) {
    Box(Modifier.fillMaxWidth().height(300.dp).padding(horizontal = 16.dp).clip(RoundedCornerShape(18.dp)).background(bg), Alignment.Center) {
        if (urls.isEmpty()) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(48.dp))
        } else {
            val pager = rememberPagerState { urls.size }
            HorizontalPager(pager, Modifier.fillMaxSize()) { i ->
                AsyncImage(stageImageUrl(urls[i]), null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            }
            if (urls.size > 1) {
                Text("${pager.currentPage + 1} / ${urls.size}", style = Medium, fontSize = 13.sp, color = Color.White,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(14.dp).clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f)).padding(horizontal = 12.dp, vertical = 6.dp))
            }
        }
    }
}

/** 상세 틀 — 상단바(‹·제목·공유·찜) · 바로가기 칩 · 붙는 탭 · 섹션 · 맨 위로 · 아래 고정 버튼. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DetailScaffold(
    title: String,
    shareText: String?,
    liked: Boolean?,
    onLike: (() -> Unit)?,
    onBack: () -> Unit,
    sections: List<StageDetailSection>,
    header: @Composable () -> Unit,
    bottom: @Composable () -> Unit,
) {
    val ctx = LocalContext.current
    val state = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val passLine = with(density) { (TAB_DP + SECTION_TOP_DP + 24).dp.roundToPx() }
    var jumping by remember { mutableStateOf<String?>(null) }
    // 항목: 0 머리, 1 칩, 2 붙는 탭, 3.. 섹션
    val pastHeader by remember { derivedStateOf { state.firstVisibleItemIndex >= 2 } }
    val current by remember {
        derivedStateOf {
            jumping ?: state.layoutInfo.visibleItemsInfo
                .filter { (it.key as? String)?.startsWith("sec.") == true && it.offset <= passLine }
                .maxByOrNull { it.offset }?.let { (it.key as String).removePrefix("sec.") } ?: sections.firstOrNull()?.id
        }
    }
    fun jump(id: String) {
        val i = sections.indexOfFirst { it.id == id }
        if (i < 0) return
        scope.launch { jumping = id; state.animateScrollToItem(3 + i); delay(150); jumping = null }
    }

    Column(Modifier.fillMaxSize().background(Color.White).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clickable(onClick = onBack).semantics { contentDescription = "stage.detail.back" }, Alignment.Center) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MuyeonColors.textHead)
            }
            Text(if (pastHeader) title else "", style = Bold, fontSize = 18.sp, color = MuyeonColors.textHead,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            if (shareText != null) {
                Box(Modifier.size(44.dp).clickable {
                    runCatching {
                        ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, shareText), null))
                    }
                }.semantics { contentDescription = "공유" }, Alignment.Center) { Icon(Icons.Filled.Share, null, tint = MuyeonColors.textHead) }
            }
            if (liked != null && onLike != null) {
                Box(Modifier.size(44.dp).clickable(onClick = onLike).semantics { contentDescription = if (liked) "찜 해제" else "찜하기" }, Alignment.Center) {
                    Icon(if (liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, null, tint = if (liked) Color(0xFFE5484D) else MuyeonColors.textHead)
                }
            }
        }
        if (pastHeader) HorizontalDivider(color = MuyeonColors.border)
        Box(Modifier.weight(1f)) {
            LazyColumn(state = state, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 120.dp)) {
                item(key = "head") { header() }
                item(key = "chips") {
                    Row(Modifier.fillMaxWidth().padding(top = 20.dp).horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        sections.forEach { s ->
                            Box(Modifier.height(40.dp).clip(CircleShape).border(1.dp, Ink300, CircleShape).clickable { jump(s.id) }.padding(horizontal = 18.dp),
                                Alignment.Center) { Text(s.title, style = Medium, fontSize = 15.sp, color = MuyeonColors.textHead) }
                        }
                    }
                }
                stickyHeader(key = "tabs") {
                    Column(Modifier.fillMaxWidth().height(TAB_DP.dp).background(Color.White)) {
                        if (pastHeader) {
                            Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                                sections.forEach { s ->
                                    val on = s.id == current
                                    Column(Modifier.fillMaxHeight().clickable { jump(s.id) }.semantics { contentDescription = "stage.detail.tab.${s.id}${if (on) ".selected" else ""}" }) {
                                        Box(Modifier.weight(1f), Alignment.Center) {
                                            Text(s.title, style = if (on) Bold else Regular, fontSize = 16.sp, color = if (on) Purple else MuyeonColors.textSub)
                                        }
                                        Box(Modifier.fillMaxWidth().height(3.dp).background(if (on) Purple else Color.Transparent))
                                    }
                                }
                            }
                            HorizontalDivider(color = MuyeonColors.border)
                        }
                    }
                }
                sections.forEach { s ->
                    item(key = "sec.${s.id}") {
                        Column(Modifier.fillMaxWidth()) {
                            Box(Modifier.fillMaxWidth().height(10.dp).background(Ink100))
                            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = (SECTION_TOP_DP - 10).dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text(s.title, style = Bold, fontSize = 22.sp, color = MuyeonColors.textHead)
                                s.content()
                            }
                        }
                    }
                }
            }
            if (pastHeader) {
                Box(
                    Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 16.dp).size(48.dp).shadow(6.dp, RoundedCornerShape(14.dp))
                        .clip(RoundedCornerShape(14.dp)).background(Color.White).border(1.dp, MuyeonColors.border, RoundedCornerShape(14.dp))
                        .clickable { scope.launch { state.animateScrollToItem(0) } }.semantics { contentDescription = "맨 위로" },
                    Alignment.Center,
                ) { Icon(Icons.Filled.KeyboardArrowUp, null, tint = MuyeonColors.textHead) }
            }
        }
        HorizontalDivider(color = MuyeonColors.border)
        Box(Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 16.dp, vertical = 10.dp)) { bottom() }
    }
}

private fun chipText(t: String, fg: Color, bg: Color): @Composable () -> Unit = {
    Text(t, style = Semi, fontSize = 12.sp, color = fg, modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(bg).padding(horizontal = 8.dp, vertical = 4.dp))
}

// MARK: 캐스팅 상세

@Composable
fun StageCastingDetailScreen(api: CastingStageApi, id: Int, onBack: () -> Unit, onOpenUrl: (String) -> Unit, onWriteResume: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var c by remember { mutableStateOf<CastingDetail?>(null) }
    var failed by remember { mutableStateOf<String?>(null) }
    var liked by remember { mutableStateOf(false) }
    var applied by remember { mutableStateOf(false) }
    var applying by remember { mutableStateOf(false) }
    val resumes = remember { mutableStateListOf<StageResume>() }
    var resumeId by remember { mutableStateOf<Int?>(null) }
    var showApply by remember { mutableStateOf(false) }
    var showNoResume by remember { mutableStateOf(false) }
    var showSwitch by remember { mutableStateOf(false) }
    var toast by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(id) {
        runCatching { api.detail(id) }.onSuccess { c = it }.onFailure { failed = it.message ?: "공고를 찾을 수 없어요." }
        applied = api.applied(id)
        liked = id in api.scrappedIds()
    }

    suspend fun startApply() {
        applying = true
        val list = api.dancerResumes()
        applying = false
        resumes.clear(); resumes.addAll(list)
        if (list.isEmpty()) { showNoResume = true; return }
        resumeId = (list.firstOrNull { it.isDefault } ?: list.first()).id
        showApply = true
    }

    val detail = c
    if (detail == null) {
        Column(Modifier.fillMaxSize().background(Color.White).statusBarsPadding()) {
            Box(Modifier.size(44.dp).clickable(onClick = onBack), Alignment.Center) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MuyeonColors.textHead) }
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                if (failed != null) Text(failed!!, style = Regular, fontSize = 14.sp, color = MuyeonColors.textSub) else CircularProgressIndicator(color = Purple)
            }
        }
        return
    }
    val dl = StageDate.deadline(detail.d("deadline"))
    val closed = detail.status == "CLOSED" || (detail.d("deadline") != null && dl == null)
    val sections = buildList {
        detail.d("summary")?.let { add(StageDetailSection("summary", "공고 요약") { StageExpandableText(it) }) }
        val cond = listOf(
            "모집 역할" to detail.role, "장르" to detail.genre, "인원" to detail.headcount.takeIf { it > 0 }?.let { "${it}명" },
            "성별" to detail.gender, "나이" to detail.d("age"), "키" to detail.height, "출연료" to detail.d("fee"),
            "영상" to (if (detail.videoRequired) "영상 제출 필수" else null), "수상 경력" to (if (detail.awardPreferred) "수상 경력 우대" else null),
        ).mapNotNull { (k, v) -> v?.let { k to it } }
        if (cond.isNotEmpty()) add(StageDetailSection("cond", "모집 조건") { InfoRows(cond) })
        val how = listOf("마감" to StageDate.short(detail.d("deadline")), "지원 방법" to detail.d("applyMethod"), "문의" to detail.d("contact"))
            .mapNotNull { (k, v) -> v?.let { k to it } }
        if (how.isNotEmpty()) add(StageDetailSection("how", "지원 방법") { InfoRows(how) })
        if (detail.isExternal) add(StageDetailSection("source", "정보 출처") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                InfoRows(listOf("출처" to (detail.d("sourceName") ?: "웹"), "수집일" to (StageDate.short(detail.d("collectedAt")) ?: "-")))
                Text("무용연이 웹에서 모은 공고예요. 조건과 마감일은 바뀔 수 있으니 지원 전에 원문을 꼭 확인해 주세요.", style = Regular, fontSize = 12.sp, color = MuyeonColors.textSub)
            }
        })
    }
    DetailScaffold(
        title = detail.title,
        shareText = "${detail.title}\n${BuildConfig.API_BASE_URL}/casting/${detail.id}",
        liked = liked,
        onLike = {
            val next = !liked
            liked = next
            scope.launch { if (!api.setScrap(id, next)) liked = !next }
        },
        onBack = onBack,
        sections = sections,
        header = {
            Column {
                HeroGallery(detail.photos, Icons.Filled.Star, Color(0xFF9D8FBB), Color(0xFFF4F1F8))
                Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (closed) chipText("마감", MuyeonColors.textSub, Ink100)()
                        else dl?.let { chipText(it, Blue, Color(0xFFEEF1FF))() }
                        detail.genre?.let { chipText(it, MuyeonColors.textHead, Ink100)() }
                        if (detail.isExternal) chipText("웹 공고", MuyeonColors.textSub, Ink100)()
                    }
                    Text(detail.title, style = Bold, fontSize = 24.sp, color = MuyeonColors.textHead)
                    detail.team?.let { Text(it, style = Regular, fontSize = 16.sp, color = MuyeonColors.textSub) }
                }
                val rows = listOf("일정" to detail.schedule, "장소" to (detail.place ?: detail.region), "마감" to StageDate.short(detail.d("deadline")))
                    .mapNotNull { (k, v) -> v?.let { k to it } }
                if (rows.isNotEmpty()) Box(Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp)) { InfoRows(rows) }
            }
        },
        bottom = {
            val type = ActiveRole.current(ctx)
            when {
                closed -> StageBottomButton("마감된 공고예요", enabled = false) {}
                detail.isExternal -> {
                    val url = detail.d("applyUrl") ?: detail.d("sourceUrl")
                    StageBottomButton(if (detail.d("applyUrl") != null) "원문에서 지원하기" else "원문 공고 보러 가기", enabled = url != null) { url?.let(onOpenUrl) }
                }
                applied -> StageBottomButton("지원한 공고예요", enabled = false) {}
                type == "DANCER" || type == "TEACHER" -> StageBottomButton("이력서로 지원하기", busy = applying) {
                    if (type == "TEACHER") showSwitch = true else scope.launch { startApply() }
                }
                else -> StageBottomButton("무용수 회원만 지원할 수 있어요", enabled = false) {}
            }
        },
    )

    if (showSwitch) AlertDialog(
        onDismissRequest = { showSwitch = false },
        title = { Text("무용수 유형으로 전환해 주세요", style = Bold) },
        text = { Text("캐스팅 공고 지원은 무용수 유형에서 이용할 수 있어요. 무용수 유형으로 전환할까요?", style = Regular) },
        confirmButton = {
            TextButton({
                showSwitch = false
                scope.launch {
                    if (ActiveRole.saveOnServer(ctx, "DANCER")) { ActiveRole.store(ctx, "DANCER"); startApply() }
                    else toast = "유형을 바꾸지 못했어요. 잠시 후 다시 시도해 주세요."
                }
            }) { Text("전환하기", color = Purple) }
        },
        dismissButton = { TextButton({ showSwitch = false }) { Text("취소", color = MuyeonColors.textSub) } },
    )
    if (showNoResume) AlertDialog(
        onDismissRequest = { showNoResume = false },
        title = { Text("저장된 이력서가 없어요. 작성할까요?", style = Bold) },
        confirmButton = { TextButton({ showNoResume = false; onWriteResume() }) { Text("이력서 작성하기", color = Purple) } },
        dismissButton = { TextButton({ showNoResume = false }) { Text("취소", color = MuyeonColors.textSub) } },
    )
    if (showApply) AlertDialog(
        onDismissRequest = { if (!applying) showApply = false },
        title = { Text("지원할까요?", style = Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("선택한 이력서로 이 공고에 지원해요.", style = Regular, fontSize = 14.sp, color = MuyeonColors.textSub)
                resumes.forEach { r ->
                    val on = r.id == resumeId
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).border(1.dp, if (on) Purple else Ink300, RoundedCornerShape(12.dp))
                            .clickable { resumeId = r.id }.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(if (on) Icons.Filled.RadioButtonChecked else Icons.Filled.RadioButtonUnchecked, null, tint = if (on) Purple else MuyeonColors.textSub)
                        Text((r.title?.takeIf { it.isNotBlank() } ?: "이력서") + if (r.isDefault) " (기본)" else "", style = Medium, fontSize = 15.sp, color = MuyeonColors.textHead)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = !applying, onClick = {
                scope.launch {
                    applying = true
                    runCatching { api.apply(id, resumeId) }
                        .onSuccess { applied = true; toast = "지원이 접수됐어요." }
                        .onFailure { toast = it.message ?: "지원에 실패했어요. 잠시 후 다시 시도해 주세요." }
                    applying = false
                    showApply = false
                }
            }) { Text(if (applying) "지원하는 중…" else "지원하기", color = Purple) }
        },
        dismissButton = { TextButton({ showApply = false }) { Text("취소", color = MuyeonColors.textSub) } },
    )
    toast?.let { msg ->
        AlertDialog(onDismissRequest = { toast = null }, text = { Text(msg, style = Regular) },
            confirmButton = { TextButton({ toast = null }) { Text("확인", color = Purple) } })
    }
}

// MARK: 소식 상세

@Composable
fun StageNewsDetailScreen(item: StageNewsItem, cat: StageCategory, onBack: () -> Unit, onOpenUrl: (String) -> Unit) {
    val info = listOf("칸" to cat.label, "일정" to StageDate.short(item.eventDate), "게시일" to StageDate.short(item.publishedDate), "출처" to item.sourceName)
        .mapNotNull { (k, v) -> v?.let { k to it } }
    DetailScaffold(
        title = item.title,
        shareText = "${item.title}\n${item.sourceUrl}",
        liked = null, onLike = null, onBack = onBack,
        sections = listOf(
            StageDetailSection("summary", "소식 요약") { StageExpandableText(item.summary) },
            StageDetailSection("source", "정보 출처") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    InfoRows(info)
                    Text("무용연이 웹에서 모아 원문을 확인한 소식이에요. 일정과 조건은 바뀔 수 있으니 원문을 꼭 확인해 주세요.", style = Regular, fontSize = 12.sp, color = MuyeonColors.textSub)
                }
            },
        ),
        header = {
            Column {
                HeroGallery(listOfNotNull(item.imageUrl), cat.icon, cat.fg, cat.bg)
                Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(cat.label, style = Semi, fontSize = 12.sp, color = cat.fg,
                        modifier = Modifier.clip(CircleShape).background(cat.bg).padding(horizontal = 10.dp, vertical = 4.dp))
                    Text(item.title, style = Bold, fontSize = 24.sp, color = MuyeonColors.textHead)
                    item.sourceName?.let { Text(it, style = Regular, fontSize = 16.sp, color = MuyeonColors.textSub) }
                }
                StageDate.short(item.eventDate)?.let { Box(Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp)) { InfoRows(listOf("일정" to it)) } }
            }
        },
        bottom = { StageBottomButton("공식 원문 확인") { onOpenUrl(item.sourceUrl) } },
    )
}

// MARK: 목록

@Composable
private fun ListBar(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp).clickable(onClick = onBack).semantics { contentDescription = "stage.detail.back" }, Alignment.Center) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MuyeonColors.textHead)
        }
        Text(title, style = Bold, fontSize = 20.sp, color = MuyeonColors.textHead)
    }
}

@Composable
private fun ListRow(image: String?, icon: ImageVector, tint: Color, bg: Color, kicker: String?, title: String, sub: String?, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(Modifier.width(104.dp).height(136.dp).clip(RoundedCornerShape(12.dp)).background(bg), Alignment.Center) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(28.dp))
            stageImageUrl(image)?.let { AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            kicker?.let { Text(it, style = Bold, fontSize = 14.sp, color = Blue) }
            Text(title, style = Semi, fontSize = 17.sp, color = MuyeonColors.textHead, maxLines = 2, overflow = TextOverflow.Ellipsis)
            sub?.takeIf { it.isNotBlank() }?.let { Text(it, style = Regular, fontSize = 14.sp, color = MuyeonColors.textSub, maxLines = 2) }
        }
    }
}

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
        ListBar("캐스팅 공고", onBack)
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
                ListRow(c.imageUrl, Icons.Filled.Star, Color(0xFF9D8FBB), Color(0xFFF4F1F8), StageDate.deadline(c.deadline), c.title,
                    listOfNotNull(c.team, c.genre).joinToString(" · ")) { onOpen(c.id) }
            }
            item {
                Box(Modifier.fillMaxWidth().padding(32.dp), Alignment.Center) {
                    when {
                        loading -> CircularProgressIndicator(color = Purple)
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
        ListBar(code?.let { c -> StageCategory.NEWS.firstOrNull { it.code == c }?.label } ?: "무대 소식 #${issueNo.toString().padStart(3, '0')}", onBack)
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 40.dp)) {
            val i = issue
            if (i == null) {
                item { Box(Modifier.fillMaxWidth().padding(40.dp), Alignment.Center) {
                    if (failed) Text("소식을 불러오지 못했어요.", style = Regular, fontSize = 14.sp, color = MuyeonColors.textSub) else CircularProgressIndicator(color = Purple)
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
                            ListRow(n.imageUrl, c.icon, c.fg, c.bg, StageDate.short(n.eventDate)?.let { "일정 $it" }, n.title, n.sourceName) { onOpen(n, c) }
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
        ListBar("지난 무대 소식", onBack)
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val list = rows
            if (list == null) item { Box(Modifier.fillMaxWidth().padding(40.dp), Alignment.Center) { CircularProgressIndicator(color = Purple) } }
            else if (list.isEmpty()) item { Text("아직 나온 무대 소식이 없어요.", style = Regular, fontSize = 14.sp, color = MuyeonColors.textSub, modifier = Modifier.padding(24.dp)) }
            else items(list, key = { it.issueNo }) { r ->
                val w = StageDate.week(r.weekStart)
                Row(Modifier.fillMaxWidth().height(118.dp).clip(RoundedCornerShape(16.dp)).border(1.dp, Color(0xFFE4DEF2), RoundedCornerShape(16.dp)).clickable { onOpen(r.issueNo) }) {
                    Column(Modifier.width(92.dp).fillMaxHeight().background(Color(0xFF4A2C6B)), Arrangement.Center, Alignment.CenterHorizontally) {
                        Text("${w?.year ?: ""} ${w?.monthEn ?: ""}", style = Semi, fontSize = 10.sp, color = Color.White.copy(alpha = 0.85f))
                        Text("W${w?.week ?: 1}", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 30.sp, color = Color.White)
                        Text(w?.label ?: "", style = Semi, fontSize = 11.sp, color = Color.White)
                    }
                    Column(Modifier.weight(1f).padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("무대 소식 #${r.issueNo.toString().padStart(3, '0')}", style = Bold, fontSize = 11.sp, color = Purple)
                        Text(r.headline, style = Bold, fontSize = 16.sp, color = MuyeonColors.textHead, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(w?.range ?: "", style = Regular, fontSize = 12.sp, color = MuyeonColors.textSub)
                    }
                }
            }
        }
    }
}
