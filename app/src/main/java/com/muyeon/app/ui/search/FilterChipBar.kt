package com.muyeon.app.ui.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.muyeon.app.R
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.Dimensions
import com.muyeon.domain.model.search.SearchFilter

/**
 * 결과 위 칩 줄 — [장르▾][지역▾](그 섹션만 든 시트) · [필터](전체 시트, 고른 게 있으면 주황 점) · [정렬▾].
 *   칩 모양은 목록 ChipRow 와 같은 높이·테두리(고른 칩은 주황 테두리·글자).
 */
@Composable
fun FilterChipBar(
    domain: SearchDomain<*>,
    filter: SearchFilter,
    sort: String,
    onSection: (String) -> Unit,
    onAll: () -> Unit,
    onSort: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val quick = domain.quickChips.mapNotNull { domain.section(it) }
    val sortLabel = domain.sortOptions.firstOrNull { it.key == sort }?.labelRes?.let { stringResource(it) } ?: stringResource(R.string.search_sort)
    LazyRow(
        modifier.fillMaxWidth().padding(vertical = 10.dp),
        contentPadding = PaddingValues(horizontal = Dimensions.gutter),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(quick, key = { "quick.${it.key}" }) { s ->
            val picked = FilterChipState.picked(filter, s)
            val label = when {
                picked == null -> stringResource(s.titleRes)
                picked.second == 0 -> picked.first.text()
                else -> stringResource(R.string.search_chip_more, picked.first.text(), picked.second)
            }
            SearchChip(label, active = picked != null, trailing = Icons.Filled.KeyboardArrowDown, tag = "search.chip.${s.key}") { onSection(s.key) }
        }
        item(key = "all") {
            SearchChip(stringResource(R.string.search_filter), active = false, leading = Icons.Filled.Tune, dot = FilterChipState.dot(filter), tag = "search.chip.filter", onClick = onAll)
        }
        item(key = "sort") {
            SearchChip(sortLabel, active = false, leading = Icons.Filled.SwapVert, tag = "search.chip.sort", onClick = onSort)
        }
    }
}

@Composable
private fun SearchChip(
    text: String,
    active: Boolean,
    tag: String,
    leading: ImageVector? = null,
    trailing: ImageVector? = null,
    dot: Boolean = false,
    onClick: () -> Unit,
) {
    val tint = if (active) AppColors.Primary else AppColors.ChipText
    Box {
        Row(
            Modifier.height(Dimensions.chipHeight).clip(CircleShape)
                .border(BorderStroke(1.dp, if (active) AppColors.Primary else AppColors.ChipBorder), CircleShape)
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp)
                .testTag(tag),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            leading?.let { Icon(it, null, tint = tint, modifier = Modifier.size(16.dp)) }
            Text(text, style = if (active) AppTypography.Chip14Selected else AppTypography.Chip14, color = tint, maxLines = 1)
            trailing?.let { Icon(it, null, tint = tint, modifier = Modifier.size(18.dp)) }
        }
        if (dot) Box(Modifier.align(Alignment.TopEnd).offset(x = (-2).dp, y = 2.dp).size(7.dp).clip(CircleShape).background(AppColors.Primary).testTag("search.chip.filter.dot"))
    }
}
