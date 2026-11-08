package com.muyeon.app.ui.auth.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.muyeon.app.R
import com.muyeon.app.auth.LoginProvider
import com.muyeon.app.ui.components.MdButton
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.Dimensions

// 카카오·네이버 버튼 규정 색(웹 Login.js signupSocialButtons 인라인 색 그대로 — 브랜드 색이라 테마에 두지 않는다).
private val KakaoYellow = Color(0xFFFEE500)
private val KakaoInk = Color(0xFF191600)
private val NaverGreen = Color(0xFF03C75A)

/**
 * [회원가입] → 가입 방법 고르기 — 웹 Login.js '무용연 시작하기' 모달(카카오 · 네이버 · 이메일로 가입하기 · 닫기).
 *   소셜은 처음 쓰는 계정이면 그 자리에서 가입된다(로그인과 같은 흐름).
 */
@Composable
fun LoginSignupDialog(showNaver: Boolean, onSocial: (LoginProvider) -> Unit, onEmail: () -> Unit, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose) {
        Surface(shape = RoundedCornerShape(Dimensions.cardRadius), color = AppColors.Surface) {
            Column(Modifier.fillMaxWidth().padding(Dimensions.gutter)) {
                Text(
                    stringResource(R.string.auth_login_signup_title), style = AppTypography.Title18, color = AppColors.TextHead,
                    modifier = Modifier.padding(bottom = Dimensions.spacingL),
                )
                BrandButton(R.drawable.ic_login_kakao, stringResource(R.string.auth_login_kakao), KakaoYellow, KakaoInk, "signup.kakao") { onSocial(LoginProvider.KAKAO) }
                if (showNaver) {
                    BrandButton(R.drawable.ic_login_naver, stringResource(R.string.auth_login_naver), NaverGreen, AppColors.TextOnPrimary, "signup.naver",
                        Modifier.padding(top = Dimensions.spacingS)) { onSocial(LoginProvider.NAVER) }
                }
                MdButton(stringResource(R.string.auth_login_signup_email), onEmail, Modifier.padding(top = Dimensions.spacingS), filled = false, tag = "signup.email")
                MdButton(stringResource(R.string.auth_login_close), onClose, Modifier.padding(top = Dimensions.spacingL), filled = false, tag = "signup.close")
            }
        }
    }
}

@Composable
private fun BrandButton(icon: Int, text: String, bg: Color, fg: Color, tag: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier.fillMaxWidth().height(Dimensions.buttonMdHeight).clip(RoundedCornerShape(Dimensions.buttonRadius)).background(bg)
            .clickable(role = Role.Button, onClick = onClick).testTag(tag),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(painterResource(icon), null, Modifier.size(Dimensions.iconM).clip(RoundedCornerShape(50)))
        Text(text, style = AppTypography.CardTitle16.copy(fontWeight = FontWeight.Bold), color = fg, modifier = Modifier.padding(start = 6.dp))
    }
}
