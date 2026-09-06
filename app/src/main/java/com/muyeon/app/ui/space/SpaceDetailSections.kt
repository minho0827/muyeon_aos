package com.muyeon.app.ui.space

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.theme.customFontFamily

/**
 * 공간 상세 조각 — iOS `SpaceDetailView+Summary.swift` / `+Sections.swift` 1:1.
 *  티켓 카드 / 요금 카드 / 읽기 섹션(공간소개·시설·유의사항·위치).
 */

// MARK: info_reservation (티켓 카드)

@Composable
internal fun SpaceTicketCard(space: SpaceDetail) {
    Column(
        Modifier.padding(horizontal = SpaceDesign.gutter).padding(top = 32.dp)
            .fillMaxWidth().clip(RoundedCornerShape(SpaceDesign.cardRadius))
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
    }
}

private fun ticketSubtitle(space: SpaceDetail): String {
    val parts = listOfNotNull(space.spaceType, space.subwayInfo).filter { it.isNotEmpty() }
    return if (parts.isEmpty()) space.region ?: "공간 대관" else parts.joinToString(" · ")
}

private fun ticketPriceCaption(space: SpaceDetail): String {
    val minHours = space.hourlyMinHours ?: 1
    return if (minHours > 1) "1시간 기준, 최소 ${minHours}시간" else "1시간 기준, 부가세 포함"
}

@Composable
private fun RowScope.PanelColumn(label: String, value: String, caption: String) {
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

// MARK: benefit 카드 → 요금 안내

@Composable
internal fun SpaceFeeCard(space: SpaceDetail) {
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

        if (space.packageOptions.isNotEmpty()) {
            SpaceDashedDivider()
            FeeBlock(
                title = "패키지 요금",
                amountText = "${space.packageOptions.size}종",
                showWonSuffix = false,
                rows = space.packageOptions.map { SpaceFeeRow(it.unit, it.label ?: "패키지", it.priceText) },
            )
        }

        val rules = (space.refundPolicy ?: emptyList()).filter { !it.label.isNullOrEmpty() }
        if (rules.isNotEmpty()) RefundBox(rules)
    }
}

/** 혜택적용 블록 — 제목행(라벨/금액/단위) + └ 상세행들. */
@Composable
private fun FeeBlock(title: String, amountText: String, showWonSuffix: Boolean, rows: List<SpaceFeeRow>) {
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
            DetailRow(SpaceFeeRow(null, "등록된 가격 옵션이 없습니다", ""))
        } else {
            rows.forEach { DetailRow(it) }
        }
    }
}

/** ic_x12_level(└) + 라벨(+배지) ↔ 값 행. */
@Composable
private fun DetailRow(row: SpaceFeeRow, color: Color = SpaceDesign.ink500) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        SpaceLevelLead()
        Row(
            Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            row.badge?.takeIf { it.isNotEmpty() }?.let { SpaceMiniLabel(it) }
            Text(
                row.title,
                fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp,
                lineHeight = 16.sp, color = color,
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            row.value,
            fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp,
            lineHeight = 16.sp, color = color, textAlign = TextAlign.End,
        )
    }
}

/** "포인트 더 받는 방법" 내부 박스 자리 → 환불 규정. */
@Composable
private fun RefundBox(rules: List<SpaceRefundRule>) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(SpaceDesign.primaryTint)
            .border(1.dp, SpaceDesign.primaryTintLine, RoundedCornerShape(12.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "환불 규정",
                fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp,
                lineHeight = 19.sp, color = SpaceDesign.ink800, modifier = Modifier.weight(1f),
            )
            Text(
                "취소 시점 기준",
                fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp,
                lineHeight = 15.sp, color = SpaceDesign.ink900,
            )
        }
        rules.forEach {
            DetailRow(SpaceFeeRow(null, it.label ?: "-", it.value ?: "-"), color = SpaceDesign.ink700)
        }
    }
}

// MARK: 읽기 섹션들

@Composable
internal fun SpaceIntroSection(space: SpaceDetail) = WhiteSection("공간 소개") {
    Text(
        firstNonEmpty(space.intro, space.description) ?: "등록된 소개가 없습니다.",
        fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp,
        lineHeight = 24.sp, color = SpaceDesign.ink700,
    )
}

