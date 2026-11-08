package com.muyeon.app.ui.support.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.muyeon.app.R
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.Dimensions
import com.muyeon.domain.model.support.SupportChatMessage

/**
 * 상담 글 한 개 — 웹 _support-chat.scss: 챗봇은 말풍선 없이 본문, 운영자는 옅은 주황 말풍선, 내 글은 오른쪽 회색 말풍선.
 *   챗봇·운영자 글 아래 '로고 무용연 도우미/고객센터, 시각'. 글 속 [버튼 글](/경로) 는 누르는 버튼([onLink]).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SupportChatBubble(m: SupportChatMessage, time: String, onLink: (String) -> Unit) {
    val mine = m.senderType == "USER"
    Column(Modifier.fillMaxWidth(), horizontalAlignment = if (mine) Alignment.End else Alignment.Start) {
        val bubble = when (m.senderType) {
            "USER" -> Modifier.background(AppColors.SurfaceMuted, RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 10.dp)
            "ADMIN" -> Modifier.background(AppColors.PrimarySoft, RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 10.dp)
            else -> Modifier
        }
        Column(Modifier.widthIn(max = 300.dp).then(bubble), verticalArrangement = Arrangement.spacedBy(Dimensions.spacingS)) {
            if (mine) Text(m.content, style = AppTypography.Body15, color = AppColors.TextHead)
            else {
                val (text, links) = splitLinks(m.content)
                if (text.isNotBlank()) Text(text, style = AppTypography.Body15, color = AppColors.TextHead)
                if (links.isNotEmpty()) FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    links.forEach { (label, href) ->
                        Box(Modifier.clip(CircleShape).border(1.dp, AppColors.Primary, CircleShape).clickable { onLink(href) }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                            Text(label, style = AppTypography.Body14SemiBold, color = AppColors.Primary)
                        }
                    }
                }
            }
        }
        if (!mine) Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Image(painterResource(R.mipmap.ic_launcher_foreground), null, Modifier.size(16.dp).clip(CircleShape).background(AppColors.Surface))
            Text(
                "${stringResource(if (m.senderType == "ADMIN") R.string.supportchat_admin else R.string.supportchat_bot)}, $time",
                style = AppTypography.Caption12, color = AppColors.TextHint,
            )
        }
    }
}

/** 운영팀·챗봇 글의 [버튼 글](/경로) — 우리 사이트 경로만(웹 LINK_RE). 글자는 링크를 뺀 나머지. */
private val LINK = Regex("""\[([^\]\n]{1,40})\]\(((?:https://(?:www\.)?muyeon\.co\.kr)?/[^\s)]*)\)""")

private fun splitLinks(src: String): Pair<String, List<Pair<String, String>>> {
    val links = LINK.findAll(src).map { it.groupValues[1] to it.groupValues[2].replace(Regex("^https://(?:www\\.)?muyeon\\.co\\.kr"), "").ifEmpty { "/" } }.toList()
    return LINK.replace(src) { it.groupValues[1] }.trim() to links
}
