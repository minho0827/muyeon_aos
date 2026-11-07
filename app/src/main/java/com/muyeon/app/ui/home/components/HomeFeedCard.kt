package com.muyeon.app.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.muyeon.app.R
import com.muyeon.app.ui.components.CardTags
import com.muyeon.app.ui.components.HeartButton
import com.muyeon.app.ui.components.RemoteImage
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.domain.model.home.HomeFeedItem

/**
 * 클래식 홈 피드 카드 — 웹 HomeClassic .card.menu-card: 사진(그리드 79:64 · 목록 2:1) · 하트(채용·캐스팅·공간) ·
 *   왼쪽 위 칩(영역 이름 또는 지원함/열람함, 60% 검정) · 제목 · 보조 두 줄 · "정보1 | 정보2" + 상태 · 태그.
 */
@Composable
fun HomeFeedCard(
    item: HomeFeedItem,
    grid: Boolean,
    chip: String?,
    tags: List<String>,
    liked: Boolean?,
    onClick: () -> Unit,
    onToggleLike: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(16.dp)
    Column(modifier.fillMaxWidth().clip(shape).border(1.dp, AppColors.Divider, shape).clickable(onClick = onClick)) {
        Box {
            RemoteImage(item.image, Modifier.fillMaxWidth().aspectRatio(if (grid) 79f / 64f else 2f), fallbackRes = R.drawable.img_noimage, thumbnail = true)
            if (liked != null) HeartButton(liked, onToggleLike, size = 20.dp,
                modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).shadow(4.dp, CircleShape).background(AppColors.OverlayWhite, CircleShape))
            chip?.let {
                Text(it, style = AppTypography.Caption12, color = Color.White,
                    modifier = Modifier.align(Alignment.TopStart).padding(12.dp).background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 2.dp))
            }
        }
        Column(Modifier.padding(start = 20.dp, end = 16.dp, top = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(item.title, style = AppTypography.CardTitle16, color = AppColors.TextHead, maxLines = 1, overflow = TextOverflow.Ellipsis)
            item.sub?.takeIf { it.isNotBlank() }?.let { Text(it, style = AppTypography.Sub13, color = AppColors.TextSub, maxLines = 2, overflow = TextOverflow.Ellipsis) }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val info = item.info.filter { it.isNotBlank() }
                if (info.isNotEmpty()) Text(info.joinToString("  |  "), style = AppTypography.Body14.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium), color = AppColors.TextHint,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                item.status?.let { Text(it, style = AppTypography.Body14SemiBold, color = statusColor(item.statusColor)) }
            }
            CardTags(tags)
        }
    }
}

/** 상태 글자 색(웹 .wating-time.red|green|orange). */
private fun statusColor(c: String?): Color = when (c) {
    "red" -> AppColors.Danger
    "green" -> AppColors.Positive
    "orange" -> AppColors.Primary
    else -> AppColors.TextSub
}
