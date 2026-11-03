package com.muyeon.app.ui.space.components

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
import com.muyeon.app.ui.space.SpaceCategory
import com.muyeon.app.ui.transfer.TransferLabels
import kotlinx.coroutines.launch

val SPACE_SORTS = listOf("recent" to "추천순", "priceAsc" to "가격 낮은순", "priceDesc" to "가격 높은순")

/** 카테고리 칩(첫 칩 "전체" = 0). */
@Composable
fun SpaceCategoryChips(categories: List<SpaceCategory>, selected: Int, onSelect: (Int) -> Unit) {
    MuyeonPillTabs(
        listOf(MuyeonPillTab(0, "전체")) + categories.map { MuyeonPillTab(it.id, it.name) },
        selected, onSelect, tagPrefix = "space.category", compact = true, showDivider = false,
    )
}

/** 필터 줄 — [지역][날짜][인원][정렬] 알약(값이 걸리면 굵게) + 아래 구분선. */
@Composable
fun SpaceFilterBar(sido: String?, date: String?, headcount: Int?, sort: String, onOpen: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(Color.White).horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MarketFilterPill(TransferLabels.regions.firstOrNull { it.second == sido }?.first?.takeIf { sido != null } ?: "지역", sido != null) { onOpen("region") }
        MarketFilterPill(date?.substring(5)?.replace('-', '/') ?: "날짜", date != null) { onOpen("date") }
        MarketFilterPill(headcount?.let { "${it}명" } ?: "인원", headcount != null) { onOpen("headcount") }
        MarketFilterPill(SPACE_SORTS.first { it.first == sort }.second, sort != "recent") { onOpen("sort") }
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(MuyeonList.ink200))
}
