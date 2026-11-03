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
import kotlinx.coroutines.launch

/** 보는 사람 하단 바 — (전화번호가 있으면) 전화 · [채팅] · [예약하기]. */
@Composable
fun SpaceBookingBar(phone: String?, onChat: () -> Unit, onReserve: () -> Unit) {
    MarketBottomBarFrame {
        phone?.let { MarketBarPhone(it) }
        MarketBarSecondary("채팅", onChat)
        MarketBarPrimary("예약하기", onClick = onReserve)
    }
}

/** 예약 내역에서 들어온 경우(예약 버튼 숨김) — (전화) · [채팅하기]. */
@Composable
fun SpaceChatOnlyBar(phone: String?, onChat: () -> Unit) {
    MarketBottomBarFrame {
        phone?.let { MarketBarPhone(it) }
        MarketBarPrimary("채팅하기", onClick = onChat)
    }
}
