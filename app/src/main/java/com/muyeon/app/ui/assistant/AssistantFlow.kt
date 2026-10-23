package com.muyeon.app.ui.assistant

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.theme.AppColor
import com.muyeon.app.theme.customFontFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/**
 * AI 작성 도우미 — 단계형 질문 화면(10-23 '보험 찾기' 시안, 색은 무용연 주황). iOS AssistantFlowView 와 1:1.
 *  - 한 화면에 질문 하나: 진행 막대 · 뒤로(이전 질문, 서버 POST back) · 흐름 이름 / 큰 질문 / 선택지 / 아래 [다음](고르기 전 흐림),
 *    마지막은 [AI로 작성하기]. 선택지 모양은 서버 display(cards·grid·checklist·checkgrid·text).
 *  - 작성 중: 2초마다 GET jobs/:id. 결과: 칸별 카드 + [이어서 등록하기](초안 id 를 붙여 기존 등록 화면) · [다시 쓰기] · [복사하기].
 *  - 프로가 아니면(AI_PRO_REQUIRED) 안내 + [멤버십 보기]. 새 Activity·웹뷰 없음(같은 Dialog 안).
 */
private val Ink = Color(0xFF101116)
private val Sub = Color(0xFF6D6E71)
private val Line = Color(0xFFE3E4E8)
private val Peach = Color(0xFFFFF3EA)
private val Bg = Color(0xFFF8F9FB)

private data class FlowQuestion(
    val key: String, val ask: String, val type: String, val chips: List<String>, val required: Boolean,
    val max: Int?, val placeholder: String?, val display: String, val icons: Map<String, String>,
) {
    companion object {
        fun from(o: JSONObject?): FlowQuestion? = o?.let {
            val chips = it.optJSONArray("chips")
            val icons = it.optJSONObject("icons")
            FlowQuestion(
                it.optString("key"), it.optString("ask"), it.optString("type"),
                (0 until (chips?.length() ?: 0)).map { i -> chips!!.optString(i) }, it.optBoolean("required"),
                it.optInt("max").takeIf { m -> m > 0 }, it.optString("placeholder").takeIf { p -> p.isNotEmpty() && p != "null" },
                it.optString("display", "cards"),
                icons?.keys()?.asSequence()?.associateWith { k -> icons.optString(k) } ?: emptyMap(),
            )
        }
    }
}

private enum class Phase { LOADING, ASK, GENERATING, RESULT, LOCKED, ERROR }

