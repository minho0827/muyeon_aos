package com.muyeon.app.ui.castingstage.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.muyeon.app.R
import com.muyeon.app.ui.common.market.MarketBarDelete

/**
 * ★ 2026-11-10 캐스팅 공고 상세 — 작성자 하단 [삭제] [수정] [마감하기]. 수정은 등록 흐름 수정 모드(CastingEditFlow).
 *  지원 버튼 자리에 대신 놓는다. 마감은 열린 공고일 때만.
 */
@Composable
fun StageCastingOwnerBar(open: Boolean, busy: Boolean, onDelete: () -> Unit, onClose: () -> Unit, onEdit: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MarketBarDelete(onDelete)
        com.muyeon.app.ui.common.market.MarketBarSecondary(stringResource(R.string.owner_edit), onEdit)
        Box(Modifier.weight(1f)) { StageBottomButton(stringResource(R.string.posting_owner_close), enabled = open, busy = busy, onClick = onClose) }
    }
}
