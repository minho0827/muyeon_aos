package com.muyeon.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.muyeon.app.R
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.Dimensions

/**
 * 화면 위 바 — 웹 components/ui/Header.js + _top-bar.scss.
 *   [primary] = 주황 바탕 흰 글자(메인 탭 `mainColor`), 아니면 흰 바탕 검정 글자(상세).
 *   뒤로가기가 없으면 좌우 여백 20, 있으면 13(웹 `:has(.back.hidden)`).
 *   상태바 영역까지 같은 색으로 칠한다(앱은 화면 끝까지 그린다).
 *
 * @param actions 오른쪽 아이콘 자리(웹 rightActions). [TopBarIconButton] 을 넣는다.
 */
@Composable
fun MainTopBar(
    title: String,
    modifier: Modifier = Modifier,
    primary: Boolean = true,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val bg = if (primary) AppColors.Primary else AppColors.Surface
    val fg = if (primary) AppColors.TextOnPrimary else AppColors.TextHead
    Box(modifier.fillMaxWidth().background(bg).statusBarsPadding()) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(Dimensions.topBarHeight)
                .padding(horizontal = if (onBack != null) BACK_PADDING else Dimensions.gutter),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimensions.spacingM),
        ) {
            if (onBack != null) {
                Icon(
                    painterResource(R.drawable.ic_web_arrow),
                    contentDescription = stringResource(R.string.common_back),
                    tint = fg,
                    modifier = Modifier.size(Dimensions.iconL).clickable(onClick = onBack).padding(1.dp).rotate(90f),
                )
            }
            Text(
                title,
                style = AppTypography.Title18,
                color = fg,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            actions()
        }
    }
}

/** 위 바 오른쪽 아이콘(웹 .ico-btn 24 · 아이콘 22). */
@Composable
fun TopBarIconButton(iconRes: Int, contentDescription: String, onClick: () -> Unit, primary: Boolean = true) {
    Icon(
        painterResource(iconRes),
        contentDescription = contentDescription,
        tint = if (primary) AppColors.TextOnPrimary else AppColors.TextHead,
        modifier = Modifier.size(Dimensions.iconL).clickable(onClick = onClick).padding(1.dp),
    )
}

private val BACK_PADDING = 13.dp
