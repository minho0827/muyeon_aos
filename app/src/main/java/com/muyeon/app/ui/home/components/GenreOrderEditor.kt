package com.muyeon.app.ui.home.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import kotlinx.coroutines.launch
import androidx.compose.ui.zIndex
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.muyeon.app.R
import com.muyeon.app.ui.components.SheetAction
import com.muyeon.app.ui.components.SheetButton
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography

private val ROW_HEIGHT = 60.dp
private val ROW_GAP = 8.dp

/**
 * ★ 2026-11-08 홈 장르 순서 편집(사용자 요청) — 줄을 길게 누르면(진동) 들어 올려 위아래로 옮긴다.
 *   한 칸 지날 때마다 짧은 진동. [저장]하면 홈 강사·레슨 위 탭과 장르 그림 줄이 이 순서로 나온다.
 */
@Composable
fun GenreOrderEditor(initial: List<String>, onSave: (List<String>) -> Unit, onReset: () -> Unit, onClose: () -> Unit) {
    val items = remember { mutableStateListOf<String>().apply { addAll(initial) } }
    var dragging by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val haptic = LocalHapticFeedback.current
    val stepPx = with(LocalDensity.current) { (ROW_HEIGHT + ROW_GAP).toPx() }

    Dialog(onClose, DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Column(Modifier.fillMaxSize().background(AppColors.Surface).statusBarsPadding().navigationBarsPadding()) {
            Box(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp), Alignment.Center) {
                Text(stringResource(R.string.home_order_title), style = AppTypography.Title18, color = AppColors.TextHead)
                Icon(Icons.Filled.Close, stringResource(R.string.common_close), tint = AppColors.TextHead,
                    modifier = Modifier.align(Alignment.CenterEnd).size(44.dp).clickable(onClick = onClose).padding(12.dp))
            }
            Text(stringResource(R.string.home_order_desc), style = AppTypography.Body14, color = AppColors.TextSub, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
            // 줄을 장르로 묶어 둔다(key) — 자리를 바꿔도 끌던 손가락이 같은 줄을 따라간다.
            //  ⚠️ LazyColumn 은 첫 줄을 옮기면 그 줄을 따라 목록이 스크롤돼 끌기가 어긋났다 — 줄 수가 적어 Column 으로 둔다.
            Column(Modifier.weight(1f).padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(ROW_GAP)) {
                items.forEach { g ->
                  key(g) {
                    val lifted = dragging == g
                    val lift by animateFloatAsState(if (lifted) 1f else 0f, label = "lift")
                    // 밀려나는 줄 — 자리를 옮길 때 살짝 작아졌다가 새 자리에서 다시 커진다(사용자 요청).
                    val pulse = remember { Animatable(1f) }
                    val index = items.indexOf(g)
                    var lastIndex by remember { mutableIntStateOf(index) }
                    // 밀려난 줄은 옛 자리에서 새 자리로 한 칸 미끄러진다(순간이동처럼 보이지 않게).
                    val slide = remember { Animatable(0f) }
                    LaunchedEffect(index) {
                        if (index != lastIndex && !lifted) {
                            slide.snapTo((lastIndex - index) * stepPx)
                            launch { slide.animateTo(0f, tween(180)) }
                            pulse.animateTo(0.92f, tween(90))
                            pulse.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 600f))
                        }
                        lastIndex = index
                    }
                    Row(
                        Modifier.zIndex(if (lifted) 1f else 0f)
                            .fillMaxWidth().height(ROW_HEIGHT)
                            .graphicsLayer {
                                translationY = if (lifted) dragOffset else slide.value
                                val sc = (1f + 0.03f * lift) * pulse.value
                                scaleX = sc; scaleY = sc
                            }
                            .shadow((8 * lift).dp, RoundedCornerShape(14.dp))
                            .background(AppColors.Surface, RoundedCornerShape(14.dp))
                            .border(1.dp, if (lifted) AppColors.TextHead else AppColors.ChipBorder, RoundedCornerShape(14.dp))
                            .pointerInput(g) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { dragging = g; dragOffset = 0f; haptic.performHapticFeedback(HapticFeedbackType.LongPress) },
                                    onDragEnd = { dragging = null; dragOffset = 0f },
                                    onDragCancel = { dragging = null; dragOffset = 0f },
                                    onDrag = { change, amount ->
                                        change.consume()
                                        dragOffset += amount.y
                                        val i = items.indexOf(g)
                                        // 한 칸의 절반을 넘으면 이웃과 자리를 바꾸고 진동.
                                        if (dragOffset > stepPx / 2 && i < items.lastIndex) {
                                            items.add(i + 1, items.removeAt(i)); dragOffset -= stepPx
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        } else if (dragOffset < -stepPx / 2 && i > 0) {
                                            items.add(i - 1, items.removeAt(i)); dragOffset += stepPx
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                    },
                                )
                            }
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(g, style = AppTypography.Body15Medium, color = AppColors.TextHead, modifier = Modifier.weight(1f))
                        Icon(Icons.Rounded.DragHandle, null, tint = AppColors.TextHint)
                    }
                  }
                }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SheetButton(SheetAction(stringResource(R.string.home_order_reset), false) { onReset() }, Modifier.weight(1f))
                SheetButton(SheetAction(stringResource(R.string.home_order_save), true) { onSave(items.toList()) }, Modifier.weight(1f))
            }
        }
    }
}
