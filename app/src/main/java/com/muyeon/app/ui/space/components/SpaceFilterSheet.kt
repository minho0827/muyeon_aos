package com.muyeon.app.ui.space.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors
import com.muyeon.app.ui.common.MuyeonList
import com.muyeon.app.ui.space.SpaceFacility
import com.muyeon.app.ui.space.SpaceFilter

/**
 * ★ 2026-11-08 공간 대관 필터(사용자 요청 — 참고 그림처럼) — 이용유형(칸 나눔) · 가격(최소 [ ],000원 ~ 최대 [ ],000원) ·
 *   편의시설(회색 칸 안 3열 체크) + 아래 [필터 초기화] [공간 찾기]. 편의시설은 DB spaces.facilities 키(SpaceFacility.ALL) 그대로.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpaceFilterSheet(initial: SpaceFilter, onApply: (SpaceFilter) -> Unit, onDismiss: () -> Unit) {
    var f by remember { mutableStateOf(initial) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = Color.White) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding()) {
            Column(
                Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(36.dp),
            ) {
                Section("이용유형") { BookingTypeSegment(f.bookingType) { f = f.copy(bookingType = it) } }
                Section("가격") { PriceRange(f, { f = f.copy(minK = it) }, { f = f.copy(maxK = it) }) }
                Section("편의시설") {
                    FacilityGrid(f.facilities) { k -> f = f.copy(facilities = if (k in f.facilities) f.facilities - k else f.facilities + k) }
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(MuyeonList.ink200))
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "필터 초기화", style = ts(FontWeight.Medium, 16), color = if (f.isEmpty) MuyeonColors.textSub.copy(alpha = 0.5f) else MuyeonColors.textHead,
                    modifier = Modifier.weight(1f).clickable(enabled = !f.isEmpty) { f = SpaceFilter() }.padding(vertical = 12.dp),
                )
                val ok = f.priceError == null
                Box(
                    Modifier.height(52.dp).clip(RoundedCornerShape(14.dp)).background(if (ok) MuyeonColors.textHead else MuyeonList.ink200)
                        .clickable(enabled = ok) { onApply(f) }.padding(horizontal = 22.dp),
                    Alignment.Center,
                ) { Text("공간 찾기", style = ts(FontWeight.SemiBold, 16), color = Color.White) }
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(title, style = ts(FontWeight.SemiBold, 17), color = MuyeonColors.textHead)
        content()
    }
}

/** 이용유형 — 한 줄 칸 나눔, 고른 칸만 흰 바탕 + 검정 굵은 테두리. */
@Composable
private fun BookingTypeSegment(selected: String?, onSelect: (String?) -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(10.dp)).background(MuyeonList.ink100)
            .border(1.dp, MuyeonList.ink200, RoundedCornerShape(10.dp)),
    ) {
        SpaceFilter.BOOKING_TYPES.forEach { (v, label) ->
            val on = v == selected
            Box(
                Modifier.weight(1f).fillMaxHeight()
                    .then(if (on) Modifier.clip(RoundedCornerShape(10.dp)).background(Color.White).border(2.dp, MuyeonColors.textHead, RoundedCornerShape(10.dp)) else Modifier)
                    .clickable { onSelect(v) },
                Alignment.Center,
            ) { Text(label, style = ts(if (on) FontWeight.SemiBold else FontWeight.Medium, 16), color = MuyeonColors.textHead) }
        }
    }
}

/** 가격 — 회색 칸 안 "최소 [#,###],000원 ~ 최대 [#,###],000원" + 안내. 천원 단위로 적는다. */
@Composable
private fun PriceRange(f: SpaceFilter, onMin: (String) -> Unit, onMax: (String) -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MuyeonList.ink100).padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 폰 폭에서 한 줄이면 입력칸이 너무 좁아져(2026-11-08 기기 확인) 최소·최대를 두 줄로.
        PriceLine("최소", f.minK, onMin, trailing = ",000원 ~")
        PriceLine("최대", f.maxK, onMax, trailing = ",000원")
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Filled.Info, null, tint = if (f.priceError != null) MuyeonColors.danger else MuyeonColors.textSub, modifier = Modifier.size(16.dp))
            Text(f.priceError ?: "시간당 최저가 기준이에요. 천원 단위로 적어 주세요.", style = ts(FontWeight.Normal, 13),
                color = if (f.priceError != null) MuyeonColors.danger else MuyeonColors.textSub)
        }
    }
}

@Composable
private fun PriceLine(label: String, value: String, onChange: (String) -> Unit, trailing: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(label, style = ts(FontWeight.SemiBold, 15), color = MuyeonColors.textHead)
        PriceBox(value, onChange, Modifier.width(120.dp))
        Text(trailing, style = ts(FontWeight.SemiBold, 15), color = MuyeonColors.textHead)
    }
}

@Composable
private fun PriceBox(value: String, onChange: (String) -> Unit, modifier: Modifier) {
    Box(modifier.height(44.dp).clip(RoundedCornerShape(10.dp)).background(Color.White).border(1.dp, MuyeonList.ink200, RoundedCornerShape(10.dp)), Alignment.Center) {
        if (value.isEmpty()) Text("#,###", style = ts(FontWeight.Normal, 15), color = MuyeonColors.textSub.copy(alpha = 0.6f))
        BasicTextField(
            value, { v -> onChange(v.filter(Char::isDigit).take(5)) }, singleLine = true,
            textStyle = ts(FontWeight.Medium, 15).copy(color = MuyeonColors.textHead, textAlign = TextAlign.Center),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp),
        )
    }
}

/** 편의시설 — 회색 칸 안 3열 체크(DB 시설 키 전부). */
@Composable
private fun FacilityGrid(selected: Set<String>, onToggle: (String) -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MuyeonList.ink100).padding(horizontal = 10.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        SpaceFacility.ALL.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                row.forEach { fac -> FacilityCheck(fac.label, fac.key in selected, Modifier.weight(1f)) { onToggle(fac.key) } }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun FacilityCheck(label: String, on: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Row(modifier.clickable(onClick = onClick).padding(vertical = 9.dp, horizontal = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        Box(
            Modifier.size(20.dp).clip(RoundedCornerShape(5.dp)).background(if (on) MuyeonColors.textHead else Color.White)
                .border(1.dp, if (on) MuyeonColors.textHead else MuyeonList.ink200, RoundedCornerShape(5.dp)),
            Alignment.Center,
        ) { if (on) Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(15.dp)) }
        // 긴 이름(남/여 화장실 구분·음식물 반입가능)은 잘리지 않고 두 줄로.
        Text(label, style = ts(FontWeight.Normal, 13).copy(lineHeight = 17.sp), color = MuyeonColors.textHead, maxLines = 2)
    }
}

private fun ts(w: FontWeight, size: Int) = TextStyle(fontFamily = customFontFamily, fontWeight = w, fontSize = size.sp)
