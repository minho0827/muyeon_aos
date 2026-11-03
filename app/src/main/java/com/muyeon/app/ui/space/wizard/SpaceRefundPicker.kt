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
import androidx.compose.material.icons.filled.Close
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
import com.muyeon.app.ui.space.SpaceDesign
import com.muyeon.app.ui.space.SpaceRefundPreset
import com.muyeon.app.ui.space.SpaceRefundPresets
import com.muyeon.app.ui.space.SpaceRefundRow
import kotlinx.coroutines.launch

/** 단계 카드 미리보기 줄 수 — 가장 긴 단계(3줄) 기준으로 자리를 잡아 카드 높이를 맞춘다. */
private const val PRESET_PREVIEW_LINES = 3

/**
 * 환불 단계 고르기 — 서버 단계(유연·보통·엄격, 표시 줄은 서버 글자) 카드 3장 + "직접 설정".
 *  직접 설정 줄([N]일 전까지 [P]%)은 늘 보이게 두고(자리 고정), 고치면 직접 설정으로 바뀐다.
 *  presets == null 이면 같은 크기의 카드 자리.
 */
@Composable
fun SpaceRefundPicker(form: SpaceForm, presets: SpaceRefundPresets?, onChange: (SpaceForm) -> Unit) {
    val ctx = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (presets == null) repeat(3) { RefundPresetCardSkeleton() }
        else presets.presets.forEach { p ->
            RefundPresetCard(p, form.refundPreset == p.key) { onChange(form.copy(refundPreset = p.key)) }
        }
        RefundCustomCard(form, onChange)
        Text(
            "무용연 취소·환불 정책 보기 ›", fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp, color = SpaceDesign.primaryText,
            modifier = Modifier.clickable { openPolicyPage(ctx, presets?.policyUrl ?: com.muyeon.app.ui.space.SPACE_REFUND_POLICY) }.padding(vertical = 4.dp),
        )
    }
}

@Composable
private fun RefundCardFrame(selected: Boolean, onClick: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
            .border(if (selected) 2.dp else 1.dp, if (selected) MuyeonColors.primary else MuyeonColors.border, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp), content = content,
    )
}

@Composable
private fun RefundCardTitle(label: String, selected: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            Modifier.size(18.dp).clip(CircleShape)
                .border(if (selected) 5.dp else 1.dp, if (selected) MuyeonColors.primary else MuyeonList.ink300, CircleShape),
        )
        Text(label, fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = MuyeonColors.textHead)
    }
}

/** 단계 카드 — 서버 표시 줄 미리보기(빈 줄로 3줄 자리 고정). */
@Composable
private fun RefundPresetCard(p: SpaceRefundPreset, selected: Boolean, onClick: () -> Unit) = RefundCardFrame(selected, onClick) {
    RefundCardTitle(p.label, selected)
    repeat(PRESET_PREVIEW_LINES) { i -> RefundPreviewLine(p.rows.getOrNull(i)) }
}

@Composable
private fun RefundPreviewLine(row: SpaceRefundRow?) {
    Row(Modifier.fillMaxWidth().padding(start = 26.dp)) {
        Text(row?.whenText ?: " ", fontFamily = customFontFamily, fontSize = 13.sp, color = MuyeonColors.textSub, maxLines = 1, modifier = Modifier.weight(1f))
        Text(row?.refundText ?: " ", fontFamily = customFontFamily, fontSize = 13.sp, color = MuyeonColors.textHead, maxLines = 1)
    }
}

@Composable
private fun RefundPresetCardSkeleton() {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).border(1.dp, MuyeonColors.border, RoundedCornerShape(10.dp)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        SkeletonLine(15, 0.25f)
        repeat(PRESET_PREVIEW_LINES) { Box(Modifier.padding(start = 26.dp)) { SkeletonLine(13, 0.8f) } }
    }
}

/** 직접 설정 — 줄마다 [N]일 전까지 [P]% (줄 추가·삭제, 최대 5줄). 줄을 고치면 직접 설정으로 바뀐다. */
@Composable
private fun RefundCustomCard(form: SpaceForm, onChange: (SpaceForm) -> Unit) {
    val selected = form.refundPreset == null
    fun setRows(rows: List<RefundInput>) = onChange(form.copy(refundPreset = null, customRefund = rows))
    RefundCardFrame(selected, onClick = { onChange(form.copy(refundPreset = null)) }) {
        RefundCardTitle("직접 설정", selected)
        form.customRefund.forEachIndexed { i, r ->
            RefundInputRow(
                r, onChange = { v -> setRows(form.customRefund.mapIndexed { j, x -> if (j == i) v else x }) },
                onRemove = { setRows(form.customRefund.filterIndexed { j, _ -> j != i }) },
            )
        }
        if (form.customRefund.size < MAX_CUSTOM_REFUND_ROWS) AddRowLink("규정 줄 추가") { setRows(form.customRefund + RefundInput()) }
    }
}

@Composable
private fun RefundInputRow(r: RefundInput, onChange: (RefundInput) -> Unit, onRemove: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.width(76.dp)) { NumberField(r.days, "예) 3", 3) { onChange(r.copy(days = it)) } }
        Text("일 전까지", fontFamily = customFontFamily, fontSize = 14.sp, color = MuyeonColors.textHead)
        Box(Modifier.width(76.dp)) { NumberField(r.percent, "예) 100", 3) { onChange(r.copy(percent = it)) } }
        Text("%", fontFamily = customFontFamily, fontSize = 14.sp, color = MuyeonColors.textHead, modifier = Modifier.weight(1f))
        Icon(Icons.Filled.Close, "삭제", tint = MuyeonColors.hint, modifier = Modifier.size(20.dp).clickable(onClick = onRemove))
    }
}
