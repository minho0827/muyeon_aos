package com.muyeon.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.muyeon.app.theme.customFontFamily

/**
 * 목록·내역 화면 공용 틀(2026-10-27) — iOS ShareUI/MuyeonListKit.swift 와 같은 치수.
 *  제목줄(‹ + 왼쪽 굵은 제목) · 캡슐 탭(고름=검정) · 회색 바탕 위 흰 둥근 카드 · 묶음 메뉴 · 바로가기 카드 ·
 *  다가오는 일정 카드 · 안내 한 줄 · "＋ 글자" 링크 · 빈 화면.
 *  캐스팅·무대(StageListBar·장르 칩)에서 먼저 쓰던 모양을 일반화했다.
 */
object MuyeonList {
    /** #F3F4F7 — 목록 화면 바탕(흰 카드가 떠 보이는 옅은 회색). iOS Color.muyeonPageBg. */
    val pageBg = Color(0xFFF3F4F7)
    val ink100 = Color(0xFFF8F8F8)
    val ink200 = Color(0xFFEAEAEA)
    val ink300 = Color(0xFFDADADA)
    val gutter = 16.dp
    val cardShape = RoundedCornerShape(16.dp)
}

private fun pt(weight: FontWeight, size: Int) =
    androidx.compose.ui.text.TextStyle(fontFamily = customFontFamily, fontWeight = weight, fontSize = size.sp)

// ───────── A 제목줄 ─────────

/** ‹(또는 ✕) + 왼쪽 정렬 굵은 제목 + 오른쪽 칸. onBack·onClose 둘 다 null 이면 제목만. */
@Composable
fun MuyeonListHeader(
    title: String,
    onBack: (() -> Unit)? = null,
    onClose: (() -> Unit)? = null,
    backTag: String = "list.back",
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth().background(MuyeonColors.surface).height(52.dp).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val action = onBack ?: onClose
        if (action != null) {
            Box(
                Modifier.size(44.dp).clickable(onClick = action).semantics { contentDescription = backTag },
                Alignment.Center,
            ) {
                Icon(
                    if (onBack != null) Icons.AutoMirrored.Filled.KeyboardArrowLeft else Icons.Filled.Close, null,
                    tint = MuyeonColors.textHead, modifier = Modifier.size(if (onBack != null) 30.dp else 24.dp),
                )
            }
        } else {
            Spacer(Modifier.width(12.dp))
        }
        Text(
            title, style = pt(FontWeight.Bold, 20), color = MuyeonColors.textHead, maxLines = 1,
            overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
        )
        trailing?.invoke(this)
    }
}

/** 제목줄 오른쪽 아이콘 버튼(44dp). */
@Composable
fun MuyeonHeaderIcon(icon: ImageVector, label: String, tint: Color = MuyeonColors.textHead, onClick: () -> Unit) {
    Box(Modifier.size(44.dp).clickable(onClick = onClick).semantics { contentDescription = label }, Alignment.Center) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(24.dp))
    }
}

// ───────── A 캡슐 탭 ─────────

data class MuyeonPillTab<T>(val id: T, val title: String, val count: Int? = null)

/**
 * 가로로 미는 캡슐 탭 + 아래 구분선. 고른 탭은 검정 바탕 흰 글자, 나머지는 연회색 바탕.
 *  고른 탭이 화면 밖이면 보이게 굴린다. trailing 은 탭 줄 오른쪽 작은 글자(예: 모두 읽음).
 *  각 캡슐 contentDescription = "$tagPrefix.$title"(+".selected").
 */
@Composable
fun <T> MuyeonPillTabs(
    tabs: List<MuyeonPillTab<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    tagPrefix: String? = null,
    showDivider: Boolean = true,
    /** 작은 크기(높이 36·글자 14) — 섹션 안에 끼는 줄(캐스팅·무대 장르 칩)용. iOS compact 와 같다. */
    compact: Boolean = false,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    val state = rememberLazyListState()
    val idx = tabs.indexOfFirst { it.id == selected }
    LaunchedEffect(idx) { if (idx >= 0) state.animateScrollToItem(idx, -120) }
    Column(Modifier.fillMaxWidth().background(MuyeonColors.surface)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LazyRow(
                state = state, modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = MuyeonList.gutter, vertical = if (compact) 0.dp else 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(tabs) { _, tab ->
                    val on = tab.id == selected
                    Row(
                        Modifier.height(if (compact) 36.dp else 40.dp).clip(CircleShape)
                            .background(if (on) MuyeonColors.textHead else MuyeonList.ink100)
                            .clickable { onSelect(tab.id) }
                            .semantics {
                                this.selected = on
                                tagPrefix?.let { contentDescription = "$it.${tab.title}" + if (on) ".selected" else "" }
                            }
                            .padding(horizontal = if (compact) 16.dp else 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        val c = if (on) Color.White else MuyeonColors.textHead
                        val size = if (compact) 14 else 15
                        Text(tab.title, style = pt(if (on) FontWeight.SemiBold else FontWeight.Medium, size), color = c, maxLines = 1)
                        tab.count?.let { Text("$it", style = pt(if (on) FontWeight.SemiBold else FontWeight.Medium, size), color = c.copy(alpha = if (on) 0.85f else 0.6f)) }
                    }
                }
            }
            trailing?.invoke(this)
        }
        if (showDivider) HorizontalDivider(color = MuyeonList.ink200, thickness = 1.dp)
    }
}

