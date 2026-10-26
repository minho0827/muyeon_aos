package com.muyeon.app.ui.castingstage.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.muyeon.app.ui.common.MuyeonColors
import com.muyeon.app.ui.castingstage.model.*
import com.muyeon.app.ui.castingstage.data.*
import com.muyeon.app.ui.castingstage.screens.*

/** 가로 카드 — 캐스팅 공고·무대 소식(iOS StageCastingCard·StageNewsCard). */

@Composable
internal fun StageCastingCard(c: StageCasting, onClick: () -> Unit) {
    Column(Modifier.width(150.dp).clickable(onClick = onClick), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            Modifier.width(150.dp).height(200.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFFFFF3EA))
                .border(0.5.dp, MuyeonColors.border, RoundedCornerShape(14.dp)),
            Alignment.Center,
        ) {
            val url = stageImageUrl(c.imageUrl)
            if (url != null) AsyncImage(url, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            // 웹 캐스팅 공고와 같은 '사진 없음' 그림(muyeon-noimage-casting.png, 10-26)
            else androidx.compose.foundation.Image(androidx.compose.ui.res.painterResource(com.muyeon.app.R.drawable.stage_noimage_casting), null,
                Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
        StageDate.deadline(c.deadline)?.let { Text(it, style = Bold, fontSize = 15.sp, color = Blue, modifier = Modifier.padding(top = 4.dp)) }
        Text(c.title, style = Semi, fontSize = 15.sp, color = MuyeonColors.textHead, maxLines = 2, overflow = TextOverflow.Ellipsis)
        c.team?.let { Text(it, style = Regular, fontSize = 12.sp, color = MuyeonColors.textSub, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        c.genre?.let {
            Text(it, style = Medium, fontSize = 12.sp, color = MuyeonColors.textSub,
                modifier = Modifier.border(1.dp, Ink300, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 3.dp))
        }
    }
}

@Composable
internal fun StageNewsCard(n: StageNewsItem, cat: StageCategory, onClick: () -> Unit) {
    Column(Modifier.width(230.dp).clickable(onClick = onClick), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.width(230.dp).height(140.dp).clip(RoundedCornerShape(14.dp)).background(cat.bg), Alignment.Center) {
            Icon(cat.icon, null, tint = cat.fg, modifier = Modifier.size(34.dp))
            stageImageUrl(n.imageUrl)?.let { AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
        }
        val when_ = StageDate.short(n.eventDate)
        if (when_ != null) Text("일정 $when_", style = Bold, fontSize = 14.sp, color = Blue, modifier = Modifier.padding(top = 4.dp))
        else StageDate.short(n.publishedDate)?.let { Text("$it 게시", style = Regular, fontSize = 12.sp, color = MuyeonColors.textSub, modifier = Modifier.padding(top = 4.dp)) }
        Text(n.title, style = Semi, fontSize = 15.sp, color = MuyeonColors.textHead, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(n.summary, style = Regular, fontSize = 12.sp, color = MuyeonColors.textSub, maxLines = 2, overflow = TextOverflow.Ellipsis)
        n.sourceName?.let { Text("$it · 원문 보기 ›", style = Regular, fontSize = 11.sp, color = MuyeonColors.textSub, maxLines = 1) }
    }
}
