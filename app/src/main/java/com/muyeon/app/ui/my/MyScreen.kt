package com.muyeon.app.ui.my

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.muyeon.app.R
import com.muyeon.app.navigation.WebPaths
import com.muyeon.app.ui.components.MainTopBar
import com.muyeon.app.ui.my.components.GuestMyHome
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography

/**
 * MY 탭 — 웹 pages/muyeon/my/MyHome.js. 비회원이면 [GuestMyHome], 회원이면 회원 MY(다음 단계에서 이식).
 */
@Composable
fun MyScreen(isLoggedIn: Boolean, onOpenWeb: (String) -> Unit) {
    Column(Modifier.fillMaxSize().background(AppColors.Surface)) {
        MainTopBar(title = stringResource(R.string.tab_my))
        if (!isLoggedIn) {
            GuestMyHome(onLogin = { onOpenWeb(WebPaths.login("/mypage")) }, onOpenPath = onOpenWeb)
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.tab_my), style = AppTypography.Section16, color = AppColors.TextSub)
            }
        }
    }
}
