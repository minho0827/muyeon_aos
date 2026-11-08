package com.muyeon.app.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muyeon.app.R
import com.muyeon.app.common_components.dialog.ContentAlignment
import com.muyeon.app.common_components.dialog.CustomDialog
import com.muyeon.app.ui.account.components.PasswordField
import com.muyeon.app.ui.common.FormField
import com.muyeon.app.ui.common.MuyeonBottomButton
import com.muyeon.app.ui.components.MainTopBar
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.Dimensions
import com.muyeon.domain.usecase.account.ChangePasswordUseCase
import org.koin.androidx.compose.koinViewModel

/** 비밀번호 변경 — 웹 /changePassword(새 비밀번호 · 확인 · [확인] → 변경할까요? → 결과). */
@Composable
fun ChangePasswordScreen(onClose: () -> Unit, viewModel: ChangePasswordViewModel = koinViewModel()) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().background(AppColors.Surface).navigationBarsPadding().imePadding()) {
        MainTopBar(title = stringResource(R.string.password_title), primary = false, onBack = onClose)
        Column(Modifier.weight(1f).padding(Dimensions.gutter), verticalArrangement = Arrangement.spacedBy(Dimensions.spacingXL)) {
            FormField(stringResource(R.string.password_new)) {
                PasswordField(s.password, viewModel::onPassword, stringResource(R.string.password_new_placeholder), "password.new")
                if (s.invalid == ChangePasswordUseCase.Invalid.POLICY) Text(stringResource(R.string.password_policy), style = AppTypography.Caption12, color = AppColors.Danger)
                else Text(stringResource(R.string.password_policy), style = AppTypography.Caption12, color = AppColors.TextSub)
            }
            FormField(stringResource(R.string.password_confirm)) {
                PasswordField(s.confirm, viewModel::onConfirm, stringResource(R.string.password_confirm_placeholder), "password.confirm")
                if (s.invalid == ChangePasswordUseCase.Invalid.MISMATCH) Text(stringResource(R.string.password_mismatch), style = AppTypography.Caption12, color = AppColors.Danger)
            }
        }
        MuyeonBottomButton(stringResource(R.string.common_confirm), "password.submit", loading = s.saving, onClick = viewModel::submit)
    }
    CustomDialog(
        title = stringResource(R.string.password_change_q), content = null,
        leftButtonText = stringResource(R.string.common_cancel), rightButtonText = stringResource(R.string.common_confirm),
        buttonCount = 2, alignment = ContentAlignment.Middle,
        onDismiss = viewModel::cancelAsk, onLeftButtonClick = viewModel::cancelAsk, onRightButtonClick = viewModel::confirmChange, showPopup = s.confirmAsk,
    )
    s.result?.let { r ->
        val close = { if (viewModel.dismissResult()) onClose() }
        CustomDialog(
            title = stringResource(r), content = null, leftButtonText = null, rightButtonText = stringResource(R.string.common_confirm),
            buttonCount = 1, alignment = ContentAlignment.Middle, onDismiss = close, onRightButtonClick = close, showPopup = true,
        )
    }
}
