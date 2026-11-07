package com.muyeon.app.ui.home.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.muyeon.app.R
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography

/** 공지형 목록 한 줄. */
data class HomeNoticeRow(val key: Any, val title: String, val date: String, val isNew: Boolean, val onClick: () -> Unit)

/**
 * 홈 공지형 목록 — 웹 .home-bottom .notice: 제목 줄(아이콘 + 제목 16 bold · 더보기) + 테두리 상자 안 줄들(제목 16 medium · 새 글 N · 날짜 12).
 *   공지사항·대타공고·강사 채용정보가 같은 모양을 쓴다(웹 HomeRecruitNotice 도 같은 마크업).
 */
@Composable
fun HomeNoticeList(title: String, iconRes: Int, iconTint: Color, rows: List<HomeNoticeRow>, onMore: () -> Unit, modifier: Modifier = Modifier) {
    if (rows.isEmpty()) return
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HomeBottomTitle(title, iconRes, iconTint, onMore)
        Column(Modifier.fillMaxWidth().border(1.dp, AppColors.Divider, RoundedCornerShape(16.dp)).padding(horizontal = 20.dp)) {
            rows.forEachIndexed { i, r ->
                Row(Modifier.fillMaxWidth().clickable(onClick = r.onClick).padding(vertical = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Text(r.title, style = AppTypography.Body15Medium.copy(fontSize = androidx.compose.ui.unit.TextUnit(16f, androidx.compose.ui.unit.TextUnitType.Sp)), color = AppColors.TextHead,
                            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                        if (r.isNew) Icon(painterResource(R.drawable.ic_web_new), stringResource(R.string.home_new), tint = AppColors.Danger, modifier = Modifier.padding(start = 4.dp).size(16.dp))
                    }
                    Text(r.date, style = AppTypography.Caption12, color = AppColors.TextSub, modifier = Modifier.padding(start = 8.dp))
                }
                if (i < rows.lastIndex) HorizontalDivider(thickness = 1.dp, color = AppColors.Divider)
            }
        }
    }
}

/** 홈 하단 블록 제목 줄 — 웹 .tit-box(아이콘 + 16 bold · 오른쪽 "더보기 ⌄"). 커뮤니티·공지가 같이 쓴다. */
@Composable
fun HomeBottomTitle(title: String, iconRes: Int, iconTint: Color, onMore: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(painterResource(iconRes), null, tint = iconTint, modifier = Modifier.size(20.dp))
        Text(title, style = AppTypography.Section16, color = AppColors.TextHead, modifier = Modifier.padding(start = 4.dp).weight(1f))
        Row(Modifier.clickable(onClick = onMore), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.home_more), style = AppTypography.More14, color = AppColors.TextSub)
            Icon(painterResource(R.drawable.ic_web_arrow_right_bold), null, tint = AppColors.TextSub, modifier = Modifier.size(16.dp))
        }
    }
}
