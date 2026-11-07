package com.muyeon.app.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.muyeon.app.R
import com.muyeon.app.ui.components.MainTopBar
import com.muyeon.app.ui.components.SheetAction
import com.muyeon.app.ui.components.SheetButton
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography

/**
 * 앱 화면을 만드는 중인 탭의 임시 자리(개편 계획 3·4단계 전까지) — 지금 쓰던 화면(웹 창·기존 앱 화면)을 여는 버튼.
 *   ⚠️ 출시 전에 모두 진짜 화면으로 바뀐다(MainTab.NOT_READY 가 비면 이 파일을 지운다).
 */
@Composable
fun InterimTabScreen(title: String, header: (@Composable () -> Unit)? = null, onOpen: () -> Unit) {
    Column(Modifier.fillMaxSize().background(AppColors.Surface)) {
        if (header != null) header() else MainTopBar(title)
        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.interim_tab_desc), style = AppTypography.Body14, color = AppColors.TextSub)
            SheetButton(SheetAction(stringResource(R.string.interim_tab_open, title), primary = true, onClick = onOpen))
        }
    }
}
