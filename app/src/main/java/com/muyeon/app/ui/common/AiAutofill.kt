package com.muyeon.app.ui.common

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.BuildConfig
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.quote.QuoteDialog
import com.muyeon.app.utils.TokenManager
import com.muyeon.app.webview.withActiveType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * ★ 2026-11-02 AI 이미지 자동입력(1인 1회 무료) — 웹 components/muyeon/AiAutofillCard.js 와 같은 계약.
 *  사진(최대 3장) → /uploads/image → POST /ai-autofill/{job|profile} → draft 로 화면 칸을 한 번에 채운다.
 *  저장은 하지 않는다 — 사용자가 확인 후 기존 [등록]/[저장] 버튼으로.
 */
enum class AiAutofillKind(val path: String) { JOB("job"), PROFILE("profile") }

/** ★ 2026-11-02 서버 에러 { code, message } — message 는 그대로 사용자 문구. */
class AiAutofillException(val code: String?, message: String) : IllegalStateException(message) {
    /** 1회 체험을 이미 썼거나 스위치가 꺼짐 → 카드를 거둔다. */
    val hidesCard: Boolean get() = code == "AI_TRIAL_USED" || code == "AI_DISABLED"
}

/**
 * ★ 2026-11-02 인증 처리 — 다른 네이티브 API 와 같은 규약으로 맞춘다.
 *  - 토큰은 컴포지션 때 잡아 둔 값이 아니라 **요청 직전마다** TokenManager(앱 공용 토큰 저장소)에서 새로 읽는다.
 *  - X-Active-Type 헤더는 공용 확장 [withActiveType] 으로 붙인다(CLAUDE.md 규약).
 *  - 앱엔 공용 OkHttpClient·Authenticator·토큰 재발급 API 가 없다(로그인/토큰 저장은 웹이 AndroidToken.saveToken 으로 한다).
 *    그래서 401 이면: 그 사이 웹이 저장해 둔 새 토큰이 있으면(요청 때와 다르면) 그 토큰으로 **한 번만** 재시도하고,
 *    없으면 "로그인이 만료됐어요. 다시 로그인해 주세요." 로 안내한다.
 */
class AiAutofillApi(context: android.content.Context) {
    private val appContext = context.applicationContext
    private val apiBase = BuildConfig.API_BASE_URL + "/api"
    // ★ 2026-11-02 AI 판독이 20~40초 걸려 기본 10초 타임아웃으론 끊긴다 — 넉넉히 90초.
    private val client = sharedClient

    private fun currentToken(): String? = TokenManager.getAccessToken(appContext)?.takeIf { it.isNotEmpty() }

    private fun Request.Builder.authed(token: String?): Request.Builder = apply {
        if (!token.isNullOrEmpty()) header("Authorization", "Bearer $token")
    }.withActiveType()

    /** 요청 실행 — 401 이면 새 토큰이 저장돼 있을 때만 1회 재시도. 응답(상태코드, 본문) 반환. */
    private fun execute(build: (Request.Builder) -> Request.Builder): Pair<Int, String> {
        val first = currentToken()
        val (code, text) = client.newCall(build(Request.Builder()).authed(first).build()).execute()
            .use { it.code to it.body?.string().orEmpty() }
        if (code != 401) return code to text
        val fresh = currentToken()
        if (fresh == null || fresh == first) return code to text
        return client.newCall(build(Request.Builder()).authed(fresh).build()).execute()
            .use { it.code to it.body?.string().orEmpty() }
    }

    /** GET /ai-autofill/config → 이 종류 카드를 보일지(enabled && !used). 실패하면 false(숨김). */
    suspend fun available(kind: AiAutofillKind): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            if (currentToken() == null) return@runCatching false
            val (code, text) = execute { it.url("$apiBase/ai-autofill/config").get() }
            if (code !in 200..299) return@runCatching false
            val c = JSONObject(text).optJSONObject(kind.path) ?: return@runCatching false
            c.optBoolean("enabled", false) && !c.optBoolean("used", false)
        }.getOrDefault(false)
    }

    /** POST /ai-autofill/{kind} { imageUrls } → draft. */
    suspend fun run(kind: AiAutofillKind, imageUrls: List<String>): Result<JSONObject> = withContext(Dispatchers.IO) {
        runCatching {
            if (currentToken() == null) throw AiAutofillException(AUTH_EXPIRED, AUTH_EXPIRED_MESSAGE)
            val bodyJson = JSONObject().put("imageUrls", JSONArray(imageUrls)).toString()
            val (code, text) = execute {
                it.url("$apiBase/ai-autofill/${kind.path}")
                    .post(bodyJson.toRequestBody("application/json".toMediaType()))
            }
            if (code == 401) throw AiAutofillException(AUTH_EXPIRED, AUTH_EXPIRED_MESSAGE)
            val obj = runCatching { JSONObject(text) }.getOrNull()
            if (code !in 200..299) {
                val msg = obj?.optJSONArray("message")
                    ?.let { arr -> (0 until arr.length()).joinToString("\n") { arr.optString(it) } }
                    ?.ifEmpty { null }
                    ?: obj?.optString("message")?.ifEmpty { null }
                throw AiAutofillException(
                    obj?.optString("code")?.ifEmpty { null },
                    msg ?: "AI가 지금 응답하지 않아요. 잠시 후 다시 시도해 주세요.",
                )
            }
            obj?.optJSONObject("draft") ?: throw AiAutofillException(null, "AI가 내용을 읽지 못했어요. 다른 사진으로 시도해 주세요.")
        }
    }

    companion object {
        const val AUTH_EXPIRED = "AUTH_EXPIRED"
        const val AUTH_EXPIRED_MESSAGE = "로그인이 만료됐어요. 다시 로그인해 주세요."
        private val sharedClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(90, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .callTimeout(100, TimeUnit.SECONDS)
                .build()
        }
    }
}

