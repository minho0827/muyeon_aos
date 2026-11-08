package com.muyeon.app.ui.region.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.muyeon.app.R
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.Dimensions
import com.muyeon.domain.model.region.ActiveRegion

/** 관심 지역 카드 — 웹 MyRegions .list-item(편집 중 왼쪽 지우기) + .resturant-card(이름 · 단위 · 오른쪽 ★ 대표). */
@Composable
fun RegionCard(region: ActiveRegion, representative: Boolean, editMode: Boolean, onRemove: () -> Unit, onRepresent: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = Dimensions.gutter, vertical = Dimensions.spacingXS), verticalAlignment = Alignment.CenterVertically) {
        AnimatedVisibility(editMode) {
            IconButton(onClick = onRemove, modifier = Modifier.testTag("regions.remove.${region.code}")) {
                Icon(Icons.Filled.RemoveCircle, stringResource(R.string.regions_remove), tint = AppColors.Danger)
            }
        }
        Row(
            Modifier.weight(1f).border(1.dp, AppColors.Divider, RoundedCornerShape(Dimensions.cardRadius)).padding(start = 18.dp, top = 14.dp, bottom = 14.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(region.name, style = AppTypography.CardTitle16, color = AppColors.TextHead)
                Text(stringResource(if (region.kind == ActiveRegion.SIGUNGU) R.string.regions_kind_sigungu else R.string.regions_kind_sido),
                    style = AppTypography.Sub13, color = AppColors.TextHint, modifier = Modifier.padding(top = 2.dp))
            }
            IconButton(onClick = onRepresent, modifier = Modifier.testTag("regions.rep.${region.code}")) {
                Icon(Icons.Filled.Star, stringResource(R.string.regions_rep), tint = if (representative) AppColors.Primary else AppColors.IconOff, modifier = Modifier.size(24.dp))
            }
        }
    }
}
