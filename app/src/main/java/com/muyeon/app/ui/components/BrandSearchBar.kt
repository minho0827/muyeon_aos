package com.muyeon.app.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.muyeon.app.R
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography

/**
 * ★ 2026-11-09 흰 위 바 + 검색창(NOL 홈 모양) — 왼쪽 DANCE 로고([BrandTopBar] 그대로) · 오른쪽 둥근 테두리 검색창.
 *   검색창은 입력칸이 아니라 버튼 — 누르면 검색 화면([onClick]).
 */
@Composable
fun BrandSearchBar(onClick: () -> Unit, modifier: Modifier = Modifier, hint: String = stringResource(R.string.search_bar_hint)) {
    BrandTopBar(modifier = modifier, center = {
        Row(
            Modifier.fillMaxWidth().padding(start = 14.dp, end = 4.dp).height(40.dp)
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, AppColors.ChipBorder, RoundedCornerShape(20.dp))
                .clickable(onClick = onClick)
                .semantics { role = Role.Button }
                .padding(start = 16.dp, end = 12.dp)
                .testTag("brandSearchBar"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(hint, style = AppTypography.Body14, color = AppColors.TextHint, maxLines = 1, modifier = Modifier.weight(1f))
            Spacer(Modifier.size(8.dp))
            Icon(Icons.Filled.Search, null, tint = AppColors.TextHead, modifier = Modifier.size(20.dp))
        }
    })
}
