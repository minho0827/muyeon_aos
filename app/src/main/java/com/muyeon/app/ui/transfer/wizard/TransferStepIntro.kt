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
import kotlinx.coroutines.launch

/** ⑥ 소개 — 상세 소개 · 입주 가능일 · 원생 수 · 월매출 + 매출 공개. */
@Composable
fun TransferStepIntro(form: TransferForm, onPickDate: () -> Unit, onChange: (TransferForm) -> Unit) {
    SectionHead("소개", "공간의 장점과 양도 조건을 자세히 적어 주세요.")
    FormField("상세 소개") { FormTextField(form.description, { onChange(form.copy(description = it)) }, "예) 역에서 3분, 탄성마루 시공 2년…", 5000, minLines = 6) }
    FormField("입주 가능일") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FormDateRow(form.availableFrom, "날짜를 선택해 주세요", Modifier.weight(1f), onClick = onPickDate)
            form.availableFrom?.let { ClearDateLink { onChange(form.copy(availableFrom = null)) } }
        }
    }
    FormField("원생 수", hint = "(명)") { NumberField(form.studentCount, "예) 60", 6) { onChange(form.copy(studentCount = it)) } }
    FormField("월매출", hint = "(만원)") { NumberField(form.monthlyRevenue, "예) 900") { onChange(form.copy(monthlyRevenue = it)) } }
    FormToggle("매출 공개", form.revenueVisible) { onChange(form.copy(revenueVisible = it)) }
}

@Composable
private fun ClearDateLink(onClear: () -> Unit) {
    Text("지우기", fontFamily = customFontFamily, fontSize = 13.sp, color = MuyeonColors.textSub, modifier = Modifier.clickable(onClick = onClear).padding(10.dp))
}
