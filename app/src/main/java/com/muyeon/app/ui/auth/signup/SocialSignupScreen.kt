package com.muyeon.app.ui.auth.signup

import androidx.activity.compose.BackHandler
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muyeon.app.R
import com.muyeon.app.ui.account.components.IdentityWebView
import com.muyeon.app.ui.auth.signup.components.SignupDoneDialog
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
import com.muyeon.domain.usecase.auth.SignupIdentityUseCase
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * 소셜 신규회원 추가정보 — 웹 /social-signup(SocialSignup.js): 이름(본인인증 뒤 잠김) · 휴대폰 본인인증 · [시작하기].
 *  뒤로 = 가입 그만두기(로그아웃 후 홈 — 다음에 소셜 로그인하면 여기부터 다시, 웹과 같다) → [onQuit].
 */
@Composable
fun SocialSignupScreen(
    currentName: String?,
    onQuit: () -> Unit,
    onCompleted: (String) -> Unit,
    onDone: () -> Unit,
    viewModel: SocialSignupViewModel = koinViewModel { parametersOf(currentName) },
) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val form = rememberRequiredForm()
    LaunchedEffect(viewModel) { viewModel.start() }
    LaunchedEffect(viewModel) { viewModel.completed.collect(onCompleted) }
    s.identityUrl?.let { url ->
        IdentityWebView(url, viewModel::onIdentityResult, returnTo = SignupIdentityUseCase.RETURN_SOCIAL)
        return
    }
    BackHandler(onBack = onQuit)
    Column(Modifier.fillMaxSize().background(AppColors.Surface).imePadding()) {
        MainTopBar(title = stringResource(R.string.auth_social_title), primary = false, onBack = onQuit)
        CompositionLocalProvider(LocalRequiredForm provides form) {
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = Dimensions.gutter, vertical = Dimensions.spacing3XL + Dimensions.spacingS),
                verticalArrangement = Arrangement.spacedBy(Dimensions.spacingXL),
            ) {
                Text(stringResource(R.string.auth_social_lead), style = AppTypography.Title18, color = AppColors.TextHead)
                FormField(stringResource(R.string.auth_signup_name), required = true, filled = s.name.isNotBlank()) {
                    if (s.identity != null) FormReadOnlyBox(s.name)
                    else FormTextField(s.name, viewModel::onName, stringResource(R.string.auth_signup_name_placeholder), RegisterViewModel.MAX_NAME, "social.name")
                }
                if (s.identityAvailable || s.identityRequired) {
                    SignupIdentitySection(
                        done = s.identity != null, required = s.identityRequired, busy = s.identityBusy,
                        onStart = viewModel::startIdentity, showHint = false, enabled = s.identityAvailable,
                    )
                }
                val msg = s.messageText ?: s.messageRes?.let { stringResource(it) }
                if (msg != null) Text(msg, style = AppTypography.Body14, color = AppColors.Danger, modifier = Modifier.fillMaxWidth())
            }
        }
        MuyeonBottomButton(stringResource(R.string.auth_social_submit), "social.submit", loading = s.submitting) {
            if (form.validate(context)) viewModel.submit()
        }
    }
    if (s.done) SignupDoneDialog(stringResource(R.string.auth_social_done)) { viewModel.closeDone(); onDone() }
}
