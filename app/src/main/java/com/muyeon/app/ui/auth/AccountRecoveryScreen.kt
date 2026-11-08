package com.muyeon.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muyeon.app.R
import com.muyeon.app.common_components.dialog.ContentAlignment
import com.muyeon.app.common_components.dialog.CustomDialog
import com.muyeon.app.ui.account.components.PasswordField
import com.muyeon.app.ui.auth.components.EmailDomainField
import com.muyeon.app.ui.common.FormField
import com.muyeon.app.ui.common.FormTextField
import com.muyeon.app.ui.common.LocalRequiredForm
import com.muyeon.app.ui.common.MuyeonBottomButton
import com.muyeon.app.ui.common.rememberRequiredForm
import com.muyeon.app.ui.components.MainTopBar
import com.muyeon.app.ui.components.MdButton
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.Dimensions
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * 아이디 찾기 · 비밀번호 재설정 — 웹 pages/account/AccountRecovery.js 와 같은 항목·단계.
 *   필수 칸은 공용 RequiredForm(빠진 칸으로 스크롤·빨간 제목·토스트). 결과는 팝업([닫기] [로그인]).
 */
@Composable
fun AccountRecoveryScreen(
    type: String,
    onBack: () -> Unit,
    onGoLogin: () -> Unit,
    viewModel: AccountRecoveryViewModel = koinViewModel { parametersOf(type) },
) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val form = rememberRequiredForm()
    Column(Modifier.fillMaxSize().background(AppColors.Surface).imePadding()) {
        MainTopBar(
            title = stringResource(if (s.isPassword) R.string.auth_recovery_pw_title else R.string.auth_recovery_id_title),
            primary = false, onBack = onBack,
        )
        CompositionLocalProvider(LocalRequiredForm provides form) {
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = Dimensions.gutter, vertical = Dimensions.spacing3XL + Dimensions.spacingS),
                verticalArrangement = Arrangement.spacedBy(Dimensions.spacingXL),
            ) {
                Text(
                    stringResource(if (s.isPassword) R.string.auth_recovery_pw_lead else R.string.auth_recovery_id_lead),
                    style = AppTypography.Title18, color = AppColors.TextHead,
                )
                if (!s.isPassword) {
                    FormField(stringResource(R.string.auth_recovery_name), required = true, filled = s.name.isNotBlank()) {
                        FormTextField(s.name, viewModel::onName, stringResource(R.string.auth_recovery_name_placeholder), 30, "recovery.name")
                    }
                    FormField(stringResource(R.string.auth_recovery_phone), required = true, filled = s.phone.isNotBlank()) {
                        FormTextField(s.phone, viewModel::onPhone, stringResource(R.string.auth_recovery_phone_placeholder), 20, "recovery.phone", KeyboardType.Phone)
                    }
                } else {
                    FormField(stringResource(R.string.auth_recovery_email), required = true, filled = s.email.isNotBlank()) {
                        EmailDomainField(s.email, viewModel::onEmail, "recovery.email")
                    }
                    MdButton(
                        stringResource(
                            when {
                                s.sending -> R.string.auth_recovery_sending
                                s.codeSent -> R.string.auth_recovery_resend_code
                                else -> R.string.auth_recovery_send_code
                            },
                        ),
                        viewModel::sendCode, filled = false, enabled = !s.sending, tag = "recovery.sendCode",
                    )
                    if (s.codeSent) {
                        FormField(stringResource(R.string.auth_recovery_code), required = true, filled = s.code.isNotBlank()) {
                            FormTextField(s.code, viewModel::onCode, stringResource(R.string.auth_recovery_code_placeholder), 6, "recovery.code", KeyboardType.Number)
                        }
                        FormField(stringResource(R.string.auth_recovery_new_password), required = true, filled = s.password.isNotEmpty()) {
                            PasswordField(s.password, viewModel::onPassword, stringResource(R.string.auth_recovery_new_password_placeholder), "recovery.password")
                        }
                        FormField(stringResource(R.string.auth_recovery_confirm), required = true, filled = s.confirm.isNotEmpty()) {
                            PasswordField(s.confirm, viewModel::onConfirm, stringResource(R.string.auth_recovery_confirm_placeholder), "recovery.confirm")
                        }
                    }
                    val info = s.infoText ?: s.infoRes?.let { stringResource(it) }
                    if (info != null) {
                        Text(info, style = AppTypography.Body14, color = if (s.infoError) AppColors.Danger else AppColors.TextSub, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
        MuyeonBottomButton(
            stringResource(if (s.isPassword) R.string.auth_recovery_submit_pw else R.string.auth_recovery_submit_id),
            "recovery.submit", loading = s.submitting,
        ) { if (form.validate(context)) viewModel.submit() }
    }
    if (s.showResult) {
        val text = s.foundEmail?.let { stringResource(R.string.auth_recovery_found_email, it) }
            ?: s.resultText ?: s.resultRes?.let { stringResource(it) }.orEmpty()
        CustomDialog(
            title = text, content = null,
            leftButtonText = stringResource(R.string.auth_recovery_close), rightButtonText = stringResource(R.string.auth_recovery_go_login),
            buttonCount = 2, alignment = ContentAlignment.Middle,
            onDismiss = viewModel::dismissResult, onLeftButtonClick = viewModel::dismissResult,
            onRightButtonClick = { viewModel.dismissResult(); onGoLogin() }, showPopup = true,
        )
    }
}
