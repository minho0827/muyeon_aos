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

/**
 * 티켓 카드 그림자색. iOS 는 Color.black.opacity(0.04) 를 그대로 쓰지만,
 *  Compose 의 shadow 는 elevation 에 따라 알파를 한 번 더 감쇠시켜 4% 면 사실상 안 보인다.
 *  같은 진하기로 보이도록 10% 로 올렸다(수치가 아니라 결과를 맞춘 값).
 */
internal val SHADOW = Color.Black.copy(alpha = 0.10f)

/**
 * 공간 상세 조각 — iOS `SpaceDetailView+Summary.swift` / `+Sections.swift` 1:1.
 *  티켓 카드 / 요금 카드 / 읽기 섹션(공간소개·시설·유의사항·위치).
 */

// MARK: info_reservation (티켓 카드)

@Composable
internal fun SpaceTicketCard(space: SpaceDetail) {
    Column(
        Modifier.padding(horizontal = SpaceDesign.gutter).padding(top = 32.dp)
            .fillMaxWidth()
            // iOS .shadow(radius 10, y 2) 대응 — Compose 는 반경 대신 elevation 이라 5dp 로 맞춘다.
            //  ★ shadow 는 clip/background 보다 앞에 와야 그림자가 카드 밖으로 나온다.
            .shadow(5.dp, RoundedCornerShape(SpaceDesign.cardRadius), spotColor = SHADOW, ambientColor = SHADOW)
            .clip(RoundedCornerShape(SpaceDesign.cardRadius))
            .background(Color.White).padding(SpaceDesign.cardPadding),
    ) {
        Text(
            space.name,
            fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 18.sp,
            lineHeight = 22.sp, color = SpaceDesign.ink900, maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        Text(
            ticketSubtitle(space),
            fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 18.sp,
            lineHeight = 22.sp, color = SpaceDesign.primaryText, maxLines = 1,
            overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp),
        )

        // 체크인/체크아웃 패널 자리 — 영업시간 / 수용인원 + 가운데 예약방식 필 배지
        Box(
            Modifier.padding(top = 20.dp).fillMaxWidth().height(100.dp)
                .clip(RoundedCornerShape(8.dp)).background(SpaceDesign.ink100),
            contentAlignment = Alignment.Center,
        ) {
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                PanelColumn("영업시간", space.businessHoursText, "휴무 ${space.holidaysText}")
                PanelColumn("수용인원", space.capacityText, space.areaText ?: "면적 문의")
            }
            Text(
                space.bookingBadge,
                fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                lineHeight = 17.sp, color = Color.White, textAlign = TextAlign.Center,
                modifier = Modifier.clip(RoundedCornerShape(50)).background(SpaceDesign.primaryFill)
                    .defaultMinSize(minWidth = 48.dp, minHeight = 32.dp)
                    .padding(horizontal = 12.dp)
                    .wrapContentHeight(Alignment.CenterVertically),
            )
        }

        Row(Modifier.padding(top = 20.dp).fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    "대관료",
                    fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 18.sp,
                    lineHeight = 22.sp, color = SpaceDesign.ink900,
                )
                Text(
                    ticketPriceCaption(space),
                    fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp,
                    lineHeight = 17.sp, color = SpaceDesign.ink600, maxLines = 1,
                )
            }
            Text(
                "${SpaceDesign.won(space.pricePerHour ?: space.lowestPrice ?: 0)}원",
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 24.sp,
                lineHeight = 29.sp, color = SpaceDesign.ink900,
            )
        }
        SpaceApprovalLine(space, Modifier.padding(top = 12.dp))
    }
}

/** 예약 방식 한 줄(88f738b) — REQUEST: "호스트 수락 후 확정"(+ " · {예상 응답}"), INSTANT: "바로 확정". */
fun approvalText(space: SpaceDetail): String =
    if (space.approvalMode == "INSTANT") "바로 확정"
    else "호스트 수락 후 확정" + (space.hostResponseLabel?.let { " · $it" } ?: "")

@Composable
internal fun SpaceApprovalLine(space: SpaceDetail, modifier: Modifier = Modifier) {
    Text(
        approvalText(space),
        fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 17.sp,
        color = SpaceDesign.primaryText, maxLines = 1, overflow = TextOverflow.Ellipsis,
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(SpaceDesign.primaryTint).padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

internal fun ticketSubtitle(space: SpaceDetail): String {
    val parts = listOfNotNull(space.spaceType, space.subwayInfo).filter { it.isNotEmpty() }
    return if (parts.isEmpty()) space.region ?: "공간 대관" else parts.joinToString(" · ")
}

internal fun ticketPriceCaption(space: SpaceDetail): String {
    val minHours = space.hourlyMinHours ?: 1
    return if (minHours > 1) "1시간 기준, 최소 ${minHours}시간" else "1시간 기준, 부가세 포함"
}

@Composable
internal fun RowScope.PanelColumn(label: String, value: String, caption: String) {
    Column(
        Modifier.weight(1f).padding(horizontal = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            label,
            fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp,
            lineHeight = 16.sp, color = SpaceDesign.ink600, maxLines = 1,
        )
        Text(
            value,
            fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp,
            lineHeight = 19.sp, color = SpaceDesign.ink900, maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        Text(
            caption,
            fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp,
            lineHeight = 17.sp, color = SpaceDesign.ink900, maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}
