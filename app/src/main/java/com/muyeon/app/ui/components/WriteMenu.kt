package com.muyeon.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.muyeon.app.R
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography

/** 글쓰기 메뉴 한 줄. */
data class WriteMenuItem(val label: String, val icon: Painter, val tint: Color, val tag: String, val onClick: () -> Unit)

/**
 * ★ 2026-11-08 [+ 글쓰기] 떠 있는 버튼 — PaceERA 런커 BoardWriteMenu(iOS·AOS 같은 모양)를 가져왔다(사용자 요청).
 *   누르면 화면이 어두워지고 버튼 바로 위에 메뉴가 커지며 펼쳐진다. 버튼은 알약 → 동그라미로 줄며 + 가 45° 돌아 ✕.
 *   채용 2계층(채용·대타공고·캐스팅)이 함께 쓴다 — 위 바 연필 버튼 대신.
 */
@Composable
fun BoxScope.WriteMenu(items: List<WriteMenuItem>, bottom: Dp, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    AnimatedVisibility(open, enter = fadeIn(), exit = fadeOut()) {
        Box(
            Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { open = false },
        )
    }
    Column(
        modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = bottom),
        horizontalAlignment = Alignment.End,
    ) {
        AnimatedVisibility(
            visible = open,
            // 버튼 바로 위 오른쪽 아래 모서리에서 살짝 커지며 나타난다(이동 없음).
            enter = fadeIn() + scaleIn(initialScale = 0.85f, transformOrigin = TransformOrigin(1f, 1f)),
            exit = fadeOut() + scaleOut(targetScale = 0.85f, transformOrigin = TransformOrigin(1f, 1f)),
        ) {
            Column(
                Modifier.padding(bottom = 12.dp).width(212.dp).clip(RoundedCornerShape(16.dp))
                    .background(AppColors.Surface).padding(vertical = 8.dp).testTag("writeMenu"),
            ) {
                items.forEach { item ->
                    Row(
                        Modifier.clickable { open = false; item.onClick() }.padding(horizontal = 18.dp, vertical = 12.dp).testTag("writeMenu_${item.tag}"),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(item.icon, null, tint = item.tint, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(14.dp))
                        Text(item.label, style = AppTypography.Body15Medium.copy(fontSize = AppTypography.Body15Medium.fontSize * 17 / 15), color = AppColors.TextHead)
                    }
                }
            }
        }
        val rotation by animateFloatAsState(if (open) 45f else 0f, label = "writeRotate")
        val container by animateColorAsState(if (open) AppColors.Surface else AppColors.Primary, label = "writeBg")
        val content by animateColorAsState(if (open) AppColors.TextHead else AppColors.TextOnPrimary, label = "writeFg")
        Row(
            Modifier.shadow(6.dp, CircleShape).clip(CircleShape).background(container)
                .clickable { open = !open }.animateContentSize().heightIn(min = 56.dp)
                .padding(horizontal = if (open) 16.dp else 20.dp, vertical = 16.dp).testTag("writeButton"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Add, stringResource(if (open) R.string.common_close else R.string.write_menu_open), tint = content,
                modifier = Modifier.size(24.dp).rotate(rotation))
            AnimatedVisibility(
                visible = !open,
                enter = fadeIn() + expandHorizontally(expandFrom = Alignment.Start),
                exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.Start),
            ) {
                Text(stringResource(R.string.write_menu_open), style = AppTypography.Section16, color = content, modifier = Modifier.padding(start = 6.dp))
            }
        }
    }
}
