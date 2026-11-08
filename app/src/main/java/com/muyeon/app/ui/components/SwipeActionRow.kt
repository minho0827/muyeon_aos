package com.muyeon.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.Dimensions

/**
 * 왼쪽으로 밀면 빨간 [label] 이 드러나고, 끝까지 밀면 [onAction](줄은 제자리로 돌아온다) — 웹 ListRow swipeAction 과 같은 동작.
 *   지우기 전 확인이 필요한 화면은 [onAction] 에서 확인 팝업을 띄운다. [enabled]=false 면 밀리지 않는다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeActionRow(label: String, onAction: () -> Unit, enabled: Boolean = true, content: @Composable () -> Unit) {
    val latest = rememberUpdatedState(onAction)
    val state = rememberSwipeToDismissBoxState(confirmValueChange = { v ->
        if (v == SwipeToDismissBoxValue.EndToStart) latest.value()
        false
    })
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = enabled,
        backgroundContent = {
            Box(Modifier.fillMaxSize().background(AppColors.Danger).padding(horizontal = Dimensions.gutter), contentAlignment = Alignment.CenterEnd) {
                Text(label, style = AppTypography.Body14SemiBold, color = AppColors.TextOnPrimary)
            }
        },
    ) {
        Box(Modifier.background(AppColors.Surface)) { content() }
    }
}
