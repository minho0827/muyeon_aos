package com.muyeon.app.ui.auth.signup

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muyeon.app.R
import com.muyeon.app.ui.account.components.IdentityWebView
import com.muyeon.app.ui.account.components.PasswordField
import com.muyeon.app.ui.auth.signup.components.SignupDoneDialog
import com.muyeon.app.ui.auth.signup.components.SignupEmailVerifySection
import com.muyeon.app.ui.auth.signup.components.SignupIdentitySection
import com.muyeon.app.ui.common.FormField
import com.muyeon.app.ui.common.FormReadOnlyBox
import com.muyeon.app.ui.common.FormTextField
import com.muyeon.app.ui.common.LocalRequiredForm
import com.muyeon.app.ui.common.MuyeonBottomButton
import com.muyeon.app.ui.common.rememberRequiredForm
import com.muyeon.app.ui.components.MainTopBar
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.Dimensions
import com.muyeon.domain.model.auth.LoginSession
import com.muyeon.domain.usecase.auth.SignupIdentityUseCase
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * 이메일 가입 — 웹 /register(Register.js)와 같은 항목·순서:
 *  이메일(인증번호) · 휴대폰 본인인증(서버가 켰을 때 — 비밀번호보다 앞) · 비밀번호 · 비밀번호 확인 · 이름(본인인증 뒤 잠김) · 휴대폰(본인인증 안 했을 때만) · [가입하기].
 *  필수 칸은 공용 RequiredForm(빠진 칸으로 스크롤·빨간 제목·토스트), 그 밖 확인은 웹 문구 그대로 아래 빨강.
 *  [onRegistered] = 가입 성공(로그인 후처리) · [onDone] = 완료 팝업 [확인](다음: 역할 온보딩 / 하던 일).
 */
@Composable
fun RegisterScreen(
    agreements: Map<String, Boolean>,
    onBack: () -> Unit,
    onRegistered: (LoginSession) -> Unit,
    onDone: () -> Unit,
    viewModel: RegisterViewModel = koinViewModel { parametersOf(agreements) },
) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val form = rememberRequiredForm()
    LaunchedEffect(viewModel) { viewModel.start() }
    LaunchedEffect(viewModel) { viewModel.registered.collect(onRegistered) }
    s.identityUrl?.let { url ->
        IdentityWebView(url, viewModel::onIdentityResult, returnTo = SignupIdentityUseCase.RETURN_REGISTER)
        return
    }
    Column(Modifier.fillMaxSize().background(AppColors.Surface).imePadding()) {
        MainTopBar(title = stringResource(R.string.auth_signup_title), primary = false, onBack = onBack)
        CompositionLocalProvider(LocalRequiredForm provides form) {
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = Dimensions.gutter, vertical = Dimensions.spacing3XL + Dimensions.spacingS),
                verticalArrangement = Arrangement.spacedBy(Dimensions.spacingXL),
            ) {
                Text(stringResource(R.string.auth_signup_lead), style = AppTypography.Title18, color = AppColors.TextHead)
                SignupEmailVerifySection(
                    email = s.email, code = s.code, codeSent = s.codeSent, verified = s.emailVerified,
                    sending = s.sending, verifying = s.verifying,
                    info = s.emailInfoText ?: s.emailInfoRes?.let { stringResource(it) }, infoError = s.emailInfoError,
                    onEmail = viewModel::onEmail, onCode = viewModel::onCode, onSendCode = viewModel::sendCode, onVerify = viewModel::verifyCode,
                )
                if (s.identityAvailable) {
                    SignupIdentitySection(
                        done = s.identity != null, required = s.identityRequired, busy = s.identityBusy,
                        onStart = viewModel::startIdentity, showHint = true,
                        message = s.identityText ?: s.identityRes?.let { stringResource(it) },
                    )
                }
                FormField(stringResource(R.string.auth_signup_password), required = true, filled = s.password.isNotEmpty()) {
                    PasswordField(s.password, viewModel::onPassword, stringResource(R.string.auth_signup_password_placeholder), "signup.password")
                }
                FormField(stringResource(R.string.auth_signup_confirm), required = true, filled = s.confirm.isNotEmpty()) {
                    PasswordField(s.confirm, viewModel::onConfirm, stringResource(R.string.auth_signup_confirm_placeholder), "signup.confirm")
                }
                // 본인확인을 마쳤으면 이름은 인증 결과가 정본(서버도 덮어쓴다) — 잠근다.
                FormField(stringResource(R.string.auth_signup_name), required = true, filled = s.name.isNotBlank()) {
                    if (s.identity != null) FormReadOnlyBox(s.name)
                    else FormTextField(s.name, viewModel::onName, stringResource(R.string.auth_signup_name_placeholder), RegisterViewModel.MAX_NAME, "signup.name")
                }
                // 본인확인을 했으면 번호를 따로 받지 않는다 — 인증된 번호가 정본.
                if (s.identity == null) {
                    FormField(stringResource(R.string.auth_signup_phone)) {
                        FormTextField(s.phone, viewModel::onPhone, stringResource(R.string.auth_signup_phone_placeholder), RegisterViewModel.MAX_PHONE, "signup.phone", KeyboardType.Phone)
                    }
                }
                val msg = s.messageText ?: s.messageRes?.let { stringResource(it) }
                if (msg != null) Text(msg, style = AppTypography.Body14, color = AppColors.Danger, modifier = Modifier.fillMaxWidth())
            }
        }
        MuyeonBottomButton(stringResource(R.string.auth_signup_submit), "signup.submit", loading = s.submitting) {
            if (form.validate(context)) viewModel.submit()
        }
    }
    if (s.done) SignupDoneDialog(stringResource(R.string.auth_signup_done)) { viewModel.closeDone(); onDone() }
}
