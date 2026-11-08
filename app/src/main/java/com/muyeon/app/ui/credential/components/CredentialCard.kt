package com.muyeon.app.ui.credential.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.muyeon.app.R
import com.muyeon.app.ui.components.RemoteImage
import com.muyeon.app.ui.credential.PickedDoc
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.Dimensions
import com.muyeon.domain.model.credential.CredentialItem

/**
 * 경력·자격 한 항목 — 웹 CredentialsManage 회색 카드(이름 · 상태 · 설명 · 필요 서류 · 확인 내용/반려 사유 · [서류 등록]).
 *   [editing] 이면 개인정보 안내 + 고른 서류(사진 미리보기·PDF) + [+ 추가] + [취소][등록하기].
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CredentialCard(
    item: CredentialItem,
    editing: Boolean,
    files: List<PickedDoc>,
    busy: Boolean,
    canAdd: Boolean,
    onStart: () -> Unit,
    onAdd: () -> Unit,
    onRemove: (Int) -> Unit,
    onCancel: () -> Unit,
    onSubmit: () -> Unit,
) {
    val (statusText, statusColor) = when (item.status) {
        "PENDING" -> R.string.credential_status_pending to AppColors.Primary
        "APPROVED" -> R.string.credential_status_approved to AppColors.Positive
        "REJECTED" -> R.string.credential_status_rejected to AppColors.Danger
        else -> R.string.credential_status_none to AppColors.TextHint
    }
    Column(
        Modifier.fillMaxWidth().background(AppColors.SurfaceMuted, RoundedCornerShape(Dimensions.spacingM)).padding(Dimensions.spacingL),
        verticalArrangement = Arrangement.spacedBy(Dimensions.spacingXS),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(item.label, style = AppTypography.Section16, color = AppColors.TextHead, modifier = Modifier.weight(1f))
            Text(stringResource(statusText), style = AppTypography.Sub13SemiBold, color = statusColor)
        }
        item.desc?.let { Text(it, style = AppTypography.Sub13, color = AppColors.TextSub) }
        item.docs?.let { Text(it, style = AppTypography.Sub13, color = AppColors.TextSub) }
        if (item.status == "APPROVED" && (item.title != null || item.years != null)) {
            Text(listOfNotNull(item.title, item.years?.let { stringResource(R.string.credential_years, it) }).joinToString(" · "),
                style = AppTypography.Sub13, color = AppColors.TextBody)
        }
        if (item.status == "REJECTED") item.memo?.let { Text(stringResource(R.string.credential_reject_reason, it), style = AppTypography.Sub13, color = AppColors.Danger) }
        if (item.canSubmit && !editing) {
            OutlinedButton(onClick = onStart, shape = CircleShape, modifier = Modifier.fillMaxWidth().padding(top = Dimensions.spacingS)) {
                Text(stringResource(if (item.status == "REJECTED") R.string.credential_resubmit else R.string.credential_submit_docs), style = AppTypography.Body14SemiBold, color = AppColors.TextBody)
            }
        }
        if (editing) {
            Text(stringResource(R.string.credential_notice), style = AppTypography.Sub13, color = AppColors.TextSub, modifier = Modifier.padding(top = Dimensions.spacingS))
            FlowRow(Modifier.padding(top = Dimensions.spacingS), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                files.forEachIndexed { i, f ->
                    Box(Modifier.size(THUMB).clip(RoundedCornerShape(6.dp)).border(1.dp, AppColors.ChipBorder, RoundedCornerShape(6.dp)).background(AppColors.Surface), Alignment.Center) {
                        if (f.isPdf) Text("PDF", style = AppTypography.Caption12, color = AppColors.TextSub)
                        else RemoteImage(f.uri, Modifier.size(THUMB))
                        Box(Modifier.align(Alignment.TopEnd).padding(2.dp).size(20.dp).background(AppColors.Scrim, CircleShape).clickable { onRemove(i) }, Alignment.Center) {
                            Text("×", style = AppTypography.Caption12, color = AppColors.TextOnPrimary)
                        }
                    }
                }
                if (canAdd) Box(
                    Modifier.size(THUMB).clip(RoundedCornerShape(6.dp)).border(1.dp, AppColors.ChipBorder, RoundedCornerShape(6.dp)).clickable(onClick = onAdd),
                    Alignment.Center,
                ) { Text(stringResource(R.string.credential_add), style = AppTypography.Sub13, color = AppColors.TextSub) }
            }
            Row(Modifier.fillMaxWidth().padding(top = Dimensions.spacingM), horizontalArrangement = Arrangement.spacedBy(Dimensions.spacingS)) {
                OutlinedButton(onClick = onCancel, enabled = !busy, shape = RoundedCornerShape(Dimensions.spacingS), modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.common_cancel), style = AppTypography.Body14SemiBold, color = AppColors.TextBody)
                }
                Button(
                    onClick = onSubmit, enabled = !busy && files.isNotEmpty(), shape = RoundedCornerShape(Dimensions.spacingS), modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary, contentColor = AppColors.TextOnPrimary),
                ) { Text(stringResource(if (busy) R.string.credential_submitting else R.string.credential_submit), style = AppTypography.Body14SemiBold) }
            }
        }
    }
}

private val THUMB = 72.dp
