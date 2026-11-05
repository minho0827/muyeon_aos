package com.muyeon.app.ui.chat

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors
import com.muyeon.app.ui.quote.QuoteUi
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * ★ 2026-11-04 채팅 말풍선 조각 — PACERA Android(ChatBubble·ChatLinkText·ChatShareCard·ChatReadReceipts·ChatTimeFormatter) 이식.
 *  무용연 메시지 모델(replyToId·replyTo{id,content,senderName}·createdAt)과 토큰(MuyeonColors·customFontFamily)에 맞췄다.
 */

// ── 시간·날짜 ──

object ChatTimeFormat {
    private val zone: ZoneId get() = ZoneId.systemDefault()
    private val timeFormat get() = DateTimeFormatter.ofPattern("a h:mm", Locale.getDefault())
    /** PACERA iOS DateSeparator — "yyyy.MM.dd(E)" → "2026.11.04(수)". */
    private val dayFormat get() = DateTimeFormatter.ofPattern("yyyy.MM.dd(E)", Locale.getDefault())

    fun parse(iso: String?): LocalDateTime? {
        if (iso.isNullOrBlank()) return null
        return runCatching { Instant.parse(iso).atZone(zone).toLocalDateTime() }.getOrNull()
            ?: runCatching { java.time.OffsetDateTime.parse(iso).atZoneSameInstant(zone).toLocalDateTime() }.getOrNull()
            ?: runCatching { LocalDateTime.parse(iso) }.getOrNull()
    }

    /** 말풍선 옆 시간 — "오후 3:12"(PACERA forBubble). */
    fun forBubble(iso: String?): String = parse(iso)?.let { timeFormat.format(it) }.orEmpty()

    fun dayOf(iso: String?): LocalDate? = parse(iso)?.toLocalDate()

    /** 날짜 구분선 — "2026.11.04(수)"(PACERA iOS). */
    fun forDivider(day: LocalDate): String = dayFormat.format(day)
}

/**
 * 묶음 규칙(PACERA ChatMessageGrouping) — 같은 사람·같은 분이면 이어진 말풍선:
 *  아바타·이름은 묶음의 첫 말풍선에만, 시간은 마지막 말풍선에만. 시스템 메시지·날짜가 바뀌면 끊는다.
 */
object ChatGrouping {
    fun continuesGroup(prev: ChatMessage?, cur: ChatMessage?): Boolean {
        if (prev == null || cur == null) return false
        if (prev.senderId <= 0 || prev.senderId != cur.senderId) return false
        // 아바타 없이 따로 그리는 안내·카드(시스템·견적 요청·설문·레슨 카드 등)는 묶음을 끊는다 —
        //  그래야 뒤이은 견적 카드·글 말풍선이 묶음 첫 줄이 되어 아바타를 갖는다.
        if (standsAlone(prev) || standsAlone(cur)) return false
        val a = ChatTimeFormat.parse(prev.createdAt) ?: return false
        val b = ChatTimeFormat.parse(cur.createdAt) ?: return false
        return a.withSecond(0).withNano(0) == b.withSecond(0).withNano(0)
    }

    private val STANDALONE_TYPES = setOf(
        "SYSTEM", "QUOTE_REQUEST", "SURVEY_UPDATE", "LESSON_CARD", "SURVEY_CARD", "LESSON_PROPOSAL",
    )

    private fun standsAlone(m: ChatMessage): Boolean = !m.isDeleted && m.type in STANDALONE_TYPES

    /** [i] 번째 메시지 위에 날짜 구분선을 그릴지 — 첫 메시지이거나 앞 메시지와 날짜가 다르면. */
    fun dayDivider(list: List<ChatMessage>, i: Int): LocalDate? {
        val day = ChatTimeFormat.dayOf(list.getOrNull(i)?.createdAt) ?: return null
        val prevDay = ChatTimeFormat.dayOf(list.getOrNull(i - 1)?.createdAt)
        return if (prevDay == day) null else day
    }
}

/**
 * 날짜 구분선 — PACERA iOS DateSeparator 와 같은 모양: 양옆 1dp 선(테두리색) + 가운데 글자(11sp Light, 3차 글자색),
 *  글자 좌우 12, 위아래 8, 좌우 20.
 */
@Composable
fun ChatDateDivider(day: LocalDate) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 20.dp).semantics { contentDescription = "chatDateDivider" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f).height(1.dp).background(MuyeonColors.border))
        Text(
            ChatTimeFormat.forDivider(day),
            fontFamily = customFontFamily, fontWeight = FontWeight.Light, fontSize = 11.sp, color = MuyeonColors.hint,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        Box(Modifier.weight(1f).height(1.dp).background(MuyeonColors.border))
    }
}

