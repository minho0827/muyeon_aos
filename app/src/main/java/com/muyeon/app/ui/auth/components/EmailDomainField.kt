package com.muyeon.app.ui.auth.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.muyeon.app.R
import com.muyeon.app.ui.common.FormTextField
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.Dimensions

/**
 * 이메일 입력 — [아이디] @ [도메인 고르기] 한 줄(웹 components/muyeon/EmailField.js).
 *   도메인을 직접 치지 않게 해 오타 주소(naver.con)로 인증 메일이 반송되는 것을 막는다. 회사 메일은 '직접입력'.
 *   [value]·[onChange] 는 합친 이메일 한 줄.
 */
@Composable
fun EmailDomainField(value: String, onChange: (String) -> Unit, tag: String) {
    val at = value.lastIndexOf('@')
    val local = if (at >= 0) value.substring(0, at) else value
    val domain = if (at >= 0) value.substring(at + 1) else ""
    var custom by rememberSaveable { mutableStateOf(domain.isNotEmpty() && domain !in EMAIL_DOMAINS) }
    fun emit(l: String, d: String) = onChange(if (l.isEmpty() && d.isEmpty()) "" else "$l@$d")

    Column(verticalArrangement = Arrangement.spacedBy(Dimensions.spacingXS)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimensions.spacingS)) {
            Box(Modifier.weight(1f)) {
                FormTextField(local, { emit(it.trim(), domain) }, stringResource(R.string.auth_recovery_email_local_placeholder), 64, "$tag.local", KeyboardType.Email)
            }
            Text("@", style = AppTypography.Body15, color = AppColors.TextHead)
            Box(Modifier.weight(1f)) {
                if (custom) {
                    FormTextField(domain, { emit(local, it.trim().lowercase()) }, stringResource(R.string.auth_recovery_email_custom_placeholder), 60, "$tag.domain", KeyboardType.Uri)
                } else {
                    DomainPicker(domain, onPick = { d -> if (d == null) { custom = true; emit(local, "") } else emit(local, d) }, tag = "$tag.domain")
                }
            }
        }
        // 직접입력에서 목록으로 돌아갈 길(웹 '목록에서 선택').
        if (custom) {
            Text(
                stringResource(R.string.auth_recovery_email_pick_list),
                style = AppTypography.Sub13.copy(textDecoration = TextDecoration.Underline), color = AppColors.TextHint,
                modifier = Modifier.clickable { custom = false; emit(local, "") }.padding(vertical = 2.dp),
            )
        }
    }
}

/** 도메인 고르기(웹 BaseSelector) — null = 직접입력. */
@Composable
private fun DomainPicker(domain: String, onPick: (String?) -> Unit, tag: String) {
    var open by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier.fillMaxWidth().background(AppColors.SurfaceMuted, RoundedCornerShape(10.dp)).clickable { open = true }.padding(12.dp).testTag(tag),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                domain.ifEmpty { stringResource(R.string.auth_recovery_email_domain_placeholder) },
                style = AppTypography.Body15, color = if (domain.isEmpty()) AppColors.TextSub else AppColors.TextHead,
                modifier = Modifier.weight(1f), maxLines = 1,
            )
            Icon(Icons.Filled.KeyboardArrowDown, null, tint = AppColors.TextSub, modifier = Modifier.size(Dimensions.iconM))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            EMAIL_DOMAINS.forEach { d -> DropdownMenuItem(text = { Text(d, style = AppTypography.Body15) }, onClick = { open = false; onPick(d) }) }
            DropdownMenuItem(text = { Text(stringResource(R.string.auth_recovery_email_custom), style = AppTypography.Body15) }, onClick = { open = false; onPick(null) })
        }
    }
}

/** 흔히 쓰는 순서(웹 EMAIL_DOMAINS 와 같다). */
private val EMAIL_DOMAINS = listOf("naver.com", "gmail.com", "daum.net", "hanmail.net", "kakao.com", "nate.com", "outlook.com", "icloud.com")
