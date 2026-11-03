package com.muyeon.app.ui.space.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Map
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.*
import com.muyeon.app.ui.common.market.*
import com.muyeon.app.ui.quote.QuoteUi
import com.muyeon.app.ui.space.*
import kotlinx.coroutines.launch
import org.json.JSONArray

// MARK: benefit 카드 → 요금 안내

@Composable
internal fun SpaceFeeCard(space: SpaceDetail, refund: SpaceRefundView) {
    Column(
        Modifier.padding(horizontal = SpaceDesign.gutter).fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)).background(Color.White)
            .border(2.dp, SpaceDesign.primary, RoundedCornerShape(12.dp))
            .padding(SpaceDesign.cardPadding),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        FeeBlock(
            title = "시간 단위 요금",
            amountText = SpaceDesign.won(space.pricePerHour ?: space.lowestPrice ?: 0),
            showWonSuffix = true,
            rows = space.hourlyOptions.map { SpaceFeeRow(null, it.label ?: "기본 요금", it.priceText) },
        )

        SpaceDashedDivider()

        Row(Modifier.fillMaxWidth().height(30.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "최저 요금",
                fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                lineHeight = 18.sp, color = SpaceDesign.ink900,
            )
            Text(
                SpaceDesign.won(space.lowestPrice ?: 0),
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 24.sp,
                lineHeight = 29.sp, color = SpaceDesign.primaryText, textAlign = TextAlign.End,
                modifier = Modifier.weight(1f),
            )
            Text(
                "원",
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp,
                lineHeight = 22.sp, color = SpaceDesign.primaryText,
            )
        }

        space.packageOptions.takeIf { it.isNotEmpty() }?.let { PackageFeeBlock(it) }
        RefundBox(refund)
    }
}

/** 패키지 요금 블록 — 패키지 옵션이 있을 때만 부른다. */
@Composable
private fun PackageFeeBlock(options: List<SpacePriceOption>) {
    SpaceDashedDivider()
    FeeBlock(
        title = "패키지 요금",
        amountText = "${options.size}종",
        showWonSuffix = false,
        rows = options.map { SpaceFeeRow(it.unit, it.label ?: "패키지", it.priceText) },
    )
}

/** 혜택적용 블록 — 제목행(라벨/금액/단위) + └ 상세행들. */
@Composable
internal fun FeeBlock(title: String, amountText: String, showWonSuffix: Boolean, rows: List<SpaceFeeRow>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                title,
                fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                lineHeight = 18.sp, color = SpaceDesign.ink900,
            )
            Text(
                amountText,
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                lineHeight = 19.sp, color = SpaceDesign.ink900, textAlign = TextAlign.End,
                modifier = Modifier.weight(1f),
            )
            if (showWonSuffix) {
                Text(
                    "원",
                    fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp,
                    lineHeight = 15.sp, color = SpaceDesign.ink900,
                )
            }
        }
        if (rows.isEmpty()) {
            DetailRow(SpaceFeeRow(null, "등록된 가격 옵션이 없어요", ""))
        } else {
            rows.forEach { DetailRow(it) }
        }
    }
}
