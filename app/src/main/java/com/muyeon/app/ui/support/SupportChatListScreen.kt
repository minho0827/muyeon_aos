package com.muyeon.app.ui.support

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muyeon.app.R
import com.muyeon.app.ui.common.MuyeonBottomButton
import com.muyeon.app.ui.common.muyeonPanel
import com.muyeon.app.ui.components.EmptyView
import com.muyeon.app.ui.components.MainTopBar
import com.muyeon.app.ui.format.KstDates
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.Dimensions
import com.muyeon.domain.model.support.SupportChatRoomInfo
import org.koin.androidx.compose.koinViewModel

/** 상담 채팅 목록 — 웹 /support/chat(방 없이). 운영자 알림의 버튼은 [onOpenPath]. */
@Composable
fun SupportChatListScreen(onBack: () -> Unit, onOpenRoom: (Long) -> Unit, onOpenPath: (String) -> Unit, viewModel: SupportChatListViewModel = koinViewModel()) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LifecycleResumeEffect(Unit) { viewModel.load(); onPauseOrDispose { } }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { e ->
            when (e) {
                is SupportChatEvent.OpenRoom -> onOpenRoom(e.id)
                is SupportChatEvent.Message -> Toast.makeText(context, e.text, Toast.LENGTH_SHORT).show()
            }
        }
    }
    Column(Modifier.fillMaxSize().background(AppColors.Surface).navigationBarsPadding()) {
        MainTopBar(title = stringResource(R.string.supportchat_title), primary = false, onBack = onBack)
        val rooms = s.rooms
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Dimensions.spacingM)) {
            item {
                Row(Modifier.fillMaxWidth().padding(Dimensions.gutter), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimensions.spacingM)) {
                    Image(painterResource(R.mipmap.ic_launcher_foreground), null, Modifier.size(44.dp).clip(CircleShape).background(AppColors.PrimarySoft))
                    Column {
                        Text(stringResource(R.string.supportchat_brand), style = AppTypography.Section16, color = AppColors.TextHead)
                        Text(stringResource(R.string.supportchat_brand_sub), style = AppTypography.Sub13, color = AppColors.TextSub)
                    }
                }
            }
            s.delayNotice?.let { d -> item { Text(d, style = AppTypography.Sub13, color = AppColors.Warning, modifier = Modifier.padding(horizontal = Dimensions.gutter)) } }
            val unread = s.notices.filter { !it.isRead }
            if (unread.isNotEmpty()) item {
                Column(Modifier.padding(horizontal = Dimensions.gutter).muyeonPanel(), verticalArrangement = Arrangement.spacedBy(Dimensions.spacingM)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.supportchat_unread_notices, unread.size), style = AppTypography.Body14SemiBold, color = AppColors.TextHead, modifier = Modifier.weight(1f))
                        Text(stringResource(R.string.supportchat_read_all), style = AppTypography.Sub13, color = AppColors.Primary, modifier = Modifier.clickable(onClick = viewModel::readAllNotices))
                    }
                    unread.forEach { n ->
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(n.title, style = AppTypography.Body14SemiBold, color = AppColors.TextHead)
                            n.body?.let { Text(it, style = AppTypography.Sub13, color = AppColors.TextBody) }
                            val href = n.buttonUrl?.takeIf { it.startsWith("/") && !it.startsWith("//") || it.startsWith("https://") }
                            if (href != null && n.buttonLabel != null) Text(n.buttonLabel!!, style = AppTypography.Sub13SemiBold, color = AppColors.Primary,
                                modifier = Modifier.clickable { onOpenPath(href) }.padding(vertical = 4.dp))
                        }
                    }
                }
            }
            item { Text(stringResource(R.string.supportchat_rooms), style = AppTypography.Section16, color = AppColors.TextHead, modifier = Modifier.padding(horizontal = Dimensions.gutter)) }
            when {
                rooms == null -> Unit
                rooms.isEmpty() -> item {
                    EmptyView(s.errorMessage ?: stringResource(R.string.supportchat_empty),
                        content = if (s.canCreate) stringResource(R.string.supportchat_empty_desc) else null)
                }
                else -> items(rooms, key = { it.id }) { r -> RoomRow(r) { onOpenRoom(r.id) } }
            }
        }
        if (s.canCreate) MuyeonBottomButton(stringResource(R.string.supportchat_new), "supportchat.new", loading = s.creating, onClick = viewModel::create)
    }
}

@Composable
private fun RoomRow(r: SupportChatRoomInfo, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = Dimensions.gutter, vertical = Dimensions.spacingS),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimensions.spacingM)) {
        Image(painterResource(R.mipmap.ic_launcher_foreground), null, Modifier.size(40.dp).clip(CircleShape).background(AppColors.PrimarySoft))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(r.title ?: stringResource(R.string.supportchat_new_title), style = AppTypography.CardTitle16, color = AppColors.TextHead,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                Text(stringResource(when (r.status) { "OPEN" -> R.string.supportchat_open; "CLOSED" -> R.string.supportchat_closed; else -> R.string.supportchat_waiting }),
                    style = AppTypography.Caption12, color = if (r.status == "CLOSED") AppColors.TextHint else AppColors.Primary)
            }
            Text(r.lastMessage.orEmpty(), style = AppTypography.Sub13, color = AppColors.TextSub, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(KstDates.dot(r.lastMessageAt ?: r.createdAt), style = AppTypography.Caption12, color = AppColors.TextHint)
            if (r.unread > 0) Box(Modifier.padding(top = 4.dp).size(8.dp).background(AppColors.Badge, CircleShape))
        }
    }
}
