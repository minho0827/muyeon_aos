package com.muyeon.app.ui.castingstage.components

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
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.muyeon.app.ui.common.MuyeonColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.muyeon.app.ui.castingstage.model.*
import com.muyeon.app.ui.castingstage.data.*
import com.muyeon.app.ui.castingstage.screens.*

/** 상세 틀 — 큰 사진·정보 줄·붙는 탭·섹션·아래 고정 버튼(iOS StageDetailScaffold·StageHeroGallery·StageInfoRows·StageBottomButton·StageExpandableText). */

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
internal fun StageInfoRows(rows: List<Pair<String, String>>) {
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
        Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(16.dp)).background(if (enabled) Brand else Ink200)
            .clickable(enabled = enabled && !busy, onClick = onClick).semantics { contentDescription = "stage.detail.cta" },
        Alignment.Center,
    ) {
        if (busy) CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
        else Text(title, style = Bold, fontSize = 17.sp, color = if (enabled) Color.White else MuyeonColors.textSub)
    }
}

@Composable
internal fun StageHeroGallery(urls: List<String>, icon: ImageVector, tint: Color, bg: Color, placeholder: Int? = null) {
    Box(Modifier.fillMaxWidth().height(300.dp).padding(horizontal = 16.dp).clip(RoundedCornerShape(18.dp)).background(bg), Alignment.Center) {
        if (urls.isEmpty()) {
            if (placeholder != null) androidx.compose.foundation.Image(androidx.compose.ui.res.painterResource(placeholder), null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            else Icon(icon, null, tint = tint, modifier = Modifier.size(48.dp))
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
internal fun StageDetailScaffold(
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
    val passLine = with(density) { (TAB_HEIGHT_DP + SECTION_TOP_DP + 24).dp.roundToPx() }
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
                    Column(Modifier.fillMaxWidth().height(TAB_HEIGHT_DP.dp).background(Color.White)) {
                        if (pastHeader) {
                            Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                                sections.forEach { s ->
                                    val on = s.id == current
                                    Column(Modifier.fillMaxHeight().clickable { jump(s.id) }.semantics { contentDescription = "stage.detail.tab.${s.id}${if (on) ".selected" else ""}" }) {
                                        Box(Modifier.weight(1f), Alignment.Center) {
                                            Text(s.title, style = if (on) Bold else Regular, fontSize = 16.sp, color = if (on) Brand else MuyeonColors.textSub)
                                        }
                                        Box(Modifier.fillMaxWidth().height(3.dp).background(if (on) Brand else Color.Transparent))
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

internal fun stageChipText(t: String, fg: Color, bg: Color): @Composable () -> Unit = {
    Text(t, style = Semi, fontSize = 12.sp, color = fg, modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(bg).padding(horizontal = 8.dp, vertical = 4.dp))
}

// MARK: 캐스팅 상세
