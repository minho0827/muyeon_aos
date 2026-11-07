package com.muyeon.app.ui.components

import androidx.compose.animation.core.animateRectAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.muyeon.app.R
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import kotlin.math.roundToInt

/**
 * 튜토리얼 대상 위치 모음 — 웹 data-tut 앵커. 화면이 [Modifier.tutorialAnchor] 로 키를 단 요소의 위치를 적는다.
 *   같은 키가 여러 개면(카드마다) 처음 그려진 것 하나만 쓴다(웹 querySelector 와 같다).
 */
class TutorialAnchors {
    internal val rects = mutableStateMapOf<String, Rect>()
}

val LocalTutorialAnchors = staticCompositionLocalOf<TutorialAnchors?> { null }

fun Modifier.tutorialAnchor(anchors: TutorialAnchors?, key: String): Modifier =
    if (anchors == null) this else onGloballyPositioned { c ->
        val r = c.boundsInRoot()
        if (r.width > 0f && (anchors.rects[key] == null || anchors.rects[key]!!.top >= r.top - 1f || anchors.rects[key]!!.top < 0f)) anchors.rects[key] = r
    }

/** 한 단계 — 강조할 앵커 키와 문구. */
data class TutorialStep(val anchor: String, val text: String)

/**
 * 첫 방문 튜토리얼 — 웹 components/muyeon/TutorialOverlay.js. 어두운 막(62%) + 대상 둘레 주황 테두리 구멍 + 번호 말풍선.
 *   아무 곳이나 누르거나 왼쪽으로 밀면 다음, 오른쪽으로 밀면 이전, 마지막에서 다음 = 닫기. 오른쪽 위 [건너뛰기].
 */
@Composable
fun TutorialOverlay(steps: List<TutorialStep>, anchors: TutorialAnchors, onClose: () -> Unit) {
    var idx by remember { mutableIntStateOf(0) }
    // 튜토리얼 동안 채팅·AI 플로팅을 숨긴다(아래 [다음]을 가린다 — 웹은 막 아래에 깔린다).
    androidx.compose.runtime.DisposableEffect(Unit) {
        com.muyeon.app.ui.floating.FloatingState.updateFloatingHidden(true)
        onDispose { com.muyeon.app.ui.floating.FloatingState.updateFloatingHidden(false) }
    }
    val step = steps.getOrNull(idx) ?: return
    val density = LocalDensity.current
    val pad = with(density) { PAD.toPx() }
    val target = anchors.rects[step.anchor]?.let { Rect(it.left - pad, it.top - pad, it.right + pad, it.bottom + pad) }
    val rect by animateRectAsState(target ?: Rect.Zero, label = "tutRect")
    val next = { if (idx < steps.lastIndex) idx++ else onClose() }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .pointerInput(idx) {
                var dx = 0f
                detectHorizontalDragGestures(
                    onDragStart = { dx = 0f },
                    onHorizontalDrag = { _, d -> dx += d },
                    onDragEnd = { if (dx < -40) next() else if (dx > 40 && idx > 0) idx-- },
                )
            }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { next() },
    ) {
        val hPx = with(density) { maxHeight.toPx() }
        Canvas(Modifier.fillMaxSize().graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)) {
            drawRect(Color.Black.copy(alpha = 0.62f))
            if (target != null) {
                val corner = CornerRadius(RADIUS.toPx())
                drawRoundRect(Color.Transparent, rect.topLeft, Size(rect.width, rect.height), corner, blendMode = BlendMode.Clear)
                drawRoundRect(AppColors.Primary, rect.topLeft, Size(rect.width, rect.height), corner, style = Stroke(2.dp.toPx()))
            }
        }
        // 말풍선 — 대상 아래 공간이 있으면 아래, 아니면 위(웹 below 규칙).
        val below = target == null || target.bottom + with(density) { 140.dp.toPx() } < hPx
        val bubbleY = when {
            target == null -> hPx * 0.4f
            below -> target.bottom + with(density) { 18.dp.toPx() }
            else -> target.top - with(density) { 18.dp.toPx() + 96.dp.toPx() }
        }
        Row(
            Modifier
                .align(Alignment.TopCenter)
                .offset { IntOffset(0, bubbleY.roundToInt()) }
                .widthIn(max = 300.dp)
                .padding(horizontal = 24.dp)
                .shadow(12.dp, RoundedCornerShape(14.dp))
                .background(Color.White.copy(alpha = 0.97f), RoundedCornerShape(14.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (steps.size > 1) {
                Box(Modifier.size(24.dp).background(AppColors.Primary, CircleShape), contentAlignment = Alignment.Center) {
                    Text("${idx + 1}", style = AppTypography.Body14SemiBold, color = Color.White)
                }
            }
            Text(step.text, style = AppTypography.Body15Medium, color = AppColors.TextHead)
        }
        Text(
            stringResource(R.string.tutorial_skip),
            style = AppTypography.CardTitle16,
            color = Color.White,
            modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(12.dp).clickable(onClick = onClose).padding(8.dp),
        )
        if (steps.size > 1) {
            Row(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(stringResource(R.string.tutorial_prev), style = AppTypography.CardTitle16, color = if (idx > 0) Color.White else Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.clickable(enabled = idx > 0) { idx-- }.padding(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    steps.indices.forEach { i -> Box(Modifier.size(if (i == idx) 8.dp else 6.dp).background(if (i == idx) AppColors.Primary else Color.White.copy(alpha = 0.5f), CircleShape)) }
                }
                Text(stringResource(if (idx < steps.lastIndex) R.string.tutorial_next else R.string.tutorial_done), style = AppTypography.CardTitle16, color = Color.White,
                    modifier = Modifier.clickable { next() }.padding(8.dp))
            }
        }
    }
}

private val PAD = 6.dp
private val RADIUS = 14.dp