@Composable
fun AssistantFlowPage(
    flowId: String, title: String, onClose: () -> Unit, onOpen: (AssistantItem) -> Unit,
    /** 지난 초안 다시 보기('AI로 쓴 글' 목록·완료 푸시) — 질문 없이 결과 화면만. 수정하기·다시 쓰기는 없다. */
    draftId: String? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var phase by remember { mutableStateOf(Phase.LOADING) }
    var sessionId by remember { mutableIntStateOf(0) }
    var q by remember { mutableStateOf<FlowQuestion?>(null) }
    var done by remember { mutableIntStateOf(0) }
    var total by remember { mutableIntStateOf(1) }
    var picked by remember { mutableStateOf<List<String>>(emptyList()) }
    var text by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var draft by remember { mutableStateOf<JSONObject?>(null) }
    // 반복 질문 줄이기 — 서버가 지난 답으로 미리 채운 질문들([바꾸기]로 다시 물음)과 뒤로 버튼이 서버에 물을지.
    var known by remember { mutableStateOf<List<Triple<String, String, String>>>(emptyList()) }
    var canBack by remember { mutableStateOf(false) }

    fun applyQuestion(o: JSONObject?, previous: Any? = null) {
        q = FlowQuestion.from(o?.optJSONObject("question"))
        o?.optJSONObject("progress")?.let { done = it.optInt("done"); total = maxOf(1, it.optInt("total")) }
        picked = when (previous) { is JSONArray -> (0 until previous.length()).map { previous.optString(it) }; is String -> if (q?.type == "text") emptyList() else listOf(previous); else -> emptyList() }
        text = if (q?.type == "text" && previous is String) previous else ""
        val k = o?.optJSONArray("known")
        known = (0 until (k?.length() ?: 0)).mapNotNull { k!!.optJSONObject(it) }.map { Triple(it.optString("key"), it.optString("label"), it.optString("value")) }
        canBack = o?.optBoolean("canBack") ?: false
        phase = Phase.ASK
    }

    fun fail(r: AssistantApi.Res) {
        busy = false
        when (r.code) {
            "AI_PRO_REQUIRED", "AI_QUOTA_EXCEEDED" -> { errorMsg = r.message; phase = Phase.LOCKED }
            else -> { errorMsg = r.message ?: "잠시 후 다시 시도해 주세요."; if (phase == Phase.LOADING) phase = Phase.ERROR else Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show() }
        }
    }

    suspend fun waitJob(jobId: String) {
        phase = Phase.GENERATING
        repeat(60) {
            delay(2000)
            val r = AssistantApi.call(context, "GET", "/assistant/jobs/$jobId")
            val st = r.json?.optString("status")
            if (st == "DONE") { draft = r.json?.optJSONObject("draft"); phase = Phase.RESULT; busy = false; return }
            if (st == "FAILED") {
                busy = false; phase = Phase.ASK
                val code = r.json?.optString("errorCode")
                Toast.makeText(context, if (code == "MODERATION") "입력하신 내용으로는 글을 만들 수 없어요. 내용을 바꿔서 다시 시도해 주세요." else "글을 만들지 못했어요. 이용 횟수는 그대로예요. 다시 시도해 주세요.", Toast.LENGTH_LONG).show()
                return
            }
        }
        busy = false; phase = Phase.ASK
        Toast.makeText(context, "생각보다 오래 걸리고 있어요. 완료되면 알림으로 알려 드릴게요.", Toast.LENGTH_LONG).show()
    }

    suspend fun generate() {
        busy = true
        val r = AssistantApi.call(context, "POST", "/assistant/sessions/$sessionId/generate")
        if (!r.ok) return fail(r)
        waitJob(r.json?.optString("jobId").orEmpty())
    }

    // 이 화면을 보는 동안은 완료 푸시를 띄우지 않는다(AssistantOverlay.isWatchingFlow).
    DisposableEffect(Unit) {
        AssistantOverlay.flowOnScreen = true
        onDispose { AssistantOverlay.flowOnScreen = false }
    }
    var pageTitle by remember { mutableStateOf(title) }

    var resumeOffer by remember { mutableStateOf<JSONObject?>(null) } // 이어서 작성할 대화
    var showExit by remember { mutableStateOf(false) }                // 나가기 확인 팝업

    suspend fun startNew() {
        val r = AssistantApi.call(context, "POST", "/assistant/sessions", JSONObject().put("flow", flowId))
        if (!r.ok) return fail(r)
        sessionId = r.json?.optInt("sessionId") ?: 0
        applyQuestion(r.json)
    }

    LaunchedEffect(flowId, draftId) {
        if (draftId != null) {
            val r = AssistantApi.call(context, "GET", "/assistant/drafts/$draftId")
            if (!r.ok) return@LaunchedEffect fail(r)
            draft = r.json
            r.json?.optString("flowTitle")?.takeIf { it.isNotEmpty() && it != "null" }?.let { pageTitle = it }
            phase = Phase.RESULT
            return@LaunchedEffect
        }
        // 7일 안에 답하다 나간 대화가 있으면 [이어서 작성하기]/[새로 작성하기]를 묻는다.
        val res = AssistantApi.call(context, "GET", "/assistant/sessions/resume?flow=$flowId")
        val prev = res.json?.optJSONObject("session")
        if (res.ok && prev != null) { resumeOffer = prev; return@LaunchedEffect }
        startNew()
    }

    fun next() {
        val cur = q ?: return
        if (busy) return
        busy = true
        scope.launch {
            val body = JSONObject().put("questionKey", cur.key)
            when {
                cur.type == "text" && text.isBlank() -> body.put("skip", true)
                cur.type == "text" -> body.put("value", text.trim())
                cur.type == "multi" -> body.put("value", JSONArray(picked)).put("fromChip", true)
                else -> body.put("value", picked.firstOrNull()).put("fromChip", true)
            }
            val r = AssistantApi.call(context, "POST", "/assistant/sessions/$sessionId/answers", body)
            if (!r.ok) return@launch fail(r)
            if (r.json?.optString("status") == "READY") { done = total; generate() } else { busy = false; applyQuestion(r.json) }
        }
    }

    fun back() {
        when (phase) {
            Phase.ASK -> if (!canBack) onClose() else if (!busy) {
                busy = true
                scope.launch {
                    val r = AssistantApi.call(context, "POST", "/assistant/sessions/$sessionId/back")
                    busy = false
                    if (r.ok) applyQuestion(r.json, r.json?.opt("previous")) else fail(r)
                }
            }
            Phase.RESULT -> if (draftId != null) onClose() else scope.launch { // 수정하기 = 마지막 질문으로
                val r = AssistantApi.call(context, "POST", "/assistant/sessions/$sessionId/back")
                if (r.ok) applyQuestion(r.json, r.json?.opt("previous"))
            }
            Phase.GENERATING -> Unit
            else -> onClose()
        }
    }

    fun reask(key: String) {
        if (busy) return
        busy = true
        scope.launch {
            val r = AssistantApi.call(context, "POST", "/assistant/sessions/$sessionId/reask", JSONObject().put("key", key))
            busy = false
            if (r.ok) applyQuestion(r.json, r.json?.opt("previous")) else fail(r)
        }
    }

    BackHandler { back() }

    if (showExit) com.muyeon.app.ui.common.WriteExitDialog(
        onKeepAndLeave = onClose,
        onDeleteAndLeave = {
            val id = sessionId
            scope.launch { AssistantApi.call(context, "POST", "/assistant/sessions/$id/abandon") }
            onClose()
        },
        onDismiss = { showExit = false },
    )
    resumeOffer?.let { prev ->
        com.muyeon.app.ui.common.MuyeonDialog(
            title = "작성하던 내용이 있어요",
            message = "이전에 답한 내용에 이어서 작성할 수 있어요.",
            actions = listOf(
                com.muyeon.app.ui.common.MuyeonDialogAction("이어서 작성하기", com.muyeon.app.ui.common.MuyeonDialogKind.PRIMARY) {
                    sessionId = prev.optInt("sessionId")
                    if (prev.optString("status") == "READY" || prev.optJSONObject("question") == null) {
                        prev.optJSONObject("progress")?.let { done = it.optInt("done"); total = maxOf(1, it.optInt("total")) }
                        scope.launch { generate() }
                    } else applyQuestion(prev)
                },
                com.muyeon.app.ui.common.MuyeonDialogAction("새로 작성하기", com.muyeon.app.ui.common.MuyeonDialogKind.CANCEL) {
                    val old = prev.optInt("sessionId")
                    scope.launch {
                        AssistantApi.call(context, "POST", "/assistant/sessions/$old/abandon")
                        startNew()
                    }
                },
            ),
            force = true, // 둘 중 하나는 골라야 화면이 시작된다
            onDismiss = { resumeOffer = null },
        )
    }

    Column(Modifier.fillMaxSize().background(if (phase == Phase.RESULT) Bg else Color.White).statusBarsPadding().navigationBarsPadding().imePadding()) {
        // 진행 막대
        val progress by animateFloatAsState(if (phase == Phase.RESULT || phase == Phase.GENERATING) 1f else done.toFloat() / total, label = "p")
        Box(Modifier.fillMaxWidth().height(3.dp).background(Color(0xFFF2F3F5))) {
            Box(Modifier.fillMaxWidth(progress).fillMaxHeight().background(AppColor.Primary))
        }
        Box(Modifier.fillMaxWidth().height(56.dp)) {
            IconButton(onClick = { back() }, modifier = Modifier.align(Alignment.CenterStart).padding(start = 4.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로", tint = Ink)
            }
            Text(pageTitle, fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = Ink, modifier = Modifier.align(Alignment.Center))
            // 어느 단계에서든 한 번에 나간다. 직접 답한 게 있으면 나가기 확인(작성 내용 유지/삭제), 만드는 중이면 그냥 닫는다(생성은 계속·완료 알림).
            if (phase != Phase.LOADING) {
                IconButton(
                    onClick = { if (phase == Phase.ASK && canBack) showExit = true else onClose() },
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 4.dp).testTag("assistant.flow.close"),
                ) { Icon(Icons.Filled.Close, "닫기", tint = Ink) }
            }
        }
        when (phase) {
            Phase.LOADING -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = AppColor.Primary) }
            Phase.ERROR -> Message("AI 도우미를 열지 못했어요", errorMsg ?: "잠시 후 다시 시도해 주세요.", "닫기", onClose)
            Phase.LOCKED -> Message("AI 작성 도우미는\n프로 멤버십 혜택이에요", errorMsg ?: "프로 멤버십에 가입하면 이력서, 레슨 소개, 공고를 AI가 작성해 드려요.", "멤버십 보기") {
                onOpen(AssistantItem("membership", "멤버십", null, "멤버십", "NATIVE", "openMembership", emptyMap(), "/pricing", null, false, emptyList(), emptyList()))
            }
            Phase.GENERATING -> Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                CircularProgressIndicator(color = AppColor.Primary, strokeWidth = 3.dp, modifier = Modifier.size(44.dp))
                Spacer(Modifier.height(20.dp))
                Text("AI가 ${withObject(title.removeSuffix(" 쓰기"))}\n만들고 있어요", fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 31.sp, color = Ink, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text("10초쯤 걸려요.\n앱을 닫아도 자동으로 만들어지고, 다 되면 알림으로 알려 드려요.", fontFamily = customFontFamily, fontSize = 15.sp, lineHeight = 22.sp, color = Sub, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 24.dp))
            }
            Phase.ASK -> q?.let { cur ->
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
                    if (known.isNotEmpty()) KnownBox(known.filter { it.first != cur.key }) { reask(it) }
                    Text(cur.ask, fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 36.sp, color = Ink, modifier = Modifier.padding(top = 20.dp, bottom = 8.dp))
                    Text(when (cur.type) { "multi" -> "여러 개 선택할 수 있어요"; "text" -> if (cur.required) "간단히 입력해 주세요" else "입력하지 않아도 돼요"; else -> "하나를 선택해 주세요" },
                        fontFamily = customFontFamily, fontSize = 15.sp, color = Sub, modifier = Modifier.padding(bottom = 24.dp))
                    Options(cur, picked, text, onPick = { picked = it }, onText = { text = it })
                    Spacer(Modifier.height(24.dp))
                }
                val last = done >= total - 1
                val enabled = !busy && when (cur.type) { "text" -> !cur.required || text.isNotBlank(); else -> picked.isNotEmpty() }
                val label = when { busy -> "잠시만요…"; cur.type == "text" && !cur.required && text.isBlank() -> if (last) "건너뛰고 AI로 작성하기" else "건너뛰기"; last -> "AI로 작성하기"; else -> "다음" }
                BottomButton(label, enabled || (cur.type == "text" && !cur.required && !busy)) { next() }
            }
            Phase.RESULT -> draft?.let { d ->
                if (draftId != null) ResultView(d, onOpen = onOpen, onRegenerate = null, onEdit = null)
                else ResultView(d, onOpen = onOpen, onRegenerate = { scope.launch { generate() } }, onEdit = { back() })
            }
        }
    }
}

