package com.muyeon.app.ui.region.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.Dimensions

/** 지역 추가 한 줄 — 웹 AddRegion .resturant-card.none-border(이름 · 아래 작은 글, 줄 사이 선). */
@Composable
fun AddRegionRow(title: String, sub: String, tag: String, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).testTag(tag)) {
        Column(Modifier.padding(horizontal = Dimensions.gutter, vertical = 14.dp)) {
            Text(title, style = AppTypography.CardTitle16, color = AppColors.TextHead)
            Text(sub, style = AppTypography.Sub13, color = AppColors.TextHint, modifier = Modifier.padding(top = 2.dp))
        }
        HorizontalDivider(Modifier.padding(horizontal = Dimensions.gutter), thickness = 1.dp, color = AppColors.Divider)
    }
}
