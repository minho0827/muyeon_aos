package com.muyeon.app.ui.components

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.muyeon.app.R
import com.muyeon.app.ui.chat.ChatActivity
import com.muyeon.app.ui.floating.FloatingState
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.utils.TokenManager

/**
 * ★ 2026-11-08 탭 위 바 오른쪽 채팅(인스타그램식 종이비행기) — 떠 있는 채팅 버튼을 대신한다(사용자 요청).
 *   안 읽은 채팅이 있으면 점(소켓 실시간 — FloatingState.chatUnread). 로그인했을 때만 보인다.
 */
@Composable
fun ChatTopBarAction(tint: Color) {
    val context = LocalContext.current
    if (TokenManager.getAccessToken(context).isNullOrEmpty()) return
    Box(Modifier.size(36.dp).clickable { ChatActivity.startList(context, "") }, contentAlignment = Alignment.Center) {
        Icon(painterResource(R.drawable.ic_paper_plane), stringResource(R.string.home_chat), tint = tint, modifier = Modifier.size(23.dp))
        if (FloatingState.chatUnread > 0) Box(
            Modifier.align(Alignment.TopEnd).offset(x = (-3).dp, y = 4.dp).size(7.dp)
                .background(if (tint == AppColors.TextOnPrimary) Color.White else AppColors.Danger, CircleShape),
        )
    }
}
