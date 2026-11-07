package com.muyeon.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.muyeon.app.R
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.Dimensions

/** 하단 시트 버튼 하나. [primary] = 주황 채움, 아니면 외곽선(웹 btn-md filled primary / outlined). */
data class SheetAction(val text: String, val primary: Boolean, val onClick: () -> Unit)

/**
 * 하단 행동 시트 — 웹 .modal-bottom(제목 줄 + 닫기 X + 버튼 줄). 내 글·내 댓글(수정/삭제), 남의 댓글(신고/차단) 등.
 *   버튼은 가로로 반씩 나눈다(웹 btn-group). 처음부터 다 펼친다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionSheet(title: String, actions: List<SheetAction>, onDismiss: () -> Unit, description: String? = null) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = AppColors.Surface,
        dragHandle = null,
    ) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(Dimensions.gutter), verticalArrangement = Arrangement.spacedBy(Dimensions.spacingXL)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = AppTypography.Title18, color = AppColors.TextHead, modifier = Modifier.weight(1f))
                Icon(
                    painterResource(R.drawable.ic_web_close),
                    contentDescription = stringResource(R.string.common_close),
                    tint = AppColors.TextHead,
                    modifier = Modifier.size(Dimensions.iconL).clickable(onClick = onDismiss),
                )
            }
            if (description != null) Text(description, style = AppTypography.Body14, color = AppColors.TextSub)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimensions.spacingS)) {
                actions.forEach { a -> SheetButton(a, Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
fun SheetButton(a: SheetAction, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val shape = RoundedCornerShape(Dimensions.spacingS)
    if (a.primary) {
        Button(
            onClick = a.onClick, enabled = enabled, shape = shape, modifier = modifier.height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary, contentColor = AppColors.TextOnPrimary),
        ) { Text(a.text, style = AppTypography.CardTitle16) }
    } else {
        OutlinedButton(
            onClick = a.onClick, enabled = enabled, shape = shape, modifier = modifier.height(48.dp),
            border = BorderStroke(1.dp, AppColors.ChipBorder),
        ) { Text(a.text, style = AppTypography.CardTitle16, color = AppColors.TextBody) }
    }
}
