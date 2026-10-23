package com.muyeon.app.ui.assistant

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.theme.AppColor
import com.muyeon.app.theme.customFontFamily
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 'AI로 쓴 글' — 내가 AI 로 쓴 초안 목록(최신순, 20개씩). 누르면 결과 화면으로 다시 열어 등록하거나 복사한다.
 *  iOS AssistantHistoryView 와 1:1. 서버 GET /assistant/drafts?before= (1년 보관).
 */
private data class HistoryRow(val draftId: String, val flowTitle: String, val preview: String, val createdAt: String, val saved: Boolean)

private val dateFmt = DateTimeFormatter.ofPattern("M월 d일 HH:mm")

private fun dateLabel(iso: String): String = runCatching {
    OffsetDateTime.parse(iso).atZoneSameInstant(ZoneId.of("Asia/Seoul")).format(dateFmt)
}.getOrDefault("")

@Composable
fun AssistantHistoryPage(onBack: () -> Unit, onOpenDraft: (String) -> Unit) {
    val context = LocalContext.current
    val rows = remember { mutableStateListOf<HistoryRow>() }
    var next by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var ended by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    suspend fun load(before: String?) {
        loading = true
        val path = "/assistant/drafts" + (before?.let { "?before=" + java.net.URLEncoder.encode(it, "UTF-8") } ?: "")
        val r = AssistantApi.call(context, "GET", path)
        val arr = r.json?.optJSONArray("items")
        for (i in 0 until (arr?.length() ?: 0)) {
            val o = arr!!.optJSONObject(i) ?: continue
            rows += HistoryRow(o.optString("draftId"), o.optString("flowTitle"), o.optString("preview"),
                o.optString("createdAt"), o.optString("outcome") == "SAVED")
        }
        next = r.json?.optString("next")?.takeIf { it.isNotEmpty() && it != "null" }
        ended = next == null
        loading = false
    }

    LaunchedEffect(Unit) { load(null) }
    // 끝 근처까지 내리면 다음 20개.
    val nearEnd by remember { derivedStateOf { (listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) >= rows.size - 3 } }
    LaunchedEffect(nearEnd, next) { if (nearEnd && !loading && !ended) load(next) }

    BackHandler { onBack() }
    Column(Modifier.fillMaxSize().background(Color(0xFFF8F9FB)).statusBarsPadding().navigationBarsPadding()) {
        Box(Modifier.fillMaxWidth().height(56.dp).background(Color.White)) {
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart).padding(start = 4.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로", tint = Color(0xFF101116))
            }
            Text("AI로 쓴 글", fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 17.sp,
                color = Color(0xFF101116), modifier = Modifier.align(Alignment.Center))
        }
        if (!loading && rows.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text("아직 AI로 쓴 글이 없어요", fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFF101116))
                Spacer(Modifier.height(8.dp))
                Text("AI 작성 도우미로 글을 쓰면 여기에 모여요.", fontFamily = customFontFamily, fontSize = 15.sp, color = Color(0xFF6D6E71))
            }
            return@Column
        }
        LazyColumn(state = listState, contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize().testTag("assistant.history")) {
            items(rows, key = { it.draftId }) { r ->
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color.White)
                    .clickable { onOpenDraft(r.draftId) }.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(r.flowTitle, fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = AppColor.Primary)
                        if (r.saved) {
                            Spacer(Modifier.width(8.dp))
                            Text("등록함", fontFamily = customFontFamily, fontSize = 12.sp, color = Color(0xFF6D6E71),
                                modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFFF2F3F5)).padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                        Spacer(Modifier.weight(1f))
                        Text(dateLabel(r.createdAt), fontFamily = customFontFamily, fontSize = 13.sp, color = Color(0xFFA8AAB0))
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(r.preview.ifEmpty { "(내용 없음)" }, fontFamily = customFontFamily, fontSize = 16.sp, lineHeight = 23.sp,
                        color = Color(0xFF101116), maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            if (loading) item { Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = AppColor.Primary) } }
        }
    }
}