/** 받침에 맞춰 을/를 — "레슨 소개를", "공지·취소 규정을". */
private fun withObject(word: String): String {
    val c = word.lastOrNull() ?: return word
    val hasFinal = c in '가'..'힣' && (c - '가') % 28 != 0
    return word + if (hasFinal) "을" else "를"
}

@Composable
private fun Options(q: FlowQuestion, picked: List<String>, text: String, onPick: (List<String>) -> Unit, onText: (String) -> Unit) {
    val multi = q.type == "multi"
    fun toggle(c: String) = onPick(if (multi) (if (c in picked) picked - c else picked + c) else listOf(c))
    when (q.display) {
        "text" -> Column {
            Box(Modifier.fillMaxWidth().heightIn(min = 160.dp).clip(RoundedCornerShape(16.dp)).border(1.dp, Line, RoundedCornerShape(16.dp)).padding(16.dp)) {
                if (text.isEmpty()) Text(q.placeholder ?: "내용을 입력해 주세요", fontFamily = customFontFamily, fontSize = 16.sp, color = Color(0xFFA8AAB0))
                BasicTextField(text, { if (it.length <= (q.max ?: 300)) onText(it) }, textStyle = TextStyle(fontFamily = customFontFamily, fontSize = 16.sp, color = Ink, lineHeight = 24.sp),
                    cursorBrush = SolidColor(AppColor.Primary), modifier = Modifier.fillMaxWidth().testTag("assistant.flow.text"))
            }
            Text("${text.length} / ${q.max ?: 300}", fontFamily = customFontFamily, fontSize = 13.sp, color = Sub, modifier = Modifier.align(Alignment.End).padding(top = 6.dp))
        }
        // 아이콘 없이 글자만(10-23 사용자 요청 — 어설픈 아이콘은 뺀다). 서버 icons 값은 쓰지 않는다.
        "grid" -> q.chips.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(bottom = 12.dp)) {
                row.forEach { c ->
                    val on = c in picked
                    Column(
                        Modifier.weight(1f).height(76.dp).clip(RoundedCornerShape(20.dp)).background(if (on) Peach else Color(0xFFF2F3F5))
                            .border(if (on) 2.dp else 0.dp, if (on) AppColor.Primary else Color.Transparent, RoundedCornerShape(20.dp)).clickable { toggle(c) },
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                    ) {
                        Text(c, fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 15.sp, color = Ink, textAlign = TextAlign.Center)
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        "checkgrid" -> q.chips.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 10.dp)) {
                row.forEach { c ->
                    val on = c in picked
                    Row(
                        Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).border(1.5.dp, if (on) AppColor.Primary else Line, RoundedCornerShape(14.dp))
                            .clickable { toggle(c) }.padding(horizontal = 14.dp, vertical = 15.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.Check, null, tint = if (on) AppColor.Primary else Color(0xFFD0D0D0), modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(c, fontFamily = customFontFamily, fontSize = 15.sp, color = if (on) AppColor.Primary else Ink, maxLines = 1)
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        else -> q.chips.forEach { c -> // cards · checklist
            val on = c in picked
            Row(
                Modifier.fillMaxWidth().padding(bottom = 12.dp).clip(RoundedCornerShape(20.dp)).background(if (on) Peach else Color.White)
                    .border(if (on) 2.dp else 1.dp, if (on) AppColor.Primary else Line, RoundedCornerShape(20.dp)).clickable { toggle(c) }
                    .padding(horizontal = 22.dp, vertical = if (q.display == "checklist") 20.dp else 24.dp).testTag("assistant.flow.chip.$c"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (q.display == "checklist") {
                    Icon(Icons.Filled.Check, null, tint = if (on) AppColor.Primary else Color(0xFFC4C6CC), modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(16.dp))
                }
                Text(c, fontFamily = customFontFamily, fontWeight = if (q.display == "cards") FontWeight.SemiBold else FontWeight.Normal, fontSize = 17.sp, color = Ink)
            }
        }
    }
}

/** '이미 알고 있어요' — 지난번 답으로 채운 질문. [바꾸기]를 누르면 그 질문을 다시 묻는다(iOS knownBox 와 같음). */
@Composable
private fun KnownBox(items: List<Triple<String, String, String>>, onChange: (String) -> Unit) {
    if (items.isEmpty()) return
    Column(Modifier.fillMaxWidth().padding(top = 16.dp).clip(RoundedCornerShape(14.dp)).background(Bg).padding(horizontal = 16.dp, vertical = 10.dp)) {
        Text("이전에 답한 내용으로 채웠어요", fontFamily = customFontFamily, fontSize = 13.sp, color = Sub, modifier = Modifier.padding(bottom = 4.dp))
        items.forEach { (key, label, value) ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text(label, fontFamily = customFontFamily, fontSize = 15.sp, color = Sub, modifier = Modifier.width(76.dp))
                Text(value, fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 15.sp, color = Ink, maxLines = 1, modifier = Modifier.weight(1f))
                Text("변경", fontFamily = customFontFamily, fontSize = 14.sp, color = AppColor.Primary,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { onChange(key) }.padding(horizontal = 8.dp, vertical = 4.dp).testTag("assistant.flow.change.$key"))
            }
        }
    }
}

@Composable
private fun BottomButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Text(
        label, fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color.White, textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp).clip(RoundedCornerShape(16.dp))
            .background(if (enabled) AppColor.Primary else AppColor.Primary.copy(alpha = 0.35f))
            .clickable(enabled = enabled, onClick = onClick).padding(vertical = 18.dp).testTag("assistant.flow.next"),
    )
}

@Composable
private fun Message(title: String, body: String, button: String, onClick: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).padding(horizontal = 24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(72.dp).clip(CircleShape).background(Peach), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.AutoAwesome, null, tint = AppColor.Primary, modifier = Modifier.size(34.dp))
            }
            Spacer(Modifier.height(20.dp))
            Text(title, fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 31.sp, color = Ink, textAlign = TextAlign.Center)
            Spacer(Modifier.height(10.dp))
            Text(body, fontFamily = customFontFamily, fontSize = 15.sp, lineHeight = 22.sp, color = Sub, textAlign = TextAlign.Center)
        }
        BottomButton(button, true, onClick)
    }
}

