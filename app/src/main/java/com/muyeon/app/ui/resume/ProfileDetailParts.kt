package com.muyeon.app.ui.resume

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.ImageViewerActivity
import com.muyeon.app.ui.common.MuyeonColors
import com.muyeon.app.ui.quote.QuoteUi
import org.json.JSONArray

/*
 * ★ 2026-11-15 공개 프로필 상세 부품 — 강사 상세(PublicProfileScreen)에서 꺼내 무용수 상세(DancerDetailScreen)와 같이 쓴다.
 *  두 화면이 같은 히어로·섹션·글자·하단 버튼 모양이 되도록 한 곳에서 그린다(모양은 강사 상세 그대로).
 */

/**
 * 히어로 — 풀블리드 사진(360) + 좌하단 태그(예: "{장르} 강사"). 사진 없으면 회색 240 + 사람 아이콘(iOS hero).
 *  인스타 스타일 — 사진은 자르지 않고 전체(Fit), 남는 여백은 같은 사진을 흐리게 확대해 비슷한 색감으로 채운다.
 *  blur 는 API 31+ 에서만 적용, 그 이하는 어둡게만. onClick 이 있으면 사진을 눌러 크게 본다.
 */
@Composable
internal fun ProfileHeroImage(image: String?, tag: String?, onClick: (() -> Unit)? = null) {
    val url = QuoteUi.imageUrl(image)
    Box(
        Modifier.fillMaxWidth().height(if (url != null) 360.dp else 240.dp).background(MuyeonColors.tileIdle)
            .then(if (url != null && onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        if (url != null) {
            com.muyeon.app.ui.components.BlurFitImage(url, Modifier.matchParentSize())
        } else {
            Icon(
                Icons.Filled.Person, null, tint = MuyeonColors.hint,
                modifier = Modifier.align(Alignment.Center).size(64.dp),
            )
        }
        tag?.let {
            Text(
                it,
                fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 12.sp,
                lineHeight = 14.sp, color = Color.White,
                modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)
                    .clip(RoundedCornerShape(50)).background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            )
        }
    }
}

/** 이름 줄 오른쪽 찜 하트 — 40 원 안에 22 아이콘(iOS nameHeader). */
@Composable
internal fun ProfileFavoriteButton(liked: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            if (liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, "찜",
            tint = if (liked) MuyeonColors.primary else MuyeonColors.hint,
            modifier = Modifier.size(22.dp),
        )
    }
}

/** 하단 CTA 줄 — 흰 바탕 + 옅은 그림자, 버튼 사이 10(iOS ctaBar). */
@Composable
internal fun ProfileCtaBar(content: @Composable RowScope.() -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .shadow(8.dp, ambientColor = Color.Black.copy(alpha = 0.05f), spotColor = Color.Black.copy(alpha = 0.05f))
            .background(MuyeonColors.surface)
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}

/**
 * ★ 2026-10-31 영상 포트폴리오 — 카드의 '영상 보유'를 보고 들어와도 상세에 영상이 없었다(웹·iOS 와 같은 자리).
 *  http(s) 링크만 연다(유튜브 앱·브라우저).
 */
@Composable
internal fun ProfileVideoSection(videoUrl: String?) {
    val ctx = LocalContext.current
    val url = videoUrl?.trim()?.takeIf { it.startsWith("http://", true) || it.startsWith("https://", true) } ?: return
    ProfileSection("영상") {
        Row(
            Modifier.fillMaxWidth().clickable {
                runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
            },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "영상 포트폴리오",
                fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                lineHeight = 18.sp, color = MuyeonColors.textHead, modifier = Modifier.weight(1f),
            )
            Icon(Icons.Filled.Slideshow, null, tint = MuyeonColors.primary, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text(
                "영상 열기",
                fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                lineHeight = 16.sp, color = MuyeonColors.primary,
            )
        }
    }
}

