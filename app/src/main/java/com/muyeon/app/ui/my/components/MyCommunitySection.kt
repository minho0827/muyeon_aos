package com.muyeon.app.ui.my.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.muyeon.app.R
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.CategoryColors
import com.muyeon.app.ui.theme.Dimensions

/**
 * ★ 2026-11-08 MY 안 커뮤니티 섹션 — 커뮤니티는 탭이 아니라 여기서 연다(사용자 결정).
 *   [말풍선] 취미무용 커뮤니티 / 무용인들과 이야기 나누기 · [바로가기] — 웹 홈 커뮤니티 칸과 같은 문구.
 */
@Composable
fun MyCommunitySection(onOpen: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(horizontal = Dimensions.gutter, vertical = Dimensions.spacingM)) {
        Text(stringResource(R.string.tab_community), style = AppTypography.Section16, color = AppColors.TextHead, modifier = Modifier.padding(bottom = 10.dp))
        Row(
            Modifier.fillMaxWidth().background(AppColors.SurfaceMuted, RoundedCornerShape(Dimensions.cardRadius)).clickable(onClick = onOpen)
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(R.drawable.ic_web_community_filled), null, tint = CategoryColors.CommunityIcon, modifier = Modifier.size(28.dp))
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(stringResource(R.string.home_community_tile_t), style = AppTypography.CardTitle16, color = AppColors.TextHead)
                Text(stringResource(R.string.home_community_tile_d), style = AppTypography.Body14, color = AppColors.TextSub, modifier = Modifier.padding(top = 2.dp))
            }
            Text(stringResource(R.string.home_go), style = AppTypography.Body14SemiBold, color = AppColors.Primary,
                modifier = Modifier.background(AppColors.PrimarySoft, RoundedCornerShape(50)).padding(horizontal = 14.dp, vertical = 7.dp))
        }
    }
}
