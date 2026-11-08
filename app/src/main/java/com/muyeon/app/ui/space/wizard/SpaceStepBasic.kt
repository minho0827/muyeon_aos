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
import com.muyeon.app.ui.space.SpaceCategory
import kotlinx.coroutines.launch

/** ③ 기본 — 이름 · 카테고리(있을 때) · 최대/기본 인원 · 추가 인원 요금 · 전화. */
@Composable
fun SpaceStepBasic(form: SpaceForm, categories: List<SpaceCategory>, onChange: (SpaceForm) -> Unit) {
    SectionHead("기본 정보", "공간 이름과 인원을 알려 주세요.")
    FormField("공간 이름", required = true, filled = form.name.isNotBlank()) { FormTextField(form.name, { onChange(form.copy(name = it)) }, "예) 사당 미러 스튜디오 A", 60) }
    categories.takeIf { it.isNotEmpty() }?.let { SpaceCategoryField(it, form.categoryId) { id -> onChange(form.copy(categoryId = id)) } }
    FormField("최대 인원", hint = "(명)") { NumberField(form.capacityMax, "예) 15", 4) { onChange(form.copy(capacityMax = it)) } }
    FormField("기본 인원", hint = "(명, 넘으면 추가 요금)") { NumberField(form.baseHeadcount, "예) 4", 4) { onChange(form.copy(baseHeadcount = it)) } }
    FormField("추가 인원 요금", hint = "(1인·1시간당 원)") { NumberField(form.extraPersonFee, "예) 2000", 7) { onChange(form.copy(extraPersonFee = it)) } }
    FormField("전화번호", hint = "(선택, 상세에 전화 버튼)") {
        FormTextField(form.phone, { v -> onChange(form.copy(phone = v.filter { it.isDigit() || it == '-' })) }, "예) 02-123-4567", 20, keyboardType = KeyboardType.Phone)
    }
}

/** 카테고리 칩 — 다시 누르면 해제. */
@Composable
private fun SpaceCategoryField(categories: List<SpaceCategory>, selected: Int?, onPick: (Int?) -> Unit) {
    FormField("카테고리") {
        FormChipWrap(categories.map { it.id.toString() to it.name }, { selected?.toString() == it }) { v ->
            onPick(v.toIntOrNull()?.takeIf { it != selected })
        }
    }
}
