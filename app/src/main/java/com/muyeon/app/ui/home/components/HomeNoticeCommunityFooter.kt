package com.muyeon.app.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.muyeon.app.R
import com.muyeon.app.ui.format.KstDates
import com.muyeon.app.ui.home.HomeLabels
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.CategoryColors
import com.muyeon.domain.model.home.HomeNotice
import com.muyeon.domain.model.home.HomeRecruit

/**
 * 홈 맨 아래 — 웹 components/muyeon/HomeNoticeCommunityFooter.js:
 *   커뮤니티(취미무용 커뮤니티·FAQ 타일) · 대타공고(최신 5) · 공지사항(최신 3) · 약관·사업자 정보.
 */
@Composable
fun HomeNoticeCommunityFooter(
    subs: List<HomeRecruit>,
    notices: List<HomeNotice>,
    onOpenPath: (String) -> Unit,
    onOpenCommunity: () -> Unit,
    onDial: (String) -> Unit,
    onMail: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 48.dp), verticalArrangement = Arrangement.spacedBy(32.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                HomeBottomTitle(stringResource(R.string.tab_community), R.drawable.ic_web_community_filled, CategoryColors.CommunityIcon, onOpenCommunity)
                CommunityTile(stringResource(R.string.home_community_tile_t), stringResource(R.string.home_community_tile_d), stringResource(R.string.home_go), onOpenCommunity)
                CommunityTile(stringResource(R.string.my_menu_faq), stringResource(R.string.home_faq_tile_d), stringResource(R.string.home_detail), { onOpenPath("/faq") })
            }
            HomeNoticeList(
                title = stringResource(R.string.tab_subs), iconRes = R.drawable.ic_web_speaker, iconTint = AppColors.Primary,
                rows = subs.map { r -> HomeNoticeRow(r.id, r.title ?: stringResource(R.string.home_sub_default), KstDates.dot(r.classDate ?: r.createdAt), HomeLabels.isRecent(r.createdAt)) { onOpenPath("/subs/${r.id}") } },
                onMore = { onOpenPath("/subs") },
            )
            HomeNoticeList(
                title = stringResource(R.string.my_menu_notice), iconRes = R.drawable.ic_web_speaker, iconTint = AppColors.Primary,
                rows = notices.map { n -> HomeNoticeRow(n.id, n.title, KstDates.dot(n.createdAt), KstDates.isToday(n.createdAt)) { onOpenPath("/notice") } },
                onMore = { onOpenPath("/notice") },
            )
        }
        HomeLegalFooter(onOpenPath, onDial, onMail)
    }
}

/** 회색 타일 — 굵은 제목 · 설명 · 오른쪽 주황 알약(웹 .community-item). */
@Composable
private fun CommunityTile(title: String, desc: String, tag: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(AppColors.SurfaceMuted, RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = AppTypography.Section16, color = AppColors.TextHead)
            Text(desc, style = AppTypography.Body14.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium), color = AppColors.TextHead)
        }
        Text(tag, style = AppTypography.Body14.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium), color = AppColors.Primary,
            modifier = Modifier.background(AppColors.PrimarySoft, CircleShape).padding(horizontal = 12.dp, vertical = 2.dp))
    }
}
