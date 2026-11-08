package com.muyeon.app.ui.account

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muyeon.app.R
import com.muyeon.app.common_components.dialog.ContentAlignment
import com.muyeon.app.common_components.dialog.CustomDialog
import com.muyeon.app.ui.account.components.IdentityWebView
import com.muyeon.app.ui.common.FormField
import com.muyeon.app.ui.common.FormReadOnlyBox
import com.muyeon.app.ui.common.FormTextField
import com.muyeon.app.ui.common.LocalRequiredForm
import com.muyeon.app.ui.common.MuyeonBottomButton
import com.muyeon.app.ui.common.rememberRequiredForm
import com.muyeon.app.ui.components.Avatar
import com.muyeon.app.ui.components.EmptyView
import com.muyeon.app.ui.components.MainTopBar
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.Dimensions
import com.muyeon.app.webview.ActiveRole
import org.koin.androidx.compose.koinViewModel

/**
 * 개인정보 수정 — 웹 /profileEdit. 사진(보기만) · 비밀번호 변경(웹) · 이메일(읽기) · 회원유형(→ 유형 화면) · 이름(필수, 본인확인이면 고정) ·
 *   휴대폰(읽기 + [본인인증] → 앱 안 표준창) · 회원탈퇴(웹) · [확인] 저장(RequiredForm).
 */
@Composable
fun ProfileEditScreen(
    onClose: () -> Unit,
    onOpenPath: (String) -> Unit,
    onMemberType: () -> Unit,
    viewModel: ProfileEditViewModel = koinViewModel(),
) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val form = rememberRequiredForm()
    LaunchedEffect(viewModel) { viewModel.events.collect { if (it == ProfileEditEvent.Close) onClose() } }
    val identityUrl = s.identityUrl
    if (identityUrl != null) {
        IdentityWebView(identityUrl, viewModel::onIdentityResult)
        return
    }
    BackHandler(onBack = viewModel::requestBack)

    Column(Modifier.fillMaxSize().background(AppColors.Surface).navigationBarsPadding().imePadding()) {
        MainTopBar(title = stringResource(R.string.account_title), primary = false, onBack = viewModel::requestBack)
        val a = s.account
        when {
            s.isLoading -> Unit
            a == null -> EmptyView(stringResource(R.string.common_list_failed), content = s.errorMessage,
                buttonText = stringResource(R.string.common_retry), onButton = viewModel::load)
            else -> {
                CompositionLocalProvider(LocalRequiredForm provides form) {
                    Column(
                        Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(Dimensions.gutter),
                        verticalArrangement = Arrangement.spacedBy(Dimensions.spacingXL),
                    ) {
                        Text(stringResource(R.string.account_head, s.name.ifBlank { stringResource(R.string.blocked_member) }), style = AppTypography.Title20, color = AppColors.TextHead)
                        // 사진 — 보기만 한다(2026-11-08 사용자 결정: 이 사진은 이력서·강사프로필 사진과 같아서 여기서는 바꾸지 않는다).
                        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) { Avatar(s.image, 96.dp) }
                        FormField(stringResource(R.string.account_password)) {
                            OutlinedButton(onClick = { onOpenPath("/changePassword") }, shape = CircleShape) {
                                Text(stringResource(R.string.account_password_change), style = AppTypography.Body14SemiBold, color = AppColors.TextBody)
                            }
                        }
                        FormField(stringResource(R.string.account_email)) { FormReadOnlyBox(a.email) }
                        val general = stringResource(R.string.account_member_general)
                        FormField(stringResource(R.string.account_member_type)) {
                            Row(Modifier.fillMaxWidth().clickable(onClick = onMemberType), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.weight(1f)) {
                                    FormReadOnlyBox(a.memberTypes.takeIf { it.isNotEmpty() }?.joinToString(", ") { t -> if (t == "HOBBY" || t == "GENERAL") general else ActiveRole.label(t) } ?: general)
                                }
                                Text(stringResource(R.string.account_member_change), style = AppTypography.Sub13, color = AppColors.TextSub, modifier = Modifier.padding(start = Dimensions.spacingS))
                            }
                        }
                        FormField(stringResource(R.string.account_name), required = !a.nameLocked, filled = s.name.isNotBlank()) {
                            if (a.nameLocked) FormReadOnlyBox(s.name)
                            else FormTextField(s.name, viewModel::onName, placeholder = stringResource(R.string.account_name_placeholder), max = 30, tag = "account.name")
                            if (a.nameLocked) Text(stringResource(R.string.account_name_locked), style = AppTypography.Sub13, color = AppColors.TextSub)
                        }
                        FormField(stringResource(R.string.account_phone)) {
                            FormReadOnlyBox(a.phone.ifBlank { stringResource(R.string.account_phone_placeholder) })
                            OutlinedButton(onClick = viewModel::startIdentity, enabled = !s.identityBusy, shape = CircleShape, modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    stringResource(if (s.identityBusy) R.string.account_identity_busy else if (a.phone.isNotBlank()) R.string.account_phone_change else R.string.account_phone_register),
                                    style = AppTypography.Body14SemiBold, color = AppColors.TextBody,
                                )
                            }
                            Text(
                                stringResource(if (a.phoneVerified) R.string.account_phone_verified else if (a.phone.isNotBlank()) R.string.account_phone_unverified else R.string.account_phone_none),
                                style = AppTypography.Sub13, color = if (a.phoneVerified) AppColors.Primary else AppColors.TextSub,
                            )
                            Text(stringResource(R.string.account_identity_hint), style = AppTypography.Sub13, color = AppColors.TextSub)
                            s.identityMessage?.let { Text(it, style = AppTypography.Sub13, color = AppColors.Danger) }
                        }
                        Text(stringResource(R.string.settings_withdraw), style = AppTypography.Sub13, color = AppColors.TextSub,
                            modifier = Modifier.clickable { onOpenPath("/withdraw") }.padding(vertical = Dimensions.spacingS))
                    }
                }
                MuyeonBottomButton(stringResource(R.string.common_confirm), "account.save", loading = s.saving) {
                    if (form.validate(context)) viewModel.save()
                }
            }
        }
    }
    s.resultMessage?.let { m ->
        CustomDialog(
            title = m, content = null, leftButtonText = null, rightButtonText = stringResource(R.string.common_confirm),
            buttonCount = 1, alignment = ContentAlignment.Middle, onDismiss = viewModel::dismissResult, onRightButtonClick = viewModel::dismissResult, showPopup = true,
        )
    }
    CustomDialog(
        title = stringResource(R.string.account_leave_q), content = stringResource(R.string.account_leave_body),
        leftButtonText = stringResource(R.string.common_cancel), rightButtonText = stringResource(R.string.account_leave),
        buttonCount = 2, alignment = ContentAlignment.Middle,
        onDismiss = viewModel::cancelLeave, onLeftButtonClick = viewModel::cancelLeave, onRightButtonClick = viewModel::leave, showPopup = s.confirmLeave,
    )
}
