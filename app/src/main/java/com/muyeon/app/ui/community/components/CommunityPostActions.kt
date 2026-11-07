package com.muyeon.app.ui.community.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.muyeon.app.R
import com.muyeon.app.ui.components.SheetAction
import com.muyeon.app.ui.components.SheetButton
import com.muyeon.app.ui.theme.Dimensions

/** 글 아래 버튼 줄 — 웹 btn-group: [♥ 좋아요 N](눌렀으면 주황) [스크랩/스크랩됨](스크랩됨 = 주황 채움). */
@Composable
fun CommunityPostActions(liked: Boolean, likeCount: Int, scrapped: Boolean, busy: Boolean, onLike: () -> Unit, onScrap: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(horizontal = Dimensions.gutter), horizontalArrangement = Arrangement.spacedBy(Dimensions.spacingS)) {
        SheetButton(SheetAction(stringResource(R.string.community_like, likeCount), primary = liked, onClick = onLike), Modifier.weight(1f), enabled = !busy)
        SheetButton(
            SheetAction(stringResource(if (scrapped) R.string.community_scrapped_label else R.string.community_scrap_label), primary = scrapped, onClick = onScrap),
            Modifier.weight(1f),
        )
    }
}
