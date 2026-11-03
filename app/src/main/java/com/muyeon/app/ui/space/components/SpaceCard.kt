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
import com.muyeon.app.ui.space.SpaceListItem
import kotlinx.coroutines.launch

/**
 * 공간 카드 — 사진 · 이름 · "지역 · 최대 N명" · "시간당 N원~". 사진 위 오른쪽 칸(topEnd)은 쓰는 곳이 고른다
 *  (목록 [SpaceCardHeart], 내 공간은 비움).
 */
@Composable
fun SpaceCard(s: SpaceListItem, onClick: () -> Unit, topEnd: @Composable BoxScope.() -> Unit = {}) {
    Column(Modifier.fillMaxWidth().clip(MuyeonList.cardShape).background(Color.White).clickable(onClick = onClick)) {
        Box {
            MarketThumb(s.image, Modifier.fillMaxWidth().height(180.dp))
            topEnd()
        }
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            // 줄 자리를 늘 잡아 둔다(요약·가격이 없어도 카드 높이 같음 — 스켈레톤과도 같은 높이).
            Text(s.name, fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = MuyeonColors.textHead, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(s.metaLine, fontFamily = customFontFamily, fontSize = 13.sp, color = MuyeonColors.textSub, minLines = 1, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(s.priceLine.orEmpty(), fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MuyeonColors.textHead, style = TabularNums, minLines = 1, maxLines = 1)
        }
    }
}

/** 카드 사진 위 하트(흰 원 배경). */
@Composable
fun BoxScope.SpaceCardHeart(liked: Boolean, onHeart: () -> Unit) {
    Box(Modifier.align(Alignment.TopEnd).padding(6.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.85f))) {
        MarketHeart(liked, 20.dp, onHeart)
    }
}

/** 카드 자리(불러오는 중) — 사진 180 + 글 3줄, SpaceCard 와 같은 높이. */
@Composable
fun SpaceCardSkeleton() {
    Column(Modifier.fillMaxWidth().clip(MuyeonList.cardShape).background(Color.White)) {
        Box(Modifier.fillMaxWidth().height(180.dp).background(SkeletonColor))
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SkeletonLine(16, 0.6f)
            SkeletonLine(13, 0.45f)
            SkeletonLine(15, 0.35f)
        }
    }
}
