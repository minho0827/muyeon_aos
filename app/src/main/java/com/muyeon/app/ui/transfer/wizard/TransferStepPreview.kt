package com.muyeon.app.ui.transfer.wizard

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
import com.muyeon.app.ui.jobposting.SectionHead
import com.muyeon.app.ui.transfer.TransferCard
import com.muyeon.app.ui.transfer.TransferForm
import com.muyeon.app.ui.transfer.TransferLabels
import com.muyeon.app.ui.transfer.components.TransferCardView
import kotlinx.coroutines.launch

/** 미리보기 — 목록 카드 모양 + 요약 표. */
@Composable
fun TransferStepPreview(f: TransferForm) {
    SectionHead("미리보기", "목록에는 이렇게 보여요. 확인 후 등록해 주세요.")
    Box(Modifier.fillMaxWidth().clip(MuyeonList.cardShape).background(MuyeonList.fillBg).padding(12.dp)) {
        TransferCardView(previewCard(f), onClick = {})
    }
    MarketInfoTable(previewRows(f))
    Spacer(Modifier.height(4.dp))
}

private fun previewCard(f: TransferForm) = TransferCard(
    id = 0, authorId = 0, authorName = "", title = f.title.ifBlank { "제목" }, status = "OPEN",
    facilityType = f.facilityType, operatingStatus = f.operatingStatus, region = f.region.ifBlank { null }, regionCode = f.regionCode,
    areaPyeong = f.areaPyeong.toDoubleOrNull(), floor = f.floor.ifBlank { null }, deposit = f.deposit.toIntOrNull(),
    monthlyRent = f.monthlyRent.toIntOrNull(), maintenanceFee = f.maintenanceFee.toIntOrNull(), premium = f.premium.toIntOrNull(),
    premiumNegotiable = f.premiumNegotiable, hourlyPrice = f.hourlyPrice.toIntOrNull(), options = f.options.toList(),
    image = f.images.firstOrNull(), viewCount = 0, createdAt = null, scrapped = false, isExposed = true,
)

private fun previewRows(f: TransferForm): List<Pair<String, String>> = buildList {
    add("사진" to "${f.images.size}장")
    if (f.roadAddress.isNotBlank()) add("주소" to (f.roadAddress + if (f.addressDetail.isNotBlank()) " ${f.addressDetail}" else ""))
    add("위치 공개" to if (f.exactLocationVisible) "정확한 위치" else "대략적인 위치")
    TransferLabels.facility(f.facilityType)?.let { add("시설 유형" to it) }
    TransferLabels.operating(f.operatingStatus)?.let { add("운영 상태" to it) }
    if (f.options.isNotEmpty()) add("옵션" to f.options.joinToString(", ") { TransferLabels.option(it) })
    f.availableFrom?.let { add("입주 가능일" to it) }
    if (f.monthlyRevenue.isNotBlank()) add("월매출" to "${f.monthlyRevenue}만원 · ${if (f.revenueVisible) "공개" else "비공개"}")
    if (f.contactName.isNotBlank()) add("담당자" to f.contactName)
    TransferLabels.ownerRelation.firstOrNull { it.first == f.ownerRelation }?.let { add("매물과의 관계" to it.second) }
    if (f.contactPhone.isNotBlank()) add("전화번호" to "${f.contactPhone} · ${if (f.phoneVisible) "회원 공개" else "비공개"}")
}
