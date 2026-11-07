package com.muyeon.app.ui.performance.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.muyeon.app.R
import com.muyeon.app.ui.components.ReadArea
import com.muyeon.app.ui.components.ReadField
import com.muyeon.app.ui.components.ReadSectionTitle
import com.muyeon.app.ui.format.KstDates
import com.muyeon.app.ui.performance.PerformanceLabels
import com.muyeon.app.ui.theme.Dimensions
import com.muyeon.domain.model.performance.PerformanceDetail

/**
 * 상세 정보 칸 — 웹 PerformanceDetail form-list: 공연 정보 · 장소 · 예매 · 공연 소개. 빈 칸은 그리지 않는다.
 */
@Composable
fun PerformanceInfoSection(detail: PerformanceDetail, modifier: Modifier = Modifier) {
    val p = detail.summary
    val subName = PerformanceLabels.code(p.subGenre)?.let { stringResource(it) }
    Column(modifier.fillMaxWidth().padding(horizontal = Dimensions.gutter), verticalArrangement = Arrangement.spacedBy(Dimensions.spacingL)) {
        ReadSectionTitle(stringResource(R.string.performance_detail_sec_info))
        ReadField(stringResource(R.string.performance_detail_genre), listOfNotNull(p.genre, subName).joinToString(" · "))
        ReadField(stringResource(R.string.performance_detail_period), KstDates.dashPeriod(p.startAt, p.endAt))
        ReadField(stringResource(R.string.performance_detail_showtimes), detail.showtimes)
        ReadField(stringResource(R.string.performance_detail_runtime), detail.runtime)
        ReadField(stringResource(R.string.performance_detail_age), detail.ageLimit)

        ReadSectionTitle(stringResource(R.string.performance_detail_sec_place))
        ReadField(stringResource(R.string.performance_detail_venue), p.venue)
        ReadField(stringResource(R.string.performance_detail_address), detail.address)
        ReadField(stringResource(R.string.performance_detail_region), detail.region)

        ReadSectionTitle(stringResource(R.string.performance_detail_sec_booking))
        ReadField(stringResource(R.string.performance_detail_price), detail.priceInfo)
        ReadField(stringResource(R.string.performance_detail_cast), detail.cast)
        if (detail.source == SOURCE_KOPIS) ReadField(stringResource(R.string.performance_detail_source), stringResource(R.string.performance_detail_source_kopis))

        if (detail.description != null) {
            ReadSectionTitle(stringResource(R.string.performance_detail_sec_intro))
            ReadArea(detail.description)
        }
    }
}

private const val SOURCE_KOPIS = "KOPIS"
