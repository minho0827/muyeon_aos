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
import com.muyeon.app.ui.transfer.TransferDetail
import com.muyeon.app.ui.transfer.pyeongText
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/** 기본정보 표 섹션. */
@Composable
fun TransferInfoTable(rows: List<Pair<String, String>>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        MarketSectionTitle("기본 정보")
        MarketInfoTable(rows)
    }
}

/** 기본정보 표 줄 — 빈 값 줄은 빼고, 금액 null 은 "협의". 금액 단위는 만원(표 라벨에 표기). */
fun transferInfoRows(d: TransferDetail): List<Pair<String, String>> {
    val c = d.card
    val rows = mutableListOf<Pair<String, String>>()
    d.ownerRelationLabel?.let { rows += "글쓴이" to it }  // 서버 표시 글자(직접 운영 중·임차인·…), 예전 글은 줄 없음
    val loc = listOfNotNull(d.roadAddress, d.addressDetail).joinToString(" ").ifBlank { c.region }
    loc?.let { rows += "소재지" to it }
    c.areaPyeong?.let { rows += "총면적" to "${pyeongText(it)}평 (약 ${(it * 3.3058).roundToInt()}㎡)" }
    val floor = c.floor?.takeIf { it.isNotBlank() }?.let { if (it.endsWith("층")) it else "${it}층" }
    val elev = d.hasElevator?.let { if (it) "엘리베이터 있음" else "엘리베이터 없음" }
    listOfNotNull(floor, elev).takeIf { it.isNotEmpty() }?.let { rows += "층/엘리베이터" to it.joinToString(" · ") }
    rows += "보증금(만원)" to MarketFormat.manwon(c.deposit)
    rows += "월세(만원)" to MarketFormat.manwon(c.monthlyRent)
    c.maintenanceFee?.let { rows += "관리비(만원)" to MarketFormat.num(it) }
    rows += "권리금(만원)" to (MarketFormat.manwon(c.premium) + if (c.premiumNegotiable && c.premium != null) " (협의가능)" else "")
    if (d.vatSeparate) rows += "부가세" to "별도"
    c.hourlyPrice?.let { rows += "시간당 대관" to (MarketFormat.won(it) ?: "") }
    d.parkingCount?.let { rows += "주차" to if (it > 0) "${it}대" else "불가" }
    d.availableFrom?.let { rows += "입주 가능일" to it.replace('-', '.') }
    d.studentCount?.let { rows += "원생 수" to "${MarketFormat.num(it)}명" }
    d.monthlyRevenue?.let { rows += "월매출(만원)" to MarketFormat.num(it) }
    return rows
}
