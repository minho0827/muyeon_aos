package com.muyeon.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.muyeon.app.R
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.Dimensions

/**
 * 목록 한 줄 — 웹 ListPage `ListRow`(#notice .notice-list .list-item).
 *   [썸네일 80×104 · 모서리 8] [배지 줄 · 제목 16 semibold 한 줄 · 설명 13 회색 한 줄] [끝 자리(기본 화살표)]
 *   줄 사이 1px #EAEAEA, 위아래 16, 칸 사이 16. [showDivider]=false 면 마지막 줄.
 *
 * @param thumbUrl 썸네일 주소(없으면 썸네일 칸 없음). [thumbFallbackRes] 는 주소가 비었을 때 그림.
 * @param tags 배지 줄(LevelBadge 들). null 이면 없음.
 * @param trailing 끝 자리. null 이면 기본 화살표(웹 i.arrow).
 */
@Composable
fun ListRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    sub: String? = null,
    thumbUrl: String? = null,
    @DrawableRes thumbFallbackRes: Int? = null,
    tags: (@Composable RowScope.() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    showDivider: Boolean = true,
) {
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = Dimensions.gutter, vertical = Dimensions.spacingL),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimensions.spacingL),
        ) {
            if (thumbUrl != null || thumbFallbackRes != null) {
                RemoteImage(
                    url = thumbUrl,
                    fallbackRes = thumbFallbackRes,
                    thumbnail = true,
                    modifier = Modifier.size(THUMB_W, THUMB_H).clip(RoundedCornerShape(8.dp)),
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Dimensions.spacingXS)) {
                if (tags != null) Row(horizontalArrangement = Arrangement.spacedBy(Dimensions.spacingXS), content = tags)
                Text(title, style = AppTypography.CardTitle16, color = AppColors.TextHead, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (!sub.isNullOrBlank()) Text(sub, style = AppTypography.Sub13, color = AppColors.TextHint, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (trailing != null) trailing()
            else Icon(painterResource(R.drawable.ic_web_arrow), contentDescription = null, tint = AppColors.TextHead, modifier = Modifier.size(18.dp).rotate(-90f))
        }
        if (showDivider) HorizontalDivider(Modifier.padding(horizontal = Dimensions.gutter), thickness = 1.dp, color = AppColors.Divider)
    }
}

private val THUMB_W = 80.dp
private val THUMB_H = 104.dp
