package com.muyeon.app.ui.credential.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.OutlinedButton
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

/** 확인 중·확인 완료 항목의 [철회] — 누르면 확인('서류가 지워지고 배지도 빠져요. 기록은 남아요.') 뒤 [onConfirm]. */
@Composable
fun CredentialWithdrawAction(busy: Boolean, onConfirm: () -> Unit) {
    var confirm by remember { mutableStateOf(false) }
    OutlinedButton(
        onClick = { confirm = true }, enabled = !busy, shape = CircleShape,
        modifier = Modifier.fillMaxWidth().padding(top = Dimensions.spacingS).testTag("credential.withdraw"),
    ) { Text(stringResource(R.string.credential_withdraw), style = AppTypography.Body14SemiBold, color = AppColors.TextSub) }

    if (confirm) {
        MuyeonDialog(
            title = stringResource(R.string.credential_withdraw_title),
            message = stringResource(R.string.credential_withdraw_body),
            actions = listOf(
                MuyeonDialogAction(stringResource(R.string.credential_withdraw), MuyeonDialogKind.DESTRUCTIVE, onConfirm),
                MuyeonDialogAction(stringResource(R.string.common_cancel), MuyeonDialogKind.CANCEL),
            ),
            onDismiss = { confirm = false },
        )
    }
}
