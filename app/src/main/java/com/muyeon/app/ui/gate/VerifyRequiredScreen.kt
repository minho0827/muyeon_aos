package com.muyeon.app.ui.gate

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import com.muyeon.app.R
import com.muyeon.app.common_components.dialog.ContentAlignment
import com.muyeon.app.common_components.dialog.CustomDialog
import com.muyeon.app.ui.components.MdButton
import com.muyeon.app.ui.gate.components.GateMetrics
import com.muyeon.app.ui.gate.components.GateNoticeHeader
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.Dimensions

/**
 * 학원 인증 강제 차단 안내 — 웹 /verify-required(VerifyRequired.js)와 같은 항목·문구:
 *  그림 · '학원 인증이 필요해요' · 설명 · 회색 상자(안내 + 공지 밑줄 링크) · [학원 인증하기] · [학원 원장이 아니에요 · 상담하기] · 로그아웃(확인 팝업).
 *  상단바·탭바 없음. 관리자 사유는 내부용이라 보이지 않는다.
 */
@Composable
fun VerifyRequiredScreen(
    onNotice: () -> Unit,
    onVerify: () -> Unit,
    onChat: () -> Unit,
    onLogout: () -> Unit,
) {
    var logoutOpen by rememberSaveable { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().background(AppColors.Surface).statusBarsPadding().verticalScroll(rememberScrollState())
            .padding(horizontal = Dimensions.gutter).padding(top = GateMetrics.TOP, bottom = Dimensions.spacing3XL),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(GateMetrics.GAP),
    ) {
        GateNoticeHeader(stringResource(R.string.gate_verify_title), stringResource(R.string.gate_verify_body))
        Column(
            Modifier.fillMaxWidth().background(AppColors.SurfaceMuted, RoundedCornerShape(Dimensions.buttonRadius)).padding(Dimensions.spacingL),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimensions.spacingS),
        ) {
            Text(stringResource(R.string.gate_verify_box), style = AppTypography.Body14, color = AppColors.TextHead, textAlign = TextAlign.Center)
            Text(
                stringResource(R.string.gate_verify_notice),
                style = AppTypography.Body14.copy(textDecoration = TextDecoration.Underline), color = AppColors.TextSub, textAlign = TextAlign.Center,
                modifier = Modifier.clickable(onClick = onNotice),
            )
        }
        Column(Modifier.fillMaxWidth().padding(top = Dimensions.spacingS), verticalArrangement = Arrangement.spacedBy(Dimensions.spacingS), horizontalAlignment = Alignment.CenterHorizontally) {
            MdButton(stringResource(R.string.gate_verify_submit), onVerify, modifier = Modifier.fillMaxWidth(), tag = "gate.verify")
            MdButton(stringResource(R.string.gate_verify_chat), onChat, modifier = Modifier.fillMaxWidth(), filled = false, tag = "gate.chat")
            Text(
                stringResource(R.string.gate_verify_logout),
                style = AppTypography.Body14.copy(textDecoration = TextDecoration.Underline), color = AppColors.TextSub,
                modifier = Modifier.clickable { logoutOpen = true }.padding(Dimensions.spacingS),
            )
        }
    }
    if (logoutOpen) {
        CustomDialog(
            title = stringResource(R.string.gate_verify_logout_confirm), content = null,
            leftButtonText = stringResource(R.string.gate_verify_logout_cancel), rightButtonText = stringResource(R.string.gate_verify_logout),
            buttonCount = 2, alignment = ContentAlignment.Middle,
            onDismiss = { logoutOpen = false }, onLeftButtonClick = { logoutOpen = false },
            onRightButtonClick = { logoutOpen = false; onLogout() }, showPopup = true,
        )
    }
}
