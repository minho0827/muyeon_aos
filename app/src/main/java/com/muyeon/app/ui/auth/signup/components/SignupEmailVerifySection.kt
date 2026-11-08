package com.muyeon.app.ui.auth.signup.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.muyeon.app.R
import com.muyeon.app.ui.auth.components.EmailDomainField
import com.muyeon.app.ui.common.FormField
import com.muyeon.app.ui.common.FormReadOnlyBox
import com.muyeon.app.ui.common.FormTextField
import com.muyeon.app.ui.components.MdButton
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.Dimensions

/**
 * 가입 이메일 + 인증번호 — 웹 Register.js 의 이메일(EmailField: 도메인 고르기) · [인증번호 받기/재전송] · 인증번호 · [인증 확인] · 안내.
 *  인증을 마치면 이메일은 잠기고 '✓ 인증 완료'.
 */
@Composable
fun SignupEmailVerifySection(
    email: String,
    code: String,
    codeSent: Boolean,
    verified: Boolean,
    sending: Boolean,
    verifying: Boolean,
    info: String?,
    infoError: Boolean,
    onEmail: (String) -> Unit,
    onCode: (String) -> Unit,
    onSendCode: () -> Unit,
    onVerify: () -> Unit,
) {
    FormField(stringResource(R.string.auth_signup_email), required = true, filled = email.isNotBlank()) {
        if (verified) FormReadOnlyBox(email) else EmailDomainField(email, onEmail, "signup.email")
    }
    if (verified) {
        Text(stringResource(R.string.auth_signup_email_verified), style = AppTypography.Body14SemiBold, color = AppColors.Primary)
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(Dimensions.spacingS)) {
        MdButton(
            stringResource(
                when {
                    sending -> R.string.auth_signup_sending
                    codeSent -> R.string.auth_signup_resend_code
                    else -> R.string.auth_signup_send_code
                },
            ),
            onSendCode, filled = false, enabled = !sending, tag = "signup.sendCode",
        )
        if (codeSent) {
            FormField(stringResource(R.string.auth_signup_code)) {
                FormTextField(code, onCode, stringResource(R.string.auth_signup_code_placeholder), 6, "signup.code", KeyboardType.Number)
            }
            MdButton(
                stringResource(if (verifying) R.string.auth_signup_verifying else R.string.auth_signup_verify_code),
                onVerify, enabled = !verifying, tag = "signup.verifyCode",
            )
        }
        if (info != null) {
            Text(info, style = AppTypography.Body14, color = if (infoError) AppColors.Danger else AppColors.TextSub, modifier = Modifier.fillMaxWidth())
        }
    }
}
