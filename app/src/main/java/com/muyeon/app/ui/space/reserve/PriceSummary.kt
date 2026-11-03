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

/** 예상 금액 — 큰 금액 + "시간당 N원 × H (+ 초과 인원) · 최종 금액은 결제 화면에서 확인해요." */
@Composable
fun PriceSummary(estimate: Int, caption: String) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SpaceDesign.ink100).padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("예상 금액", fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = SpaceDesign.ink900)
            Text(
                SpaceDesign.won(estimate), fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 24.sp,
                color = SpaceDesign.primaryText, textAlign = TextAlign.End, modifier = Modifier.weight(1f), style = TabularNums, maxLines = 1,
            )
            Text("원", fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = SpaceDesign.primaryText)
        }
        // 3줄 자리 고정 — 시간·초과 인원 요금이 바뀌어 문장이 길어져도 상자 높이가 그대로.
        Caption(caption, lines = 3)
    }
}