// ── 답장 인용 ──

/** 인용 한 줄 — 사진이면 "사진", 동영상이면 "동영상", 글이 없으면 "메시지"(PACERA chatReplySnippet). */
fun chatReplySnippet(type: String?, snippet: String?): String = when (type) {
    "IMAGE" -> "사진"
    "VIDEO" -> "동영상"
    else -> snippet?.takeIf { it.isNotBlank() }?.replace('\n', ' ') ?: "메시지"
}

/** 인용 상자에 그릴 값 — 원문이 화면에 있으면 종류·사진을 원문에서 가져온다. */
data class ReplyQuoteModel(val targetId: Int, val senderName: String, val snippet: String, val imageUrl: String?)

fun replyQuoteModel(m: ChatMessage, loaded: List<ChatMessage>): ReplyQuoteModel? {
    val r = m.replyTo ?: return null
    val original = loaded.firstOrNull { it.id == r.id }
    val type = original?.type
    return ReplyQuoteModel(
        targetId = r.id,
        senderName = r.senderName ?: original?.sender?.displayName ?: "",
        snippet = chatReplySnippet(type, r.content ?: original?.content),
        imageUrl = original?.imageUrls?.firstOrNull()?.takeIf { type == "IMAGE" },
    )
}

/**
 * 인용 상자 — 세로 줄 + 닉네임 + 한 줄(+ 사진 답장이면 32dp 썸네일). PACERA ReplyQuote 와 같은 치수.
 *  [standalone] 이면 말풍선 밖(사진·카드 답장)이라 자체 배경. 누르면 원문으로.
 */
