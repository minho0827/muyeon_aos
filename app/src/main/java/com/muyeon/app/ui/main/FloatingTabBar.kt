package com.muyeon.app.ui.main

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.muyeon.app.ui.theme.AppColors

/** 1계층 바 위쪽 모서리(토스 하단 바처럼 위만 둥글다). */
private const val DOCKED_TOP_RADIUS = 24f
/** 2계층 캡슐 — 양 끝이 반원(바 높이 72 의 절반). */
private const val FLOAT_RADIUS = 36f
private const val FLOAT_SIDE = 12f
private const val FLOAT_BOTTOM = 8f

/**
 * ★ 2026-11-08 토스 방식 탭바 그릇(사용자 요청 — 토스뱅크·토스증권 화면 참고).
 *   1계층([floating]=false): 화면 아래에 **붙은** 바. 위 모서리만 둥글고, 내비게이션 바 자리까지 같은 흰 면이 이어진다.
 *   2계층([floating]=true) : 양옆·아래가 **살짝 떨어진** 둥근 캡슐(그림자). 칸 이동 애니메이션과 같이 모양이 바뀐다.
 *   두 모양 사이는 한 값(0→1)으로 이어서 움직인다 — 모서리·여백·그림자가 같이 바뀐다.
 */
@Composable
fun FloatingTabBar(floating: Boolean, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val anim by animateFloatAsState(if (floating) 1f else 0f, spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMedium), label = "tabBarFloat")
    // 스프링이 0·1 을 살짝 넘는다 — 여백·모서리가 음수가 되면 앱이 죽는다(Padding must be non-negative). 0~1 로 자른다.
    val f = anim.coerceIn(0f, 1f)
    val nav = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val top = lerp(DOCKED_TOP_RADIUS, FLOAT_RADIUS, f).dp
    val bottom = lerp(0f, FLOAT_RADIUS, f).dp
    val shape = RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
    Box(
        modifier
            .fillMaxWidth()
            .padding(start = (FLOAT_SIDE * f).dp, end = (FLOAT_SIDE * f).dp, bottom = nav * f + (FLOAT_BOTTOM * f).dp)
            .shadow(lerp(4f, 12f, f).dp, shape, clip = false, ambientColor = Color.Black.copy(alpha = 0.10f), spotColor = Color.Black.copy(alpha = 0.16f))
            .clip(shape)
            .background(AppColors.TabBarGlass)
            .border(1.dp, AppColors.TabBarBorder.copy(alpha = 0.8f), shape),
    ) {
        Column {
            content()
            // 붙은 바는 내비게이션 바 자리까지 같은 면 — 떠오르면 이 칸이 줄어든다.
            Spacer(Modifier.height(nav * (1f - f)))
        }
    }
}
