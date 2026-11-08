package com.muyeon.app.ui.academyedit.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import com.muyeon.app.ui.space.SpaceFacility
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.Dimensions

/** 고른 시설 아이콘 4열 — 웹 .space-facility-grid(공간 상세 SpaceFacilitySection 과 같은 아이콘·이름, SpaceFacility.ALL 순서). */
@Composable
fun AcademyFacilityGrid(keys: List<String>) {
    val items = SpaceFacility.ALL.filter { it.key in keys }
    Column(verticalArrangement = Arrangement.spacedBy(Dimensions.spacingM)) {
        items.chunked(COLUMNS).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(Dimensions.spacingS)) {
                row.forEach { f ->
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(Dimensions.spacingS)).background(AppColors.SurfaceMuted)
                            .padding(vertical = Dimensions.spacingM),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Dimensions.spacingXS),
                    ) {
                        Icon(f.icon, null, tint = AppColors.Primary, modifier = Modifier.size(Dimensions.iconM))
                        Text(f.label, style = AppTypography.Caption12, color = AppColors.TextBody, textAlign = TextAlign.Center, maxLines = 2)
                    }
                }
                repeat(COLUMNS - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

private const val COLUMNS = 4
