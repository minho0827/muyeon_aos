package com.muyeon.app.ui.account.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography

/** 비밀번호 칸 — FormTextField 와 같은 회색 상자, 글자는 가린다. */
@Composable
fun PasswordField(value: String, onChange: (String) -> Unit, placeholder: String, tag: String) {
    Box(Modifier.fillMaxWidth().background(AppColors.SurfaceMuted, RoundedCornerShape(10.dp)).padding(12.dp)) {
        if (value.isEmpty()) Text(placeholder, style = AppTypography.Body15, color = AppColors.TextSub)
        BasicTextField(
            value = value, onValueChange = onChange, singleLine = true,
            textStyle = AppTypography.Body15.copy(color = AppColors.TextHead),
            cursorBrush = SolidColor(AppColors.Primary),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth().testTag(tag),
        )
    }
}
