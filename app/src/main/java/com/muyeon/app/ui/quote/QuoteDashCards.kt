package com.muyeon.app.ui.quote

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.muyeon.app.theme.AppColor
import com.muyeon.app.theme.customFontFamily
import org.json.JSONArray

/**
 * '내 레슨 요청' 화면 카드 격자(2026-10-23 모니모 '자산' 시안, 색은 무용연 주황) — iOS QuoteDashCards.swift 와 1:1.
 *  - 2열 흰 카드: 굵은 제목 + 둘째 줄(실제 숫자면 주황, 없으면 할 일 문구) + 왼쪽 아래 옅은 원 아이콘.
 *  - [순서 편집] → 위아래 화살표·기본값으로. 바꾼 채 나가면 "변경된 목록을 저장할까요?", 저장하면 "저장했어요".
 *  - 순서는 기기에 저장(역할별 키). 카드가 늘면 저장된 순서 뒤에 자동으로 붙는다.
 */
data class QuoteDashCard(
    val id: String,
    val title: String,
    val line2: String,
    val isValue: Boolean,
    val icon: ImageVector,
    val dot: Boolean = false,
    val action: () -> Unit,
)

private val Peach = Color(0xFFFFF3EA)
private val Ink = Color(0xFF101116)
private val Sub = Color(0xFF6D6E71)
private val Line = Color(0xFFEAEAEA)

object QuoteDashCardOrder {
    private const val PREFS = "muyeon.quoteDash"
    private fun key(role: String) = "cardOrder.$role"
    fun load(c: Context, role: String): List<String> = runCatching {
        val a = JSONArray(c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(key(role), "[]"))
        (0 until a.length()).map { a.getString(it) }
    }.getOrDefault(emptyList())
    fun save(c: Context, role: String, ids: List<String>) =
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(key(role), JSONArray(ids).toString()).apply()
    fun reset(c: Context, role: String) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(key(role)).apply()

    /** 저장된 순서 → 남아 있는 카드만, 그 뒤에 새 카드. */
    fun apply(cards: List<QuoteDashCard>, saved: List<String>): List<QuoteDashCard> {
        val byId = cards.associateBy { it.id }
        val out = saved.mapNotNull { byId[it] }.toMutableList()
        cards.forEach { c -> if (out.none { it.id == c.id }) out.add(c) }
        return out
    }
}

@Composable
fun QuoteDashCardGrid(role: String, cards: List<QuoteDashCard>) {
    val context = LocalContext.current
    var saved by remember { mutableStateOf(QuoteDashCardOrder.load(context, role)) }
    var editing by remember { mutableStateOf(false) }
    val ordered = QuoteDashCardOrder.apply(cards, saved)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ordered.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.height(IntrinsicSize.Max)) {
                row.forEach { c -> CardView(c, Modifier.weight(1f).fillMaxHeight()) }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        Text(
            "순서 편집", fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 15.sp, color = Ink,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.White)
                .border(1.dp, Line, RoundedCornerShape(14.dp)).clickable { editing = true }.padding(vertical = 14.dp),
        )
    }
    if (editing) {
        QuoteDashCardEditor(ordered, onReset = {
            QuoteDashCardOrder.reset(context, role); saved = emptyList(); editing = false
        }) { next ->
            if (next != null) { QuoteDashCardOrder.save(context, role, next); saved = next }
            editing = false
        }
    }
}

@Composable
private fun CardView(c: QuoteDashCard, modifier: Modifier) {
    Column(
        modifier.heightIn(min = 150.dp).shadow(6.dp, RoundedCornerShape(20.dp), ambientColor = Color(0x14000000), spotColor = Color(0x14000000))
            .clip(RoundedCornerShape(20.dp)).background(Color.White).clickable(onClick = c.action)
            .padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 16.dp).testTag("quoteDash.card.${c.id}"),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(c.title, fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 20.sp, color = Ink, maxLines = 2)
            if (c.dot) { Spacer(Modifier.width(5.dp)); Box(Modifier.size(7.dp).clip(CircleShape).background(Color(0xFFFF3B30))) }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            c.line2, fontFamily = customFontFamily, fontWeight = if (c.isValue) FontWeight.SemiBold else FontWeight.Normal,
            fontSize = 16.sp, lineHeight = 21.sp, color = if (c.isValue) AppColor.Primary else Ink, maxLines = 2,
        )
        Spacer(Modifier.weight(1f).heightIn(min = 14.dp))
        Box(Modifier.size(46.dp).clip(CircleShape).background(Peach), contentAlignment = Alignment.Center) {
            Icon(c.icon, null, tint = AppColor.Primary, modifier = Modifier.size(22.dp))
        }
    }
}

