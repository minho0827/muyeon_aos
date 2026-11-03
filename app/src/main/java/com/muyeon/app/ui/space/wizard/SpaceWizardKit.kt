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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
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
import kotlinx.coroutines.launch

/** "＋ 줄 추가" 주황 링크. */
@Composable
internal fun AddRowLink(label: String, onClick: () -> Unit) {
    Text(
        "＋ $label", fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = MuyeonColors.primary,
        modifier = Modifier.clickable(onClick = onClick).padding(vertical = 6.dp),
    )
}

/** 시(時) −/＋ 스테퍼 "09시". */
@Composable
internal fun HourStepper(value: Int, min: Int, max: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(Icons.Filled.Remove to (value > min), Icons.Filled.Add to (value < max)).forEachIndexed { i, (icon, en) ->
            if (i == 1) Text(String.format(java.util.Locale.KOREA, "%02d시", value), fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = MuyeonColors.textHead)
            Box(
                Modifier.size(32.dp).alpha(if (en) 1f else 0.35f).clip(CircleShape).border(1.dp, MuyeonList.ink300, CircleShape)
                    .clickable(enabled = en) { onChange(if (i == 0) value - 1 else value + 1) },
                Alignment.Center,
            ) { Icon(icon, null, tint = MuyeonColors.textHead, modifier = Modifier.size(16.dp)) }
        }
    }
}

/** 두 칸 + ✕ 줄(세부공간·환불 규정 편집). */
@Composable
internal fun PairEditRow(left: @Composable () -> Unit, right: @Composable () -> Unit, leftWeight: Float, rightWeight: Float, onRemove: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.weight(leftWeight)) { left() }
        Box(Modifier.weight(rightWeight)) { right() }
        Icon(Icons.Filled.Close, "삭제", tint = MuyeonColors.hint, modifier = Modifier.size(20.dp).clickable(onClick = onRemove))
    }
}