// ───────── B 흰 카드·묶음 메뉴 ─────────

/** 회색 바탕 위 흰 둥근 카드(그림자 없음). */
fun Modifier.muyeonPanel(padding: Int = 16): Modifier =
    this.fillMaxWidth().clip(MuyeonList.cardShape).background(MuyeonColors.surface).padding(padding.dp)

/** 카드 위 굵은 묶음 제목. trailing 은 오른쪽 작은 회색 글이나 링크. */
@Composable
fun MuyeonSectionTitle(title: String, trailing: (@Composable RowScope.() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = pt(FontWeight.Bold, 18), color = MuyeonColors.textHead, modifier = Modifier.weight(1f))
        trailing?.invoke(this)
    }
}

/** 묶음 제목 + 흰 카드 안 줄들(줄 사이 선 없음). */
@Composable
fun MuyeonGroupSection(title: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        title?.let { MuyeonSectionTitle(it) }
        Column(Modifier.fillMaxWidth().clip(MuyeonList.cardShape).background(MuyeonColors.surface).padding(vertical = 8.dp), content = content)
    }
}

/** 묶음 카드 안 한 줄 — 왼쪽 굵은 제목(+아래 회색 설명), 오른쪽 회색 값, 끝에 ›(누를 수 있을 때). */
@Composable
fun MuyeonGroupRow(
    title: String,
    subtitle: String? = null,
    value: String? = null,
    valueColor: Color = MuyeonColors.textSub,
    showChevron: Boolean = true,
    onClick: (() -> Unit)? = null,
    accessory: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth().let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = pt(FontWeight.SemiBold, 16), color = MuyeonColors.textHead)
            subtitle?.let { Text(it, style = pt(FontWeight.Normal, 13), color = MuyeonColors.textSub) }
        }
        value?.let { Text(it, style = pt(FontWeight.Normal, 14), color = valueColor, maxLines = 1) }
        accessory?.invoke(this)
        if (showChevron && onClick != null) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MuyeonList.ink300, modifier = Modifier.size(20.dp))
        }
    }
}

// ───────── D 바로가기 카드 ─────────

data class MuyeonShortcut(val icon: ImageVector, val title: String, val badge: Int = 0, val onClick: () -> Unit)

