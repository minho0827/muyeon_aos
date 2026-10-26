package com.muyeon.app.ui.castingstage.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.ui.common.MuyeonColors
import com.muyeon.app.ui.castingstage.model.*
import com.muyeon.app.ui.castingstage.data.*
import com.muyeon.app.ui.castingstage.components.*

/** 무대 소식 상세(iOS StageNewsDetailView). */

@Composable
fun StageNewsDetailScreen(item: StageNewsItem, cat: StageCategory, onBack: () -> Unit, onOpenUrl: (String) -> Unit) {
    val info = listOf("칸" to cat.label, "일정" to StageDate.short(item.eventDate), "게시일" to StageDate.short(item.publishedDate), "출처" to item.sourceName)
        .mapNotNull { (k, v) -> v?.let { k to it } }
    StageDetailScaffold(
        title = item.title,
        shareText = "${item.title}\n${item.sourceUrl}",
        liked = null, onLike = null, onBack = onBack,
        sections = listOf(
            StageDetailSection("summary", "소식 요약") { StageExpandableText(item.summary) },
            StageDetailSection("source", "정보 출처") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    StageInfoRows(info)
                    Text("무용연이 웹에서 모아 원문을 확인한 소식이에요. 일정과 조건은 바뀔 수 있으니 원문을 꼭 확인해 주세요.", style = Regular, fontSize = 12.sp, color = MuyeonColors.textSub)
                }
            },
        ),
        header = {
            Column {
                StageHeroGallery(listOfNotNull(item.imageUrl), cat.icon, cat.fg, cat.bg)
                Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(cat.label, style = Semi, fontSize = 12.sp, color = cat.fg,
                        modifier = Modifier.clip(CircleShape).background(cat.bg).padding(horizontal = 10.dp, vertical = 4.dp))
                    Text(item.title, style = Bold, fontSize = 24.sp, color = MuyeonColors.textHead)
                    item.sourceName?.let { Text(it, style = Regular, fontSize = 16.sp, color = MuyeonColors.textSub) }
                }
                StageDate.short(item.eventDate)?.let { Box(Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp)) { StageInfoRows(listOf("일정" to it)) } }
            }
        },
        bottom = { StageBottomButton("공식 원문 확인") { onOpenUrl(item.sourceUrl) } },
    )
}

// MARK: 목록
