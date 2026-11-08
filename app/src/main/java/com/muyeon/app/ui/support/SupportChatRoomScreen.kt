package com.muyeon.app.ui.support

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muyeon.app.R
import com.muyeon.app.common_components.dialog.ContentAlignment
import com.muyeon.app.common_components.dialog.CustomDialog
import com.muyeon.app.ui.components.EmptyView
import com.muyeon.app.ui.components.MainTopBar
import com.muyeon.app.ui.format.KstDates
import com.muyeon.app.ui.support.components.SupportChatBubble
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.Dimensions
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 상담방 — 웹 /support/chat?room=N(운영자 답장 푸시의 착지점). 날짜 구분 · 글 · 챗봇 버튼(이전 단계·처음으로·상담원 연결) ·
 *   아래 입력줄(종료면 막힘) · 위 바 [상담 종료](확인). 글 속 [버튼](/경로) 는 [onOpenPath].
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SupportChatRoomScreen(viewModel: SupportChatRoomViewModel, onBack: () -> Unit, onOpenPath: (String) -> Unit, onMy: () -> Unit) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val list = rememberLazyListState()
    LifecycleResumeEffect(Unit) { viewModel.startPolling(); onPauseOrDispose { viewModel.stopPolling() } }
    LaunchedEffect(viewModel) { viewModel.events.collect { if (it is SupportChatEvent.Message) Toast.makeText(context, it.text, Toast.LENGTH_SHORT).show() } }
    // 새 글·버튼이 생기면 맨 아래로.
    LaunchedEffect(s.messages.size, s.menu) { if (s.messages.isNotEmpty()) list.animateScrollToItem(s.messages.size + (if (s.menu != null) 1 else 0)) }
    val closed = s.room?.closed == true

    Column(Modifier.fillMaxSize().background(AppColors.Surface).navigationBarsPadding().imePadding()) {
        MainTopBar(title = stringResource(R.string.supportchat_brand), primary = false, onBack = onBack) {
            if (s.room != null && !closed) Text(stringResource(R.string.supportchat_close), style = AppTypography.Body14SemiBold, color = AppColors.TextSub,
                modifier = Modifier.clickable { viewModel.askClose(true) }.padding(Dimensions.spacingXS))
        }
        val failed = s.failed
        when {
            failed != null -> EmptyView(stringResource(R.string.supportchat_not_found), content = failed, buttonText = stringResource(R.string.supportchat_go_my), onButton = onMy)
            s.room == null -> Unit
            else -> {
                LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = list, contentPadding = androidx.compose.foundation.layout.PaddingValues(Dimensions.gutter),
                    verticalArrangement = Arrangement.spacedBy(Dimensions.spacingL)) {
                    itemsIndexed(s.messages, key = { _, m -> m.id }) { i, m ->
                        val day = KstDates.localDate(m.createdAt)
                        val prev = s.messages.getOrNull(i - 1)?.let { KstDates.localDate(it.createdAt) }
                        Column(verticalArrangement = Arrangement.spacedBy(Dimensions.spacingM)) {
                            if (day != null && day != prev) Text(day.format(DAY), style = AppTypography.Caption12, color = AppColors.TextHint, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                            SupportChatBubble(m, clock(m.createdAt), onOpenPath)
                        }
                    }
                    s.menu?.let { menu ->
                        item(key = "menu") {
                            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                menu.nodes.forEach { (id, label) -> Chip(label, primary = true, enabled = !s.busy) { viewModel.choose(id) } }
                                if (menu.canBack) Chip(stringResource(R.string.supportchat_back), false, !s.busy, viewModel::back)
                                if (menu.canHome) Chip(stringResource(R.string.supportchat_home), false, !s.busy, viewModel::home)
                                if (menu.canHandoff) Chip(stringResource(R.string.supportchat_handoff), false, !s.busy, viewModel::handoff)
                            }
                        }
                    }
                }
                val status = when {
                    closed -> stringResource(R.string.supportchat_status_closed)
                    s.room?.status == "WAITING" -> stringResource(R.string.supportchat_status_waiting) + (s.delayNotice?.let { " · $it" } ?: "")
                    else -> null
                }
                status?.let { Text(it, style = AppTypography.Caption12, color = if (closed) AppColors.TextHint else AppColors.Warning, modifier = Modifier.padding(horizontal = Dimensions.gutter)) }
                Row(Modifier.fillMaxWidth().padding(horizontal = Dimensions.spacingM, vertical = Dimensions.spacingS), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimensions.spacingS)) {
                    Box(Modifier.weight(1f).background(AppColors.SurfaceMuted, RoundedCornerShape(20.dp)).padding(horizontal = 14.dp, vertical = 10.dp)) {
                        if (s.text.isEmpty()) Text(stringResource(if (closed) R.string.supportchat_status_closed else R.string.supportchat_placeholder), style = AppTypography.Body15, color = AppColors.TextHint)
                        BasicTextField(s.text, viewModel::onText, enabled = !closed, maxLines = 5, textStyle = AppTypography.Body15.copy(color = AppColors.TextHead),
                            cursorBrush = SolidColor(AppColors.Primary), modifier = Modifier.fillMaxWidth())
                    }
                    val can = !closed && !s.busy && s.text.isNotBlank()
                    Box(Modifier.size(40.dp).clip(CircleShape).background(if (can) AppColors.Primary else AppColors.IconOff).clickable(enabled = can, onClick = viewModel::send), Alignment.Center) {
                        Icon(Icons.AutoMirrored.Filled.Send, stringResource(R.string.supportchat_send), tint = AppColors.TextOnPrimary, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
    CustomDialog(
        title = stringResource(R.string.supportchat_close_q),
        content = stringResource(if (s.canCreate) R.string.supportchat_close_body_new else R.string.supportchat_close_body),
        leftButtonText = stringResource(R.string.common_cancel), rightButtonText = stringResource(R.string.supportchat_close_confirm),
        buttonCount = 2, alignment = ContentAlignment.Middle,
        onDismiss = { viewModel.askClose(false) }, onLeftButtonClick = { viewModel.askClose(false) }, onRightButtonClick = viewModel::close, showPopup = s.confirmClose,
    )
}

@Composable
private fun Chip(text: String, primary: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.clip(CircleShape).background(AppColors.Surface).border(1.dp, if (primary) AppColors.Primary else AppColors.ChipBorder, CircleShape)
            .clickable(enabled = enabled, onClick = onClick).padding(horizontal = 14.dp, vertical = 8.dp),
    ) { Text(text, style = AppTypography.Body14SemiBold, color = if (primary) AppColors.Primary else AppColors.TextBody) }
}

private val KST: ZoneId = ZoneId.of("Asia/Seoul")
private val DAY: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy년 M월 d일 EEEE", Locale.KOREAN)
private val CLOCK: DateTimeFormatter = DateTimeFormatter.ofPattern("a h:mm", Locale.KOREAN)
private fun clock(iso: String?): String = KstDates.parse(iso)?.atZone(KST)?.format(CLOCK).orEmpty()
