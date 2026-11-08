package com.muyeon.app.ui.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muyeon.app.BuildConfig
import com.muyeon.app.R
import com.muyeon.app.common_components.dialog.ContentAlignment
import com.muyeon.app.common_components.dialog.CustomDialog
import com.muyeon.app.ui.common.MuyeonGroupRow
import com.muyeon.app.ui.common.MuyeonGroupSection
import com.muyeon.app.ui.common.MuyeonSwitch
import com.muyeon.app.ui.components.MainTopBar
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.Dimensions
import org.koin.androidx.compose.koinViewModel

/**
 * 설정 — 웹 /setting(알림설정 · 개인정보 보호 · 고객센터 · 약관안내 · 앱 버전) + 계정(로그아웃 · 회원탈퇴).
 *   PUSH 수신은 기기 알림 권한이다 — 켜고 끄기는 시스템 앱 알림 설정에서(돌아오면 다시 읽는다).
 *
 * @param onOpenPath 웹에 남은 화면(대타 신뢰도·약관·회원탈퇴)·앱 화면 주소
 * @param onLoggedOut 로그아웃 뒤(셸이 탭·유형을 다시 읽고 홈으로)
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenPath: (String) -> Unit,
    onNotificationSettings: () -> Unit,
    onBlockedUsers: () -> Unit,
    onLoggedOut: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var pushOn by remember { mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled()) }
    LifecycleResumeEffect(Unit) {
        pushOn = NotificationManagerCompat.from(context).areNotificationsEnabled()
        viewModel.onResume()
        onPauseOrDispose { }
    }
    LaunchedEffect(viewModel) { viewModel.events.collect { if (it == SettingsEvent.LoggedOut) onLoggedOut() } }
    val openAppNotificationSettings = {
        val i = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        runCatching { context.startActivity(i) }
        Unit
    }
    val open: (Intent) -> Unit = { runCatching { context.startActivity(it) } }
    val tel = stringResource(R.string.company_tel)
    val email = stringResource(R.string.company_email)

    Column(Modifier.fillMaxSize().background(AppColors.Surface).navigationBarsPadding()) {
        MainTopBar(title = stringResource(R.string.settings_title), primary = false, onBack = onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Dimensions.gutter),
            verticalArrangement = Arrangement.spacedBy(Dimensions.spacingXXL),
        ) {
            MuyeonGroupSection(stringResource(R.string.settings_section_noti)) {
                MuyeonGroupRow(stringResource(R.string.settings_push), onClick = openAppNotificationSettings, showChevron = false,
                    accessory = { MuyeonSwitch(pushOn, { openAppNotificationSettings() }) })
                if (s.isLoggedIn) {
                    MuyeonGroupRow(stringResource(R.string.settings_noti_kinds), onClick = onNotificationSettings)
                    MuyeonGroupRow(stringResource(R.string.settings_sub_trust), onClick = { onOpenPath("/subTrust") })
                }
            }
            if (s.isLoggedIn) MuyeonGroupSection(stringResource(R.string.settings_section_privacy)) {
                MuyeonGroupRow(stringResource(R.string.settings_blocked_users), onClick = onBlockedUsers)
            }
            MuyeonGroupSection(stringResource(R.string.settings_section_support)) {
                MuyeonGroupRow(stringResource(R.string.settings_tel, tel), onClick = { open(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + tel.replace("-", "")))) })
                MuyeonGroupRow(stringResource(R.string.settings_email, email), onClick = { open(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email"))) })
                MuyeonGroupRow(stringResource(R.string.settings_report_24h))
            }
            MuyeonGroupSection(stringResource(R.string.settings_section_policy)) {
                listOf(
                    "terms" to R.string.settings_policy_terms, "paid" to R.string.settings_policy_paid, "privacy" to R.string.settings_policy_privacy,
                    "location" to R.string.settings_policy_location, "refund" to R.string.settings_policy_refund, "company" to R.string.settings_policy_company,
                ).forEach { (doc, label) -> MuyeonGroupRow(stringResource(label), onClick = { onOpenPath("/policy?doc=$doc") }) }
            }
            MuyeonGroupSection(stringResource(R.string.settings_section_version)) {
                MuyeonGroupRow(stringResource(R.string.settings_version), value = BuildConfig.VERSION_NAME)
            }
            if (s.isLoggedIn) MuyeonGroupSection(stringResource(R.string.settings_section_account)) {
                MuyeonGroupRow(stringResource(R.string.my_logout), onClick = { viewModel.askLogout(true) })
                MuyeonGroupRow(stringResource(R.string.settings_withdraw), valueColor = AppColors.TextSub, onClick = { onOpenPath("/withdraw") })
            }
        }
    }
    CustomDialog(
        title = stringResource(R.string.my_logout_q), content = null,
        leftButtonText = stringResource(R.string.common_cancel), rightButtonText = stringResource(R.string.my_logout),
        buttonCount = 2, alignment = ContentAlignment.Middle,
        onDismiss = { viewModel.askLogout(false) }, onLeftButtonClick = { viewModel.askLogout(false) },
        onRightButtonClick = viewModel::logout, showPopup = s.confirmLogout,
    )
}