@Composable
fun ReplyQuote(q: ReplyQuoteModel, onMine: Boolean, onClick: (Int) -> Unit, standalone: Boolean = false) {
    val accent = if (onMine) Color.White else MuyeonColors.primary
    val sub = if (onMine) Color.White.copy(alpha = 0.8f) else MuyeonColors.textSub
    Row(
        Modifier.widthIn(max = 232.dp).height(IntrinsicSize.Min).clip(RoundedCornerShape(8.dp))
            .then(if (standalone) Modifier.background(MuyeonColors.groupedBg) else Modifier)
            .clickable(enabled = q.targetId > 0) { onClick(q.targetId) }
            .padding(if (standalone) 8.dp else 2.dp)
            .semantics { contentDescription = "chatReplyQuote" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(2.dp).fillMaxHeight().background(accent))
        Spacer(Modifier.width(8.dp))
        q.imageUrl?.let {
            AsyncImage(QuoteUi.imageUrl(it), null, contentScale = ContentScale.Crop, modifier = Modifier.size(32.dp).clip(RoundedCornerShape(4.dp)))
            Spacer(Modifier.width(6.dp))
        }
        Column {
            Text(q.senderName, fontFamily = customFontFamily, color = accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(q.snippet, fontFamily = customFontFamily, color = sub, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

// ── 링크 ──

/** 글 안 링크 찾기·밑줄(PACERA ChatLinks) — http(s)://, www. 를 찾고 끝 문장부호는 뺀다. */
object ChatLinks {
    private val URL = Regex("""(?i)\b(?:https?://|www\.)[^\s<>"'`]+""")
    private const val TRAILING = ".,!?:;)]}>'\"」』"

    data class Found(val start: Int, val end: Int, val url: String)

    fun find(text: String): List<Found> {
        if (text.isEmpty() || !(text.contains("://") || text.contains("www.", ignoreCase = true))) return emptyList()
        val out = ArrayList<Found>()
        for (m in URL.findAll(text)) {
            var end = m.range.last + 1
            while (end > m.range.first && text[end - 1] in TRAILING) {
                val c = text[end - 1]
                if (c == ')' && text.substring(m.range.first, end).count { it == '(' } >= text.substring(m.range.first, end).count { it == ')' }) break
                end--
            }
            val raw = text.substring(m.range.first, end)
            if (raw.length < 5 || raw.endsWith("://")) continue
            val url = if (raw.startsWith("www.", ignoreCase = true)) "https://$raw" else raw
            out += Found(m.range.first, end, url)
        }
        return out
    }

    fun annotate(text: String, linkColor: Color, onOpen: (String) -> Unit): AnnotatedString {
        val found = find(text)
        if (found.isEmpty()) return AnnotatedString(text)
        val style = TextLinkStyles(style = SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline))
        return buildAnnotatedString {
            var cursor = 0
            for (f in found) {
                if (f.start > cursor) append(text.substring(cursor, f.start))
                withLink(LinkAnnotation.Clickable(f.url, style) { onOpen(f.url) }) { append(text.substring(f.start, f.end)) }
                cursor = f.end
            }
            if (cursor < text.length) append(text.substring(cursor))
        }
    }
}

private val MUYEON_HOSTS = setOf("muyeon.co.kr", "www.muyeon.co.kr")

/** 우리(무용연) 링크인지 — https://muyeon.co.kr/… 는 앱 안 링크 처리로 연다. 그 밖의 http(s) 는 확인 시트. */
fun isMuyeonLink(url: String): Boolean {
    val uri = runCatching { java.net.URI(url.trim()) }.getOrNull() ?: return false
    return uri.scheme?.lowercase() in setOf("http", "https") && uri.host?.lowercase() in MUYEON_HOSTS
}

/** http(s) 이고 우리 링크가 아닌 주소 — "공유된 웹사이트" 확인 시트를 거친다(PACERA isExternalWebLink). */
fun isExternalWebLink(url: String): Boolean {
    val t = url.trim()
    val uri = runCatching { java.net.URI(t) }.getOrNull() ?: return t.startsWith("http://", true) || t.startsWith("https://", true)
    val scheme = uri.scheme?.lowercase()
    if (scheme != "http" && scheme != "https") return false
    return !isMuyeonLink(t)
}

/** 무용연 링크 — 앱 링크 보관소(AppLinkManager)에 맡기고 본문을 앞으로 → 본문이 링크 화면(네이티브·웹)을 연다. */
fun openMuyeonLinkInApp(context: Context, url: String) {
    val uri = Uri.parse(url.trim())
    com.muyeon.app.utils.AppLinkManager.save(uri.buildUpon().scheme("https").build())
    context.startActivity(
        Intent(context, com.muyeon.app.webview.WebViewActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
    )
}

/**
 * 말풍선 본문 — 링크는 **짧게 누를 때만** 연다(PACERA ChatLinkBodyText). 링크 위에서 길게 누르면 링크를 열지 않고
 *  [onLongPress](행동 시트)를 부른다.
 */
@Composable
fun ChatLinkBodyText(text: String, color: Color, style: TextStyle, linkColor: Color, onOpenLink: (String) -> Unit, onLongPress: (() -> Unit)?) {
    val longPressMs = LocalViewConfiguration.current.longPressTimeoutMillis
    val longPressed = remember { mutableStateOf(false) }
    val annotated = remember(text, linkColor, onOpenLink) {
        ChatLinks.annotate(text, linkColor) { url -> if (!longPressed.value) onOpenLink(url) }
    }
    Text(
        annotated, color = color, style = style,
        modifier = Modifier.pointerInput(onLongPress, longPressMs) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                longPressed.value = false
                val slop = viewConfiguration.touchSlop
                val notLongPress = withTimeoutOrNull(longPressMs) {
                    while (true) {
                        val change = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed || change.isConsumed) break
                        if ((change.position - down.position).getDistance() > slop) break
                    }
                    true
                }
                if (notLongPress == null) {
                    longPressed.value = true
                    onLongPress?.invoke()
                    waitForUpOrCancellation(PointerEventPass.Initial)
                }
            }
        },
    )
}

/** "공유된 웹사이트" 확인 시트(PACERA ChatExternalLinkSheet) — 주소를 보여 주고 [웹사이트로 이동] / [돌아가기]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatExternalLinkSheet(url: String, onOpen: () -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
        containerColor = MuyeonColors.surface,
    ) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp).testTag("chatExternalLinkSheet"),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("공유된 웹사이트", fontFamily = customFontFamily, color = MuyeonColors.textHead, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Text(
                "이 웹사이트와 해당 웹사이트를 보낸 사람을 믿을 수 있는지 확인하세요.",
                fontFamily = customFontFamily, color = MuyeonColors.textSub, fontSize = 14.sp, textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            Box(
                Modifier.fillMaxWidth().heightIn(max = 150.dp).border(1.dp, MuyeonColors.border, RoundedCornerShape(14.dp))
                    .verticalScroll(rememberScrollState()).padding(16.dp),
            ) {
                SelectionContainer {
                    Text(url, fontFamily = customFontFamily, color = MuyeonColors.textHead, fontSize = 15.sp, modifier = Modifier.testTag("chatExternalLinkUrl"))
                }
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onOpen,
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("chatExternalLinkOpen"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MuyeonColors.primary, contentColor = Color.White),
            ) { Text("웹사이트로 이동", fontFamily = customFontFamily, fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(48.dp).testTag("chatExternalLinkBack")) {
                Text("돌아가기", fontFamily = customFontFamily, color = MuyeonColors.primary, fontSize = 16.sp)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
