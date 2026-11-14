package com.muyeon.app.ui.chat.proposal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.FormField
import com.muyeon.app.ui.common.MuyeonColors
import com.muyeon.app.ui.lesson.UserCalendar

/**
 * ★ 2026-11-14 (강사만) 확정되면 등록할 캘린더 — 회색 줄을 누르면 무용연 흰 시트에서 고른다.
 */
@Composable
internal fun ProposalCalendarField(selected: UserCalendar, calendars: List<UserCalendar>, onPick: (Int?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    FormField("캘린더") {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0xFFF8F8F8))
                .clickable { open = true }.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CalendarDot(selected)
            Text(
                selected.name, fontFamily = customFontFamily, fontSize = 15.sp, color = MuyeonColors.textHead,
                modifier = Modifier.weight(1f),
            )
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MuyeonColors.textSub, modifier = Modifier.size(16.dp))
        }
    }
    if (open) {
        ProposalCalendarPickerSheet(selected, listOf(UserCalendar.DEFAULT) + calendars, onDismiss = { open = false }) { c ->
            open = false
            onPick(c.id.takeIf { it > 0 })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProposalCalendarPickerSheet(
    selected: UserCalendar,
    options: List<UserCalendar>,
    onDismiss: () -> Unit,
    onPick: (UserCalendar) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MuyeonColors.surface,
        dragHandle = null,
    ) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(vertical = 12.dp)) {
            Text(
                "캘린더 선택", fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                color = MuyeonColors.textHead, modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            )
            options.forEach { c ->
                Row(
                    Modifier.fillMaxWidth().clickable { onPick(c) }.padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CalendarDot(c)
                    Text(c.name, fontFamily = customFontFamily, fontSize = 15.sp, color = MuyeonColors.textHead, modifier = Modifier.weight(1f))
                    if (c.id == selected.id) Icon(Icons.Filled.Check, null, tint = MuyeonColors.primary, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun CalendarDot(c: UserCalendar) {
    Box(Modifier.size(10.dp).clip(CircleShape).background(c.uiColor))
}
