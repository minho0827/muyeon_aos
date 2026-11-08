package com.muyeon.app.ui.alerts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muyeon.app.R
import com.muyeon.app.common_components.dialog.ContentAlignment
import com.muyeon.app.common_components.dialog.CustomDialog
import com.muyeon.app.ui.alerts.components.AlertCheckGroup
import com.muyeon.app.ui.alerts.components.AlertCheckRow
import com.muyeon.app.ui.alerts.components.AlertRegionField
import com.muyeon.app.ui.components.MainTopBar
import com.muyeon.app.ui.components.MdButton
import com.muyeon.app.ui.lessons.LessonLabels
import com.muyeon.app.ui.recruit.RecruitLabels
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.Dimensions
import com.muyeon.domain.model.alert.AlertOptions
import com.muyeon.domain.model.teacher.LessonGenres
import org.koin.androidx.compose.koinViewModel

/**
 * ★ 2026-11-08 (b4) 관심 조건 알림 — 웹 /alerts 그대로(위에서부터): 안내 2줄 · 받고 싶은 알림 · 관심 지역 · 관심 장르 ·
 *   (강사·학원·공연팀) 관심 수업 분야/원하는 강사 분야 · (강사·무용수) 관심 수업 대상 · '조건 매칭 알림 받기' · [취소][저장].
 *   필수 칸이 없다(비워 두면 그 조건은 따지지 않는다). 저장되면 팝업 → 닫으면 MY.
 */
@Composable
fun AlertSettingsScreen(onBack: () -> Unit, onSaved: () -> Unit, viewModel: AlertSettingsViewModel = koinViewModel()) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    val token = com.muyeon.app.utils.TokenManager.getAccessToken(LocalContext.current)
    Column(Modifier.fillMaxSize().background(AppColors.Surface).navigationBarsPadding()) {
        MainTopBar(title = stringResource(R.string.alerts_title), primary = false, onBack = onBack)
        if (s.isLoading) return@Column
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Dimensions.gutter, vertical = Dimensions.spacingXL),
            verticalArrangement = Arrangement.spacedBy(Dimensions.spacingXL),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Dimensions.spacingXS)) {
                Text(stringResource(R.string.alerts_intro), style = AppTypography.Body14, color = AppColors.TextHead)
                Text(stringResource(R.string.alerts_sub), style = AppTypography.Sub13, color = AppColors.TextSub)
            }
            val p = s.prefs
            if (s.types.isNotEmpty()) AlertCheckGroup(stringResource(R.string.alerts_types), s.types.map { it to stringResource(typeLabel(it)) }, p.types, "atype", viewModel::toggleType)
            AlertRegionField(stringResource(R.string.alerts_region), s.regionName, token, viewModel::pickRegion)
            AlertCheckGroup(stringResource(R.string.alerts_genres), LessonGenres.ALL.map { it to it }, p.genres, "agenre", viewModel::toggleGenre)
            if (s.showFields) AlertCheckGroup(
                stringResource(if (s.fieldsAsTeacher) R.string.alerts_fields_teacher else R.string.alerts_fields_academy),
                AlertOptions.FIELDS.map { it to (LessonLabels.field(it)?.let { r -> stringResource(r) } ?: it) }, p.fields, "afield", viewModel::toggleField,
            )
            if (s.showTargets) AlertCheckGroup(
                stringResource(R.string.alerts_targets),
                AlertOptions.TARGETS.map { it to (RecruitLabels.target(it)?.let { r -> stringResource(r) } ?: it) }, p.targets, "atarget", viewModel::toggleTarget,
            )
            AlertCheckRow(stringResource(R.string.alerts_push), p.push, "alerts.push", viewModel::togglePush)
            Row(horizontalArrangement = Arrangement.spacedBy(Dimensions.spacingS), modifier = Modifier.fillMaxWidth()) {
                MdButton(stringResource(R.string.common_cancel), onBack, Modifier.weight(1f), filled = false, tag = "alerts.cancel")
                MdButton(stringResource(R.string.common_save), viewModel::save, Modifier.weight(1f), loading = s.saving, tag = "alerts.save")
            }
        }
    }
    CustomDialog(
        title = stringResource(R.string.alerts_saved), content = null, leftButtonText = null, rightButtonText = stringResource(R.string.common_confirm),
        buttonCount = 1, alignment = ContentAlignment.Middle, onDismiss = onSaved, onRightButtonClick = onSaved, showPopup = s.saved,
    )
    CustomDialog(
        title = stringResource(R.string.alerts_save_failed), content = stringResource(R.string.alerts_save_failed_body), leftButtonText = null,
        rightButtonText = stringResource(R.string.common_confirm), buttonCount = 1, alignment = ContentAlignment.Middle,
        onDismiss = viewModel::dismissFailed, onRightButtonClick = viewModel::dismissFailed, showPopup = s.saveFailed,
    )
}

private fun typeLabel(t: String) = when (t) {
    "JOB" -> R.string.alerts_type_job
    "SUB" -> R.string.alerts_type_sub
    "CASTING" -> R.string.alerts_type_casting
    else -> R.string.alerts_type_teacher
}
