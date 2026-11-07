package com.muyeon.app.ui.my

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muyeon.app.R
import com.muyeon.app.common_components.dialog.ContentAlignment
import com.muyeon.app.common_components.dialog.CustomDialog
import com.muyeon.app.navigation.WebPaths
import com.muyeon.app.ui.components.MainTopBar
import com.muyeon.app.ui.main.MainTab
import com.muyeon.app.ui.my.components.GuestMyHome
import com.muyeon.app.ui.my.components.MemberMyHome
import com.muyeon.app.ui.theme.AppColors
import org.koin.androidx.compose.koinViewModel

/**
 * MY 탭 — 웹 pages/muyeon/my/MyHome.js. 비회원이면 [GuestMyHome], 회원이면 [MemberMyHome].
 *
 * @param onNativeAction 기존 앱 화면(NativeActionRouter 표)
 * @param onRoleManage 회원유형 화면(탭 셸이 역할 원문을 받아 연다)
 * @param onOpenCasting 공연팀 '일정' 바로가기 — 캐스팅 화면
 * @param onOpenTab 다른 탭
 * @param onLoggedOut 로그아웃 뒤(탭 셸이 탭 목록·유형을 다시 읽고 홈으로)
 */
@Composable
fun MyScreen(
    isLoggedIn: Boolean,
    onOpenWeb: (String) -> Unit,
    onNativeAction: (String, Map<String, String>) -> Unit,
    onRoleManage: () -> Unit,
    onOpenCasting: () -> Unit,
    onOpenTab: (MainTab) -> Unit,
    /** 커뮤니티(탭이 아니라 MY 안 섹션 — 탭 위 화면으로 연다). */
    onOpenCommunity: () -> Unit,
    onLoggedOut: () -> Unit,
    viewModel: MyViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    LifecycleResumeEffect(isLoggedIn) {
        viewModel.refresh()
        onPauseOrDispose { }
    }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { e -> if (e is MyEvent.LoggedOut) onLoggedOut() }
    }

    Column(Modifier.fillMaxSize().background(AppColors.Surface)) {
        MainTopBar(title = stringResource(R.string.tab_my))
        if (!isLoggedIn) {
            GuestMyHome(onLogin = { onOpenWeb(WebPaths.login(MainTab.MY.webPath)) }, onOpenPath = onOpenWeb, onOpenCommunity = onOpenCommunity)
        } else {
            MemberMyHome(
                state = state,
                onHeader = onRoleManage,
                onMenu = { item ->
                    when {
                        item.bridge == MyActions.MEMBER_TYPE -> onRoleManage()
                        else -> MyActions.of(item.bridge, state.activeType)?.let { onNativeAction(it.action, it.data) }
                            ?: item.path?.let(onOpenWeb)
                    }
                },
                onShortcutCalendar = { onNativeAction("openLessonCalendar", emptyMap()) },
                onShortcutCasting = onOpenCasting,
                onShortcutCommunity = onOpenCommunity,
                onSettings = { onOpenWeb(WebPaths.SETTINGS) },
                onLogout = { viewModel.askLogout(true) },
            )
        }
    }

    CustomDialog(
        title = stringResource(R.string.my_logout_q), content = null,
        leftButtonText = stringResource(R.string.common_cancel), rightButtonText = stringResource(R.string.my_logout),
        buttonCount = 2, alignment = ContentAlignment.Middle,
        onDismiss = { viewModel.askLogout(false) }, onLeftButtonClick = { viewModel.askLogout(false) },
        onRightButtonClick = { viewModel.askLogout(false); viewModel.logout() },
        showPopup = state.confirmLogout,
    )
    val pending = state.info?.pending
    CustomDialog(
        title = stringResource(R.string.my_pending_title),
        content = pending?.let { stringResource(R.string.my_pending_body, it.total, MyMenuBuilder.roleLabel(it.roles.firstOrNull()?.role, context::getString)) },
        leftButtonText = stringResource(R.string.my_pending_later), rightButtonText = stringResource(R.string.my_pending_verify),
        buttonCount = 2, alignment = ContentAlignment.Middle,
        onDismiss = viewModel::dismissPendingNotice, onLeftButtonClick = viewModel::dismissPendingNotice,
        onRightButtonClick = {
            viewModel.dismissPendingNotice()
            // 인증하러 가기 — 비공개 공고의 유형 인증(앱 온보딩 인증 화면), 유형을 모르면 내 공고(웹).
            //  강사·무용수는 서류 없이 본인 인증으로 부여 — 그 흐름은 웹(내 공고)이 맡는다(웹 goRoleVerification).
            val role = pending?.roles?.firstOrNull()?.role
            if (role == "ACADEMY" || role == "TEAM") onNativeAction("openRoleVerification", mapOf("role" to role)) else onOpenWeb("/myPostings")
        },
        showPopup = state.pendingNotice,
    )
}
