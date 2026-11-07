package com.muyeon.app.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.R
import com.muyeon.app.ui.home.HomeMode
import com.muyeon.app.ui.theme.AppColors

/**
 * ★ 2026-11-08 홈 위 바 — 컬리 방식: 흰 바탕 · 왼쪽 舞踊緣 글자 로고 · 가운데 [강사 | 레슨] 토글 · 오른쪽 알림 종(안 읽음 점).
 *   지역 고르기는 MY 로 옮겼다(사용자 결정).
 */
@Composable
fun HomeTopBar(mode: HomeMode, onMode: (HomeMode) -> Unit, unread: Int, onBell: () -> Unit, modifier: Modifier = Modifier) {
    // 흰 바 — 상태바 아이콘은 검정(다른 탭의 주황 바는 흰 아이콘).
    val view = LocalView.current
    if (!view.isInEditMode) DisposableEffect(Unit) {
        (view.context as? android.app.Activity)?.window?.let { w ->
            androidx.core.view.WindowCompat.getInsetsController(w, view).isAppearanceLightStatusBars = true
        }
        onDispose { }
    }
    Box(modifier.fillMaxWidth().background(AppColors.Surface).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.home_logo), style = LOGO, color = AppColors.TextHead)
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { HomeModeToggle(mode, onMode) }
            Box(Modifier.size(36.dp).clickable(onClick = onBell), contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_web_bell), stringResource(R.string.home_notifications), tint = AppColors.TextHead, modifier = Modifier.size(28.dp))
                if (unread > 0) Box(Modifier.align(Alignment.TopEnd).offset(x = (-4).dp, y = 4.dp).size(7.dp).background(AppColors.Danger, CircleShape))
            }
        }
    }
}

/** 舞踊緣 — 검은 굵은 명조(기기 명조 글꼴), 19sp. 글자 사이를 살짝 좁혀 로고처럼. */
private val LOGO = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Black, fontSize = 19.sp, letterSpacing = (-0.5).sp)
