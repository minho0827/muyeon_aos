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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muyeon.app.R
import com.muyeon.app.auth.LoginProvider
import com.muyeon.app.ui.account.components.PasswordField
import com.muyeon.app.ui.auth.components.LoginOrDivider
import com.muyeon.app.ui.auth.components.LoginRecoveryLinks
import com.muyeon.app.ui.auth.components.LoginRememberRow
import com.muyeon.app.ui.auth.components.LoginSignupDialog
import com.muyeon.app.ui.auth.components.LoginSocialButtons
import com.muyeon.app.ui.common.FormField
import com.muyeon.app.ui.common.FormTextField
import com.muyeon.app.ui.components.LevelBadge
import com.muyeon.app.ui.components.MainTopBar
import com.muyeon.app.ui.components.MdButton
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.CategoryColors
import com.muyeon.app.ui.theme.Dimensions
import com.muyeon.domain.model.auth.LoginSession
import org.koin.androidx.compose.koinViewModel

private val SidePadding = 32.dp   // 웹 #login .content-box padding 0 3.2rem

/**
 * 로그인 — 웹 pages/login/Login.js 와 같은 항목·순서:
 *   인사말 → 이메일 · 비밀번호 → 아이디·비밀번호 저장 → (오류) → [로그인] [회원가입] → 또는 → 카카오(·네이버) → 아이디 찾기 | 비밀번호 찾기.
 *   [회원가입]은 가입 방법 고르기 팝업(웹 '무용연 시작하기').
 */
@Composable
fun LoginScreen(
    onBack: () -> Unit,
    onLoggedIn: (LoginSession) -> Unit,
    onFindId: () -> Unit,
    onFindPassword: () -> Unit,
    onSocial: (LoginProvider) -> Unit,
    onSignupEmail: () -> Unit,
    showNaver: Boolean,
    viewModel: LoginViewModel = koinViewModel(),
) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val recent = remember { LoginProvider.last(context) }
    LaunchedEffect(viewModel) { viewModel.loggedIn.collect(onLoggedIn) }

    Column(Modifier.fillMaxSize().background(AppColors.Surface).imePadding()) {
        MainTopBar(title = stringResource(R.string.auth_login_title), primary = false, onBack = onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = SidePadding).padding(top = Dimensions.spacing3XL, bottom = Dimensions.spacing3XL),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                stringResource(R.string.auth_login_greeting), style = AppTypography.Title22, color = AppColors.TextHead,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
            )
            Column(Modifier.fillMaxWidth().padding(top = Dimensions.spacing3XL), verticalArrangement = Arrangement.spacedBy(Dimensions.spacingM)) {
                FormField(stringResource(R.string.auth_login_email), required = true) {
                    FormTextField(s.email, viewModel::onEmail, stringResource(R.string.auth_login_email_placeholder), 50, "login.email", KeyboardType.Email)
                }
                FormField(stringResource(R.string.auth_login_password), required = true) {
                    PasswordField(s.password, viewModel::onPassword, stringResource(R.string.auth_login_password_placeholder), "login.password")
                }
            }
            Column(Modifier.fillMaxWidth()) {
                LoginRememberRow(stringResource(R.string.auth_login_remember), s.remember, viewModel::onRemember)
                val msg = s.messageText ?: s.messageRes?.let { stringResource(it) }
                if (msg != null) Text(msg, style = AppTypography.Body14, color = AppColors.Danger, modifier = Modifier.padding(top = Dimensions.spacingXS))
            }
            Column(
                Modifier.fillMaxWidth().padding(top = Dimensions.spacingL),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Dimensions.spacingL),
            ) {
                if (recent == LoginProvider.EMAIL) LevelBadge(stringResource(R.string.auth_login_recent), CategoryColors.Level.End)
                MdButton(stringResource(R.string.auth_login_submit), viewModel::submit, loading = s.loading, tag = "login.submit")
                MdButton(stringResource(R.string.auth_login_signup), viewModel::openSignup, filled = false, tag = "login.signup")
            }
            LoginOrDivider(stringResource(R.string.auth_login_or))
            LoginSocialButtons(showNaver, recent, onSocial)
            LoginRecoveryLinks(
                stringResource(R.string.auth_login_find_id), stringResource(R.string.auth_login_find_password),
                onFindId, onFindPassword,
            )
        }
    }
    if (s.signupOpen) {
        LoginSignupDialog(
            showNaver = showNaver,
            onSocial = { p -> viewModel.closeSignup(); onSocial(p) },
            onEmail = { viewModel.closeSignup(); onSignupEmail() },
            onClose = viewModel::closeSignup,
        )
    }
}
