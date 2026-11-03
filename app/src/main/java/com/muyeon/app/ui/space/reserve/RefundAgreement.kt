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
import androidx.compose.material.icons.filled.Check
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
import com.muyeon.app.ui.space.SpaceRefundView
import kotlinx.coroutines.launch

/** 환불 규정 표 — 서버 refund.rows("when · refund") + 결제 직후 무료 취소 안내(freeCancelNote). */
@Composable
fun ReserveRefundTable(refund: SpaceRefundView) {
    FieldGroup("환불 규정") {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).border(1.dp, SpaceDesign.ink200, RoundedCornerShape(12.dp)).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            refund.rows.forEach { r ->
                Row {
                    Text(r.whenText, fontFamily = customFontFamily, fontSize = 14.sp, color = SpaceDesign.ink600, modifier = Modifier.weight(1f))
                    Text(r.refundText, fontFamily = customFontFamily, fontSize = 14.sp, color = SpaceDesign.ink900)
                }
            }
            refund.freeCancelNote?.let { Caption(it) }
        }
    }
}

/** 예약 시트 체크 상자(22dp, r6). */
@Composable
internal fun ReserveCheckBox(checked: Boolean) {
    Box(
        Modifier.size(22.dp).clip(RoundedCornerShape(6.dp))
            .background(if (checked) SpaceDesign.primary else Color.White)
            .border(1.dp, if (checked) SpaceDesign.primary else SpaceDesign.ink300, RoundedCornerShape(6.dp)),
        Alignment.Center,
    ) { if (checked) Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(16.dp)) }
}
