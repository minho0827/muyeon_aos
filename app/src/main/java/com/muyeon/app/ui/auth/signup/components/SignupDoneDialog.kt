package com.muyeon.app.ui.auth.signup.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.muyeon.app.R
import com.muyeon.app.common_components.dialog.ContentAlignment
import com.muyeon.app.common_components.dialog.CustomDialog

/** 가입 완료 팝업 — 웹 CommonModal(제목만 · [확인]). 확인·바깥 누름 모두 다음 단계로(웹 closeModal). */
@Composable
fun SignupDoneDialog(title: String, onConfirm: () -> Unit) {
    CustomDialog(
        title = title, content = null,
        leftButtonText = null, rightButtonText = stringResource(R.string.auth_login_confirm),
        buttonCount = 1, alignment = ContentAlignment.Middle,
        onDismiss = onConfirm, onRightButtonClick = onConfirm, showPopup = true,
    )
}
