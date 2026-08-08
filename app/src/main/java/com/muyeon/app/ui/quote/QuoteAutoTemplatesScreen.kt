package com.muyeon.app.ui.quote

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.muyeon.app.BuildConfig
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors
import com.muyeon.app.utils.TokenManager
import com.muyeon.app.webview.NativeWebRoute
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

/**
 * 견적 자동응답 "메시지+금액 묶음" 관리 — iOS `QuoteAutoTemplatesView.swift` 1:1.
 *  배달의민족 '가게 요청사항/주소 설정' 패턴. 저장된 묶음을 라디오로 선택(활성) + 추가/편집/삭제.
 *  활성 묶음의 message+price 가 백엔드 chat_auto_messages 로 복사되어 자동견적 발송에 사용된다.
 *  웹 `openAutoQuoteTemplates` 브릿지로 진입.
 */

data class AutoQuoteTemplate(val id: Int, val title: String?, val message: String, val price: String?)

/** /api/chat/my/auto-quote — iOS AutoQuoteTemplatesViewModel 의 엔드포인트와 동일. */
class AutoQuoteApi(private val token: String?) {
    private val client = OkHttpClient()
    private val base = BuildConfig.API_BASE_URL + "/api/chat/my/auto-quote"

    private suspend fun fire(path: String, method: String, body: JSONObject? = null): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                val payload = when {
                    body != null -> body.toString().toRequestBody(JSON)
                    method != "GET" && method != "DELETE" -> "".toRequestBody(JSON)
                    else -> null
                }
                val req = Request.Builder().url(base + path).method(method, payload)
                    .apply { if (!token.isNullOrEmpty()) addHeader("Authorization", "Bearer $token") }
                    .apply { if (body != null) addHeader("Content-Type", "application/json") }
                    .build()
                client.newCall(req).execute().use { res ->
                    if (!res.isSuccessful) return@use null
                    res.body?.string().orEmpty()
                }
            }.getOrNull()
        }

    /** 활성 템플릿 id(설정 조회). */
    suspend fun activeId(): Int? =
        fire("", "GET")?.let { runCatching { JSONObject(it).intOrNull("activeTemplateId") }.getOrNull() }

    suspend fun templates(): List<AutoQuoteTemplate> =
        fire("/templates", "GET")?.let { text ->
            runCatching {
                JSONArray(text).map { o ->
                    AutoQuoteTemplate(
                        o.optInt("id"), o.stringOrNull("title"),
                        o.optString("message"), o.stringOrNull("price"),
                    )
                }
            }.getOrNull()
        } ?: emptyList()

    suspend fun activate(id: Int): Boolean = fire("/templates/$id/activate", "POST") != null
    suspend fun remove(id: Int): Boolean = fire("/templates/$id", "DELETE") != null
    suspend fun save(id: Int?, title: String, message: String, price: String): Boolean {
        val body = JSONObject().put("title", title).put("message", message).put("price", price)
        return if (id != null) fire("/templates/$id", "PUT", body) != null
        else fire("/templates", "POST", body) != null
    }

    private companion object { val JSON = "application/json".toMediaType() }
}

class QuoteAutoTemplatesActivity : ComponentActivity() {
    companion object {
        fun start(context: Context) {
            val i = Intent(context, QuoteAutoTemplatesActivity::class.java)
            if (context !is Activity) i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(i)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val api = remember { AutoQuoteApi(TokenManager.getAccessToken(this)) }
            QuoteAutoTemplatesScreen(
                api = api,
                // 닫힘 콜백 __onAutoQuoteChanged 로 웹 요약(자동응답 켜짐/꺼짐)을 갱신.
                //  웹에 핸들러가 없으면 no-op (iOS presentAutoQuoteTemplates 동일).
                onClose = {
                    NativeWebRoute.notifyWebAndFinish(
                        this,
                        "if(window.__onAutoQuoteChanged){ window.__onAutoQuoteChanged(); }",
                    )
                },
            )
        }
    }
}

/** 편집 시트 상태 — id null = 신규(iOS EditorState). */
private data class EditorState(val id: Int?, val title: String, val message: String, val price: String)

