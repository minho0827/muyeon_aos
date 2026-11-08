package com.muyeon.app.ui.application

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muyeon.app.R
import com.muyeon.app.common_components.dialog.ContentAlignment
import com.muyeon.app.common_components.dialog.CustomDialog
import com.muyeon.app.ui.application.components.MyApplicationCard
import com.muyeon.app.ui.common.ListFilter
import com.muyeon.app.ui.common.ListFilterButton
import com.muyeon.app.ui.common.ListFilterEmpty
import com.muyeon.app.ui.common.ListFilterSheet
import com.muyeon.app.ui.common.MuyeonPillTab
import com.muyeon.app.ui.common.MuyeonPillTabs
import com.muyeon.app.ui.common.appliedPills
import com.muyeon.app.ui.components.ActionSheet
import com.muyeon.app.ui.components.CursorListFooter
import com.muyeon.app.ui.components.CursorLoadMoreEffect
import com.muyeon.app.ui.components.EmptyView
import com.muyeon.app.ui.components.MainTopBar
import com.muyeon.app.ui.components.SheetAction
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.Dimensions
import com.muyeon.domain.model.application.MyApplicationTab
import org.koin.androidx.compose.koinViewModel

/**
 * 내가 지원한 공고 — 웹 /myApplications. 카드를 누르면 그 공고(앱 화면이면 앱 — [onOpenPath]).
 *   지원한 공고가 없으면 빈 화면에 [공고 둘러보기](채용 탭).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyApplicationsScreen(onBack: () -> Unit, onOpenPath: (String) -> Unit, onBrowseJobs: () -> Unit, viewModel: MyApplicationsViewModel = koinViewModel()) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val list = rememberLazyListState()
    var sheet by remember { mutableStateOf(false) }
    CursorLoadMoreEffect(list, canLoad = s.cursor.canAutoLoad && !s.isLoadingMore, onLoadMore = viewModel::loadMore)
    LaunchedEffect(viewModel) {
        viewModel.events.collect { e ->
            when (e) {
                is MyApplicationsEvent.Message -> Toast.makeText(context, e.text, Toast.LENGTH_SHORT).show()
                is MyApplicationsEvent.MessageRes -> Toast.makeText(context, e.res, Toast.LENGTH_SHORT).show()
            }
        }
    }
    val tabNames = mapOf(
        MyApplicationTab.ALL to R.string.common_all, MyApplicationTab.PROGRESS to R.string.application_tab_progress,
        MyApplicationTab.CONFIRMED to R.string.application_tab_confirmed, MyApplicationTab.REJECTED to R.string.application_tab_rejected,
    )
    val tabs = MyApplicationTab.entries.map { t -> MuyeonPillTab(t, stringResource(tabNames.getValue(t)), count = s.counts[t]) }

    Column(Modifier.fillMaxSize().background(AppColors.Surface).navigationBarsPadding()) {
        MainTopBar(title = stringResource(R.string.application_title), primary = false, onBack = onBack)
        MuyeonPillTabs(
            tabs, s.tab, viewModel::selectTab, tagPrefix = "application.tab", fixedWeight = true,
            leadingPills = s.filter.appliedPills(emptyList(), viewModel::setFilter),
            trailing = { ListFilterButton(s.filter.isActive, onClick = { sheet = true }) },
        )
        PullToRefreshBox(s.isRefreshing, onRefresh = { viewModel.refresh(pull = true) }, modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                Modifier.fillMaxSize(), state = list,
                contentPadding = PaddingValues(Dimensions.gutter), verticalArrangement = Arrangement.spacedBy(Dimensions.spacingM),
            ) {
                when {
                    s.items.isEmpty() && s.isInitialLoading -> Unit
                    s.items.isEmpty() && s.errorMessage != null -> item {
                        EmptyView(stringResource(R.string.common_list_failed), content = s.errorMessage,
                            buttonText = stringResource(R.string.common_retry), onButton = { viewModel.refresh() })
                    }
                    s.items.isEmpty() && s.filter.isActive -> item { ListFilterEmpty(onReset = { viewModel.setFilter(ListFilter()) }) }
                    s.items.isEmpty() -> item {
                        if (s.tab != MyApplicationTab.ALL) EmptyView(stringResource(R.string.application_empty_tab))
                        else EmptyView(stringResource(R.string.application_empty), buttonText = stringResource(R.string.application_browse), onButton = onBrowseJobs)
                    }
                    else -> items(s.items, key = { it.key }) { a ->
                        MyApplicationCard(a, onClick = a.postingPath?.let { p -> { onOpenPath(p) } }, onMore = { viewModel.openMenu(a) })
                    }
                }
                item { CursorListFooter(s.isLoadingMore, s.moreError, s.cursor.needsTap || s.moreError != null, viewModel::loadMore) }
            }
        }
    }
    if (sheet) ListFilterSheet(s.filter, onApply = { viewModel.setFilter(it); sheet = false }, onDismiss = { sheet = false })
    s.menuFor?.let { a ->
        ActionSheet(
            title = a.title ?: stringResource(R.string.application_posting),
            actions = buildList {
                if (a.canWithdraw) add(SheetAction(stringResource(R.string.application_withdraw), primary = false, onClick = { viewModel.askWithdraw(a) }))
                add(SheetAction(stringResource(R.string.common_delete), primary = false, onClick = { viewModel.askDelete(a) }))
            },
            onDismiss = { viewModel.openMenu(null) },
        )
    }
    CustomDialog(
        title = stringResource(R.string.application_withdraw_q), content = stringResource(R.string.application_withdraw_body),
        leftButtonText = stringResource(R.string.application_keep), rightButtonText = stringResource(if (s.working) R.string.common_loading else R.string.application_withdraw),
        buttonCount = 2, alignment = ContentAlignment.Middle,
        onDismiss = { if (!s.working) viewModel.askWithdraw(null) }, onLeftButtonClick = { if (!s.working) viewModel.askWithdraw(null) },
        onRightButtonClick = viewModel::withdraw, showPopup = s.withdrawTarget != null,
    )
    CustomDialog(
        title = stringResource(R.string.application_delete_q), content = stringResource(R.string.application_delete_body),
        leftButtonText = stringResource(R.string.common_cancel), rightButtonText = stringResource(R.string.common_delete),
        buttonCount = 2, alignment = ContentAlignment.Middle,
        onDismiss = { viewModel.askDelete(null) }, onLeftButtonClick = { viewModel.askDelete(null) },
        onRightButtonClick = viewModel::delete, showPopup = s.deleteTarget != null,
    )
}
