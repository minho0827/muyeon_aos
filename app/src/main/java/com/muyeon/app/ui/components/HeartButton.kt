package com.muyeon.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.muyeon.app.R
import com.muyeon.app.ui.theme.AppColors

/**
 * 찜(하트) 버튼 — 웹 components/muyeon/HeartButton.js. 찜 = 주황 채움, 아니면 회색(#c4c4c4) 외곽선. 패딩 4.
 *   행·카드 위에서 눌러도 행 누름으로 번지지 않는다(자기 clickable).
 */
@Composable
fun HeartButton(
    liked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    emptyColor: Color = AppColors.HeartOff,
) {
    Icon(
        painterResource(if (liked) R.drawable.ic_web_heart else R.drawable.ic_web_heart_line),
        contentDescription = stringResource(if (liked) R.string.common_like_cancel else R.string.common_like),
        tint = if (liked) AppColors.Primary else emptyColor,
        modifier = modifier
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onToggle)
            .padding(4.dp)
            .size(size),
    )
}
