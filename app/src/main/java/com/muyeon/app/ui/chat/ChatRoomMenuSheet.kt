package com.muyeon.app.ui.chat

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.R
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors

/**
 * ★ 2026-11-26 채팅방 우상단 ⋯ 시트 — 손잡이 / [차단하기·신고하기] 카드 / [알림끄기(켜기)·채팅방 나가기] 카드 / [닫기] 카드.
 *  (예전 종 토글 + 드롭다운 메뉴를 대신한다. 전체 알림 설정은 MY/설정에 있다.)
 *  동작은 부르는 쪽이 정한다 — 이 시트는 고른 줄을 알리기만 한다. 시트 닫기도 부르는 쪽([onDismiss]).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatRoomMenuSheet(
    canBlock: Boolean,
    muted: Boolean,
    muteBusy: Boolean,
    onBlock: () -> Unit,
    onReport: () -> Unit,
    onToggleMute: () -> Unit,
    onLeave: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
        containerColor = MuyeonColors.groupedBg,
    ) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ChatMenuCard {
                if (canBlock) {
                    ChatMenuRow(R.drawable.ic_chat_block, stringResource(R.string.chat_menu_block), tag = "block", onClick = onBlock)
                    ChatMenuDivider()
                }
                ChatMenuRow(R.drawable.ic_chat_report, stringResource(R.string.chat_menu_report), tag = "report", onClick = onReport)
            }
            ChatMenuCard {
                ChatMenuRow(
                    if (muted) R.drawable.ic_chat_bell else R.drawable.ic_chat_bell_off,
                    stringResource(if (muted) R.string.chat_menu_unmute else R.string.chat_menu_mute),
                    tag = "mute", enabled = !muteBusy, onClick = onToggleMute,
                )
                ChatMenuDivider()
                ChatMenuRow(
                    R.drawable.ic_chat_leave, stringResource(R.string.chat_menu_leave),
                    tint = MuyeonColors.danger, tag = "leave", onClick = onLeave,
                )
            }
            ChatMenuCard {
                Box(
                    Modifier.fillMaxWidth().height(52.dp).clickable(onClick = onDismiss).testTag("chatMenu_close"),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        stringResource(R.string.chat_menu_close),
                        fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp,
                        color = MuyeonColors.textHead,
                    )
                }
            }
        }
    }
}

/** 둥근 모서리 흰 카드 — 시트 안 묶음 하나. */
@Composable
private fun ChatMenuCard(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MuyeonColors.surface)) { content() }
}

@Composable
private fun ChatMenuDivider() {
    HorizontalDivider(Modifier.padding(start = 52.dp), thickness = 0.5.dp, color = MuyeonColors.border)
}

/** 시트 한 줄 — 왼쪽 아이콘(벡터, tint) + 글자. [enabled] 가 false 면 흐리게·누름 무시(요청 중). */
@Composable
fun ChatMenuRow(
    @DrawableRes icon: Int,
    label: String,
    tag: String,
    tint: Color = MuyeonColors.textHead,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .testTag("chatMenu_$tag")
            .fillMaxWidth()
            .height(52.dp)
            .clickable(enabled = enabled, onClick = onClick)
            .alpha(if (enabled) 1f else 0.4f)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(icon), null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Text(label, fontFamily = customFontFamily, fontSize = 16.sp, color = tint)
    }
}

/**
 * 머리글 부제 — 입력 중이면 "입력 중…", 아니면 [presence] · [responseLabel] 중 있는 것만 " · " 로 한 줄(넘치면 말줄임).
 *  둘 다 없으면 아무것도 그리지 않는다.
 */
@Composable
fun ChatRoomHeaderSubtitle(typing: Boolean, presence: String?, responseLabel: String?) {
    if (typing) {
        Text(
            stringResource(R.string.chat_header_typing),
            fontFamily = customFontFamily, fontSize = 11.sp, lineHeight = 13.sp,
            color = MuyeonColors.primary, maxLines = 1,
        )
        return
    }
    val line = chatHeaderSubtitleLine(presence, responseLabel, stringResource(R.string.chat_header_subtitle_sep)) ?: return
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (presence == ChatRoomViewModel.PRESENCE_ONLINE) {
            Box(Modifier.size(6.dp).clip(RoundedCornerShape(50)).background(MuyeonColors.green))
            Spacer(Modifier.width(4.dp))
        }
        Text(
            line,
            fontFamily = customFontFamily, fontSize = 11.sp, lineHeight = 13.sp,
            color = MuyeonColors.secondary, maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}

/** 부제 한 줄 조합(빈 값은 뺀다). 둘 다 없으면 null. */
internal fun chatHeaderSubtitleLine(presence: String?, responseLabel: String?, sep: String = " · "): String? =
    listOfNotNull(presence?.takeIf { it.isNotBlank() }, responseLabel?.takeIf { it.isNotBlank() })
        .takeIf { it.isNotEmpty() }
        ?.joinToString(sep)