/**
 * ★ 2026-11-02 AI 자동입력 카드 — 첫 화면 맨 위. config 가 허용할 때만 그린다.
 *  [이미지 첨부하기] → 기존 다중 사진 선택기(최대 3장) → 기존 업로드([upload]) → AI 호출 → [onDraft].
 *  성공/1회 사용 완료/스위치 꺼짐이면 카드를 거둔다. 진행 중엔 버튼을 막아 중복 호출을 방지한다.
 */
@Composable
fun AiAutofillCard(
    kind: AiAutofillKind,
    upload: suspend (ByteArray) -> Result<String>,
    onDraft: (JSONObject) -> Unit,
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val api = remember { AiAutofillApi(ctx) }
    var available by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(kind) { available = api.available(kind) }

    val isJob = kind == AiAutofillKind.JOB
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(maxItems = 3)) { uris ->
        if (uris.isEmpty() || busy) return@rememberLauncherForActivityResult
        busy = true
        scope.launch {
            val urls = mutableListOf<String>()
            uris.take(3).forEach { u ->
                UploadImagePreparer.prepare(ctx, u)?.let { bytes -> upload(bytes).onSuccess { if (it.isNotEmpty()) urls += it } }
            }
            if (urls.isEmpty()) {
                errorMessage = "이미지 업로드에 실패했어요."
                busy = false
                return@launch
            }
            api.run(kind, urls)
                .onSuccess { draft ->
                    onDraft(draft)
                    available = false   // 1회 체험 — 쓰고 나면 카드를 거둔다.
                    Toast.makeText(
                        ctx,
                        if (isJob) "AI가 공고를 채웠어요. 급여·마감일 등 내용을 확인하고 등록해 주세요."
                        else "AI가 이력서를 채웠어요. 내용을 확인하고 저장해 주세요.",
                        Toast.LENGTH_LONG,
                    ).show()
                }
                .onFailure { e ->
                    if (e is AiAutofillException && e.hidesCard) available = false
                    errorMessage = e.message?.takeIf { e is AiAutofillException }
                        ?: "AI가 지금 응답하지 않아요. 잠시 후 다시 시도해 주세요."
                }
            busy = false
        }
    }

    if (available || busy) {
        // 근처 안내 박스(상세 이미지 잠금 카드)와 같은 회색 박스 + 위저드 기본 버튼 모양.
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(MuyeonColors.tileIdle).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                if (isJob) "✨ AI 간편공고 등록" else "✨ AI 간편 이력서 등록",
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                lineHeight = 20.sp, color = MuyeonColors.textHead,
            )
            // ★ 2026-11-02 사용자 요청 — 제목만, 설명 문구 없이
            // ★ 2026-11-02 사용자 요청 — 회전 로딩 + 문구(웹·iOS 와 같은 문구)
            if (busy) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MuyeonColors.primary)
                    Text(
                        if (isJob) "AI가 공고를 작성하고 있어요" else "AI가 이력서를 작성하고 있어요",
                        fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp,
                        lineHeight = 18.sp, color = MuyeonColors.primary,
                    )
                }
            }
            Text(
                if (busy) "작성 중…" else "이미지 첨부하기",
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 18.sp,
                color = Color.White, textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(if (busy) MuyeonColors.primary.copy(alpha = 0.5f) else MuyeonColors.primary)
                    .clickable(enabled = !busy) {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                    .padding(vertical = 14.dp),
            )
        }
    }

    errorMessage?.let { msg ->
        QuoteDialog("안내", msg, "확인", onConfirm = { errorMessage = null }, onDismiss = { errorMessage = null })
    }
}

/** ★ 2026-11-02 draft 헬퍼 — 빈 문자열/빈 배열 = 모름(기존 값 유지). */
fun JSONObject.aiText(key: String): String? = optString(key, "").trim().takeIf { it.isNotEmpty() && it != "null" }

fun JSONObject.aiList(key: String): List<String> {
    val arr = optJSONArray(key) ?: return emptyList()
    return (0 until arr.length()).mapNotNull { arr.optString(it).trim().ifEmpty { null } }
}
