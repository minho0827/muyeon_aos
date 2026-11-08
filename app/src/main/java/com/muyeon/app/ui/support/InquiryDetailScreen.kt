package com.muyeon.app.ui.support

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muyeon.app.R
import com.muyeon.app.common_components.dialog.ContentAlignment
import com.muyeon.app.common_components.dialog.CustomDialog
import com.muyeon.app.ui.components.EmptyView
import com.muyeon.app.ui.components.MainTopBar
import com.muyeon.app.ui.components.ReadArea
import com.muyeon.app.ui.components.ReadField
import com.muyeon.app.ui.components.ReadSectionTitle
import com.muyeon.app.ui.format.KstDates
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.Dimensions

/** 1:1 문의 상세 — 웹 /support/inquiries/:id(문의 정보 · 답변). 위 바 [삭제]는 확인 뒤 지운다. */
@Composable
fun InquiryDetailScreen(viewModel: InquiryDetailViewModel, onBack: () -> Unit, onDeleted: () -> Unit) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(viewModel) {
        viewModel.events.collect { e ->
            when (e) {
                InquiryDetailEvent.Deleted -> {
                    Toast.makeText(context, R.string.support_inquiry_deleted, Toast.LENGTH_SHORT).show()
                    onDeleted()
                }
                is InquiryDetailEvent.Message -> Toast.makeText(context, e.text, Toast.LENGTH_SHORT).show()
            }
        }
    }
    Column(Modifier.fillMaxSize().background(AppColors.Surface).navigationBarsPadding()) {
        MainTopBar(title = stringResource(R.string.support_inquiry_detail_title), primary = false, onBack = onBack) {
            if (s.inquiry != null) Text(
                stringResource(R.string.common_delete), style = AppTypography.Body15Medium, color = AppColors.TextSub,
                modifier = Modifier.clickable(enabled = !s.deleting) { viewModel.askDelete(true) }.padding(Dimensions.spacingXS),
            )
        }
        val i = s.inquiry
        when {
            s.isLoading -> Unit
            i == null -> EmptyView(stringResource(R.string.support_inquiry_not_found), content = s.errorMessage,
                buttonText = stringResource(R.string.common_retry), onButton = viewModel::load)
            else -> Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Dimensions.gutter),
                verticalArrangement = Arrangement.spacedBy(Dimensions.spacingL),
            ) {
                ReadSectionTitle(stringResource(R.string.support_inquiry_info))
                ReadField(stringResource(R.string.support_inquiry_status), stringResource(if (i.resolved) R.string.support_inquiry_status_done else R.string.support_inquiry_status_waiting))
                ReadField(stringResource(R.string.support_inquiry_date_label), KstDates.dotTime(i.createdAt))
                ReadArea(i.content, label = stringResource(R.string.support_inquiry_content))
                ReadSectionTitle(stringResource(R.string.support_inquiry_answer))
                ReadArea(i.answer ?: stringResource(R.string.support_inquiry_answer_waiting))
                i.answeredAt?.let { ReadField(stringResource(R.string.support_inquiry_answered_at), KstDates.dotTime(it)) }
            }
        }
    }
    CustomDialog(
        title = stringResource(R.string.support_inquiry_delete_q), content = null,
        leftButtonText = stringResource(R.string.common_cancel), rightButtonText = stringResource(R.string.common_delete),
        buttonCount = 2, alignment = ContentAlignment.Middle,
        onDismiss = { viewModel.askDelete(false) }, onLeftButtonClick = { viewModel.askDelete(false) },
        onRightButtonClick = viewModel::delete, showPopup = s.confirmDelete,
    )
}
