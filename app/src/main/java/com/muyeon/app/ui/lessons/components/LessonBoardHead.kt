package com.muyeon.app.ui.lessons.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.muyeon.app.R
import com.muyeon.app.ui.theme.AppColors

/**
 * 보기 전환 + 필터 줄 — 웹 .job-board-head: 왼쪽 [그리드][목록](고른 쪽 회색 바탕 32), 오른쪽 필터(적용 중이면 주황 점).
 */
@Composable
fun LessonBoardHead(grid: Boolean, filterOn: Boolean, onGrid: (Boolean) -> Unit, onFilter: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 8.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        com.muyeon.app.ui.components.ViewModeToggle(grid, onGrid)
        Spacer(Modifier.weight(1f))
        Box(Modifier.size(40.dp).clickable(onClick = onFilter), contentAlignment = Alignment.Center) {
            Icon(painterResource(R.drawable.ic_web_filter), contentDescription = stringResource(R.string.lesson_filter), tint = AppColors.TextSub, modifier = Modifier.size(24.dp))
            if (filterOn) Box(Modifier.align(Alignment.TopEnd).padding(5.dp).size(7.dp).background(AppColors.Primary, CircleShape))
        }
    }
}

