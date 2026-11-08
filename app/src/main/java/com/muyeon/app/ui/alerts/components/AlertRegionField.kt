package com.muyeon.app.ui.alerts.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.muyeon.app.R
import com.muyeon.app.ui.quote.RegionPickRow
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.Dimensions

/**
 * 관심 지역 한 칸 — 웹 RegionSelect(시/도 → 시군구). 앱은 기존 지역 고르기 시트(RegionPickRow — 'OO 전체' 포함)를 쓴다.
 *   웹은 '시/도 선택'으로 되돌려 비울 수 있다 — 고른 뒤에는 [선택 해제] 로 비운다.
 */
@Composable
fun AlertRegionField(label: String, name: String, token: String?, onPick: (String, String?) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimensions.spacingS)) {
        Text(label, style = AppTypography.Body14SemiBold, color = AppColors.TextHead)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                RegionPickRow(token = token, placeholder = stringResource(R.string.alerts_region_pick), name = name, allowSidoAll = true, onPick = onPick)
            }
            if (name.isNotEmpty()) TextButton(onClick = { onPick("", null) }, modifier = Modifier.padding(start = Dimensions.spacingXS)) {
                Text(stringResource(R.string.alerts_region_clear), style = AppTypography.Sub13, color = AppColors.TextSub)
            }
        }
    }
}
