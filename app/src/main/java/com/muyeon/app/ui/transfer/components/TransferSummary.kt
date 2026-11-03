package com.muyeon.app.ui.transfer.components

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
import com.muyeon.app.ui.transfer.TransferCard
import com.muyeon.app.ui.transfer.TransferLabels
import kotlinx.coroutines.launch

/** 상단 요약 — 상태·시설유형·운영 칩 → 제목 → "지역 · 등록일 · 조회 N". */
@Composable
fun TransferSummary(c: TransferCard) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            MarketStatusBadge(
                TransferLabels.status(c.status), TRANSFER_STATUS_BADGE_WIDTH,
                bg = when (c.status) { "OPEN" -> MuyeonColors.primary; "HOLD" -> MuyeonColors.primaryDeep; else -> MuyeonColors.textSub },
            )
            TransferLabels.facility(c.facilityType)?.let { TransferGrayChip(it) }
            TransferLabels.operating(c.operatingStatus)?.let { TransferGrayChip(it) }
        }
        Text(c.title, fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 30.sp, color = MuyeonColors.textHead, minLines = 2)
        Text(
            listOfNotNull(c.region, c.createdAt?.take(10)?.replace('-', '.'), "조회 ${c.viewCount}").joinToString(" · "),
            fontFamily = customFontFamily, fontSize = 13.sp, color = MuyeonColors.textSub, style = TabularNums, maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun TransferGrayChip(text: String) = MarketBadge(text, fg = MuyeonColors.textHead, bg = MuyeonList.ink100)
