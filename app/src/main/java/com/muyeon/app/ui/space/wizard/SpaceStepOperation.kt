package com.muyeon.app.ui.space.wizard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.*
import com.muyeon.app.ui.common.market.*
import com.muyeon.app.ui.jobposting.SectionHead
import kotlinx.coroutines.launch

/** ⑤ 운영 — 영업시간 · 운영 요일 · 최소 예약 시간 · 예약 마감. */
@Composable
fun SpaceStepOperation(form: SpaceForm, onChange: (SpaceForm) -> Unit) {
    SectionHead("운영", "영업시간과 예약 규칙을 정해 주세요.")
    FormField("영업시간") {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HourStepper(form.openHour, 0, 23) { onChange(form.copy(openHour = it)) }
            Text("~", fontFamily = customFontFamily, color = MuyeonColors.textSub)
            HourStepper(form.closeHour, 1, 24) { onChange(form.copy(closeHour = it)) }
        }
    }
    FormField("운영 요일") {
        FormChipWrap(DAYS, { it in form.openDays }) { v -> onChange(form.copy(openDays = if (v in form.openDays) form.openDays - v else form.openDays + v)) }
    }
    FormField("최소 예약 시간") { FormChipWrap(MIN_HOURS, { form.minHours == it }) { onChange(form.copy(minHours = it)) } }
    FormField("예약 마감", hint = "(이용 시작 기준)") { FormChipWrap(LEADS, { form.leadMinutes == it }) { onChange(form.copy(leadMinutes = it)) } }
    FormField("예약 방식") { ApprovalModePicker(form.approvalMode) { onChange(form.copy(approvalMode = it)) } }
}

/** 예약 방식 — 수락 후 확정(추천·기본) / 바로 확정, 한 줄 설명. */
@Composable
private fun ApprovalModePicker(mode: String, onPick: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ApprovalModeCard("수락 후 확정 (추천)", "결제 후 내가 확인하고 수락하면 예약이 확정돼요. 거절·무응답은 전액 환불돼요.", mode != "INSTANT") { onPick("REQUEST") }
        ApprovalModeCard("바로 확정", "결제가 끝나면 바로 예약이 확정돼요.", mode == "INSTANT") { onPick("INSTANT") }
    }
}

@Composable
private fun ApprovalModeCard(title: String, desc: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
            .border(if (selected) 2.dp else 1.dp, if (selected) MuyeonColors.primary else MuyeonColors.border, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick).padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier.padding(top = 1.dp).size(18.dp).clip(CircleShape)
                .border(if (selected) 5.dp else 1.dp, if (selected) MuyeonColors.primary else MuyeonList.ink300, CircleShape),
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = MuyeonColors.textHead)
            Text(desc, fontFamily = customFontFamily, fontSize = 13.sp, lineHeight = 18.sp, color = MuyeonColors.textSub, minLines = 2, maxLines = 2)
        }
    }
}
