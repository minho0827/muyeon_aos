package com.muyeon.app.ui.home.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.muyeon.app.R
import com.muyeon.app.ui.components.ActionSheet
import com.muyeon.app.ui.components.SheetAction

/** "이 강사 추천 안 받기" 시트 — 웹 components/muyeon/RecommendBlockSheet.js. 제목 = 강사 이름, [취소][추천 안 받기]. */
@Composable
fun RecommendBlockSheet(name: String?, busy: Boolean, onBlock: () -> Unit, onDismiss: () -> Unit) {
    ActionSheet(
        title = name ?: stringResource(R.string.home_rec_this_teacher),
        actions = listOf(
            SheetAction(stringResource(R.string.common_cancel), false, onDismiss),
            SheetAction(stringResource(if (busy) R.string.home_rec_busy else R.string.home_rec_block), true, onBlock),
        ),
        onDismiss = onDismiss,
        description = stringResource(R.string.home_rec_desc),
    )
}
