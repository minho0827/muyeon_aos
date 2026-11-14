package com.muyeon.app.ui.chat.dealbar

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.ui.common.MuyeonColors
import com.muyeon.app.theme.customFontFamily

/**
 * ★ 2026-11-14 대화 시작 전 가운데 안내(당근 빈 채팅방 안내, iOS ChatEmptyNotice 1:1).
 *  두 사람이 아직 글을 주고받지 않았을 때만(시스템 안내·카드만 있는 방 포함) 목록 끝에 보인다.
 */
@Composable
fun ChatEmptyNotice(pair: String?, myRole: String?) {
    Text(
        ChatNoticeCopy.of(pair, myRole),
        color = MuyeonColors.secondary,
        textAlign = TextAlign.Center,
        fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 20.sp,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 40.dp).testTag("chat.emptyNotice"),
    )
}

/** 두 사람이 주고받은 글(텍스트·사진·파일)이 아직 없는가 — 시스템 안내·카드 메시지는 세지 않는다. */
fun chatHasNoConversation(types: List<String>): Boolean =
    types.none { it == "TEXT" || it == "IMAGE" || it == "VIDEO" || it == "FILE" }
