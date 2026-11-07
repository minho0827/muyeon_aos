package com.muyeon.app.ui.home.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.muyeon.app.R
import com.muyeon.app.ui.components.BlurFitImage
import com.muyeon.app.ui.theme.AppTypography
import kotlinx.coroutines.delay

/** 배너 한 장 — 사진 · 흰 글자 제목·부제(사진 아래쪽). */
data class HomeBanner(val key: Any, val image: String, val title: String, val sub: String?, val onClick: () -> Unit)

private const val AUTO_MS = 4000L

/**
 * ★ 2026-11-08 홈 맨 위 넘기는 배너 — 뷰티컬리 첫 띠와 같은 모양(사용자 요청): 꽉 찬 사진(4:3) · 아래 어둡게 + 흰 글자 ·
 *   사진은 자르지 않고 양옆을 흐린 같은 사진으로 채운다(BlurFitImage). 오른쪽 아래 [⏸] [01 | 10 전체보기 ›] 표시 · 4초마다 넘어감(멈춤 가능) · 손으로 넘기기(뷰페이저).
 *   [onAll] 이 없으면 표시는 숫자만.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeBannerPager(banners: List<HomeBanner>, onAll: (() -> Unit)?, modifier: Modifier = Modifier, ratio: Float = 4f / 3f) {
    if (banners.isEmpty()) return
    // 들어올 때마다 순서를 섞는다(사용자 요청) — 같은 배너 묶음이면 화면에 있는 동안은 그 순서를 지킨다.
    val items = remember(banners.map { it.key }) { banners.shuffled() }
    val pager = rememberPagerState { items.size }
    var playing by rememberSaveable { mutableStateOf(true) }
    // ⚠️ 다 넘어간 페이지(settledPage)를 기준으로 기다린다 — currentPage 는 넘기는 도중 절반에서 바뀌어,
    //   그걸 기준으로 하면 타이머가 다시 시작되며 진행 중인 넘김을 취소했다(절반만 움직이고 멈춤, 로그로 확인 2026-11-08).
    //   손으로 넘기는 중에는 자동 넘김을 하지 않는다.
    LaunchedEffect(playing, pager.settledPage, items.size) {
        if (!playing || items.size < 2) return@LaunchedEffect
        delay(AUTO_MS)
        if (!pager.isScrollInProgress) pager.animateScrollToPage((pager.settledPage + 1) % items.size)
    }
    Box(modifier.fillMaxWidth().aspectRatio(ratio)) {
        HorizontalPager(pager, Modifier.fillMaxSize(), key = { items[it].key }) { i ->
            val b = items[i]
            Box(Modifier.fillMaxSize().clickable(onClick = b.onClick)) {
                // 인스타 방식 — 사진 전체가 보이고 양옆은 같은 사진을 흐리게(공개 프로필 히어로와 같다, 사용자 요청).
                BlurFitImage(b.image, Modifier.fillMaxSize())
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.45f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.55f))))
                Column(Modifier.align(Alignment.BottomStart).padding(start = 22.dp, end = 22.dp, bottom = 64.dp)) {
                    Text(b.title, style = AppTypography.Title20.copy(color = Color.White), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    b.sub?.let { Text(it, style = AppTypography.Body14.copy(color = Color.White.copy(alpha = 0.85f)), modifier = Modifier.padding(top = 6.dp), maxLines = 1) }
                }
            }
        }
        Row(Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(34.dp).clip(CircleShape).background(PILL).clickable { playing = !playing }, Alignment.Center) {
                Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    stringResource(if (playing) R.string.home_banner_pause else R.string.home_banner_play), tint = Color.White, modifier = Modifier.size(18.dp))
            }
            Row(
                Modifier.clip(RoundedCornerShape(50)).background(PILL).then(if (onAll != null) Modifier.clickable(onClick = onAll) else Modifier)
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.home_banner_counter, pager.currentPage + 1, items.size), style = AppTypography.Caption12.copy(color = Color.White))
                if (onAll != null) Text(stringResource(R.string.home_view_all_arrow), style = AppTypography.Caption12.copy(color = Color.White), modifier = Modifier.padding(start = 6.dp))
            }
        }
    }
}

private val PILL = Color.Black.copy(alpha = 0.35f)
