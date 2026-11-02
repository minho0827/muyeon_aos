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

class AiAutofillApi(private val token: String?) {
    private val apiBase = BuildConfig.API_BASE_URL + "/api"
    // ★ 2026-11-02 AI 판독이 20~40초 걸려 기본 10초 타임아웃으론 끊긴다 — 넉넉히 90초.
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .callTimeout(100, TimeUnit.SECONDS)
        .build()

    /** GET /ai-autofill/config → 이 종류 카드를 보일지(enabled && !used). 실패하면 false(숨김). */
    suspend fun available(kind: AiAutofillKind): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder().url("$apiBase/ai-autofill/config").get()
                .apply { if (!token.isNullOrEmpty()) addHeader("Authorization", "Bearer $token") }
                .build()
            client.newCall(req).execute().use { res ->
                if (!res.isSuccessful) return@use false
                val c = JSONObject(res.body?.string().orEmpty()).optJSONObject(kind.path) ?: return@use false
                c.optBoolean("enabled", false) && !c.optBoolean("used", false)
            }
        }.getOrDefault(false)
    }

    /** POST /ai-autofill/{kind} { imageUrls } → draft. */
    suspend fun run(kind: AiAutofillKind, imageUrls: List<String>): Result<JSONObject> = withContext(Dispatchers.IO) {
        runCatching {
            val body = JSONObject().put("imageUrls", JSONArray(imageUrls)).toString()
                .toRequestBody("application/json".toMediaType())
            val req = Request.Builder().url("$apiBase/ai-autofill/${kind.path}").post(body)
                .addHeader("Content-Type", "application/json")
                .apply { if (!token.isNullOrEmpty()) addHeader("Authorization", "Bearer $token") }
                .build()
            client.newCall(req).execute().use { res ->
                val text = res.body?.string().orEmpty()
                val obj = runCatching { JSONObject(text) }.getOrNull()
                if (!res.isSuccessful) {
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
    token: String?,
    upload: suspend (ByteArray) -> Result<String>,
    onDraft: (JSONObject) -> Unit,
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val api = remember(token) { AiAutofillApi(token) }
    var available by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(kind, token) { available = api.available(kind) }

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
                if (isJob) "✨ 기존 공고 이미지로 AI 자동 작성 (1회 무료)" else "✨ 기존 이력서 이미지로 AI 자동 작성 (1회 무료)",
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                lineHeight = 20.sp, color = MuyeonColors.textHead,
            )
            Text(
                if (isJob) "다른 곳에 올렸던 채용공고 사진을 올리면 AI가 칸을 한 번에 채워 드려요."
                else "갖고 있는 이력서·프로필 캡처를 올리면 AI가 경력·학력·소개를 채워 드려요.",
                fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp,
                lineHeight = 18.sp, color = MuyeonColors.textSub,
            )
            if (busy) {
                Text(
                    "AI가 사진을 읽고 있어요. 20~40초 정도 걸려요…",
                    fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp,
                    lineHeight = 18.sp, color = MuyeonColors.primary,
                )
            }
            Text(
                if (busy) "읽는 중…" else "이미지 첨부하기",
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
