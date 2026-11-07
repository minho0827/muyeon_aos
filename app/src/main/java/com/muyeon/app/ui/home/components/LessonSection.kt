package com.muyeon.app.ui.home.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.muyeon.app.R
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.domain.model.home.HomeLesson

/**
 * 뷰티컬리 섹션 — 제목(굵게 20) + 설명(회색) + 오른쪽 '전체보기 >'(주황), 아래 가로로 넘기는 상품 카드(폭 150).
 *   비면 그리지 않는다.
 */
@Composable
fun LessonSection(
    title: String,
    desc: String?,
    lessons: List<HomeLesson>,
    tag: String?,
    liked: Set<Long>,
    onOpen: (HomeLesson) -> Unit,
    onToggleLike: (Long) -> Unit,
    onAll: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    if (lessons.isEmpty()) return
    Column(modifier.fillMaxWidth().padding(top = 28.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(title, style = AppTypography.Title20, color = AppColors.TextHead)
                desc?.let { Text(it, style = AppTypography.Body14, color = AppColors.TextHint, modifier = Modifier.padding(top = 4.dp)) }
            }
            onAll?.let {
                Text(stringResource(R.string.home_view_all_arrow), style = AppTypography.Body14SemiBold, color = AppColors.Primary,
                    modifier = Modifier.clickable(onClick = it).padding(start = 12.dp, top = 4.dp))
            }
        }
        LazyRow(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(lessons, key = { it.id }) { l ->
                LessonProductCard(l, tag, l.id in liked, onClick = { onOpen(l) }, onToggleLike = { onToggleLike(l.id) }, modifier = Modifier.width(150.dp))
            }
        }
    }
}