/** 직접 설정 — 위아래 화살표로 순서, [저장]. 바꾼 채 뒤로 가면 저장 여부를 묻는다. onDone(null) = 저장 안 함. */
@Composable
private fun QuoteDashCardEditor(initial: List<QuoteDashCard>, onReset: () -> Unit, onDone: (List<String>?) -> Unit) {
    val context = LocalContext.current
    var cards by remember { mutableStateOf(initial) }
    var askLeave by remember { mutableStateOf(false) }
    val dirty = cards.map { it.id } != initial.map { it.id }
    fun save() { Toast.makeText(context, "저장했어요", Toast.LENGTH_SHORT).show(); onDone(cards.map { it.id }) }
    fun back() { if (dirty) askLeave = true else onDone(null) }
    fun move(i: Int, d: Int) {
        val j = i + d
        if (j !in cards.indices) return
        cards = cards.toMutableList().also { val t = it[i]; it[i] = it[j]; it[j] = t }
    }

    Dialog(onDismissRequest = { back() }, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        BackHandler { back() }
        Column(Modifier.fillMaxSize().background(Color(0xFFF8F9FB)).statusBarsPadding().navigationBarsPadding()) {
            Box(Modifier.fillMaxWidth().height(56.dp)) {
                IconButton(onClick = { back() }, modifier = Modifier.align(Alignment.CenterStart).padding(start = 4.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로", tint = Ink)
                }
                Text("메뉴 순서 편집", fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = Ink, modifier = Modifier.align(Alignment.Center))
                Text("기본값으로", fontFamily = customFontFamily, fontSize = 14.sp, color = Sub,
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 16.dp).clickable(onClick = onReset))
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
                Text("자주 쓰는 메뉴를\n위로 올려 보세요", fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 33.sp, color = Ink, modifier = Modifier.padding(top = 12.dp))
                Text("정한 순서대로 메뉴를 보여드려요.", fontFamily = customFontFamily, fontSize = 15.sp, color = Sub, modifier = Modifier.padding(top = 8.dp, bottom = 20.dp))
                cards.forEachIndexed { i, c ->
                    Row(
                        Modifier.fillMaxWidth().padding(bottom = 12.dp).shadow(4.dp, RoundedCornerShape(20.dp), ambientColor = Color(0x14000000), spotColor = Color(0x14000000))
                            .clip(RoundedCornerShape(20.dp)).background(Color.White).padding(start = 18.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(40.dp).clip(CircleShape).background(Peach), contentAlignment = Alignment.Center) {
                            Icon(c.icon, null, tint = AppColor.Primary, modifier = Modifier.size(19.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(c.title, fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Ink, modifier = Modifier.weight(1f))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(onClick = { move(i, -1) }, enabled = i > 0, modifier = Modifier.size(36.dp, 30.dp)) {
                                Icon(Icons.Filled.KeyboardArrowUp, "위로", tint = if (i > 0) Sub else Line)
                            }
                            Box(Modifier.width(24.dp).height(1.dp).background(Line))
                            IconButton(onClick = { move(i, 1) }, enabled = i < cards.lastIndex, modifier = Modifier.size(36.dp, 30.dp)) {
                                Icon(Icons.Filled.KeyboardArrowDown, "아래로", tint = if (i < cards.lastIndex) Sub else Line)
                            }
                        }
                    }
                }
            }
            Text(
                "저장", fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color.White,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp).clip(RoundedCornerShape(14.dp))
                    .background(AppColor.Primary).clickable { save() }.padding(vertical = 17.dp),
            )
        }
        if (askLeave) {
            AlertDialog(
                onDismissRequest = { askLeave = false },
                title = { Text("변경된 목록을 저장할까요?", fontFamily = customFontFamily, fontSize = 17.sp) },
                confirmButton = { TextButton(onClick = { askLeave = false; save() }) { Text("저장", color = AppColor.Primary) } },
                dismissButton = { TextButton(onClick = { askLeave = false; onDone(null) }) { Text("취소", color = Sub) } },
            )
        }
    }
}
