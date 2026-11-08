package com.muyeon.app.ui.gate

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.muyeon.app.R
import com.muyeon.app.ui.common.MuyeonBottomButton
import com.muyeon.app.ui.gate.components.GateMetrics
import com.muyeon.app.ui.gate.components.GateNoticeHeader
import com.muyeon.app.ui.gate.components.MaintenanceErrorBox
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.Dimensions

/** 시스템 점검 안내 — 웹 /maintenance(Maintenance.js): 그림 · '시스템 점검 중이에요…' · '더 나은 서비스로…' · (개발용 에러) · [홈으로 이동]. */
@Composable
fun MaintenanceScreen(errorJson: String?, onHome: () -> Unit) {
    Column(Modifier.fillMaxSize().background(AppColors.Surface).statusBarsPadding()) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = Dimensions.gutter).padding(top = GateMetrics.TOP),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(GateMetrics.GAP),
        ) {
            GateNoticeHeader(stringResource(R.string.gate_maintenance_title), stringResource(R.string.gate_maintenance_body))
            if (errorJson != null) MaintenanceErrorBox(errorJson)
        }
        MuyeonBottomButton(stringResource(R.string.gate_maintenance_home), "maintenance.home", onClick = onHome)
    }
}
