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

/** ④ 가격 — 기본 시간당 + 세부공간 목록(추가·삭제, 최대 10). */
@Composable
fun SpaceStepPrice(form: SpaceForm, onChange: (SpaceForm) -> Unit) {
    SectionHead("가격", "기본 시간당 가격과 세부공간(홀·룸)별 가격을 정해 주세요.")
    FormField("기본 시간당 가격", required = true, hint = "(원)", filled = (form.pricePerHour.toIntOrNull() ?: 0) > 0 || form.options.any { it.label.isNotBlank() && (it.price.toIntOrNull() ?: 0) > 0 }) { NumberField(form.pricePerHour, "예) 7000", 8) { onChange(form.copy(pricePerHour = it)) } }
    FormField("세부공간", hint = "(선택)") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            form.options.forEachIndexed { i, op ->
                fun set(o: SpaceOptionInput) = onChange(form.copy(options = form.options.mapIndexed { j, x -> if (j == i) o else x }))
                PairEditRow(
                    left = { FormTextField(op.label, { v -> set(op.copy(label = v)) }, "예) A홀 20평", 60) },
                    right = { NumberField(op.price, "시간당 원", 8) { v -> set(op.copy(price = v)) } },
                    leftWeight = 1.4f, rightWeight = 1f,
                    onRemove = { onChange(form.copy(options = form.options.filterIndexed { j, _ -> j != i })) },
                )
            }
            if (form.options.size < 10) AddRowLink("세부공간 추가") { onChange(form.copy(options = form.options + SpaceOptionInput())) }
        }
    }
}
