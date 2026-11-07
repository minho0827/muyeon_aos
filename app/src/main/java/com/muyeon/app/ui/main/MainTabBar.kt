package com.muyeon.app.ui.main

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.Dimensions

/**
 * 하단 탭바 — 웹 components/ui/Footer.js + _bottom-tab-bar.scss 를 옮겼다(그리기 전용, 상태 없음).
 *   흰 바탕 · 위 1px 선(#DFDFDF) · 칸마다 아이콘 28 + 글자 13 semibold. 선택 = 주황, 아니면 회색.
 *
 *   [onLongPress]·[onDoubleTap] 은 해당 칸에서만 켠다(MY: 로그인 회원 길게 누르기 = 유형 시트, 두 번 누르기 = 다른 유형).
 *   두 번 누르기를 켠 칸은 한 번 누르기가 두 번 누르기 판정 시간만큼 늦다(웹 350ms 판정과 같다).
 */
@Composable
fun MainTabBar(
    items: List<TabBarItem>,
    selectedKey: String?,
    onTap: (String) -> Unit,
    modifier: Modifier = Modifier,
    longPressKeys: Set<String> = emptySet(),
    doubleTapKeys: Set<String> = emptySet(),
    onLongPress: (String) -> Unit = {},
    onDoubleTap: (String) -> Unit = {},
) {
    Column(modifier.fillMaxWidth().background(AppColors.Surface).navigationBarsPadding()) {
        HorizontalDivider(thickness = 1.dp, color = AppColors.DividerStrong)
        Row(
            Modifier
                .fillMaxWidth()
                .height(Dimensions.tabBarHeight)
                .padding(horizontal = Dimensions.tabBarPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { item ->
                MainTabButton(
                    item = item,
                    selected = item.key == selectedKey,
                    onTap = { onTap(item.key) },
                    onLongPress = if (item.key in longPressKeys) ({ onLongPress(item.key) }) else null,
                    onDoubleTap = if (item.key in doubleTapKeys) ({ onDoubleTap(item.key) }) else null,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MainTabButton(
    item: TabBarItem,
    selected: Boolean,
    onTap: () -> Unit,
    onLongPress: (() -> Unit)?,
    onDoubleTap: (() -> Unit)?,
) {
    val tint = if (selected) AppColors.Primary else AppColors.IconOff
    val textColor = if (selected) AppColors.Primary else AppColors.TextHint
    Column(
        Modifier
            .width(Dimensions.tabButtonWidth)
            .testTag("tab_${item.key}")
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onTap,
                onLongClick = onLongPress,
                onDoubleClick = onDoubleTap,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimensions.tabIconGap),
    ) {
        Box {
            Icon(painterResource(item.iconRes), contentDescription = null, tint = tint, modifier = Modifier.size(Dimensions.tabIconSize))
            if (item.showDot) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 2.dp, y = (-1).dp)
                        .size(DOT)
                        .clip(CircleShape)
                        .background(AppColors.Badge),
                )
            }
        }
        Text(item.label, style = AppTypography.Tab13, color = textColor, maxLines = 1, overflow = TextOverflow.Clip)
    }
}

private val DOT = 8.dp
