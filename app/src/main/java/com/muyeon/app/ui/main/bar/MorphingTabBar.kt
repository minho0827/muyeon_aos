package com.muyeon.app.ui.main.bar

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** 캡슐 안쪽 높이(PaceERA BAR_HEIGHT). */
private val BAR_HEIGHT = 72.dp
/** 사라진 칸의 크기 — 나타날 때 여기서 커지며 살짝 튕긴다(PaceERA HIDDEN_SCALE). */
private const val HIDDEN_SCALE = 0.5f
private const val FADE_MS = 140

/** 칸 하나의 지금 값(가운데 위치 0~1 · 투명도 · 크기). */
private class ItemAnim(item: BarItem, mode: BarMode) {
    val center = Animatable(item.centerIn(mode))
    val alpha = Animatable(if (item.visibleIn(mode)) 1f else 0f)
    val scale = Animatable(if (item.visibleIn(mode)) 1f else HIDDEN_SCALE)
}

/**
 * ★ 2026-11-08 모양이 바뀌는 탭바 — PaceERA MorphingTabBar 를 무용연 4모드로 옮긴 것. 캡슐 **안쪽**만 그린다
 *   (붙은 바 ↔ 떠 있는 캡슐 모양은 [com.muyeon.app.ui.main.FloatingTabBar]).
 *   칸 수가 모드마다 달라(5·3·4·3) 위치는 '탭바 폭 대비 가운데'로 움직인다. 도중에 반대로 눌러도 지금 값에서 되돌아간다.
 *
 * @param label 칸 이름(부모 칸은 모드에 따라 다르다 — 구인구직/채용, 공간/대관).
 * @param icon 칸 그림(BACK 은 쓰지 않는다).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MorphingTabBar(
    mode: BarMode,
    selected: BarItem?,
    label: @Composable (BarItem, BarMode) -> String,
    icon: @Composable (BarItem) -> Painter,
    dotOn: (BarItem) -> Boolean,
    onItemClick: (BarItem) -> Unit,
    modifier: Modifier = Modifier,
    onItemLongClick: ((BarItem) -> Unit)? = null,
    onItemDoubleClick: ((BarItem) -> Unit)? = null,
    /**
     * 길게·두 번 누르기를 받는 칸(MY — 유형 시트·유형 바꾸기). 나머지 칸은 보통 누르기만.
     *   ⚠️ 두 번 누르기를 받는 칸은 첫 누르기를 두 번째가 오는지 기다렸다(약 0.3초) 처리한다 — 모든 칸에 걸면 탭바가 늦게 반응한다.
     */
    gestureItems: Set<BarItem> = emptySet(),
    backDescription: String,
) {
    val anims = remember { BarItem.entries.associateWith { ItemAnim(it, mode) } }
    LaunchedEffect(mode) {
        coroutineScope {
            BarItem.entries.forEach { item ->
                val a = anims.getValue(item)
                val show = item.visibleIn(mode)
                launch { a.center.animateTo(item.centerIn(mode), spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)) }
                launch {
                    // 나타나는 칸만 차례를 기다린다. 이미 반쯤 보이면 기다리지 않는다.
                    if (show && a.alpha.value < 0.01f) delay(item.enterDelayMs(mode).toLong())
                    launch { a.alpha.animateTo(if (show) 1f else 0f, tween(FADE_MS, easing = if (show) FastOutSlowInEasing else FastOutLinearInEasing)) }
                    a.scale.animateTo(
                        if (show) 1f else HIDDEN_SCALE,
                        if (show) spring(dampingRatio = 0.45f, stiffness = 380f) else tween(FADE_MS, easing = FastOutLinearInEasing),
                    )
                }
            }
        }
    }

    BoxWithConstraints(modifier.fillMaxWidth().height(BAR_HEIGHT).padding(horizontal = 6.dp)) {
        val itemWidth = maxWidth / BarMode.MAIN.slotCount
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val itemPx = with(density) { itemWidth.toPx() }
        BarItem.entries.forEach { item ->
            val a = anims.getValue(item)
            if (!item.visibleIn(mode) && a.alpha.value <= 0f) return@forEach
            val interactive = item.visibleIn(mode)
            val isSelected = selected == item
            Box(
                Modifier
                    .width(itemWidth)
                    .fillMaxHeight()
                    .offset { IntOffset((a.center.value * widthPx - itemPx / 2f).roundToInt(), 0) }
                    .graphicsLayer { alpha = a.alpha.value; scaleX = a.scale.value; scaleY = a.scale.value }
                    .then(
                        if (interactive) Modifier
                            .clip(RoundedCornerShape(percent = 50))
                            .then(
                                if (item in gestureItems) Modifier.combinedClickable(
                                    role = Role.Tab,
                                    onClick = { onItemClick(item) },
                                    onLongClick = onItemLongClick?.let { f -> { f(item) } },
                                    onDoubleClick = onItemDoubleClick?.let { f -> { f(item) } },
                                ) else Modifier.clickable(role = Role.Tab) { onItemClick(item) },
                            )
                            .semantics { this.selected = isSelected }
                        else Modifier,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (item == BarItem.BACK) {
                    // 토스증권처럼 왼쪽 동그란 ← (회색 원).
                    Box(Modifier.size(52.dp).clip(CircleShape).background(AppColors.SurfaceMuted), Alignment.Center) {
                        Icon(androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack, backDescription, tint = AppColors.TextHead, modifier = Modifier.size(24.dp))
                    }
                } else {
                    LabeledTab(icon(item), label(item, mode), isSelected, dotOn(item))
                }
            }
        }
    }
}

/** 칸 — 토스처럼 알약 표시 없이 색으로만 고른 칸을 보인다(고른 칸 주황, 나머지 회색). */
@Composable
private fun LabeledTab(icon: Painter, label: String, selected: Boolean, dot: Boolean) {
    val tint by animateColorAsState(if (selected) AppColors.Primary else AppColors.TextHint, label = "tabTint")
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(26.dp))
            if (dot) Box(Modifier.align(Alignment.TopEnd).size(6.dp).clip(CircleShape).background(AppColors.Badge))
        }
        Text(label, style = AppTypography.Caption12, color = tint, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp))
    }
}
