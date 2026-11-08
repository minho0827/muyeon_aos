package com.muyeon.app.ui.home.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.muyeon.app.R
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography

/**
 * 홈 [레슨] 섹션 머리 — 제목(굵게 20) + 설명(회색) + 오른쪽 '전체보기 ›'(주황).
 *   레슨 섹션([LessonSection])과 '레슨을 찾는 회원들'(QuoteFeedSection)이 같이 쓴다.
 */
@Composable
fun HomeSectionHeader(title: String, desc: String?, onAll: (() -> Unit)?, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            Text(title, style = AppTypography.Title20, color = AppColors.TextHead)
            desc?.let { Text(it, style = AppTypography.Body14, color = AppColors.TextHint, modifier = Modifier.padding(top = 4.dp)) }
        }
        onAll?.let {
            Text(stringResource(R.string.home_view_all_arrow), style = AppTypography.Body14SemiBold, color = AppColors.Primary,
                modifier = Modifier.clickable(onClick = it).padding(start = 12.dp, top = 4.dp))
        }
    }
}
