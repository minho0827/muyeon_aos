package com.muyeon.app.ui.performance.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.muyeon.app.R
import com.muyeon.app.ui.components.ReadField
import com.muyeon.app.ui.components.ReadSectionTitle
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.Dimensions
import com.muyeon.domain.model.performance.PerformanceDetail

/** 웹에서 모은 공연(details.source=WEB) — 출처·수집일·원문 링크 + 안내(웹과 같은 말). */
@Composable
fun PerformanceSourceSection(detail: PerformanceDetail, onOpenUrl: (String) -> Unit, modifier: Modifier = Modifier) {
    if (detail.source != "WEB") return
    Column(modifier.fillMaxWidth().padding(horizontal = Dimensions.gutter), verticalArrangement = Arrangement.spacedBy(Dimensions.spacingL)) {
        ReadSectionTitle(stringResource(R.string.performance_detail_sec_source))
        ReadField(stringResource(R.string.performance_detail_source_name), detail.sourceName)
        ReadField(stringResource(R.string.performance_detail_collected_at), detail.collectedAt)
        detail.sourceUrl?.let { url ->
            ReadField(stringResource(R.string.performance_detail_source_url), null, linkText = stringResource(R.string.performance_detail_source_open), onLink = { onOpenUrl(url) })
        }
        Text(stringResource(R.string.performance_detail_source_notice), style = AppTypography.Sub13, color = AppColors.TextSub)
    }
}
