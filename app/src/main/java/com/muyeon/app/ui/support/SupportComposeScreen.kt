package com.muyeon.app.ui.support

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muyeon.app.R
import com.muyeon.app.common_components.dialog.ContentAlignment
import com.muyeon.app.common_components.dialog.CustomDialog
import com.muyeon.app.ui.common.FormField
import com.muyeon.app.ui.common.FormTextField
import com.muyeon.app.ui.common.LocalRequiredForm
import com.muyeon.app.ui.common.MuyeonBottomButton
import com.muyeon.app.ui.common.rememberRequiredForm
import com.muyeon.app.ui.components.MainTopBar
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.Dimensions
import com.muyeon.domain.model.support.SupportComposeType
import com.muyeon.domain.usecase.support.SendSupportUseCase

/**
 * 1:1 문의 / 의견 남기기 — 웹 SupportCompose.js(웹은 모달, 앱은 화면). 내용은 필수(RequiredForm) · 1000자.
 *   보내면 완료 팝업: 문의는 [문의내역 확인](→ [onDoneInquiry]), 의견은 [확인](→ [onBack]).
 */
@Composable
fun SupportComposeScreen(
    type: SupportComposeType,
    viewModel: SupportComposeViewModel,
    onBack: () -> Unit,
    onDoneInquiry: () -> Unit,
) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val form = rememberRequiredForm()
    val inquiry = type == SupportComposeType.INQUIRY
    Column(Modifier.fillMaxSize().background(AppColors.Surface).navigationBarsPadding().imePadding()) {
        MainTopBar(title = stringResource(if (inquiry) R.string.support_inquiry_title else R.string.support_feedback_title), primary = false, onBack = onBack)
        CompositionLocalProvider(LocalRequiredForm provides form) {
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(Dimensions.gutter),
                verticalArrangement = Arrangement.spacedBy(Dimensions.spacingL),
            ) {
                Text(stringResource(if (inquiry) R.string.support_inquiry_head else R.string.support_feedback_head), style = AppTypography.Title18, color = AppColors.TextHead)
                Text(stringResource(if (inquiry) R.string.support_inquiry_desc else R.string.support_feedback_desc), style = AppTypography.Body14, color = AppColors.TextSub)
                FormField(
                    stringResource(if (inquiry) R.string.support_inquiry_content else R.string.support_feedback_content),
                    required = true, filled = s.text.isNotBlank(),
                ) {
                    FormTextField(
                        s.text, viewModel::onText,
                        placeholder = stringResource(if (inquiry) R.string.support_inquiry_placeholder else R.string.support_feedback_placeholder),
                        max = SendSupportUseCase.MAX, tag = "support.content", minLines = 6,
                    )
                    Text("${s.text.length}/${SendSupportUseCase.MAX}", style = AppTypography.Caption12, color = AppColors.TextHint,
                        textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
                }
                s.errorMessage?.let { Text(it, style = AppTypography.Body14, color = AppColors.Danger) }
            }
        }
        MuyeonBottomButton(
            stringResource(if (inquiry) R.string.support_inquiry_send else R.string.support_feedback_send),
            "support.send", loading = s.sending,
        ) { if (form.validate(context)) viewModel.submit() }
    }
    CustomDialog(
        title = stringResource(if (inquiry) R.string.support_inquiry_done else R.string.support_feedback_done),
        content = stringResource(if (inquiry) R.string.support_inquiry_done_desc else R.string.support_feedback_done_desc),
        leftButtonText = null,
        rightButtonText = stringResource(if (inquiry) R.string.support_inquiry_done_button else R.string.common_confirm),
        buttonCount = 1, alignment = ContentAlignment.Middle,
        onDismiss = { if (inquiry) onDoneInquiry() else onBack() },
        onRightButtonClick = { if (inquiry) onDoneInquiry() else onBack() },
        showPopup = s.done,
    )
}
