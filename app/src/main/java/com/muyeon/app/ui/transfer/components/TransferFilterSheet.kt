package com.muyeon.app.ui.transfer.components

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
import com.muyeon.app.ui.transfer.TransferFilter
import com.muyeon.app.ui.transfer.TransferLabels
import kotlinx.coroutines.launch

/** 필터 시트 — 시설 유형 · 평수 범위 · 보증금/월세/권리금 상한 · 옵션 + [초기화][적용]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferFilterSheet(initial: TransferFilter, onApply: (TransferFilter) -> Unit, onDismiss: () -> Unit) {
    var f by remember { mutableStateOf(initial) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = Color.White) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding()) {
            Text(
                "필터", fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MuyeonColors.textHead,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            Column(
                Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                FormField("시설 유형") {
                    FormChipWrap(TransferLabels.facility, { f.facilityType == it }) { f = f.copy(facilityType = if (f.facilityType == it) null else it) }
                }
                FormField("평수 범위", hint = "(평)") { AreaRange(f.minArea, f.maxArea, { f = f.copy(minArea = it) }, { f = f.copy(maxArea = it) }) }
                FormField("보증금 상한", hint = "(만원)") { CapField(f.maxDeposit, "예) 3000") { f = f.copy(maxDeposit = it) } }
                FormField("월세 상한", hint = "(만원)") { CapField(f.maxRent, "예) 200") { f = f.copy(maxRent = it) } }
                FormField("권리금 상한", hint = "(만원)") { CapField(f.maxPremium, "예) 2000") { f = f.copy(maxPremium = it) } }
                FormField("옵션") {
                    FormChipWrap(TransferLabels.options, { it in f.options }) { v -> f = f.copy(options = if (v in f.options) f.options - v else f.options + v) }
                }
                Spacer(Modifier.height(4.dp))
            }
            SheetActions(onReset = { f = TransferFilter() }, onApply = { onApply(f) })
        }
    }
}

@Composable
private fun AreaRange(min: String, max: String, onMin: (String) -> Unit, onMax: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f)) { FormTextField(min, { onMin(it.filter(Char::isDigit)) }, "최소", 5, keyboardType = KeyboardType.Number) }
        Text("~", fontFamily = customFontFamily, color = MuyeonColors.textSub)
        Box(Modifier.weight(1f)) { FormTextField(max, { onMax(it.filter(Char::isDigit)) }, "최대", 5, keyboardType = KeyboardType.Number) }
    }
}

@Composable
private fun CapField(value: String, placeholder: String, onChange: (String) -> Unit) {
    FormTextField(value, { onChange(it.filter(Char::isDigit)) }, placeholder, 8, keyboardType = KeyboardType.Number)
}
