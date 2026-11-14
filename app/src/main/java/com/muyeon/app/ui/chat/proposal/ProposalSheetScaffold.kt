package com.muyeon.app.ui.chat.proposal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors

/**
 * ★ 2026-11-14 약속잡기 창 틀 — 흰 아래 시트 + [취소 | 레슨 약속잡기 | 제안 보내기] + 스크롤 본문.
 *  내용(강사용·회원용 칸)은 [content] 가 채운다. 틀은 역할을 모른다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProposalSheetScaffold(
    sending: Boolean,
    canSend: Boolean,
    onCancel: () -> Unit,
    onSend: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onCancel,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MuyeonColors.surface,
        // 손잡이를 둔다 — 키가 큰 시트라 위로 끝까지 올라가면 손잡이 자리가 상태바(시계·배터리)와 겹치지 않게 해 준다.
        dragHandle = { BottomSheetDefaults.DragHandle(color = MuyeonColors.border) },
    ) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(52.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "취소", fontFamily = customFontFamily, fontSize = 15.sp, color = MuyeonColors.textSub,
                    modifier = Modifier.clickable(onClick = onCancel),
                )
                Text(
                    "레슨 약속잡기", fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 17.sp,
                    color = MuyeonColors.textHead, textAlign = TextAlign.Center, modifier = Modifier.weight(1f),
                )
                Text(
                    if (sending) "보내는 중…" else "제안 보내기",
                    fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                    color = if (canSend) MuyeonColors.primary else MuyeonColors.chevron,
                    modifier = Modifier.clickable(enabled = canSend, onClick = onSend).testTag("proposal.send"),
                )
            }
            HorizontalDivider(color = MuyeonColors.border)
            Column(
                Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp),
                content = content,
            )
        }
    }
}

/** 창 맨 위 한 줄 안내(역할별 문구는 부르는 쪽이 준다). */
@Composable
internal fun ProposalIntro(text: String) {
    Text(text, fontFamily = customFontFamily, fontSize = 13.sp, lineHeight = 18.sp, color = MuyeonColors.textSub)
}

/** 오류 한 줄. */
@Composable
internal fun ProposalError(text: String?) {
    text?.let { Text(it, fontFamily = customFontFamily, fontSize = 13.sp, lineHeight = 16.sp, color = MuyeonColors.danger) }
}