/** 흰 카드 하나에 아이콘+이름을 한 줄에 최대 3개. 넘치면 다음 줄. */
@Composable
fun MuyeonShortcutCard(items: List<MuyeonShortcut>) {
    Column(
        Modifier.fillMaxWidth().clip(MuyeonList.cardShape).background(MuyeonColors.surface).padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { item ->
                    Column(
                        Modifier.weight(1f).clickable(onClick = item.onClick).semantics { contentDescription = "shortcut.${item.title}" }
                            .padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box {
                            Icon(item.icon, null, tint = MuyeonColors.textHead, modifier = Modifier.size(28.dp))
                            if (item.badge > 0) {
                                Box(
                                    Modifier.align(Alignment.TopEnd).offset(x = 12.dp, y = (-6).dp).widthIn(min = 16.dp).height(16.dp)
                                        .clip(CircleShape).background(MuyeonColors.primary).padding(horizontal = 4.dp),
                                    Alignment.Center,
                                ) { Text(if (item.badge > 99) "99+" else "${item.badge}", style = pt(FontWeight.Bold, 10), color = Color.White) }
                            }
                        }
                        Text(item.title, style = pt(FontWeight.Normal, 14), color = MuyeonColors.textHead, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

// ───────── E 다가오는 일정 카드 ─────────

data class MuyeonHighlightItem(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val meta: String? = null,
    val metaTint: Color = MuyeonColors.textHead,
    val imageUrl: String? = null,
    val icon: ImageVector = Icons.Filled.DateRange,
    val onClick: () -> Unit,
)

/** 제목 줄(오른쪽 작은 회색 글) + 썸네일·굵은 제목·회색 부제·메타·› 행들. 행 사이 선 없음. */
@Composable
fun MuyeonHighlightCard(title: String, items: List<MuyeonHighlightItem>, side: String? = null, onSide: (() -> Unit)? = null) {
    Column(Modifier.fillMaxWidth().clip(MuyeonList.cardShape).background(MuyeonColors.surface).padding(20.dp)) {
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = pt(FontWeight.Bold, 20), color = MuyeonColors.textHead, modifier = Modifier.weight(1f))
            side?.let { s ->
                Row(
                    Modifier.let { if (onSide != null) it.clickable(onClick = onSide) else it },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(s, style = pt(FontWeight.Normal, 13), color = MuyeonColors.textSub)
                    if (onSide != null) Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MuyeonColors.textSub, modifier = Modifier.size(16.dp))
                }
            }
        }
        items.forEach { item ->
            Row(
                Modifier.fillMaxWidth().clickable(onClick = item.onClick).padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)).background(MuyeonColors.primary.copy(alpha = 0.1f)), Alignment.Center) {
                    Icon(item.icon, null, tint = MuyeonColors.primary, modifier = Modifier.size(22.dp))
                    item.imageUrl?.let { AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(item.title, style = pt(FontWeight.Bold, 16), color = MuyeonColors.textHead, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    item.subtitle?.let { Text(it, style = pt(FontWeight.Normal, 14), color = MuyeonColors.textSub, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    item.meta?.let { Text(it, style = pt(FontWeight.SemiBold, 14), color = item.metaTint, maxLines = 1) }
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MuyeonColors.textHead, modifier = Modifier.size(22.dp))
            }
        }
    }
}

// ───────── F 안내 한 줄·＋ 링크 ─────────

/** 흰 카드 한 줄 안내(›). highlight 는 앞에 주황 굵게 붙는다. */
@Composable
fun MuyeonInfoRow(text: String, onClick: () -> Unit, detail: String? = null, icon: ImageVector? = null, highlight: String? = null) {
    Row(
        Modifier.fillMaxWidth().clip(MuyeonList.cardShape).background(MuyeonColors.surface).clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        icon?.let { Icon(it, null, tint = MuyeonColors.primary, modifier = Modifier.size(22.dp)) }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                buildAnnotatedString {
                    highlight?.let { withStyle(SpanStyle(color = MuyeonColors.primary, fontWeight = FontWeight.Bold)) { append("$it ") } }
                    append(text)
                },
                style = pt(FontWeight.SemiBold, 16), color = MuyeonColors.textHead,
            )
            detail?.let { Text(it, style = pt(FontWeight.Normal, 13), color = MuyeonColors.textSub) }
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MuyeonColors.textHead, modifier = Modifier.size(22.dp))
    }
}

/** 목록 위 오른쪽 "＋ 글자" 링크(회색 동그라미 안 ＋). */
@Composable
fun MuyeonAddLink(title: String, onClick: () -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick).padding(vertical = 6.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(24.dp).clip(CircleShape).background(MuyeonList.ink200), Alignment.Center) {
            Icon(Icons.Filled.Add, null, tint = MuyeonColors.textHead, modifier = Modifier.size(14.dp))
        }
        Text(title, style = pt(FontWeight.Medium, 15), color = MuyeonColors.textHead)
    }
}

// ───────── C 빈 화면 ─────────

/** 가운데 작은 회색 선 아이콘 + 제목 + 회색 설명 + 주황 둥근 사각 버튼(선택). iOS MuyeonEmptyState. */
@Composable
fun MuyeonEmptyState(
    title: String,
    message: String? = null,
    icon: ImageVector = Icons.AutoMirrored.Filled.FormatListBulleted,
    actionTitle: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, null, tint = MuyeonList.ink300, modifier = Modifier.size(44.dp))
        Spacer(Modifier.height(18.dp))
        Text(title, style = pt(FontWeight.Medium, 17), color = MuyeonColors.textHead, textAlign = TextAlign.Center)
        message?.let {
            Spacer(Modifier.height(4.dp))
            Text(it, style = pt(FontWeight.Normal, 14), color = MuyeonColors.textSub, textAlign = TextAlign.Center)
        }
        if (actionTitle != null && onAction != null) {
            Spacer(Modifier.height(18.dp))
            Box(
                Modifier.height(46.dp).clip(RoundedCornerShape(12.dp)).background(MuyeonColors.primary)
                    .clickable(onClick = onAction).testTag("empty.action").semantics { contentDescription = "empty.action" }
                    .padding(horizontal = 22.dp),
                Alignment.Center,
            ) { Text(actionTitle, style = pt(FontWeight.SemiBold, 16), color = Color.White) }
        }
    }
}
