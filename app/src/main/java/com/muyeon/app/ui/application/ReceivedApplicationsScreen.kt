package com.muyeon.app.ui.application

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muyeon.app.R
import com.muyeon.app.common_components.dialog.ContentAlignment
import com.muyeon.app.common_components.dialog.CustomDialog
import com.muyeon.app.ui.application.components.ReceivedApplicationRow
import com.muyeon.app.ui.components.CursorListFooter
import com.muyeon.app.ui.components.CursorLoadMoreEffect
import com.muyeon.app.ui.components.EmptyView
import com.muyeon.app.ui.components.MainTopBar
import com.muyeon.app.ui.components.SwipeActionRow
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.Dimensions
import com.muyeon.domain.model.application.ReceivedApplication

/**
 * 받은 지원 — 웹 /receivedApplications. 줄을 누르면 지원자 이력서(앱 화면 [onOpenResume]).
 *   위 바 오른쪽 '휴지통 N' ↔ '받은 지원'. 밀면 휴지통으로(휴지통에서는 영구 삭제 — 확인 팝업).
 *   받은 지원이 없으면 빈 화면에 [공고 관리](→ 내 공고 · [onManagePostings]).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceivedApplicationsScreen(
    viewModel: ReceivedApplicationsViewModel,
    onBack: () -> Unit,
    onOpenResume: (ReceivedApplication) -> Unit,
    onManagePostings: () -> Unit,
    /** ★ 2026-11-10 잠긴 지원자 — 학원 인증 화면 경로로(돌아오면 다시 읽는다). */
    onVerify: (String) -> Unit = {},
) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    var lockedTarget by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<ReceivedApplication?>(null) }
    val lockedItems = if (s.trash) emptyList() else s.items.filter { it.locked }
    fun verifyPath(a: ReceivedApplication?) = a?.verificationPath ?: lockedItems.firstNotNullOfOrNull { it.verificationPath } ?: DEFAULT_VERIFY_PATH
    val context = LocalContext.current
    val list = rememberLazyListState()
    // 이력서를 보고 돌아오면 열람 표시(새 지원 점)가 바뀐다 — 처음 한 번은 ViewModel 이 읽었다.
    val seen = androidx.compose.runtime.remember { java.util.concurrent.atomic.AtomicBoolean(false) }
    androidx.lifecycle.compose.LifecycleResumeEffect(Unit) {
        if (seen.getAndSet(true)) viewModel.refresh()
        onPauseOrDispose { }
    }
    CursorLoadMoreEffect(list, canLoad = s.cursor.canAutoLoad && !s.isLoadingMore, onLoadMore = viewModel::loadMore)
    LaunchedEffect(viewModel) {
        viewModel.events.collect { e ->
            when (e) {
                is ReceivedApplicationsEvent.Message -> Toast.makeText(context, e.text, Toast.LENGTH_SHORT).show()
                is ReceivedApplicationsEvent.MessageRes -> Toast.makeText(context, e.res, Toast.LENGTH_SHORT).show()
            }
        }
    }
    Column(Modifier.fillMaxSize().background(AppColors.Surface).navigationBarsPadding()) {
        MainTopBar(title = stringResource(if (s.trash) R.string.received_trash_title else R.string.received_title), primary = false, onBack = onBack) {
            Text(
                if (s.trash) stringResource(R.string.received_title) else stringResource(R.string.received_trash, s.archivedCount),
                style = AppTypography.Body14SemiBold, color = AppColors.TextSub,
                modifier = Modifier.clickable(onClick = viewModel::toggleTrash).padding(Dimensions.spacingXS),
            )
        }
        Text(
            stringResource(if (s.trash) R.string.received_trash_desc else R.string.received_desc),
            style = AppTypography.Body14, color = AppColors.TextBody,
            modifier = Modifier.padding(horizontal = Dimensions.gutter, vertical = Dimensions.spacingS),
        )
        // ★ 2026-11-10 레슨 요청으로 인증 없이 올린 공고 — 학원 인증 전에는 인원만 알리고 인증으로 잇는다.
        if (lockedItems.isNotEmpty()) {
            Column(
                Modifier.padding(horizontal = Dimensions.gutter, vertical = Dimensions.spacingS).fillMaxWidth()
                    .background(AppColors.SurfaceMuted, androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                    .padding(Dimensions.spacingL),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(Dimensions.spacingS),
            ) {
                Text(stringResource(R.string.received_locked_notice, lockedItems.size), style = AppTypography.Body14SemiBold, color = AppColors.TextHead)
                androidx.compose.material3.OutlinedButton(
                    onClick = { onVerify(verifyPath(null)) }, shape = androidx.compose.foundation.shape.CircleShape,
                ) { Text(stringResource(R.string.received_verify_academy), style = AppTypography.Body14SemiBold, color = AppColors.TextBody) }
            }
        }
        PullToRefreshBox(s.isRefreshing, onRefresh = { viewModel.refresh(pull = true) }, modifier = Modifier.fillMaxSize()) {
            LazyColumn(Modifier.fillMaxSize(), state = list) {
                when {
                    s.items.isEmpty() && s.isInitialLoading -> Unit
                    s.items.isEmpty() && s.errorMessage != null -> item {
                        EmptyView(stringResource(R.string.received_failed), content = s.errorMessage,
                            buttonText = stringResource(R.string.common_retry), onButton = { viewModel.refresh() })
                    }
                    s.items.isEmpty() && s.trash -> item { EmptyView(stringResource(R.string.received_trash_empty)) }
                    s.items.isEmpty() -> item {
                        EmptyView(stringResource(R.string.received_empty), content = stringResource(R.string.received_empty_desc),
                            buttonText = stringResource(R.string.received_manage), onButton = onManagePostings)
                    }
                    else -> itemsIndexed(s.items, key = { _, a -> a.key }) { i, a ->
                        SwipeActionRow(
                            label = stringResource(R.string.common_delete),
                            onAction = { if (s.trash) viewModel.askPurge(a) else viewModel.archive(a) },
                        ) {
                            ReceivedApplicationRow(
                                a, s.trash,
                                onClick = when {
                                    a.withdrawn || s.trash -> null
                                    a.locked -> ({ lockedTarget = a })
                                    else -> ({ onOpenResume(a) })
                                },
                                onRestore = { viewModel.restore(a) },
                                showDivider = i < s.items.lastIndex,
                            )
                        }
                    }
                }
                item { CursorListFooter(s.isLoadingMore, s.moreError, s.cursor.needsTap || s.moreError != null, viewModel::loadMore) }
            }
        }
    }
    CustomDialog(
        title = stringResource(R.string.received_purge_q), content = stringResource(R.string.received_purge_body),
        leftButtonText = stringResource(R.string.common_cancel), rightButtonText = stringResource(R.string.common_delete),
        buttonCount = 2, alignment = ContentAlignment.Middle,
        onDismiss = { viewModel.askPurge(null) }, onLeftButtonClick = { viewModel.askPurge(null) },
        onRightButtonClick = viewModel::purge, showPopup = s.purgeTarget != null,
    )
    CustomDialog(
        title = stringResource(R.string.received_locked_title), content = stringResource(R.string.received_locked_body),
        leftButtonText = stringResource(R.string.common_cancel), rightButtonText = stringResource(R.string.received_verify_academy),
        buttonCount = 2, alignment = ContentAlignment.Middle,
        onDismiss = { lockedTarget = null }, onLeftButtonClick = { lockedTarget = null },
        onRightButtonClick = { val p = verifyPath(lockedTarget); lockedTarget = null; onVerify(p) },
        showPopup = lockedTarget != null,
    )
}

/** 서버가 경로를 안 줬을 때의 학원 인증(백엔드 verification.path 와 같은 값). */
const val DEFAULT_VERIFY_PATH = "/my/memberType?verify=ACADEMY"
