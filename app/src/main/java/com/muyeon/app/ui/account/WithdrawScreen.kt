package com.muyeon.app.ui.account

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muyeon.app.R
import com.muyeon.app.common_components.dialog.ContentAlignment
import com.muyeon.app.common_components.dialog.CustomDialog
import com.muyeon.app.ui.account.components.PasswordField
import com.muyeon.app.ui.account.components.WithdrawCheckRow
import com.muyeon.app.ui.account.components.WithdrawNoticeCard
import com.muyeon.app.ui.account.components.WithdrawReasonGroup
import com.muyeon.app.ui.common.FormField
import com.muyeon.app.ui.common.FormTextField
import com.muyeon.app.ui.components.EmptyView
import com.muyeon.app.ui.components.MainTopBar
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.Dimensions
import com.muyeon.domain.usecase.account.WithdrawUseCase
import org.koin.androidx.compose.koinViewModel

/**
 * 회원탈퇴 — 웹 /withdraw(3단계). 정리할 내역(blocker)의 [action] 은 [onOpenPath] 로 그 화면을 연다.
 *
 * @param onLoggedOut 탈퇴 완료 뒤(셸이 로그아웃 상태로 홈)
 */
@Composable
fun WithdrawScreen(onClose: () -> Unit, onOpenPath: (String) -> Unit, onLoggedOut: () -> Unit, viewModel: WithdrawViewModel = koinViewModel()) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.events.collect { if (it == WithdrawEvent.LoggedOut) onLoggedOut() } }
    val back = { if (s.step == WithdrawStep.INFO) onClose() else viewModel.back() }
    BackHandler(onBack = back)
    val info = s.info
    Column(Modifier.fillMaxSize().background(AppColors.Surface).navigationBarsPadding().imePadding()) {
        MainTopBar(title = stringResource(R.string.settings_withdraw), primary = false, onBack = back)
        when {
            s.isLoading -> Unit
            info == null -> EmptyView(s.errorMessage ?: stringResource(R.string.withdraw_load_failed), buttonText = stringResource(R.string.common_retry), onButton = viewModel::load)
            else -> Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(Dimensions.gutter),
                verticalArrangement = Arrangement.spacedBy(Dimensions.spacingXXL),
            ) {
                when (s.step) {
                    WithdrawStep.INFO -> {
                        Text(stringResource(R.string.withdraw_head, info.nickname), style = AppTypography.Title20, color = AppColors.TextHead)
                        if (info.blockers.isNotEmpty()) WithdrawNoticeCard(stringResource(R.string.withdraw_blockers),
                            info.blockers.map { b -> "${b.label}${b.detail?.let { " · $it" }.orEmpty()}" to b.action?.let { a -> { onOpenPath(a) } } })
                        if (info.autoCancel.isNotEmpty()) WithdrawNoticeCard(stringResource(R.string.withdraw_auto_cancel),
                            info.autoCancel.map { "${it.label}${it.detail?.let { d -> " · $d" }.orEmpty()}" to null })
                        if (info.warnings.isNotEmpty()) WithdrawNoticeCard(stringResource(R.string.withdraw_warnings),
                            info.warnings.map { "${it.label}${it.detail?.let { d -> " · $d" }.orEmpty()}" to null })
                        val labels = stringArrayResource(R.array.withdraw_summary_labels)
                        val keys = listOf("posts", "comments", "chatRooms", "reviews", "resumes", "postings", "spaces")
                        WithdrawNoticeCard(stringResource(R.string.withdraw_effects),
                            listOf(stringResource(R.string.withdraw_effect_login) to null, stringResource(R.string.withdraw_effect_profile) to null) +
                                keys.mapIndexedNotNull { i, k -> info.summary[k]?.takeIf { it > 0 }?.let { n -> stringResource(R.string.withdraw_effect_delete, labels[i], n) to null } } +
                                (stringResource(R.string.withdraw_effect_keep) to null))
                        WithdrawNoticeCard(stringResource(R.string.withdraw_retention),
                            listOf(
                                stringResource(R.string.withdraw_retention_1, info.retentionMonths) to null,
                                stringResource(R.string.withdraw_retention_2) to null,
                                stringResource(R.string.withdraw_retention_3, info.retentionMonths) to null,
                                stringResource(R.string.withdraw_retention_more) to { onOpenPath("/policy?doc=privacy") },
                            ))
                        Column {
                            Text(stringResource(R.string.withdraw_agree_title), style = AppTypography.Section16, color = AppColors.TextHead, modifier = Modifier.padding(bottom = Dimensions.spacingS))
                            WithdrawCheckRow(stringResource(R.string.withdraw_agree_all), s.allChecked, { viewModel.toggleAll(!s.allChecked) }, strong = true)
                            val agreeLabels = mapOf(
                                "dataDeleted" to R.string.withdraw_agree_data, "retention" to R.string.withdraw_agree_retention,
                                "rejoinBlocked" to R.string.withdraw_agree_rejoin, WithdrawUseCase.AUTO_CANCEL to R.string.withdraw_agree_auto_cancel,
                            )
                            s.agreementKeys.forEach { k -> WithdrawCheckRow(stringResource(R.string.withdraw_required, stringResource(agreeLabels.getValue(k))), k in s.checked, { viewModel.toggle(k) }) }
                        }
                    }
                    WithdrawStep.REASON -> {
                        Text(stringResource(R.string.withdraw_reason_head), style = AppTypography.Title20, color = AppColors.TextHead)
                        info.reasonGroups.forEach { g -> WithdrawReasonGroup(g.title, g.items, s.reasonCode, viewModel::onReason) }
                        if (s.reasonCode == WithdrawUseCase.ETC) FormField(stringResource(R.string.withdraw_etc_label)) {
                            FormTextField(s.reasonText, viewModel::onReasonText, stringResource(R.string.withdraw_etc_placeholder), max = 200, tag = "withdraw.etc")
                        }
                    }
                    WithdrawStep.CONFIRM -> {
                        Text(
                            if (info.requiresPassword) stringResource(R.string.withdraw_confirm_password_head)
                            else stringResource(R.string.withdraw_confirm_text_head, info.confirmText),
                            style = AppTypography.Title20, color = AppColors.TextHead,
                        )
                        if (!info.requiresPassword) Text(
                            stringResource(R.string.withdraw_social_note, when (info.provider) { "apple" -> "애플로"; "naver" -> "네이버로"; "kakao" -> "카카오로"; else -> "소셜 로그인으로" }),
                            style = AppTypography.Body14, color = AppColors.TextSub,
                        )
                        FormField(stringResource(if (info.requiresPassword) R.string.password_new else R.string.withdraw_confirm_text)) {
                            if (info.requiresPassword) PasswordField(s.secret, viewModel::onSecret, stringResource(R.string.withdraw_password_placeholder), "withdraw.secret")
                            else FormTextField(s.secret, viewModel::onSecret, info.confirmText, max = 50, tag = "withdraw.secret")
                        }
                    }
                }
                s.message?.let { Text(it, style = AppTypography.Body14, color = AppColors.Danger) }
            }
        }
        if (info != null) {
            val etcMissing = stringResource(R.string.withdraw_etc_missing)
            Row(Modifier.fillMaxWidth().padding(horizontal = Dimensions.gutter, vertical = Dimensions.spacingM), horizontalArrangement = Arrangement.spacedBy(Dimensions.spacingS)) {
                OutlinedButton(onClick = back, shape = RoundedCornerShape(Dimensions.spacingS), modifier = Modifier.weight(1f).height(52.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = Dimensions.spacingS)) {
                    Text(stringResource(if (s.step == WithdrawStep.INFO) R.string.common_cancel else R.string.withdraw_prev), style = AppTypography.Body15Medium, color = AppColors.TextBody, maxLines = 1)
                }
                val (label, enabled, action) = when (s.step) {
                    WithdrawStep.INFO -> Triple(R.string.withdraw_agree_go, s.canProceed, viewModel::toReason)
                    WithdrawStep.REASON -> Triple(R.string.withdraw_next, s.reasonCode.isNotEmpty(), { viewModel.toConfirm(etcMissing) })
                    WithdrawStep.CONFIRM -> Triple(if (s.busy) R.string.withdraw_busy else R.string.withdraw_do, !s.busy && s.secret.isNotBlank(), viewModel::submit)
                }
                // 오른쪽(주 버튼)을 넓게 — '동의하고 탈퇴하기'가 두 줄로 꺾여 아래가 잘리던 것(2026-11-08 사용자 신고).
                Button(
                    onClick = action, enabled = enabled, shape = RoundedCornerShape(Dimensions.spacingS), modifier = Modifier.weight(1.8f).height(52.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = Dimensions.spacingS),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary, contentColor = AppColors.TextOnPrimary),
                ) { Text(stringResource(label), style = AppTypography.Body15Medium, maxLines = 1) }
            }
        }
    }
    s.doneRejoin?.let { d ->
        CustomDialog(
            title = stringResource(R.string.withdraw_done), content = d.ifBlank { null }?.let { stringResource(R.string.withdraw_rejoin, it) },
            leftButtonText = null, rightButtonText = stringResource(R.string.common_confirm), buttonCount = 1, alignment = ContentAlignment.Middle,
            onDismiss = viewModel::finish, onRightButtonClick = viewModel::finish, showPopup = true,
        )
    }
}
