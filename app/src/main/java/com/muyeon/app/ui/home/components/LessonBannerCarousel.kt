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
import com.muyeon.app.ui.components.RemoteImage
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.domain.model.home.HomeLesson
import kotlinx.coroutines.delay

private const val AUTO_MS = 4000L

/**
 * 레슨 쪽 맨 위 큰 배너 — 뷰티컬리 첫 띠처럼: 꽉 찬 사진(4:3) · 아래 어둡게 깔고 흰 글자(제목·장소) ·
 *   오른쪽 아래 [⏸][01 / 10 전체보기 >] · 4초마다 넘어간다(멈춤 가능). 사진 있는 추천 레슨으로 채운다.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LessonBannerCarousel(lessons: List<HomeLesson>, onOpen: (HomeLesson) -> Unit, onAll: () -> Unit, modifier: Modifier = Modifier) {
    val items = lessons.filter { it.image != null }
    if (items.isEmpty()) return
    val pager = rememberPagerState { items.size }
    var playing by rememberSaveable { mutableStateOf(true) }
    LaunchedEffect(playing, pager.currentPage, items.size) {
        if (!playing || items.size < 2) return@LaunchedEffect
        delay(AUTO_MS)
        pager.animateScrollToPage((pager.currentPage + 1) % items.size)
    }
    Box(modifier.fillMaxWidth().aspectRatio(4f / 3f)) {
        HorizontalPager(pager, Modifier.fillMaxSize()) { i ->
            val l = items[i]
            Box(Modifier.fillMaxSize().clickable { onOpen(l) }) {
                RemoteImage(l.image, Modifier.fillMaxSize())
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.45f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.55f))))
                Column(Modifier.align(Alignment.BottomStart).padding(start = 22.dp, end = 22.dp, bottom = 64.dp)) {
                    Text(l.title, style = AppTypography.Title20.copy(color = Color.White), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    l.place?.let { Text(it, style = AppTypography.Body14.copy(color = Color.White.copy(alpha = 0.85f)), modifier = Modifier.padding(top = 6.dp), maxLines = 1) }
                }
            }
        }
        Row(Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(34.dp).clip(CircleShape).background(PILL).clickable { playing = !playing }, Alignment.Center) {
                Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    stringResource(if (playing) R.string.home_banner_pause else R.string.home_banner_play), tint = Color.White, modifier = Modifier.size(18.dp))
            }
            Row(Modifier.clip(RoundedCornerShape(50)).background(PILL).clickable(onClick = onAll).padding(horizontal = 14.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.home_banner_counter, pager.currentPage + 1, items.size), style = AppTypography.Caption12.copy(color = Color.White))
                Text(stringResource(R.string.home_view_all_arrow), style = AppTypography.Caption12.copy(color = Color.White), modifier = Modifier.padding(start = 6.dp))
            }
        }
    }
}

private val PILL = Color.Black.copy(alpha = 0.35f)
