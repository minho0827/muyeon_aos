package com.muyeon.app.ui.auth.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.Dimensions

/** 체크 + 글자 한 줄 — 웹 로그인 '아이디·비밀번호 저장'(label.flex-box-st-ct · t-14-single). */
@Composable
fun LoginRememberRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.padding(top = Dimensions.spacingS, bottom = Dimensions.spacingXS)
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = onChange)
            .testTag("login.remember"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimensions.spacingXS),
    ) {
        Checkbox(
            checked = checked, onCheckedChange = null,
            colors = CheckboxDefaults.colors(checkedColor = AppColors.Primary, uncheckedColor = AppColors.IconOff, checkmarkColor = AppColors.TextOnPrimary),
        )
        Text(label, style = AppTypography.Body14, color = AppColors.TextHead)
    }
}
