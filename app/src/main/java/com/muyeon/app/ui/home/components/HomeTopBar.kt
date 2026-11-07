package com.muyeon.app.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.muyeon.app.R
import com.muyeon.app.ui.components.BrandTopBar
import com.muyeon.app.ui.components.ChipOption
import com.muyeon.app.ui.components.SegmentToggle
import com.muyeon.app.ui.home.HomeMode
import com.muyeon.app.ui.theme.AppColors

/**
 * 홈 위 바 — [DANCE] [강사 | 레슨] [🔔(안 읽음 점)] [✈ 채팅(안 읽음 점)]. 지역 고르기는 MY 로 옮겼다(사용자 결정).
 *   ★ 2026-11-08 채팅은 떠 있는 버튼 대신 알림 옆 인스타그램식 종이비행기 아이콘(로그인했을 때만).
 */
@Composable
fun HomeTopBar(
    mode: HomeMode, onMode: (HomeMode) -> Unit, unread: Int, onBell: () -> Unit,
    chatUnread: Int, onChat: (() -> Unit)?, modifier: Modifier = Modifier,
) {
    BrandTopBar(
        modifier,
        center = {
            SegmentToggle(
                listOf(ChipOption(HomeMode.TEACHERS.name, stringResource(R.string.home_mode_teachers)), ChipOption(HomeMode.LESSONS.name, stringResource(R.string.home_mode_lessons))),
                mode.name, { onMode(HomeMode.valueOf(it)) },
            )
        },
    ) {
        Box(Modifier.size(36.dp).clickable(onClick = onBell), contentAlignment = Alignment.Center) {
            Icon(painterResource(R.drawable.ic_web_bell), stringResource(R.string.home_notifications), tint = AppColors.TextHead, modifier = Modifier.size(28.dp))
            if (unread > 0) Box(Modifier.align(Alignment.TopEnd).offset(x = (-4).dp, y = 4.dp).size(7.dp).background(AppColors.Danger, CircleShape))
        }
        if (onChat != null) Box(Modifier.size(36.dp).clickable(onClick = onChat), contentAlignment = Alignment.Center) {
            Icon(painterResource(R.drawable.ic_paper_plane), stringResource(R.string.home_chat), tint = AppColors.TextHead, modifier = Modifier.size(23.dp))
            // 안 읽은 채팅이 있으면 점(알림 종과 같은 모양) — 소켓으로 새 메시지가 오면 바로 켜진다.
            if (chatUnread > 0) Box(Modifier.align(Alignment.TopEnd).offset(x = (-3).dp, y = 4.dp).size(7.dp).background(AppColors.Danger, CircleShape))
        }
    }
}
