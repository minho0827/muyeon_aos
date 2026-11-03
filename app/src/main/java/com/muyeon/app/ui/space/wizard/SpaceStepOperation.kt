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
}