@Composable
internal fun SpaceFacilitySection(space: SpaceDetail) = WhiteSection("시설 안내") {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ReadField("수용인원", space.capacityText)
        space.spaceType?.takeIf { it.isNotEmpty() }?.let { ReadField("공간유형", it) }
        space.areaText?.let { ReadField("공간면적", it) }
        ReadField("영업시간", space.businessHoursText)
        ReadField("휴무일", space.holidaysText)

        val items = space.enabledFacilities
        if (items.isNotEmpty()) {
            // LazyVerticalGrid 는 스크롤 컨테이너 안에서 높이가 무한이라 못 쓴다 — 4열로 직접 쪼갠다.
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                items.chunked(4).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { f ->
                            Column(
                                Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                                    .background(SpaceDesign.ink100).padding(vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(f.icon, null, tint = SpaceDesign.primaryText, modifier = Modifier.size(20.dp))
                                Text(
                                    f.label,
                                    fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 11.sp,
                                    lineHeight = 14.sp, color = SpaceDesign.ink700,
                                    textAlign = TextAlign.Center, maxLines = 2,
                                )
                            }
                        }
                        // 마지막 줄이 4개가 안 되면 빈 칸으로 채워 열 폭을 유지한다.
                        repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
        space.facilityList?.filter { it.isNotEmpty() }?.takeIf { it.isNotEmpty() }?.let {
            NumberedBlock("시설 상세", it)
        }
    }
}

@Composable
internal fun SpaceCautionSection(space: SpaceDetail) = WhiteSection("예약 시 유의사항") {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(SpaceDesign.primaryTint).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Info, null, tint = SpaceDesign.primaryText, modifier = Modifier.size(16.dp))
            Text(
                "알려드립니다",
                fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                lineHeight = 18.sp, color = SpaceDesign.ink900,
            )
        }
        Text(
            cautionText(space),
            fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp,
            lineHeight = 24.sp, color = SpaceDesign.ink700,
        )
    }
}

private fun cautionText(space: SpaceDetail): String {
    val items = (space.cautions ?: emptyList()).filter { it.isNotEmpty() }
    if (items.isEmpty()) {
        return firstNonEmpty(space.description) ?: "예약 확정 전 공간 소유자와 이용 시간을 확인해 주세요."
    }
    return items.mapIndexed { i, s -> "${i + 1}. $s" }.joinToString("\n")
}

@Composable
internal fun SpaceLocationSection(space: SpaceDetail, onOpenMap: () -> Unit) = WhiteSection("위치") {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ReadField("주소", space.addressText ?: "-")
        if (space.addressText != null) {
            Text(
                "지도 앱으로 열기",
                fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                lineHeight = 18.sp, color = SpaceDesign.ink900, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, SpaceDesign.ink200, RoundedCornerShape(8.dp))
                    .clickable(onClick = onOpenMap)
                    .wrapContentHeight(Alignment.CenterVertically),
            )
        }
    }
}

// MARK: 공용 조각

@Composable
private fun WhiteSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().background(Color.White)
            .padding(horizontal = SpaceDesign.gutter, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        content = {
            Text(
                title,
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp,
                lineHeight = 22.sp, color = SpaceDesign.ink900,
            )
            content()
        },
    )
}

/** 시안 입력 필드(라벨 + 테두리 박스)를 읽기 전용으로 사용. */
@Composable
private fun ReadField(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            label,
            fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
            lineHeight = 17.sp, color = SpaceDesign.ink900,
        )
        Text(
            value.ifEmpty { "-" },
            fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 15.sp,
            lineHeight = 18.sp, color = SpaceDesign.ink700,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, SpaceDesign.ink200, RoundedCornerShape(8.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp),
        )
    }
}

@Composable
private fun NumberedBlock(title: String, items: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title,
            fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
            lineHeight = 17.sp, color = SpaceDesign.ink900,
        )
        Text(
            items.mapIndexed { i, s -> "${i + 1}. $s" }.joinToString("\n"),
            fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp,
            lineHeight = 24.sp, color = SpaceDesign.ink700,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                .background(SpaceDesign.ink100).padding(16.dp),
        )
    }
}

private fun firstNonEmpty(vararg values: String?): String? =
    values.filterNotNull().firstOrNull { it.trim().isNotEmpty() }
