package com.muyeon.app.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.R
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.Dimensions

/**
 * 홈 위 바 — 웹 Header(isMain): 주황 · 왼쪽 [위치 아이콘 · 대표 지역 이름 · >](지역 고르기) · 오른쪽 종(안 읽은 알림 수 · 99+).
 */
@Composable
fun HomeTopBar(regionName: String?, unread: Int, onRegion: () -> Unit, onBell: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().background(AppColors.Primary).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().height(Dimensions.topBarHeight).padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.clickable(onClick = onRegion), verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(R.drawable.ic_web_location), null, tint = AppColors.TextOnPrimary, modifier = Modifier.size(24.dp))
                Text(regionName ?: stringResource(R.string.home_region_pick), style = AppTypography.Title18, color = AppColors.TextOnPrimary, modifier = Modifier.padding(start = 4.dp))
                Icon(painterResource(R.drawable.ic_web_arrow_right_bold), null, tint = AppColors.TextOnPrimary, modifier = Modifier.size(24.dp).rotate(90f).offset(x = (-3).dp))
            }
            Spacer(Modifier.weight(1f))
            Box(Modifier.size(32.dp).clickable(onClick = onBell), contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_web_bell), stringResource(R.string.home_notifications), tint = AppColors.TextOnPrimary, modifier = Modifier.size(32.dp))
                if (unread > 0) {
                    Text(
                        if (unread > 99) "99+" else unread.toString(),
                        style = AppTypography.Tiny10.copy(fontSize = 10.sp), color = AppColors.TextOnPrimary, textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.TopEnd).offset(x = 2.dp, y = 2.dp).heightIn(min = 16.dp).widthIn(min = 16.dp)
                            .background(AppColors.Danger, CircleShape).padding(horizontal = 4.dp, vertical = 2.dp),
                    )
                }
            }
        }
    }
}