/**
 * 레슨 가능 시간 — 월~일 × 오전/오후/저녁 도트 그리드(iOS lessonTimeGrid 와 같은 규칙).
 *  행(시간대) 켜짐 = 그 행 버킷 중 하나라도 선택 || 시간대 미선택,
 *  점 켜짐 = 행 켜짐 && (요일 미선택 || 그 요일 선택), 둘 다 비어 있으면 전부 회색.
 *  켜짐 = 주황(primary), 꺼짐 = 회색(border EAEAEA), 8dp 원.
 */
@Composable
internal fun ProfileLessonTimeGrid(availableDays: List<String>, availableTimeSlots: List<String>) {
    val days = availableDays.toSet()
    val slots = availableTimeSlots.toSet()
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.width(34.dp))
            ResumeOptions.weekDays.forEach { d ->
                Text(
                    d,
                    fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp,
                    lineHeight = 16.sp, color = MuyeonColors.textSub, textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        ResumeOptions.gridRows.forEach { (label, keys) ->
            val rowOn = keys.any { it in slots } || slots.isEmpty()
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    label,
                    fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp,
                    lineHeight = 16.sp, color = MuyeonColors.textSub, modifier = Modifier.width(34.dp),
                )
                ResumeOptions.weekDays.forEach { d ->
                    val on = rowOn && (days.isEmpty() || d in days) && !(days.isEmpty() && slots.isEmpty())
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(if (on) MuyeonColors.primary else MuyeonColors.border))
                    }
                }
            }
        }
    }
}

@Composable
internal fun GrayChip(text: String) {
    Text(
        text,
        fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 15.sp,
        color = MuyeonColors.textSub, maxLines = 1,
        modifier = Modifier.clip(RoundedCornerShape(50)).background(MuyeonColors.tileIdle)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

/** 라벨 · 값 줄들 — 값이 빈 줄은 그리지 않는다(웹 ProfileRow). */
@Composable
internal fun InfoRows(rows: List<Pair<String, String?>>) {
    rows.forEach { (label, value) -> value?.takeIf { it.isNotBlank() }?.let { v -> InfoRow(label) { BodyText(v) } } }
}

/** 한 줄 — 왼쪽 회색 라벨(고정폭 104, 간격 12 — 웹 .dancer-profile-row·iOS 와 같음) + 오른쪽 값. */
@Composable
internal fun InfoRow(label: String, value: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(
            label, fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 20.sp,
            color = MuyeonColors.textSub, modifier = Modifier.width(104.dp),
        )
        Spacer(Modifier.width(12.dp))
        Box(Modifier.weight(1f)) { value() }
    }
}

@Composable
internal fun BodyText(text: String) {
    Text(text, fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, color = MuyeonColors.body)
}

/**
 * 여러 줄(줄당 1건) 텍스트 — 자격증·수상(iOS lineList).
 *  ★ 2026-11-08 줄맞춤: 회원이 공백 여러 개로 칸을 맞춰 적은 글(예: "제8회          “     2010.4.25금상")은
 *   글자 폭이 다른 글꼴에서 어긋난다(웹도 공백을 줄여 같은 문제). 줄마다 [이름 | 날짜·결과] 두 칸으로 나눠
 *   오른쪽 칸을 세로로 맞춘다 — 나누는 기준: 공백 2칸 이상, 없으면 'YYYY.M.D' 날짜가 시작하는 곳. 못 나누면 한 칸 그대로.
 */
@Composable
internal fun LineList(text: String) {
    val lines = text.replace("\r", "").split("\n").map { it.trim() }.filter { it.isNotEmpty() }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        lines.forEach { line ->
            val cells = splitAligned(line)
            if (cells == null) BodyText(line)
            else Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Box(Modifier.weight(1f)) { BodyText(cells.first) }
                Spacer(Modifier.width(12.dp))
                Text(cells.second, fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp,
                    color = MuyeonColors.body, maxLines = 1, softWrap = false)
            }
        }
    }
}

