package com.muyeon.app.ui.chat

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.muyeon.app.ui.common.ImageViewerActivity
import com.muyeon.app.ui.quote.QuoteUi
import org.json.JSONArray

/**
 * 채팅 이미지 말풍선 — iOS `ChatImageBubble` 1:1.
 *  1장은 200x200, 여러 장은 카톡식 행배치(최대 10장). 탭하면 풀스크린 뷰어.
 *  저장은 **내가 보낸 사진만** 허용한다(iOS viewerAllowsSaving = isMine).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatImageBubble(
    urls: List<String>,
    allowsSaving: Boolean,
    onLongPress: () -> Unit,
) {
    if (urls.isEmpty()) return
    val ctx = LocalContext.current
    val shown = urls.take(10)

    fun open(index: Int) {
        val raw = JSONArray(shown).toString()
        ImageViewerActivity.start(ctx, raw, index, allowsSaving)
    }

    @Composable
    fun cell(url: String, index: Int, side: androidx.compose.ui.unit.Dp, corner: Int = 0) {
        AsyncImage(
            QuoteUi.imageUrl(url), null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(side)
                .then(if (corner > 0) Modifier.clip(RoundedCornerShape(corner.dp)) else Modifier)
                .background(Color(0xFFF2F2F7))
                .combinedClickable(onClick = { open(index) }, onLongClick = onLongPress),
        )
    }

    if (shown.size == 1) {
        cell(shown[0], 0, 200.dp, corner = 12)
        return
    }

    val gap = 2.dp
    val gridWidth = 240.dp
    var start = 0
    Column(
        Modifier.width(gridWidth).clip(RoundedCornerShape(12.dp)),
        verticalArrangement = Arrangement.spacedBy(gap),
    ) {
        rowConfig(shown.size).forEach { cols ->
            val side = (gridWidth - gap * (cols - 1)) / cols
            val rowStart = start
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                (0 until cols).forEach { c ->
                    val idx = rowStart + c
                    if (idx < shown.size) cell(shown[idx], idx, side)
                }
            }
            start += cols
        }
    }
}

/** 행 구성: 1[1] 2[2] 3[3] 4[2,2] 5[3,2] 6[3,3] 7[3,2,2] 8[3,3,2] 9[3,3,3] 10[4,3,3] */
private fun rowConfig(n: Int): List<Int> = when (n) {
    1 -> listOf(1)
    2 -> listOf(2)
    3 -> listOf(3)
    4 -> listOf(2, 2)
    5 -> listOf(3, 2)
    6 -> listOf(3, 3)
    7 -> listOf(3, 2, 2)
    8 -> listOf(3, 3, 2)
    9 -> listOf(3, 3, 3)
    else -> listOf(4, 3, 3)
}
