package com.muyeon.app.ui.transfer.wizard

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
import com.muyeon.app.ui.transfer.TransferForm
import com.muyeon.app.ui.transfer.TransferLabels
import kotlinx.coroutines.launch

/** ③ 시설·면적 — 제목 · 시설 유형 · 운영 상태 · 평수 · 층 · 엘리베이터 · 주차. */
@Composable
fun TransferStepFacility(form: TransferForm, onChange: (TransferForm) -> Unit) {
    SectionHead("시설·면적", "어떤 공간인지 알려 주세요.")
    FormField("제목", required = true) { FormTextField(form.title, { onChange(form.copy(title = it)) }, "예) 강서구 45평 무용학원 양도", 60) }
    FormField("시설 유형") { FormChipWrap(TransferLabels.facility, { form.facilityType == it }) { onChange(form.copy(facilityType = if (form.facilityType == it) null else it)) } }
    FormField("운영 상태") { FormChipWrap(TransferLabels.operating, { form.operatingStatus == it }) { onChange(form.copy(operatingStatus = if (form.operatingStatus == it) null else it)) } }
    FormField("평수", hint = "(평)") { FormTextField(form.areaPyeong, { v -> onChange(form.copy(areaPyeong = v.filter { it.isDigit() || it == '.' })) }, "예) 45", 7, keyboardType = KeyboardType.Decimal) }
    FormField("층") { FormTextField(form.floor, { onChange(form.copy(floor = it)) }, "예) 2 또는 지하1", 10) }
    FormToggle("엘리베이터", form.hasElevator) { onChange(form.copy(hasElevator = it)) }
    FormField("주차", hint = "(대)") { NumberField(form.parkingCount, "예) 2", 4) { onChange(form.copy(parkingCount = it)) } }
}
