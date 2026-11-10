package com.muyeon.app.ui.jobposting

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.muyeon.app.R
import com.muyeon.app.ui.theme.AppColors

/**
 * ★ 2026-11-10 공고 상세 — 작성자 하단 바 [삭제] [마감] [수정]. 지원자 하단 바([찜][지원하기]) 자리에 대신 놓는다.
 *  동작은 내 공고 관리와 같은 [PostingActions](마감 확인·삭제 확인·서버 문구 그대로).
 *  마감은 열린 공고(OPEN)일 때만 누를 수 있다.
 */
@Composable
fun PostingOwnerBar(open: Boolean, busy: Boolean, onDelete: () -> Unit, onClose: () -> Unit, onEdit: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(AppColors.Surface).navigationBarsPadding()) {
        HorizontalDivider(color = AppColors.Divider)
        Row(Modifier.padding(horizontal = 16.dp).padding(top = 8.dp, bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            JobButton(stringResource(R.string.owner_delete), filled = false, enabled = !busy, modifier = Modifier.weight(1f), onClick = onDelete)
            JobButton(stringResource(R.string.owner_close), filled = false, enabled = open && !busy, modifier = Modifier.weight(1f), onClick = onClose)
            JobButton(stringResource(R.string.owner_edit), filled = true, enabled = !busy, modifier = Modifier.weight(1f), onClick = onEdit)
        }
    }
}
