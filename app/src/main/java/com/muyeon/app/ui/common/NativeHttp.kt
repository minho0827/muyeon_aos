package com.muyeon.app.ui.common

import android.content.Context
import com.muyeon.app.BuildConfig
import com.muyeon.app.ui.jobposting.JobPostingApi
import com.muyeon.app.utils.TokenManager
import com.muyeon.app.webview.withActiveType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

/** 서버가 돌려준 오류 문구(`message`) 그대로 — 화면은 이 문구를 그대로 보여 준다. */
/** code = 서버 오류 코드(예: HOST_CONSENT_REQUIRED) — 코드로 분기하는 화면이 쓴다. */
class ApiMessageError(message: String, val status: Int, val code: String? = null) : Exception(message)

/**
 * 네이티브 화면 공용 REST 호출(2026-11-03 양도양수·공간 대관부터).
 *  - 토큰은 생성 때 잡지 않고 **요청 직전마다** TokenManager 에서 새로 읽는다(AiAutofill 과 같은 규약 —
 *    화면을 띄운 뒤 로그인·토큰 갱신이 일어나도 낡은 토큰을 쓰지 않게).
 *  - 모든 요청에 X-Active-Type(활성 회원유형)을 싣는다 — 등록 API(ActiveRoleGuard)·채팅 한도 판정이 이 헤더를 본다.
 *  - 비2xx 는 서버 `message`(배열이면 첫 줄)를 담은 [ApiMessageError] 로 올린다.
 */
class NativeHttp(context: Context) {
    private val app = context.applicationContext
    private val apiBase = BuildConfig.API_BASE_URL + "/api"

    fun token(): String? = TokenManager.getAccessToken(app)?.takeIf { it.isNotEmpty() }
    val isLoggedIn: Boolean get() = token() != null

    /** 내 회원 번호 — JWT sub(AssistantBadges 와 같은 방식). 비로그인·해석 실패는 null. */
    fun myUserId(): Int? = token()?.let { t ->
        runCatching {
            val part = t.split(".")[1]
            val json = String(android.util.Base64.decode(part, android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING or android.util.Base64.NO_WRAP))
            JSONObject(json).opt("sub")?.toString()?.toIntOrNull()
        }.getOrNull()
    }

    suspend fun get(path: String): Result<String> = call(path, "GET", null)
    suspend fun call(path: String, method: String = "GET", body: Any? = null): Result<String> =
        withContext(Dispatchers.IO) {
            com.muyeon.app.util.runCatchingCancellable {
                val payload = when {
                    body != null -> body.toString().toRequestBody(JSON)
                    method != "GET" && method != "DELETE" -> "{}".toRequestBody(JSON)
                    else -> null
                }
                val token = token()
                val req = Request.Builder().url(apiBase + path).method(method, payload)
                    .apply { if (token != null) addHeader("Authorization", "Bearer $token") }
                    .withActiveType()
                    .build()
                client.newCall(req).execute().use { res ->
                    val text = res.body?.string().orEmpty()
                    if (!res.isSuccessful) {
                        // ★ 2026-11-04 다른 기기 로그인·정지 — 네이티브 화면에서도 바로 강제 로그아웃.
                        com.muyeon.app.chat.socket.ForceLogout.checkResponse(app, res.code, text)
                        throw ApiMessageError(messageOf(text, res.code), res.code, codeOf(text))
                    }
                    text
                }
            }
        }

    /** 이미지 업로드 — 채용 공고와 같은 /uploads/image 함수 재사용. */
    suspend fun uploadImage(bytes: ByteArray): Result<String> = JobPostingApi(token()).uploadImage(bytes)

    companion object {
        /** 네이티브 화면 공용 OkHttp — 하나만 만들어 연결을 나눠 쓴다. 기본 10초는 느린 망에서 자주 끊겨 30초. */
        val sharedClient: OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build()
        private val client get() = sharedClient
        private val JSON = "application/json; charset=utf-8".toMediaType()

        fun codeOf(text: String): String? =
            runCatching { JSONObject(text).optString("code") }.getOrNull()?.takeIf { it.isNotBlank() }

        fun messageOf(text: String, code: Int): String {
            val o = runCatching { JSONObject(text) }.getOrNull()
            val raw = o?.opt("message")
            val msg = when (raw) {
                is JSONArray -> raw.optString(0)
                is String -> raw
                else -> null
            }?.takeIf { it.isNotBlank() }
            return msg ?: when (code) {
                401 -> "로그인 후 이용할 수 있어요."
                403 -> "이용 권한이 없어요."
                404 -> "찾을 수 없어요."
                else -> "요청에 실패했어요. 잠시 후 다시 시도해 주세요."
            }
        }
    }
}

// JSON 읽기 도우미 — 서버가 null 을 자주 보내 optInt 의 0 과 구분해야 한다.
fun JSONObject.jInt(key: String): Int? = if (has(key) && !isNull(key)) optDouble(key).takeIf { !it.isNaN() }?.toInt() else null
fun JSONObject.jDouble(key: String): Double? = if (has(key) && !isNull(key)) optDouble(key).takeIf { !it.isNaN() } else null
fun JSONObject.jStr(key: String): String? = if (has(key) && !isNull(key)) optString(key).takeIf { it.isNotEmpty() } else null
fun JSONObject.jBool(key: String, def: Boolean = false): Boolean = if (has(key) && !isNull(key)) optBoolean(key, def) else def
fun JSONObject.jStrings(key: String): List<String> = optJSONArray(key)?.let { a -> (0 until a.length()).mapNotNull { a.optString(it).takeIf { s -> s.isNotEmpty() } } } ?: emptyList()
fun JSONArray.jObjects(): List<JSONObject> = (0 until length()).mapNotNull { optJSONObject(it) }
