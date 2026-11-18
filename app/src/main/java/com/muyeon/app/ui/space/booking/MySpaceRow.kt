package com.muyeon.app.ui.space.booking

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
import com.muyeon.app.ui.space.SpaceListItem
import com.muyeon.app.ui.space.components.SpaceCard
import kotlinx.coroutines.launch

/** 내 공간 한 줄 — 카드 + [수정][삭제]. */
@Composable
fun MySpaceRow(s: SpaceListItem, onOpen: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit, onBusiness: () -> Unit = {}) {
    Column(Modifier.fillMaxWidth().clip(MuyeonList.cardShape).muyeonCardBorder().background(Color.White)) {
        SpaceCard(s, onClick = onOpen)
        // ★ 2026-11-18 사업자 확인 상태 한 줄(확인 필요·확인 중·반려). VERIFIED 는 표시 없음.
        com.muyeon.app.ui.space.wizard.SpaceBusinessRules.mineLine(s.business)?.let { line -> BusinessStatusLine(line, onBusiness) }
        Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MarketRowButton("수정", Modifier.weight(1f), onClick = onEdit)
            MarketRowButton("삭제", Modifier.weight(1f), danger = true, onClick = onDelete)
        }
    }
}

/** 내 공간 목록 자리 — 카드 자리 + 버튼 줄(40), MySpaceRow 와 같은 높이. */
@Composable
fun MySpaceListSkeleton(count: Int = 3) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(count) {
            Column(Modifier.fillMaxWidth().clip(MuyeonList.cardShape).muyeonCardBorder().background(Color.White)) {
                com.muyeon.app.ui.space.components.SpaceCardSkeleton()
                Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(2) { Box(Modifier.weight(1f).height(40.dp).skeleton(10)) }
                }
            }
        }
    }
}

/** 내 공간 카드 사업자 확인 줄 — 글(주황=할 일 있음, 회색=확인 중) + [확인하기]. */
@Composable
private fun BusinessStatusLine(line: com.muyeon.app.ui.space.wizard.SpaceBusinessLine, onAction: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 10.dp)
            .clip(RoundedCornerShape(10.dp)).background(if (line.warn) MuyeonColors.tileHeld.copy(alpha = 0.6f) else MuyeonList.ink100)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            line.text, fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 17.sp,
            color = if (line.warn) MuyeonColors.primaryDeep else MuyeonColors.textSub, modifier = Modifier.weight(1f),
        )
        line.action?.let { label ->
            Text(
                label, fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(MuyeonColors.primary).clickable(onClick = onAction)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }
    }
}
