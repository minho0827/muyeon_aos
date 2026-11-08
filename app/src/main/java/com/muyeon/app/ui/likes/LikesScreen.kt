package com.muyeon.app.ui.likes

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muyeon.app.R
import com.muyeon.app.ui.components.ChipOption
import com.muyeon.app.ui.components.ChipRow
import com.muyeon.app.ui.components.EmptyView
import com.muyeon.app.ui.components.HeartButton
import com.muyeon.app.ui.components.ListRow
import com.muyeon.app.ui.components.MainTopBar
import com.muyeon.app.ui.components.ViewModeToggle
import com.muyeon.app.ui.likes.components.LikedCard
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.Dimensions
import com.muyeon.domain.model.scrap.LikedItem
import org.koin.androidx.compose.koinViewModel

/**
 * 찜/관심 — 웹 /likes. 누르면 그 상세(앱 화면이 있으면 앱 — [onOpenPath] 가 NativeRoutes 로 연다).
 *   찜이 없으면 빈 화면에 [둘러보기](→ 홈).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LikesScreen(onBack: () -> Unit, onOpenPath: (String) -> Unit, onBrowse: () -> Unit, viewModel: LikesViewModel = koinViewModel()) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val seen = remember { java.util.concurrent.atomic.AtomicBoolean(false) }
    LifecycleResumeEffect(Unit) {
        // 상세에서 찜을 풀고 돌아왔을 수 있다 — 처음 한 번은 ViewModel 이 읽었다.
        if (seen.getAndSet(true)) viewModel.reload()
        onPauseOrDispose { }
    }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { e ->
            when (e) {
                LikesEvent.Removed -> Toast.makeText(context, R.string.common_scrap_removed, Toast.LENGTH_SHORT).show()
                is LikesEvent.Message -> Toast.makeText(context, e.text, Toast.LENGTH_SHORT).show()
            }
        }
    }
    val chips = viewModel.tabs.map { ChipOption(it, stringResource(LikesLabels.tab(it))) }
    val closed = stringResource(R.string.likes_closed)
    val deleted = stringResource(R.string.likes_deleted_item)
    val open: (LikedItem) -> Unit = { i -> LikesLabels.path(i)?.let(onOpenPath) }

    Column(Modifier.fillMaxSize().background(AppColors.Surface).navigationBarsPadding()) {
        MainTopBar(title = stringResource(R.string.likes_title), primary = false, onBack = onBack)
        PullToRefreshBox(s.isRefreshing, onRefresh = { viewModel.reload(pull = true) }, modifier = Modifier.fillMaxSize()) {
            LazyVerticalGrid(GridCells.Fixed(if (s.grid) 2 else 1), Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = Dimensions.spacing3XL)) {
                item(key = "chips", span = { GridItemSpan(maxLineSpan) }) {
                    ChipRow(chips, s.tab, viewModel::selectTab, Modifier.padding(top = Dimensions.spacingM))
                }
                item(key = "toggle", span = { GridItemSpan(maxLineSpan) }) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = Dimensions.gutter, vertical = Dimensions.spacingM), horizontalArrangement = Arrangement.End) {
                        ViewModeToggle(s.grid, viewModel::setGrid)
                    }
                }
                when {
                    s.isLoading -> Unit
                    s.errorMessage != null && s.items.isEmpty() -> item(key = "error", span = { GridItemSpan(maxLineSpan) }) {
                        EmptyView(stringResource(R.string.common_list_failed), content = s.errorMessage,
                            buttonText = stringResource(R.string.common_retry), onButton = { viewModel.reload() })
                    }
                    s.items.isEmpty() -> item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                        EmptyView(stringResource(R.string.likes_empty), content = stringResource(R.string.likes_empty_desc),
                            buttonText = stringResource(R.string.likes_browse), onButton = onBrowse)
                    }
                    s.grid -> itemsIndexed(s.items, key = { _, i -> "${i.targetType}-${i.targetId}" }) { idx, i ->
                        val left = idx % 2 == 0
                        LikedCard(
                            title = i.title.ifBlank { deleted }, sub = i.region, corner = LikesLabels.corner(i, context),
                            image = i.image, fallbackRes = fallback(i), closedLabel = closed.takeIf { i.closed },
                            onClick = { open(i) }, onUnlike = { viewModel.unlike(i) },
                            modifier = Modifier.padding(start = if (left) 20.dp else 5.dp, end = if (left) 5.dp else 20.dp, bottom = 22.dp),
                        )
                    }
                    else -> itemsIndexed(s.items, key = { _, i -> "${i.targetType}-${i.targetId}" }) { idx, i ->
                        ListRow(
                            title = i.title.ifBlank { deleted }, onClick = { open(i) },
                            sub = listOfNotNull(i.region, LikesLabels.corner(i, context).ifBlank { null }).joinToString(" · "),
                            thumbUrl = i.image, thumbFallbackRes = fallback(i),
                            trailing = { HeartButton(true, { viewModel.unlike(i) }) },
                            showDivider = idx < s.items.lastIndex,
                        )
                    }
                }
            }
        }
    }
}

private fun fallback(i: LikedItem): Int = when (i.targetType) {
    "PERFORMANCE" -> R.drawable.img_noimage_performance
    "CASTING" -> R.drawable.stage_noimage_casting
    "TEACHER", "DANCER" -> R.drawable.img_noimage_teacher
    else -> R.drawable.img_noimage
}
