package com.muyeon.app.ui.auth.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.Dimensions

/** 아이디 찾기 | 비밀번호 찾기 — 웹 로그인 .other-pages(.t-btn: 14 semibold 회색 밑줄 · 높이 3.2rem). */
@Composable
fun LoginRecoveryLinks(findId: String, findPassword: String, onFindId: () -> Unit, onFindPassword: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(top = Dimensions.spacingS),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextLink(findId, "login.findId", onFindId)
        Text("|", style = AppTypography.Body14, color = AppColors.ChipBorder)
        TextLink(findPassword, "login.findPassword", onFindPassword)
    }
}

@Composable
private fun TextLink(text: String, tag: String, onClick: () -> Unit) {
    Text(
        text,
        style = AppTypography.Body14SemiBold.copy(textDecoration = TextDecoration.Underline),
        color = AppColors.TextSub,
        modifier = Modifier.height(32.dp).clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp).testTag(tag),
    )
}
