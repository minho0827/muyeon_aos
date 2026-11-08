package com.muyeon.app.ui.auth.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.muyeon.app.R
import com.muyeon.app.auth.LoginProvider
import com.muyeon.app.ui.components.LevelBadge
import com.muyeon.app.ui.theme.CategoryColors
import com.muyeon.app.ui.theme.Dimensions

private val CircleSize = 64.dp          // 웹 6.4rem
private val CircleGap = 24.dp           // 웹 gap 2.4rem
private val RecentSlot = 20.dp          // 웹 minHeight 2rem(배지 자리 — 있든 없든 원 높이를 맞춘다)

/**
 * 소셜 로그인 원형 버튼 줄 — 웹 Login.js socialLoginButtons(카카오 · 네이버 · 애플).
 *   Android 는 애플을 그리지 않는다(웹도 AOS 앱에선 iOS 시트가 없다). 네이버는 [showNaver](검수 전 숨김).
 *   마지막으로 쓴 수단 위에 '최근 로그인' 배지.
 */
@Composable
fun LoginSocialButtons(showNaver: Boolean, recent: LoginProvider?, onSocial: (LoginProvider) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(CircleGap, Alignment.CenterHorizontally)) {
        SocialCircle(R.drawable.ic_login_kakao, stringResource(R.string.auth_login_kakao), recent == LoginProvider.KAKAO, "login.kakao") { onSocial(LoginProvider.KAKAO) }
        if (showNaver) {
            SocialCircle(R.drawable.ic_login_naver, stringResource(R.string.auth_login_naver), recent == LoginProvider.NAVER, "login.naver") { onSocial(LoginProvider.NAVER) }
        }
    }
}

@Composable
private fun SocialCircle(icon: Int, label: String, recent: Boolean, tag: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.heightIn(min = RecentSlot).padding(bottom = Dimensions.spacingXS + Dimensions.spacingXXS), contentAlignment = Alignment.Center) {
            if (recent) LevelBadge(stringResource(R.string.auth_login_recent), CategoryColors.Level.End)
        }
        Image(
            painterResource(icon), contentDescription = label,
            modifier = Modifier.size(CircleSize).clip(CircleShape).clickable(role = Role.Button, onClick = onClick).testTag(tag),
        )
    }
}
