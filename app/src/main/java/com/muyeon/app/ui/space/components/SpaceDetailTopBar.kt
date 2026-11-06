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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
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
import com.muyeon.app.ui.space.SpaceBackArrow
import com.muyeon.app.ui.space.SpaceDesign
import kotlinx.coroutines.launch

/** 헤더(MO_header/top — h56, 타이틀 Bold 18, ic_x24_back) — ← · "공간 상세" · 찜. */
@Composable
fun SpaceDetailTopBar(scrapped: Boolean, onClose: () -> Unit, onScrap: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(SpaceDesign.headerHeight).background(Color.White),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.width(44.dp).fillMaxHeight().padding(start = 12.dp).clickable(onClick = onClose),
            contentAlignment = Alignment.Center,
        ) { SpaceBackArrow() }
        Text(
            "공간 상세",
            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp,
            lineHeight = 26.sp, color = SpaceDesign.ink900, maxLines = 1,
            modifier = Modifier.padding(start = 8.dp).weight(1f),
        )
        Box(Modifier.width(48.dp).fillMaxHeight().clickable(onClick = onScrap), contentAlignment = Alignment.Center) {
            Icon(
                if (scrapped) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, "찜",
                tint = if (scrapped) SpaceDesign.primary else SpaceDesign.ink900,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/** 공간을 못 찾음. */
@Composable
fun SpaceNotFound() {
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Text("공간을 찾을 수 없어요.", fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 15.sp, color = SpaceDesign.ink500)
    }
}

/** 상세 자리(불러오는 중) — 회색 바탕 · 갤러리(240) · 티켓 카드 · 섹션 제목 · 요금 카드 자리. 실제 화면과 같은 여백. */
@Composable
fun SpaceDetailSkeleton() {
    // ★ 2026-11-06 목록·화면 바탕 흰색(웹과 같게, 사용자 결정) — 상세 바탕과 같게 흰색, 자리 카드는 테두리.
    Column(Modifier.fillMaxSize().background(Color.White)) {
        Box(Modifier.fillMaxWidth().height(240.dp).background(SkeletonColor))
        Column(
            Modifier.padding(horizontal = SpaceDesign.gutter).padding(top = 32.dp).fillMaxWidth()
                .clip(RoundedCornerShape(SpaceDesign.cardRadius))
                .border(1.dp, com.muyeon.app.ui.common.MuyeonColors.border, RoundedCornerShape(SpaceDesign.cardRadius))
                .background(Color.White).padding(SpaceDesign.cardPadding),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            SkeletonLine(18, 0.6f, lineHeight = 22)
            SkeletonLine(18, 0.4f, lineHeight = 22)
            Box(Modifier.padding(top = 16.dp).fillMaxWidth().height(100.dp).skeleton(8))
            Box(Modifier.padding(top = 16.dp)) { SkeletonLine(24, 0.5f, lineHeight = 29) }
        }
        Box(Modifier.fillMaxWidth().padding(horizontal = 21.dp).padding(top = 32.dp, bottom = 15.dp)) { SkeletonLine(18, 0.45f, lineHeight = 22) }
        Box(Modifier.padding(horizontal = SpaceDesign.gutter).fillMaxWidth().height(200.dp).skeleton(12))
    }
}
