package com.muyeon.app.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors

/**
 * ★ 2026-11-04 메시지 길게 누르기 · 반응 — PACERA Android(ChatMessageActionSheet·ChatBubble.ReactionChips·
 *  domain ChatMessageActions) 이식. 무용연 백엔드에 맞춘 차이:
 *   · 반응은 글리프 문자열로 저장된다(chat_reactions.emoji) — PACERA 키(like…sad)를 같은 글리프로 보낸다.
 *   · 서버는 한 사람이 여러 반응을 둘 수 있는 토글(POST …/messages/:messageId/reactions {emoji})이다 —
 *     PACERA 처럼 "한 사람 하나"로 쓰려고 다른 반응을 누르면 옛것 해제 + 새것 추가를 차례로 보낸다.
 *   · 소켓 'message-reaction' 은 {roomId, messageId} 만 준다 → 뷰모델이 반응 묶음을 다시 읽는다(기존 그대로).
 *   · 전달·공지 등록은 서버에 없어 메뉴에 띄우지 않는다. 편집·삭제 24시간 제한도 서버에 없어 두지 않는다.
 */
object ChatReactionKeys {
    /** PACERA 화면 순서 like · ok · heart · smile · party · sad 의 글리프. 이모지는 이 반응 줄에만 쓴다. */
    val ALL: List<String> = listOf("👍", "👌", "❤️", "😄", "🎉", "😭")
}

object ChatReactionRules {
    /**
     * 내가 [glyph] 를 눌렀다 — (낙관적으로 바뀐 칩 목록, 서버에 보낼 토글 목록).
     *  같은 반응을 다시 누르면 취소, 다른 반응이면 교체(한 사람 하나 — 예전에 여러 개 둔 것도 모두 해제).
     */
    fun toggle(current: List<ChatReaction>, glyph: String): Pair<List<ChatReaction>, List<String>> {
        val mineKeys = current.filter { it.mine }.map { it.emoji }
        val cancel = glyph in mineKeys
        val calls = mineKeys.filter { it != glyph }.toMutableList()
        calls += glyph // 취소면 그 글리프를 다시 토글(해제), 아니면 추가
        val counts = LinkedHashMap<String, Int>()
        current.forEach { counts[it.emoji] = it.count }
        mineKeys.forEach { counts[it] = (counts[it] ?: 1) - 1 }
        val mine: String? = if (cancel) null else glyph
        if (mine != null) counts[mine] = (counts[mine] ?: 0) + 1
        return ordered(counts, mine) to calls
    }

    /** 화면 순서(PACERA 6종 먼저, 그 밖의 예전 글리프는 뒤에), 0 은 뺀다. */
    fun ordered(counts: Map<String, Int>, mine: String?): List<ChatReaction> {
        val keys = ChatReactionKeys.ALL + counts.keys.filter { it !in ChatReactionKeys.ALL }
        return keys.mapNotNull { k ->
            val c = counts[k] ?: return@mapNotNull null
            if (c <= 0) null else ChatReaction(k, c, k == mine)
        }
    }

    /** 서버 응답 정렬 — 내 반응 표시는 서버 mine 그대로. */
    fun sorted(list: List<ChatReaction>): List<ChatReaction> {
        val order = ChatReactionKeys.ALL
        return list.filter { it.count > 0 }.sortedBy { order.indexOf(it.emoji).let { i -> if (i < 0) Int.MAX_VALUE else i } }
    }
}

object ChatMessageRules {
    /** 시트를 띄울 메시지 — 지운 메시지·시스템 메시지는 없음. */
    fun hasActionSheet(m: ChatMessage): Boolean = !m.isDeleted && m.type != "SYSTEM"

    /** 서버 메시지 id 가 있어야 반응·편집·삭제를 가리킬 수 있다(보내는 중 말풍선은 없음). */
    fun canAddress(m: ChatMessage): Boolean = m.id > 0

    fun canReact(m: ChatMessage): Boolean = canAddress(m) && hasActionSheet(m)
    fun canReply(m: ChatMessage): Boolean = canAddress(m) && hasActionSheet(m)
    fun canCopy(m: ChatMessage): Boolean = m.type == "TEXT" && m.content.isNotBlank() && !m.isDeleted
    fun canEdit(m: ChatMessage, mine: Boolean): Boolean = mine && canAddress(m) && m.type == "TEXT" && !m.isDeleted
    fun canDelete(m: ChatMessage, mine: Boolean): Boolean = mine && canAddress(m) && !m.isDeleted

