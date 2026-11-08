package com.muyeon.app.ui.account.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.muyeon.app.ui.common.MuyeonGroupRow
import com.muyeon.app.ui.common.MuyeonGroupSection
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.Dimensions

/** 탈퇴 안내 묶음 한 장 — 제목 + 줄들(누를 수 있으면 ›). 웹 .card.line(head corner + menu-list). */
@Composable
fun WithdrawNoticeCard(title: String, rows: List<Pair<String, (() -> Unit)?>>) {
    MuyeonGroupSection(title) { rows.forEach { (text, onClick) -> MuyeonGroupRow(text, onClick = onClick, showChevron = onClick != null) } }
}

/** 동의 체크 한 줄. [strong] 은 '모두 동의'. */
@Composable
fun WithdrawCheckRow(text: String, checked: Boolean, onToggle: () -> Unit, strong: Boolean = false) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(horizontal = Dimensions.spacingS, vertical = Dimensions.spacingXS),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked, { onToggle() }, colors = CheckboxDefaults.colors(checkedColor = AppColors.Primary, uncheckedColor = AppColors.IconOff))
        Text(text, style = if (strong) AppTypography.Body14SemiBold else AppTypography.Body14, color = AppColors.TextHead, modifier = Modifier.weight(1f))
    }
}

/** 탈퇴 사유 한 묶음(제목 + 라디오). */
@Composable
fun WithdrawReasonGroup(title: String, items: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimensions.spacingXXS)) {
        Text(title, style = AppTypography.Section16, color = AppColors.TextHead, modifier = Modifier.padding(bottom = Dimensions.spacingXS))
        items.forEach { (code, label) ->
            Row(Modifier.fillMaxWidth().clickable { onSelect(code) }, verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected == code, { onSelect(code) }, colors = RadioButtonDefaults.colors(selectedColor = AppColors.Primary))
                Text(label, style = AppTypography.Body15, color = AppColors.TextHead, modifier = Modifier.weight(1f))
            }
        }
    }
}
