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
internal fun Caption(text: String) {
    Text(text, fontFamily = customFontFamily, fontSize = 12.sp, lineHeight = 16.sp, color = SpaceDesign.ink600)
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

/** 시간 칩 — (글자, 고름, 고를 수 있음). 못 고르는 칩은 흐리게. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ChipFlow(chips: List<Triple<String, Boolean, Boolean>>, onTap: (Int) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        chips.forEachIndexed { i, (label, on, enabled) ->
            Text(
                label, fontFamily = customFontFamily, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium, fontSize = 14.sp,
                color = if (on) Color.White else SpaceDesign.ink900,
                modifier = Modifier.alpha(if (enabled) 1f else 0.35f).clip(RoundedCornerShape(50))
                    .background(if (on) SpaceDesign.primary else SpaceDesign.ink100)
                    .clickable(enabled = enabled) { onTap(i) }.padding(horizontal = 14.dp, vertical = 8.dp),
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
