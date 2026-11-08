package com.muyeon.app.ui.auth.signup.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.muyeon.app.R
import com.muyeon.app.ui.components.MdButton
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.Dimensions

/**
 * 휴대폰 본인인증 줄 — 웹 Register.js · SocialSignup.js 의 [휴대폰 본인인증 (필수)] / '✓ 본인인증 완료'.
 *  [showHint] = 가입 화면만('본인인증 창으로 이동해요…' + 오류 [message]). 소셜 화면은 버튼만(오류는 화면 아래 안내).
 *  [enabled] = 소셜 화면은 본인확인이 꺼져 있으면(필수만 켜진 경우) 버튼을 막는다(웹 disabled={!idAvailable}).
 */
@Composable
fun SignupIdentitySection(
    done: Boolean,
    required: Boolean,
    busy: Boolean,
    onStart: () -> Unit,
    showHint: Boolean,
    enabled: Boolean = true,
    message: String? = null,
) {
    if (done) {
        Text(stringResource(R.string.auth_signup_identity_done), style = AppTypography.Body14SemiBold, color = AppColors.Primary)
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(Dimensions.spacingS)) {
        val label = when {
            busy -> R.string.auth_signup_identity_moving
            required && showHint -> R.string.auth_signup_identity_required
            else -> R.string.auth_signup_identity
        }
        MdButton(stringResource(label), onStart, filled = false, enabled = enabled && !busy, tag = "signup.identity")
        if (showHint) {
            Text(stringResource(R.string.auth_signup_identity_hint), style = AppTypography.Body14, color = AppColors.TextSub, modifier = Modifier.fillMaxWidth())
        }
        if (message != null) {
            Text(message, style = AppTypography.Body14, color = AppColors.Danger, modifier = Modifier.fillMaxWidth())
        }
    }
}
