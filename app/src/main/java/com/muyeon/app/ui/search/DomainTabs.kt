package com.muyeon.app.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography

/** 밑줄 탭 [공연] [캐스팅] — 칸을 똑같이 나눈다. 고른 칸은 굵은 검정 + 검정 밑줄. */
@Composable
fun DomainTabs(domains: List<SearchDomain<*>>, selected: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().height(46.dp)) {
            domains.forEach { d ->
                val on = d.key == selected
                Box(
                    Modifier.weight(1f).height(46.dp).clickable { onSelect(d.key) }
                        .semantics { this.selected = on }.testTag("search.tab.${d.key}"),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        stringResource(d.titleRes),
                        style = AppTypography.Body15.copy(fontWeight = if (on) FontWeight.Bold else FontWeight.Medium),
                        color = if (on) AppColors.TextHead else AppColors.TextHint,
                    )
                    if (on) Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 16.dp).height(2.dp).background(AppColors.TextHead))
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(AppColors.Divider))
    }
}
