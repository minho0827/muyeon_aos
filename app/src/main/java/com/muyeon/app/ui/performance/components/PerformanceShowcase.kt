package com.muyeon.app.ui.performance.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.R
import com.muyeon.app.ui.components.RemoteImage
import com.muyeon.app.ui.format.KstDates
import com.muyeon.app.ui.performance.PerformanceLabels
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.domain.model.performance.PerformanceStatus
import com.muyeon.domain.model.performance.PerformanceSummary

/**
 * ★ 2026-11-09 공연 탭 NOL식 머리(사용자 시안 승인) — [전체]일 때 목록 위에:
 *   큰 포스터 배너(공연 중 먼저, 최대 8장, '01 | N') → 장르 아이콘 줄 → '지금 공연 중' · '곧 시작해요' 포스터 줄.
 *   데이터는 목록이 이미 받아 온 첫 페이지에서 고른다(따로 부르지 않는다). 목록에는 가격이 없어 가격은 보이지 않는다.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PerformanceBanner(items: List<PerformanceSummary>, onOpen: (Long) -> Unit, modifier: Modifier = Modifier) {
    if (items.isEmpty()) return
    val pager = rememberPagerState { items.size }
    HorizontalPager(
        state = pager,
        contentPadding = PaddingValues(start = 16.dp, end = 40.dp),
        pageSpacing = 10.dp,
        modifier = modifier.fillMaxWidth().height(300.dp).testTag("perf.banner"),
    ) { page ->
        val p = items[page]
        Box(Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp)).background(Color(0xFF2E3A59)).clickable { onOpen(p.id) }) {
            RemoteImage(p.posterUrl, Modifier.fillMaxSize(), fallbackRes = R.drawable.img_noimage_performance)
            Column(
                Modifier.align(Alignment.BottomStart).fillMaxWidth().background(Color(0xD1121624)).padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    stringResource(PerformanceLabels.status(p.status)), style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                    color = AppColors.TextHead, modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(AppColors.PrimarySoft).padding(horizontal = 7.dp, vertical = 2.dp),
                )
                Text(p.title, style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 28.sp), color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    listOfNotNull(p.venue, KstDates.dashPeriod(p.startAt, p.endAt).ifEmpty { null }).joinToString(" · "),
                    style = TextStyle(fontSize = 13.sp), color = Color(0xFFC9CEDC), maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                "%02d | %02d".format(page + 1, items.size), style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold), color = Color.White,
                modifier = Modifier.align(Alignment.TopEnd).padding(14.dp).clip(RoundedCornerShape(14.dp)).background(Color(0x73000000)).padding(horizontal = 12.dp, vertical = 5.dp),
            )
        }
    }
}

/** '지금 공연 중' · '곧 시작해요' — 포스터 120×160 가로 줄. 비면 그리지 않는다. */
@Composable
fun PerformancePosterRow(title: String, items: List<PerformanceSummary>, onOpen: (Long) -> Unit, modifier: Modifier = Modifier) {
    if (items.isEmpty()) return
    Column(modifier.fillMaxWidth().padding(top = 22.dp)) {
        Text(title, style = AppTypography.Section16, color = AppColors.TextHead, modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(items, key = { it.id }) { p ->
                Column(Modifier.width(120.dp).clickable { onOpen(p.id) }, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    RemoteImage(p.posterUrl, Modifier.width(120.dp).height(160.dp).clip(RoundedCornerShape(10.dp)), fallbackRes = R.drawable.img_noimage_performance, thumbnail = true)
                    Text(p.title, style = TextStyle(fontSize = 13.5.sp, fontWeight = FontWeight.Bold), color = AppColors.TextHead, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(p.venue.orEmpty(), style = TextStyle(fontSize = 11.5.sp), color = AppColors.TextHint, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(KstDates.dashPeriod(p.startAt, p.endAt), style = TextStyle(fontSize = 11.5.sp), color = AppColors.TextHint, maxLines = 1)
                }
            }
        }
    }
}

/** 목록 위 소제목 — '전체 공연'. */
@Composable
fun PerformanceListTitle(title: String) {
    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 4.dp)) {
        Text(title, style = AppTypography.Section16, color = AppColors.TextHead)
    }
}

/** 배너 고르기 — 공연 중 → 곧 시작 순서, 포스터 있는 것만, 최대 8장. */
fun pickBanner(items: List<PerformanceSummary>): List<PerformanceSummary> =
    (items.filter { it.status == PerformanceStatus.ONGOING } + items.filter { it.status == PerformanceStatus.UPCOMING })
        .filter { !it.posterUrl.isNullOrBlank() }.take(8)