@Composable
private fun ResultView(d: JSONObject, onOpen: (AssistantItem) -> Unit, onRegenerate: (() -> Unit)?, onEdit: (() -> Unit)?) {
    val context = LocalContext.current
    val payload = d.optJSONObject("payload") ?: JSONObject()
    val fields = d.optJSONArray("fields")
    val open = d.optJSONObject("open")
    val draftId = d.optString("draftId")
    val kind = d.optString("kind")
    val sections = payload.optJSONObject("sections")
    fun valueOf(key: String): String {
        val v = sections?.opt(key) ?: payload.opt(key)
        return when (v) { is JSONArray -> (0 until v.length()).joinToString("\n") { "· " + v.optString(it) }; null -> ""; else -> v.toString() }
    }
    val allText = (0 until (fields?.length() ?: 0)).mapNotNull { i -> fields!!.optJSONObject(i) }
        .joinToString("\n\n") { "${it.optString("label")}\n${valueOf(it.optString("key"))}" }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Text("AI가 작성한 초안이에요", fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Ink, modifier = Modifier.padding(top = 16.dp))
            Text("등록하면서 원하는 대로 수정할 수 있어요.", fontFamily = customFontFamily, fontSize = 15.sp, color = Sub, modifier = Modifier.padding(top = 6.dp, bottom = 20.dp))
            (0 until (fields?.length() ?: 0)).mapNotNull { fields!!.optJSONObject(it) }.forEach { f ->
                val v = valueOf(f.optString("key"))
                if (v.isNotBlank()) Column(Modifier.fillMaxWidth().padding(bottom = 12.dp).clip(RoundedCornerShape(20.dp)).background(Color.White).padding(20.dp)) {
                    Text(f.optString("label"), fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = AppColor.Primary)
                    Spacer(Modifier.height(8.dp))
                    Text(v, fontFamily = customFontFamily, fontSize = 16.sp, lineHeight = 25.sp, color = Ink)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)) {
                onEdit?.let { SmallButton("수정하기", Modifier.weight(1f), it) }
                onRegenerate?.let { SmallButton("다시 만들기", Modifier.weight(1f), it) }
                SmallButton("복사하기", Modifier.weight(1f)) {
                    (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("AI 작성", allText))
                    Toast.makeText(context, "복사했어요", Toast.LENGTH_SHORT).show()
                }
            }
        }
        val action = open?.optString("nativeAction")?.takeIf { it.isNotEmpty() && it != "null" }
        val path = open?.optString("webPath")?.takeIf { it.isNotEmpty() && it != "null" }
        if (kind != "TEXT" && (action != null || path != null)) {
            val params = buildMap {
                open?.optJSONObject("nativeParams")?.let { p -> p.keys().forEach { put(it, p.optString(it)) } }
                put("aiDraftId", draftId)
            }
            val webPath = path?.let { it + (if ('?' in it) "&" else "?") + "aiDraft=$draftId" }
            BottomButton("이어서 등록하기", true) {
                onOpen(AssistantItem("ai.draft.$draftId", "등록", null, "AI 도우미", "NATIVE", action, params, webPath, null, false, emptyList(), emptyList()))
            }
        }
    }
}

@Composable
private fun SmallButton(label: String, modifier: Modifier, onClick: () -> Unit) {
    Text(label, fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 15.sp, color = Ink, textAlign = TextAlign.Center,
        modifier = modifier.clip(RoundedCornerShape(14.dp)).background(Color.White).border(1.dp, Line, RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(vertical = 14.dp))
}