private val MULTI_SPACE = Regex("\\s{2,}")
private val DATE_AT = Regex("(?<!\\d)(19|20)\\d{2}\\.\\s?\\d{1,2}\\.\\s?\\d{1,2}\\.?")

/** 한 줄 → (이름, 날짜·결과). 나눌 곳이 없으면 null. 가운데 칸(“ 같은 반복 표시)은 이름 쪽에 붙인다. */
private fun splitAligned(line: String): Pair<String, String>? {
    val parts = line.split(MULTI_SPACE).filter { it.isNotBlank() }
    if (parts.size >= 2) return parts.dropLast(1).joinToString(" ") to parts.last()
    val m = DATE_AT.find(line) ?: return null
    if (m.range.first == 0) return null
    return line.substring(0, m.range.first).trim() to line.substring(m.range.first).trim()
}

/** 포트폴리오 — 4열 정사각 그리드 + 마지막 셀 "+N", 탭 → 풀스크린 뷰어(본인만 저장 허용). iOS portfolioGrid. */
@Composable
internal fun PortfolioGrid(imgs: List<String>, allowSave: Boolean) {
    val ctx = LocalContext.current
    val visible = imgs.take(4)
    val more = imgs.size - visible.size
    val urls = imgs.mapNotNull { QuoteUi.imageUrl(it) }
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        visible.forEachIndexed { idx, path ->
            Box(
                Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(8.dp))
                    .background(MuyeonColors.tileIdle)
                    .clickable { ImageViewerActivity.start(ctx, JSONArray(urls).toString(), idx, allowSave) },
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(QuoteUi.imageUrl(path), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(), placeholder = com.muyeon.app.ui.components.imageLoadingPainter())
                if (idx == visible.size - 1 && more > 0) {
                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)))
                    Text(
                        "+$more",
                        fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                        lineHeight = 18.sp, color = Color.White,
                    )
                }
            }
        }
        repeat(4 - visible.size) { Spacer(Modifier.weight(1f)) }
    }
}

/** 섹션 — 위 구분선(F7F7F7, 위아래 14) + 제목(Bold 15) + 내용, 간격 10(iOS section). */
@Composable
internal fun ProfileSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        HorizontalDivider(Modifier.padding(vertical = 14.dp), thickness = Dp.Hairline, color = MuyeonColors.tileIdle)
        Spacer(Modifier.height(10.dp))
        Text(
            title,
            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp,
            lineHeight = 18.sp, color = MuyeonColors.textHead,
        )
        Spacer(Modifier.height(10.dp))
        content()
    }
}

/** 히어로 위 원형 버튼 — 검정 35% 원 + 흰 아이콘(iOS backButton/moreMenu). */
@Composable
internal fun CircleIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    desc: String,
    modifier: Modifier = Modifier,
    iconSize: androidx.compose.ui.unit.Dp = 20.dp,
    onClick: () -> Unit,
) {
    Box(
        modifier.size(36.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.35f)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, desc, tint = Color.White, modifier = Modifier.size(iconSize))
    }
}

/** 하단 CTA 버튼 — 채움: 주황 바탕 흰 글자 / 테두리: EAEAEA 선 + 검정 글자 / 비활성: 회색(iOS ctaBar). */
@Composable
internal fun CtaButton(
    text: String,
    filled: Boolean,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    keepFilledWhenDisabled: Boolean = false,
    onClick: () -> Unit,
) {
    val active = enabled || keepFilledWhenDisabled
    val shape = RoundedCornerShape(12.dp)
    Text(
        text,
        fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 19.sp,
        color = when {
            filled -> Color.White
            active -> MuyeonColors.textHead
            else -> MuyeonColors.hint
        },
        textAlign = TextAlign.Center,
        modifier = modifier
            .clip(shape)
            .then(
                if (filled) Modifier.background(if (active) MuyeonColors.primary else MuyeonColors.hint)
                else Modifier.border(1.dp, MuyeonColors.border, shape)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 15.dp),
    )
}
