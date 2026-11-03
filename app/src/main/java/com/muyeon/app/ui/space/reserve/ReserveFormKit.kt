package com.muyeon.app.ui.space.reserve

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
import androidx.compose.material.icons.filled.ExpandMore
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
import kotlinx.coroutines.launch

// 예약 시트 폼 조각 — 라벨 + 테두리 박스(h48, r8, #EAEAEA)

@Composable
internal fun FieldGroup(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 17.sp, color = SpaceDesign.ink900)
        content()
    }
}

@Composable
internal fun Caption(text: String, visible: Boolean = true, lines: Int? = null) {
    Text(
        text, fontFamily = customFontFamily, fontSize = 12.sp, lineHeight = 16.sp, style = TabularNums,
        color = if (visible) SpaceDesign.ink600 else Color.Transparent,
        minLines = lines ?: 1, maxLines = lines ?: Int.MAX_VALUE, overflow = TextOverflow.Ellipsis,
    )
}

@Composable
internal fun Notice(text: String) {
    Text(
        text, fontFamily = customFontFamily, fontSize = 14.sp, color = SpaceDesign.ink700,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(SpaceDesign.primaryTint).padding(14.dp),
    )
}

@Composable
internal fun BoxField(text: String, placeholder: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(8.dp))
            .border(1.dp, SpaceDesign.ink200, RoundedCornerShape(8.dp)).clickable(onClick = onClick).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text, fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 18.sp,
            color = if (placeholder) SpaceDesign.ink500 else SpaceDesign.ink900, modifier = Modifier.weight(1f),
        )
        Icon(Icons.Filled.ExpandMore, null, tint = SpaceDesign.ink500, modifier = Modifier.size(12.dp))
    }
}

/** 칩 그리기 방식 — 실제 / 회색 자리(불러오는 중) / 투명 자리(높이만 잡기, 누를 수 없음). */
enum class ChipMode { Live, Skeleton, Reserve }

/**
 * 시간 칩 — (글자, 고름, 고를 수 있음). 못 고르는 칩은 흐리게.
 *  ★ 고른 칩도 글자 굵기를 바꾸지 않는다(SemiBold 고정) — 굵기가 바뀌면 칩 폭이 변해 줄바꿈이 달라지고 아래가 밀린다.
 *  숫자는 고정폭(tnum).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ChipFlow(chips: List<Triple<String, Boolean, Boolean>>, mode: ChipMode = ChipMode.Live, onTap: (Int) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        chips.forEachIndexed { i, (label, on, enabled) ->
            val live = mode == ChipMode.Live
            Text(
                label, fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, style = TabularNums, maxLines = 1,
                color = when { !live -> Color.Transparent; on -> Color.White; else -> SpaceDesign.ink900 },
                modifier = Modifier.alpha(if (!live || enabled) 1f else 0.35f).clip(RoundedCornerShape(50))
                    .background(
                        when (mode) {
                            ChipMode.Live -> if (on) SpaceDesign.primary else SpaceDesign.ink100
                            ChipMode.Skeleton -> SkeletonColor
                            ChipMode.Reserve -> Color.Transparent
                        },
                    )
                    .clickable(enabled = live && enabled) { onTap(i) }.padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
internal fun StepButton(icon: androidx.compose.ui.graphics.vector.ImageVector, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(36.dp).alpha(if (enabled) 1f else 0.35f).clip(CircleShape).border(1.dp, SpaceDesign.ink300, CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        Alignment.Center,
    ) { Icon(icon, null, tint = SpaceDesign.ink900, modifier = Modifier.size(18.dp)) }
}