    /**
     * @param locked 대화 종료 방 — 서버가 반응·수정을 ROOM_LOCKED 로 거절하고 입력창도 없으므로 반응·답장·수정을 숨긴다. 내 메시지 삭제는 그대로.
     */
    fun actions(m: ChatMessage, mine: Boolean, canBlock: Boolean, locked: Boolean = false) = ChatMessageActions(
        canReact = !locked && canReact(m),
        myReaction = m.reactions?.firstOrNull { it.mine }?.emoji,
        canReply = !locked && canReply(m),
        canCopy = canCopy(m),
        canEdit = !locked && canEdit(m, mine),
        canDelete = canDelete(m, mine),
        canReport = !mine && canAddress(m),
        canBlock = !mine && canBlock,
    )
}

/** 시트에 띄울 행동 — 화면이 [ChatMessageRules.actions] 로 채운다. */
data class ChatMessageActions(
    val canReact: Boolean,
    val myReaction: String?,
    val canReply: Boolean,
    val canCopy: Boolean,
    val canEdit: Boolean,
    val canDelete: Boolean,
    val canReport: Boolean,
    val canBlock: Boolean,
)

/**
 * 메시지 길게 누르기 → 아래 시트(PACERA 와 같은 구성·순서·문구).
 *  첫 줄 반응 6개(내 반응은 동그란 바탕) + 답장 · 복사 · 편집 · 삭제 · 신고 · 차단.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatMessageActionSheet(
    actions: ChatMessageActions,
    onDismiss: () -> Unit,
    onReact: (String) -> Unit,
    onReply: () -> Unit,
    onCopy: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onReport: () -> Unit,
    onBlock: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
        containerColor = MuyeonColors.surface,
    ) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 8.dp)) {
            if (actions.canReact) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    ChatReactionKeys.ALL.forEach { glyph ->
                        val mine = actions.myReaction == glyph
                        Box(
                            Modifier.size(48.dp).clip(CircleShape)
                                .background(if (mine) MuyeonColors.primary.copy(alpha = 0.15f) else Color.Transparent)
                                .clickable { onReact(glyph) }
                                .semantics { contentDescription = "reaction_$glyph"; selected = mine },
                            contentAlignment = Alignment.Center,
                        ) { Text(glyph, fontSize = 26.sp) }
                    }
                }
                HorizontalDivider(color = MuyeonColors.border, modifier = Modifier.padding(vertical = 4.dp))
            }
            if (actions.canReply) ActionRow(Icons.AutoMirrored.Filled.Reply, "답장", tag = "reply", onClick = onReply)
            if (actions.canCopy) ActionRow(Icons.Filled.ContentCopy, "복사", tag = "copy", onClick = onCopy)
            if (actions.canEdit) ActionRow(Icons.Filled.Edit, "편집", tag = "edit", onClick = onEdit)
            if (actions.canDelete) ActionRow(Icons.Filled.Delete, "삭제", tint = MuyeonColors.danger, tag = "delete", onClick = onDelete)
            if (actions.canReport) ActionRow(Icons.Filled.Flag, "신고", tag = "report", onClick = onReport)
            if (actions.canBlock) ActionRow(Icons.Filled.Block, "차단", tint = MuyeonColors.danger, tag = "block", onClick = onBlock)
        }
    }
}

@Composable
private fun ActionRow(icon: ImageVector, label: String, tint: Color = MuyeonColors.textHead, tag: String, onClick: () -> Unit) {
    Row(
        Modifier.testTag("chatAction_$tag").fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(16.dp))
        Text(label, color = tint, fontSize = 16.sp, fontFamily = customFontFamily)
    }
}

/** 반응 칩 "👍 2" — 말풍선 아래(PACERA ReactionChips). 내가 누른 것은 주황 테두리. 누르면 내 반응 토글. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReactionChips(reactions: List<ChatReaction>, onClick: ((String) -> Unit)?) {
    if (reactions.isEmpty()) return
    FlowRow(
        Modifier.padding(top = 4.dp).widthIn(max = 260.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        reactions.forEach { r ->
            val shape = RoundedCornerShape(12.dp)
            Row(
                Modifier.clip(shape)
                    .background(if (r.mine) MuyeonColors.primary.copy(alpha = 0.10f) else MuyeonColors.surface)
                    .border(if (r.mine) 1.5.dp else 1.dp, if (r.mine) MuyeonColors.primary else MuyeonColors.border, shape)
                    .clickable(enabled = onClick != null) { onClick?.invoke(r.emoji) }
                    .padding(horizontal = 8.dp, vertical = 3.dp)
                    .semantics { contentDescription = "${r.emoji} ${r.count}" },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(r.emoji, fontSize = 13.sp)
                Spacer(Modifier.width(3.dp))
                Text(
                    r.count.toString(), fontFamily = customFontFamily, fontSize = 12.sp, fontWeight = FontWeight.Medium,
                    color = if (r.mine) MuyeonColors.primary else MuyeonColors.textSub,
                )
            }
        }
    }
}
