package com.muyeon.app.ui.my.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.muyeon.app.R
import com.muyeon.app.ui.components.PrimaryBottomButton
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.Dimensions

/**
 * 비회원 MY — 웹 pages/muyeon/my/GuestMyHome.js. 주황 머리("로그인하고 / 무용연을 시작해 보세요") ·
 *   [로그인 / 회원가입] · 공지사항·FAQ·이용약관·개인정보처리 방침 · 앱 설정.
 *
 * @param onLogin 웹 로그인 창(마치면 /mypage → 앱 MY 탭으로 돌아온다)
 * @param onOpenPath 메뉴의 웹 화면
 */
@Composable
fun GuestMyHome(onLogin: () -> Unit, onOpenPath: (String) -> Unit, onOpenCommunity: () -> Unit, regionName: String?, onRegion: () -> Unit, modifier: Modifier = Modifier) {
    // 비회원은 위 바로가기 카드가 없어 커뮤니티를 메뉴 첫 줄로 둔다(회원은 바로가기 카드에 있다).
    val rows = listOf(
        MyMenuRow(COMMUNITY_KEY, stringResource(R.string.tab_community), R.drawable.ic_menu_community_filled),
        MyMenuRow("/notice", stringResource(R.string.my_menu_notice), R.drawable.ic_menu_notice),
        MyMenuRow("/faq", stringResource(R.string.my_menu_faq), R.drawable.ic_menu_faq),
        MyMenuRow("/policy?doc=terms", stringResource(R.string.my_menu_terms), R.drawable.ic_menu_myinfo),
        MyMenuRow("/policy?doc=privacy", stringResource(R.string.my_menu_privacy), R.drawable.ic_menu_myinfo),
    )
    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = com.muyeon.app.ui.main.tabBarBottomPadding()), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            stringResource(R.string.my_guest_title),
            style = AppTypography.Title20.copy(fontSize = androidx.compose.ui.unit.TextUnit(24f, androidx.compose.ui.unit.TextUnitType.Sp)),
            color = AppColors.TextOnPrimary,
            modifier = Modifier.fillMaxWidth().background(AppColors.Primary).padding(start = Dimensions.gutter, end = Dimensions.gutter, top = Dimensions.spacingL, bottom = Dimensions.spacing3XL),
        )
        PrimaryBottomButton(stringResource(R.string.my_guest_login), onClick = onLogin, modifier = Modifier.padding(top = Dimensions.spacingS), atScreenBottom = false)
        MyRegionRow(regionName, onRegion, Modifier.padding(top = Dimensions.spacingL))
        MyMenuList(rows, onClick = { if (it.key == COMMUNITY_KEY) onOpenCommunity() else onOpenPath(it.key) })
        MySubMenu(listOf(MySubMenuItem("/setting", stringResource(R.string.my_app_settings), R.drawable.ic_web_gear)), onClick = { onOpenPath(it.key) })
        androidx.compose.foundation.layout.Spacer(Modifier.padding(bottom = Dimensions.spacing3XL))
    }
}

private const val COMMUNITY_KEY = "community"
