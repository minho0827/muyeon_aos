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
import kotlinx.coroutines.launch

/** 내 양도 글 한 줄 — 카드 · (숨김이면) 안내 · [수정][상태 변경][삭제]. */
@Composable
fun MyTransferRow(c: TransferCard, onOpen: () -> Unit, onEdit: () -> Unit, onStatus: () -> Unit, onDelete: () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(MuyeonList.cardShape).muyeonCardBorder().background(Color.White)) {
        TransferCardView(c, onClick = onOpen)
        c.takeIf { !it.isExposed }?.let { HiddenNotice() }
        Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MarketRowButton("수정", Modifier.weight(1f), onClick = onEdit)
            MarketRowButton("상태 변경", Modifier.weight(1f), onClick = onStatus)
            MarketRowButton("삭제", Modifier.weight(1f), danger = true, onClick = onDelete)
        }
    }
}

@Composable
private fun HiddenNotice() {
    Text(
        "관리자 확인으로 숨겨진 글이에요.", fontFamily = customFontFamily, fontSize = 12.sp, color = MuyeonColors.danger,
        modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 8.dp),
    )
}

/** 내 글 줄 자리(불러오는 중) — 카드 자리 + 버튼 줄 높이(40). */
@Composable
fun MyTransferRowSkeleton() {
    Column(Modifier.fillMaxWidth().clip(MuyeonList.cardShape).muyeonCardBorder().background(Color.White)) {
        TransferCardSkeleton()
        Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(3) { Box(Modifier.weight(1f).height(40.dp).skeleton(10)) }
        }
    }
}
