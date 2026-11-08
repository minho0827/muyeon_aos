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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.res.stringResource
import com.muyeon.app.R

/** 비밀번호 칸 — FormTextField 와 같은 회색 상자, 글자는 가린다. */
@Composable
fun PasswordField(value: String, onChange: (String) -> Unit, placeholder: String, tag: String) {
    // ★ 2026-11-08 눈 아이콘으로 비밀번호 보기/숨기기(로그인·재설정·변경 공통).
    var visible by rememberSaveable { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().background(AppColors.SurfaceMuted, RoundedCornerShape(10.dp)).padding(start = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f).padding(vertical = 12.dp)) {
            if (value.isEmpty()) Text(placeholder, style = AppTypography.Body15, color = AppColors.TextSub)
            BasicTextField(
                value = value, onValueChange = onChange, singleLine = true,
                textStyle = AppTypography.Body15.copy(color = AppColors.TextHead),
                cursorBrush = SolidColor(AppColors.Primary),
                visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = if (visible) KeyboardType.Text else KeyboardType.Password, autoCorrectEnabled = false),
                modifier = Modifier.fillMaxWidth().testTag(tag),
            )
        }
        IconButton(onClick = { visible = !visible }, modifier = Modifier.size(44.dp).testTag("$tag.eye")) {
            Icon(
                if (visible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                contentDescription = stringResource(if (visible) R.string.password_hide else R.string.password_show),
                tint = AppColors.TextHint, modifier = Modifier.size(20.dp),
            )
        }
    }
}