@Composable
fun QuoteAutoTemplatesScreen(api: AutoQuoteApi, onClose: () -> Unit) {
    var templates by remember { mutableStateOf<List<AutoQuoteTemplate>>(emptyList()) }
    var activeId by remember { mutableStateOf<Int?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<EditorState?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun load() {
        activeId = api.activeId()
        templates = api.templates()
        isLoading = false
    }

    LaunchedEffect(Unit) { load() }

    Column(Modifier.fillMaxSize().background(MuyeonColors.surface)) {
        // topBar — 제목 17 bold 가운데, 좌측 X 17 semibold
        Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), contentAlignment = Alignment.Center) {
            Text(
                "견적 자동응답",
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 17.sp,
                lineHeight = 20.sp, color = MuyeonColors.textHead,
            )
            Icon(
                Icons.Filled.Close, "닫기", tint = MuyeonColors.textHead,
                modifier = Modifier.align(Alignment.CenterStart).size(17.dp).clickable(onClick = onClose),
            )
        }
        HorizontalDivider(color = MuyeonColors.border)

        if (isLoading) {
            Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                CircularProgressIndicator(color = MuyeonColors.primary)
            }
        } else {
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    "상황에 맞는 금액·메시지를 저장해두고, 하나를 선택하면 자동 견적에 그대로 사용돼요.",
                    fontFamily = customFontFamily, fontSize = 13.sp, lineHeight = 16.sp,
                    color = MuyeonColors.secondary,
                    modifier = Modifier.padding(horizontal = 20.dp).padding(top = 16.dp),
                )

                if (templates.isEmpty()) {
                    Column(
                        Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Outlined.ChatBubbleOutline, null, tint = MuyeonColors.secondary, modifier = Modifier.size(34.dp))
                        Text(
                            "저장된 자동응답이 없어요.\n아래에서 추가해보세요.",
                            fontFamily = customFontFamily, fontSize = 14.sp, lineHeight = 17.sp,
                            color = MuyeonColors.secondary, textAlign = TextAlign.Center,
                        )
                    }
                } else {
                    templates.forEach { t ->
                        TemplateRow(
                            t = t,
                            isActive = activeId == t.id,
                            onActivate = { scope.launch { busy = true; if (api.activate(t.id)) load(); busy = false } },
                            onEdit = { editing = EditorState(t.id, t.title ?: "", t.message, t.price ?: "") },
                            onDelete = { scope.launch { busy = true; if (api.remove(t.id)) load(); busy = false } },
                        )
                    }
                }

                Row(
                    Modifier
                        .padding(horizontal = 20.dp).padding(top = 4.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, MuyeonColors.primary.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .clickable { editing = EditorState(null, "", "", "") }
                        .padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Add, null, tint = MuyeonColors.primary, modifier = Modifier.size(15.dp))
                    Text(
                        "새 메시지 추가",
                        fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                        lineHeight = 18.sp, color = MuyeonColors.primary,
                    )
                }
            }
        }

        // bottomBar — 완료
        HorizontalDivider(color = MuyeonColors.border)
        Text(
            "완료",
            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp,
            lineHeight = 19.sp, color = Color.White, textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MuyeonColors.primary)
                .clickable(onClick = onClose)
                .padding(vertical = 15.dp),
        )
    }

    editing?.let { st ->
        AutoQuoteEditor(
            state = st,
            busy = busy,
            onSave = { title, message, price ->
                scope.launch {
                    busy = true
                    if (api.save(st.id, title, message, price)) { load(); editing = null }
                    busy = false
                }
            },
            onCancel = { editing = null },
        )
    }
}

