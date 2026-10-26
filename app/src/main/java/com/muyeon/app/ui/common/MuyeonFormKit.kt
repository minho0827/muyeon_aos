package com.muyeon.app.ui.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.theme.customFontFamily

/*
 * 등록 화면 공용 부품 — PACERA 일정 만들기(iOS EventFormComponents·EventFormControls, AOS components/
 * BottomPrimaryButton·PaceraSwitch)를 옮겨 왔다(2026-10-26). iOS ShareUI/MuyeonFormKit.swift 와 1:1.
 *  카드(FormCard) 안에 칸 제목(FormFieldHeader)·회색 입력 상자(FormTextField)·알약 칩(FormChip/FormChipWrap)·
 *  스위치 줄(FormToggle)·날짜 줄(FormDateRow)을 쌓고, 아래에 고정 주 버튼(MuyeonBottomButton)을 둔다.
 *  색만 무용연 주황·회색 계단, 크기·간격·모양은 PACERA 값 그대로.
 */

private val FormInk100 = Color(0xFFF8F8F8)
private val Head = TextStyle(fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
private val Sub = TextStyle(fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp)
private val Body = TextStyle(fontFamily = customFontFamily, fontWeight = FontWeight.Normal, fontSize = 15.sp)

/** 제목(아이콘 + 글자) 아래에 칸을 쌓는 흰 카드(PACERA eventCard). */
@Composable
fun FormCard(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MuyeonColors.primary, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(6.dp))
            Text(title, style = Head, color = MuyeonColors.textHead)
        }
        content()
    }
}

/** 칸 제목 + (필수면) 주황 *. */
@Composable
fun FormFieldHeader(text: String, required: Boolean = false, hint: String? = null) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = Sub, color = MuyeonColors.textSub)
        if (required) Text("*", style = Sub, color = MuyeonColors.primary, modifier = Modifier.padding(start = 2.dp))
        if (hint != null) Text(hint, style = Sub.copy(fontSize = 12.sp), color = MuyeonColors.textSub, modifier = Modifier.padding(start = 4.dp))
    }
}

/** 칸 제목 + 내용 한 묶음(간격 8). */
@Composable
fun FormField(label: String, required: Boolean = false, hint: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FormFieldHeader(label, required, hint)
        content()
    }
}

/** 회색 상자 입력칸 — 글자 수 제한(max). */
@Composable
fun FormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    max: Int,
    tag: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    minLines: Int = 1,
) {
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(FormInk100).padding(12.dp)) {
        if (value.isEmpty()) Text(placeholder, style = Body, color = MuyeonColors.textSub)
        BasicTextField(
            value = value,
            onValueChange = { onValueChange(it.take(max)) },
            textStyle = Body.copy(color = MuyeonColors.textHead),
            cursorBrush = SolidColor(MuyeonColors.primary),
            singleLine = minLines == 1,
            minLines = minLines,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier.fillMaxWidth().let { m -> if (tag != null) m.testTag(tag) else m },
        )
    }
}

/** 보여 주기만 하는 회색 상자(활동명 등). */
@Composable
fun FormReadOnlyBox(text: String) {
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(FormInk100).padding(12.dp)) {
        Text(text, style = Body, color = MuyeonColors.textSub)
    }
}

/** 알약 칩 — 고르면 주황 바탕 흰 글자(PACERA eventChip). */
@Composable
fun FormChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.clip(CircleShape)
            .background(if (selected) MuyeonColors.primary else FormInk100)
            .clickable(onClick = onClick)
            .semantics { this.selected = selected; role = Role.Button }
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(label, style = Sub, color = if (selected) Color.White else MuyeonColors.textHead)
    }
}

/** 칩 여러 개 줄바꿈 나열. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FormChipWrap(options: List<Pair<String, String>>, isOn: (String) -> Boolean, onTap: (String) -> Unit) {
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { (value, label) -> FormChip(label, isOn(value)) { onTap(value) } }
    }
}

/** 커스텀 스위치 — PACERA PaceraSwitch 그대로(주황 트랙 + 흰 손잡이). */
@Composable
fun MuyeonSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, label: String? = null) {
    val thumb by animateDpAsState(if (checked) 18.dp else 0.dp, label = "thumb")
    val track by animateColorAsState(if (checked) MuyeonColors.primary else Color(0xFFDADADA), label = "track")
    Box(
        Modifier.size(width = 46.dp, height = 28.dp)
            .clip(RoundedCornerShape(50))
            .background(track)
            .clickable(remember { MutableInteractionSource() }, indication = null) { onCheckedChange(!checked) }
            .semantics {
                role = Role.Switch
                stateDescription = if (checked) "켜짐" else "꺼짐"
                if (label != null) contentDescription = label
            }
            .padding(3.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(Modifier.offset(x = thumb).size(22.dp).clip(CircleShape).background(Color.White))
    }
}

/** 글자 + 스위치 한 줄(PACERA eventToggle). */
@Composable
fun FormToggle(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = Body, color = MuyeonColors.textHead, modifier = Modifier.weight(1f))
        MuyeonSwitch(checked, onCheckedChange, label = title)
    }
}

/** 날짜 줄 — 누르면 날짜 선택. 값이 없으면 placeholder 를 흐리게(PACERA eventDateRow). */
@Composable
fun FormDateRow(text: String?, placeholder: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Row(
        modifier.clip(RoundedCornerShape(10.dp)).background(FormInk100)
            .clickable(enabled = enabled, onClick = onClick)
            .alpha(if (enabled) 1f else 0.5f)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.CalendarToday, null, tint = MuyeonColors.primary, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(text ?: placeholder, style = Body, color = if (text == null) MuyeonColors.textSub else MuyeonColors.textHead,
            modifier = Modifier.weight(1f), maxLines = 1)
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MuyeonColors.textSub, modifier = Modifier.size(16.dp))
    }
}

/** 화면 아래 고정 주 버튼 — PACERA BottomPrimaryButton(높이 52·캡슐, 로딩은 스피너만). */
@Composable
fun MuyeonBottomButton(label: String, tag: String, enabled: Boolean = true, loading: Boolean = false, onClick: () -> Unit) {
    val canTap = enabled && !loading
    Box(Modifier.fillMaxWidth().background(Color.White).navigationBarsPadding().padding(horizontal = 24.dp, vertical = 12.dp)) {
        Box(
            Modifier.fillMaxWidth().height(52.dp).clip(CircleShape)
                .background(if (canTap) MuyeonColors.primary else MuyeonColors.textHead.copy(alpha = 0.12f))
                .clickable(enabled = canTap, onClick = onClick)
                .testTag(tag),
            contentAlignment = Alignment.Center,
        ) {
            if (loading) CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
            else Text(label, style = Head, color = if (canTap) Color.White else MuyeonColors.textHead.copy(alpha = 0.38f))
        }
    }
}
