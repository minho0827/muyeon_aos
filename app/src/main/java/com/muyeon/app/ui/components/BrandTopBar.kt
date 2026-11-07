package com.muyeon.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.R
import com.muyeon.app.ui.theme.AppColors

/**
 * ★ 2026-11-08 컬리 방식 흰 위 바 — 왼쪽 舞踊緣 · 가운데 [center](알약 토글) · 오른쪽 [actions].
 *   홈([강사 | 레슨])과 캐스팅 탭([캐스팅 | 공연])이 같이 쓴다. 흰 바라 상태바 아이콘은 검정.
 */
@Composable
fun BrandTopBar(modifier: Modifier = Modifier, center: @Composable () -> Unit, actions: @Composable RowScope.() -> Unit = {}) {
    val view = LocalView.current
    if (!view.isInEditMode) DisposableEffect(Unit) {
        (view.context as? android.app.Activity)?.window?.let { w ->
            androidx.core.view.WindowCompat.getInsetsController(w, view).isAppearanceLightStatusBars = true
        }
        onDispose { }
    }
    Box(modifier.fillMaxWidth().background(AppColors.Surface).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().height(60.dp).padding(start = 20.dp, end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.home_logo), style = LOGO, color = AppColors.TextHead)
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { center() }
            Row(verticalAlignment = Alignment.CenterVertically, content = actions)
        }
    }
}

/** 舞踊緣 — 검은 굵은 명조(기기 명조 글꼴), 19sp. */
private val LOGO = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Black, fontSize = 19.sp, letterSpacing = (-0.5).sp)
