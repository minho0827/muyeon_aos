package com.muyeon.app.ui.alerts.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.muyeon.app.R
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.Dimensions

/** 복수 선택 묶음 — 웹 AlertSettings CheckGroup(.field label '(복수 선택)' + ul.check-list 체크박스 줄바꿈 나열, 간격 12). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AlertCheckGroup(label: String, options: List<Pair<String, String>>, selected: List<String>, tagPrefix: String, onToggle: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimensions.spacingS)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = AppTypography.Body14SemiBold, color = AppColors.TextHead)
            Text(" " + stringResource(R.string.alerts_multi), style = AppTypography.Sub13, color = AppColors.TextSub)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            options.forEach { (v, t) -> AlertCheckRow(t, v in selected, "$tagPrefix-$v") { onToggle(v) } }
        }
    }
}

/** 체크박스 한 칸 — 웹 .checkbox-sm(네모 + 글자). 켜짐 = 주황. */
@Composable
fun AlertCheckRow(label: String, checked: Boolean, tag: String, onToggle: () -> Unit) {
    Row(
        Modifier.clickable(role = Role.Checkbox, onClick = onToggle).testTag(tag).padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(if (checked) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank, null,
            tint = if (checked) AppColors.Primary else AppColors.IconOff, modifier = Modifier.size(20.dp))
        Text(label, style = AppTypography.Body14, color = AppColors.TextBody)
    }
}
