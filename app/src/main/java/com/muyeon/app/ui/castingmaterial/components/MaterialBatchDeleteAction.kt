package com.muyeon.app.ui.castingmaterial.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.muyeon.app.R
import com.muyeon.app.ui.common.MuyeonDialog
import com.muyeon.app.ui.common.MuyeonDialogAction
import com.muyeon.app.ui.common.MuyeonDialogKind
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.Dimensions

/** 자료 묶음 [삭제] — 확인('이미 만들어진 공고·공연은 그대로 남아요.') 뒤 [onConfirm]. AI 가 읽는 중이면 부르는 쪽이 숨긴다. */
@Composable
fun MaterialBatchDeleteAction(busy: Boolean, onConfirm: () -> Unit, modifier: Modifier = Modifier) {
    var confirm by remember { mutableStateOf(false) }
    Text(
        stringResource(R.string.casting_mat_delete), style = AppTypography.Sub13SemiBold,
        color = if (busy) AppColors.TextHint else AppColors.TextSub,
        modifier = modifier.clickable(enabled = !busy) { confirm = true }.padding(Dimensions.spacingXS).testTag("castingMat.delete"),
    )
    if (confirm) {
        MuyeonDialog(
            title = stringResource(R.string.casting_mat_delete_title),
            message = stringResource(R.string.casting_mat_delete_body),
            actions = listOf(
                MuyeonDialogAction(stringResource(R.string.casting_mat_delete), MuyeonDialogKind.DESTRUCTIVE, onConfirm),
                MuyeonDialogAction(stringResource(R.string.common_cancel), MuyeonDialogKind.CANCEL),
            ),
            onDismiss = { confirm = false },
        )
    }
}
