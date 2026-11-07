package com.muyeon.app.ui.my.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.muyeon.app.ui.theme.Dimensions

/**
 * ★ 2026-11-08 MY 대표 지역 줄 — 홈 위 바에서 옮겼다(사용자 결정). [📍 대표 지역   서울 강남구 >] → 지역 고르기(웹 /myRegions).
 *   고른 지역은 홈 강사·레슨의 '가까운 순' 정렬에 쓰인다.
 */
@Composable
fun MyRegionRow(regionName: String?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = Dimensions.gutter, vertical = Dimensions.spacingS)
            .background(AppColors.SurfaceMuted, RoundedCornerShape(Dimensions.cardRadius)).clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(R.drawable.ic_web_location), null, tint = AppColors.TextHead, modifier = Modifier.size(22.dp))
        Text(stringResource(R.string.my_region), style = AppTypography.CardTitle16, color = AppColors.TextHead, modifier = Modifier.padding(start = 10.dp).weight(1f))
        Text(regionName ?: stringResource(R.string.home_region_pick), style = AppTypography.Body14SemiBold,
            color = if (regionName != null) AppColors.TextBody else AppColors.TextHint)
        Icon(painterResource(R.drawable.ic_web_arrow_right_bold), null, tint = AppColors.TextHint, modifier = Modifier.padding(start = 4.dp).size(18.dp))
    }
}
