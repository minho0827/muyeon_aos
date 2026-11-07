package com.muyeon.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.Dimensions

/** 입력칸 + 오른쪽 버튼 한 줄 — 웹 components/ui/InputWithButton.js(댓글·답글 등록). */
@Composable
fun InputWithButton(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    buttonText: String,
    onButton: () -> Unit,
    modifier: Modifier = Modifier,
    maxLength: Int = 300,
    enabled: Boolean = true,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimensions.spacingS)) {
        OutlinedTextField(
            value = value,
            onValueChange = { if (it.length <= maxLength) onValueChange(it) },
            placeholder = { Text(placeholder, style = AppTypography.Body14, color = AppColors.TextHint) },
            modifier = Modifier.weight(1f).testTag("input_$buttonText"),
            textStyle = AppTypography.Body15,
            maxLines = 4,
            shape = RoundedCornerShape(Dimensions.spacingS),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AppColors.Primary, unfocusedBorderColor = AppColors.ChipBorder),
        )
        Button(
            onClick = onButton,
            enabled = enabled && value.isNotBlank(),
            shape = RoundedCornerShape(Dimensions.spacingS),
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary, contentColor = AppColors.TextOnPrimary),
            modifier = Modifier.height(52.dp),
        ) { Text(buttonText, style = AppTypography.Body14SemiBold) }
    }
}
