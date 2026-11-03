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
import com.muyeon.app.ui.space.SpaceFacility
import com.muyeon.app.ui.space.SpaceRefundPresets
import kotlinx.coroutines.launch

/** ⑥ 시설·주의사항·환불 규정(유연·보통·엄격 단계 또는 직접 설정, 기본 보통). presets null = 불러오는 중. */
@Composable
fun SpaceStepRules(form: SpaceForm, presets: SpaceRefundPresets?, onChange: (SpaceForm) -> Unit) {
    SectionHead("시설·주의사항·환불 규정", "예약 전에 꼭 알아야 할 내용을 적어 주세요.")
    FormField("시설") {
        FormChipWrap(SpaceFacility.ALL.map { it.key to it.label }, { it in form.facilities }) { v ->
            onChange(form.copy(facilities = if (v in form.facilities) form.facilities - v else form.facilities + v))
        }
    }
    FormField("시설 상세", hint = "(한 줄에 하나)") { FormTextField(form.facilityList, { onChange(form.copy(facilityList = it)) }, "예) 전면 거울 8m", 2000, minLines = 3) }
    FormField("예약 시 주의사항", hint = "(한 줄에 하나)") { FormTextField(form.cautions, { onChange(form.copy(cautions = it)) }, "예) 실내화를 꼭 챙겨 주세요", 2000, minLines = 3) }
    FormField("환불 규정") { SpaceRefundPicker(form, presets, onChange) }
}
