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
import com.muyeon.app.R
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.*
import com.muyeon.app.ui.common.market.*
import com.muyeon.app.ui.transfer.TransferCard
import com.muyeon.app.ui.transfer.TransferLabels
import kotlinx.coroutines.launch

/**
 * 양도 글 카드 — 썸네일(상태 배지) · 제목·요약·금액 · 오른쪽 칸(trailing).
 *  오른쪽 칸은 쓰는 곳이 고른다: 목록(남의 글) [TransferViewerCardActions], 목록(내 글) [TransferOwnCardActions],
 *  내 글·미리보기는 비워 둔다.
 */
@Composable
fun TransferCardView(c: TransferCard, onClick: () -> Unit, trailing: @Composable ColumnScope.() -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().clip(MuyeonList.cardShape).background(Color.White).clickable(onClick = onClick).padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TransferCardThumb(c.image, c.status.takeIf { it != "OPEN" })
        TransferCardText(c, Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp), content = trailing)
    }
}

/** 썸네일(없으면 건물 그림) + 판매중이 아닐 때만 상태 배지. */
@Composable
private fun TransferCardThumb(image: String?, badgeStatus: String?) {
    Box {
        MarketThumb(image, Modifier.size(96.dp).clip(RoundedCornerShape(12.dp)), placeholderRes = R.drawable.transfer_noimage)
        badgeStatus?.let { TransferThumbBadge(it, Modifier.align(Alignment.TopStart)) }
    }
}

@Composable
private fun TransferThumbBadge(status: String, modifier: Modifier) {
    Box(modifier.padding(6.dp)) {
        MarketBadge(TransferLabels.status(status), bg = if (status == "HOLD") MuyeonColors.primary else MuyeonColors.textSub)
    }
}

@Composable
private fun TransferCardText(c: TransferCard, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            c.title, fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp,
            color = MuyeonColors.textHead, maxLines = 2, overflow = TextOverflow.Ellipsis,
        )
        c.metaLine.takeIf { it.isNotEmpty() }?.let {
            Text(it, fontFamily = customFontFamily, fontSize = 13.sp, color = MuyeonColors.textSub, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(
            c.priceLine, fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
            color = MuyeonColors.textHead, maxLines = 2, overflow = TextOverflow.Ellipsis,
        )
    }
}

/** 남의 글 — 하트 + 종이비행기(바로 채팅). */
@Composable
fun TransferViewerCardActions(scrapped: Boolean, onHeart: () -> Unit, onChat: () -> Unit) {
    MarketHeart(scrapped, 20.dp, onHeart)
    DmPlaneButton("바로 채팅", onChat)
}

/** 내 글 — 하트만(자기에게 채팅할 일이 없다). */
@Composable
fun TransferOwnCardActions(scrapped: Boolean, onHeart: () -> Unit) {
    MarketHeart(scrapped, 20.dp, onHeart)
}
