package com.muyeon.app.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.Dimensions

/** 화면 아래 주 버튼 — 웹 .btn-group > .btn-md.filled.primary(주황 · 높이 48). */
@Composable
fun PrimaryBottomButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** 화면 맨 아래에 붙는 버튼이면 내비게이션 바만큼 띄운다. 화면 중간에 쓰면 false. */
    atScreenBottom: Boolean = true,
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(Dimensions.spacingS),
        colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary, contentColor = AppColors.TextOnPrimary),
        modifier = modifier
            .fillMaxWidth()
            .then(if (atScreenBottom) Modifier.navigationBarsPadding() else Modifier)
            .padding(horizontal = Dimensions.gutter, vertical = Dimensions.spacingM)
            .height(48.dp),
    ) {
        Text(text, style = AppTypography.CardTitle16)
    }
}
