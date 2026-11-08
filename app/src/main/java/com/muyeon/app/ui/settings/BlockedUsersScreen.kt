package com.muyeon.app.ui.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muyeon.app.R
import com.muyeon.app.ui.components.Avatar
import com.muyeon.app.ui.components.EmptyView
import com.muyeon.app.ui.components.GuideBanner
import com.muyeon.app.ui.components.MainTopBar
import com.muyeon.app.ui.format.KstDates
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.Dimensions
import org.koin.androidx.compose.koinViewModel

/** 차단한 사용자 — 웹 /blockedUsers(안내 띠 · 사진·이름·차단일 · [해제]). */
@Composable
fun BlockedUsersScreen(onBack: () -> Unit, viewModel: BlockedUsersViewModel = koinViewModel()) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val member = stringResource(R.string.blocked_member)
    LaunchedEffect(viewModel) {
        viewModel.events.collect { e ->
            when (e) {
                is BlockedUsersEvent.Unblocked -> Toast.makeText(context, context.getString(R.string.blocked_unblocked, e.name ?: member), Toast.LENGTH_SHORT).show()
                is BlockedUsersEvent.Message -> Toast.makeText(context, e.text, Toast.LENGTH_SHORT).show()
            }
        }
    }
    Column(Modifier.fillMaxSize().background(AppColors.Surface).navigationBarsPadding()) {
        MainTopBar(title = stringResource(R.string.settings_blocked_users), primary = false, onBack = onBack)
        LazyColumn(Modifier.fillMaxSize()) {
            item { GuideBanner(stringResource(R.string.blocked_guide), Modifier.padding(horizontal = Dimensions.gutter, vertical = Dimensions.spacingM)) }
            when {
                s.isLoading -> Unit
                s.errorMessage != null -> item {
                    EmptyView(stringResource(R.string.common_list_failed), content = s.errorMessage,
                        buttonText = stringResource(R.string.common_retry), onButton = viewModel::load)
                }
                s.items.isEmpty() -> item { EmptyView(stringResource(R.string.blocked_empty)) }
                else -> itemsIndexed(s.items, key = { _, u -> u.userId }) { i, u ->
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = Dimensions.gutter, vertical = Dimensions.spacingM),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimensions.spacingM),
                    ) {
                        Avatar(u.image, 48.dp)
                        Column(Modifier.weight(1f)) {
                            Text(u.name ?: member, style = AppTypography.CardTitle16, color = AppColors.TextHead)
                            if (u.blockedAt != null) Text(stringResource(R.string.blocked_at, KstDates.dot(u.blockedAt)), style = AppTypography.Sub13, color = AppColors.TextHint)
                        }
                        OutlinedButton(onClick = { viewModel.unblock(u) }, enabled = s.busyId == null, shape = CircleShape) {
                            Text(stringResource(if (s.busyId == u.userId) R.string.blocked_unblocking else R.string.blocked_unblock), style = AppTypography.Body14SemiBold, color = AppColors.TextBody)
                        }
                    }
                    if (i < s.items.lastIndex) HorizontalDivider(Modifier.padding(horizontal = Dimensions.gutter), thickness = 1.dp, color = AppColors.Divider)
                }
            }
        }
    }
}
