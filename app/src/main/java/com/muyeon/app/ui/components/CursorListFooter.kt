package com.muyeon.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.muyeon.app.R
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.Dimensions

/**
 * 목록 끝 — 웹 components/muyeon/CursorListFooter.js. 스크롤로 저절로 부르고([CursorLoadMoreEffect]),
 *   실패·멈춤이면 문구 + [다시 시도]/[더 보기] 를 보인다. 끝이면 아무것도 그리지 않는다.
 */
@Composable
fun CursorListFooter(
    loading: Boolean,
    error: String?,
    hasMore: Boolean,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!hasMore && error == null) return
    Column(modifier.fillMaxWidth().padding(horizontal = Dimensions.gutter, vertical = Dimensions.spacingS), horizontalAlignment = Alignment.CenterHorizontally) {
        if (error != null) {
            Text(error, style = AppTypography.Body14, color = AppColors.TextSub, textAlign = TextAlign.Center, modifier = Modifier.padding(bottom = Dimensions.spacingM))
        }
        OutlinedButton(
            onClick = onLoadMore,
            enabled = !loading,
            shape = RoundedCornerShape(Dimensions.spacingS),
            border = BorderStroke(1.dp, AppColors.ChipBorder),
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) {
            Text(
                stringResource(
                    when {
                        loading -> R.string.common_loading
                        error != null -> R.string.common_retry
                        else -> R.string.common_more
                    },
                ),
                style = AppTypography.Body15Medium,
                color = AppColors.TextBody,
            )
        }
    }
}