/** 배민 '주소 설정' 카드 — 라디오(활성) + 금액 강조 + 메시지 + 편집/삭제. iOS row(). */
@Composable
private fun TemplateRow(
    t: AutoQuoteTemplate,
    isActive: Boolean,
    onActivate: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFF2F2F7))
            .then(if (isActive) Modifier.border(1.5.dp, MuyeonColors.primary.copy(alpha = 0.5f), RoundedCornerShape(14.dp)) else Modifier)
            .clickable(onClick = onActivate)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            if (isActive) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
            null,
            tint = if (isActive) MuyeonColors.primary else Color(0xFFC7C7CC),
            modifier = Modifier.padding(top = 2.dp).size(22.dp),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                val hasPrice = !t.price.isNullOrEmpty()
                Text(
                    if (hasPrice) t.price!! else "금액 미입력",
                    fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                    lineHeight = 18.sp, color = if (hasPrice) MuyeonColors.textHead else MuyeonColors.secondary,
                )
                if (isActive) {
                    Text(
                        "사용 중",
                        fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 11.sp,
                        lineHeight = 13.sp, color = MuyeonColors.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(MuyeonColors.primary.copy(alpha = 0.12f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
            Text(
                t.message,
                fontFamily = customFontFamily, fontSize = 13.sp, lineHeight = 16.sp, color = MuyeonColors.secondary,
            )
            Row(Modifier.padding(top = 2.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "편집",
                    fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp,
                    lineHeight = 16.sp, color = MuyeonColors.primary,
                    modifier = Modifier.clickable(onClick = onEdit),
                )
                Text(
                    "삭제",
                    fontFamily = customFontFamily, fontSize = 13.sp, lineHeight = 16.sp, color = MuyeonColors.secondary,
                    modifier = Modifier.clickable(onClick = onDelete),
                )
            }
        }
    }
}

/** 추가/편집 — iOS AutoQuoteEditor(이름/금액/메시지 300자). */
@Composable
private fun AutoQuoteEditor(
    state: EditorState,
    busy: Boolean,
    onSave: (String, String, String) -> Unit,
    onCancel: () -> Unit,
) {
    var title by remember { mutableStateOf(state.title) }
    var message by remember { mutableStateOf(state.message) }
    var price by remember { mutableStateOf(state.price) }
    val canSave = !busy && message.trim().isNotEmpty()

    Dialog(onDismissRequest = onCancel) {
        Column(
            Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(MuyeonColors.surface)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text(
                if (state.id == null) "새 메시지" else "메시지 편집",
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 17.sp,
                lineHeight = 20.sp, color = MuyeonColors.textHead,
            )
            EditorField("이름 (선택)") {
                OutlinedTextField(
                    value = title, onValueChange = { title = it }, singleLine = true,
                    placeholder = { Text("예: 기본 인사, 주말 전용", fontFamily = customFontFamily, fontSize = 14.sp) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            EditorField("자동 견적 금액") {
                OutlinedTextField(
                    value = price, onValueChange = { price = it }, singleLine = true,
                    placeholder = { Text("예: 회당 60,000원", fontFamily = customFontFamily, fontSize = 14.sp) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            EditorField("자동 응답 메시지") {
                Column {
                    OutlinedTextField(
                        value = message,
                        onValueChange = { if (it.length <= 300) message = it },
                        placeholder = {
                            Text(
                                "예: 안녕하세요! 발레 전문 강사입니다. 편하게 상담 도와드릴게요 :)",
                                fontFamily = customFontFamily, fontSize = 14.sp,
                            )
                        },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                    )
                    Text(
                        "${message.length}/300",
                        fontFamily = customFontFamily, fontSize = 12.sp, lineHeight = 14.sp,
                        color = MuyeonColors.secondary, textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "취소",
                    fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                    lineHeight = 18.sp, color = MuyeonColors.textSub, textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, MuyeonColors.border, RoundedCornerShape(12.dp))
                        .clickable(onClick = onCancel).padding(vertical = 13.dp),
                )
                Text(
                    if (state.id == null) "추가" else "저장",
                    fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                    lineHeight = 18.sp, color = Color.White, textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (canSave) MuyeonColors.primary else Color.Gray.copy(alpha = 0.4f))
                        .clickable(enabled = canSave) { onSave(title.trim(), message.trim(), price.trim()) }
                        .padding(vertical = 13.dp),
                )
            }
        }
    }
}

@Composable
private fun EditorField(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            label,
            fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
            lineHeight = 17.sp, color = MuyeonColors.textHead,
        )
        content()
    }
}
