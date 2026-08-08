package com.muyeon.app.ui.survey

import com.muyeon.app.BuildConfig
import com.muyeon.app.ui.quote.intOrNull
import com.muyeon.app.ui.quote.map
import com.muyeon.app.ui.quote.stringOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

/**
 * 레슨 전 설문지 — iOS `Survey/SurveyModels.swift` + `SurveyService.swift` 1:1.
 *  백엔드 `/api/surveys` 계열. 문항(SINGLE/MULTI/TEXT) + 레벨별 항목(입문~전공).
 *
 * ⚠️ answers 는 `{ "questionId": { optionIds, etc, text } }` 동적 구조라 타입 모델로 못 받는다.
 *   iOS 도 raw 로 다루므로 여기서도 Map 으로 유지한다.
 */

data class SurveyTemplate(val id: Int, val title: String, val genre: String?, val description: String?) {
    companion object {
        fun from(o: JSONObject) = SurveyTemplate(
            o.optInt("id"), o.optString("title"), o.stringOrNull("genre"), o.stringOrNull("description"),
        )
    }
}

data class SurveyOption(val id: Int, val questionId: Int, val text: String, val imageUrl: String?) {
    companion object {
        fun from(o: JSONObject) = SurveyOption(
            o.optInt("id"), o.optInt("questionId"), o.optString("text"), o.stringOrNull("imageUrl"),
        )
    }
}

/** type: SINGLE(단일선택) | MULTI(복수선택) | TEXT(자유입력). */
data class SurveyQuestion(
    val id: Int,
    val templateId: Int,
    val type: String,
    val title: String,
    val description: String?,
    val required: Boolean,
    val allowEtc: Boolean,
    val placeholder: String?,
    val sortOrder: Int,
    val options: List<SurveyOption>,
) {
    companion object {
        fun from(o: JSONObject) = SurveyQuestion(
            o.optInt("id"), o.optInt("templateId"), o.optString("type"), o.optString("title"),
            o.stringOrNull("description"), o.optBoolean("required", false), o.optBoolean("allowEtc", false),
            o.stringOrNull("placeholder"), o.optInt("sortOrder"),
            o.optJSONArray("options")?.map(SurveyOption::from) ?: emptyList(),
        )
    }
}

/** 레벨별 항목 — 1단계 실력 선택 → 2단계 항목. */
data class SurveyItem(val id: Int, val level: String, val levelOrder: Int, val text: String, val imageUrl: String?, val sortOrder: Int) {
    companion object {
        fun from(o: JSONObject) = SurveyItem(
            o.optInt("id"), o.optString("level"), o.optInt("levelOrder"),
            o.optString("text"), o.stringOrNull("imageUrl"), o.optInt("sortOrder"),
        )
    }
}

data class SurveyLevelGroup(val level: String, val levelOrder: Int, val items: List<SurveyItem>) {
    companion object {
        fun from(o: JSONObject) = SurveyLevelGroup(
            o.optString("level"), o.optInt("levelOrder"),
            o.optJSONArray("items")?.map(SurveyItem::from) ?: emptyList(),
        )
    }
}

data class SurveyTemplateDetail(
    val template: SurveyTemplate,
    val questions: List<SurveyQuestion>,
    val levels: List<SurveyLevelGroup>?,
) {
    companion object {
        fun from(o: JSONObject) = SurveyTemplateDetail(
            SurveyTemplate.from(o.optJSONObject("template") ?: JSONObject()),
            o.optJSONArray("questions")?.map(SurveyQuestion::from) ?: emptyList(),
            o.optJSONArray("levels")?.map(SurveyLevelGroup::from),
        )
    }
}

data class SurveyDispatch(
    val id: Int,
    val templateId: Int,
    val roomId: Int,
    val senderId: Int,
    val recipientId: Int,
    val status: String,
) {
    companion object {
        fun from(o: JSONObject) = SurveyDispatch(
            o.optInt("id"), o.optInt("templateId"), o.optInt("roomId"),
            o.optInt("senderId"), o.optInt("recipientId"), o.optString("status"),
        )
    }
}

/** 문항 1개 답변 — optionIds/etc/text. */
data class SurveyAnswer(val optionIds: List<Int> = emptyList(), val etc: String? = null, val text: String? = null) {
    fun toJson(): JSONObject = JSONObject().apply {
        if (optionIds.isNotEmpty()) put("optionIds", JSONArray(optionIds))
        etc?.takeIf { it.isNotEmpty() }?.let { put("etc", it) }
        text?.takeIf { it.isNotEmpty() }?.let { put("text", it) }
    }

    companion object {
        fun from(o: JSONObject): SurveyAnswer {
            val ids = o.optJSONArray("optionIds")?.let { arr -> (0 until arr.length()).map { arr.optInt(it) } }
            return SurveyAnswer(ids ?: emptyList(), o.stringOrNull("etc"), o.stringOrNull("text"))
        }
    }
}

data class SurveyResponseRaw(
    val dispatchId: Int?,
    val note: String?,
    val answers: Map<String, SurveyAnswer>,
    val revision: Int?,
    val submittedAt: String?,
    val updatedAt: String?,
) {
    companion object {
        fun from(o: JSONObject): SurveyResponseRaw {
            val a = o.optJSONObject("answers")
            val map = a?.keys()?.asSequence()
                ?.mapNotNull { k -> a.optJSONObject(k)?.let { k to SurveyAnswer.from(it) } }
                ?.toMap() ?: emptyMap()
            return SurveyResponseRaw(
                o.intOrNull("dispatchId"), o.stringOrNull("note"), map,
                o.intOrNull("revision"), o.stringOrNull("submittedAt"), o.stringOrNull("updatedAt"),
            )
        }
    }
}

data class SurveyLessonInfo(val startAt: String?, val place: String?) {
    companion object {
        fun from(o: JSONObject?) = o?.let { SurveyLessonInfo(it.stringOrNull("startAt"), it.stringOrNull("place")) }
    }
}

data class SurveyDispatchDetail(
    val dispatch: SurveyDispatch,
    val template: SurveyTemplate,
    val questions: List<SurveyQuestion>,
    val levels: List<SurveyLevelGroup>?,
    val response: SurveyResponseRaw?,
    val recipientName: String?,
    val lesson: SurveyLessonInfo?,
) {
    companion object {
        fun from(o: JSONObject) = SurveyDispatchDetail(
            SurveyDispatch.from(o.optJSONObject("dispatch") ?: JSONObject()),
            SurveyTemplate.from(o.optJSONObject("template") ?: JSONObject()),
            o.optJSONArray("questions")?.map(SurveyQuestion::from) ?: emptyList(),
            o.optJSONArray("levels")?.map(SurveyLevelGroup::from),
            o.optJSONObject("response")?.let { SurveyResponseRaw.from(it) },
            o.stringOrNull("recipientName"),
            SurveyLessonInfo.from(o.optJSONObject("lesson")),
        )
    }
}

/** SURVEY_CARD 채팅 메시지 content(JSON) 파싱용. */
data class SurveyCardData(val dispatchId: Int, val templateId: Int?, val title: String?, val genre: String?) {
    companion object {
        fun parse(content: String): SurveyCardData? = runCatching {
            val o = JSONObject(content)
            SurveyCardData(o.optInt("dispatchId"), o.intOrNull("templateId"), o.stringOrNull("title"), o.stringOrNull("genre"))
        }.getOrNull()
    }
}

class SurveyApi(private val token: String?) {

    private val client = OkHttpClient()
    private val apiBase = BuildConfig.API_BASE_URL + "/api"

    suspend fun listTemplates(genre: String? = null): Result<List<SurveyTemplate>> {
        val path = "/surveys/templates" + if (genre.isNullOrEmpty()) "" else "?genre=" + java.net.URLEncoder.encode(genre, "UTF-8")
        return call(path).map { JSONArray(it.ifBlank { "[]" }).map(SurveyTemplate::from) }
    }

    suspend fun getTemplate(id: Int): Result<SurveyTemplateDetail> =
        call("/surveys/templates/$id").map { SurveyTemplateDetail.from(JSONObject(it)) }

    /** 강사 → 회원에게 설문 발송(채팅방에 SURVEY_CARD 생성). */
    suspend fun dispatch(templateId: Int, roomId: Int, recipientId: Int): Result<SurveyDispatch> =
        call(
            "/surveys/dispatch", "POST",
            JSONObject().put("templateId", templateId).put("roomId", roomId).put("recipientId", recipientId),
        ).map { SurveyDispatch.from(JSONObject(it).optJSONObject("dispatch") ?: JSONObject(it)) }

    suspend fun getDispatch(id: Int): Result<SurveyDispatchDetail> =
        call("/surveys/dispatch/$id").map { SurveyDispatchDetail.from(JSONObject(it)) }

    suspend fun responses(recipientId: Int): Result<List<SurveyDispatchDetail>> =
        call("/surveys/responses?recipientId=$recipientId")
            .map { JSONArray(it.ifBlank { "[]" }).map(SurveyDispatchDetail::from) }

    suspend fun respond(dispatchId: Int, answers: Map<String, SurveyAnswer>, note: String?): Result<Unit> {
        val a = JSONObject().apply { answers.forEach { (k, v) -> put(k, v.toJson()) } }
        val body = JSONObject().put("answers", a).apply { note?.takeIf { it.isNotEmpty() }?.let { put("note", it) } }
        return call("/surveys/dispatch/$dispatchId/respond", "POST", body).map { }
    }

    private suspend fun call(path: String, method: String = "GET", body: JSONObject? = null): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val payload = when {
                    body != null -> body.toString().toRequestBody(JSON)
                    method != "GET" && method != "DELETE" -> "".toRequestBody(JSON)
                    else -> null
                }
                val req = Request.Builder().url(apiBase + path).method(method, payload)
                    .addHeader("Content-Type", "application/json")
                    .apply { if (!token.isNullOrEmpty()) addHeader("Authorization", "Bearer $token") }
                    .build()
                client.newCall(req).execute().use { res ->
                    val text = res.body?.string().orEmpty()
                    if (!res.isSuccessful) {
                        val msg = runCatching { JSONObject(text).optString("message") }.getOrNull()
                        throw IllegalStateException(msg?.ifEmpty { null } ?: "요청에 실패했어요.")
                    }
                    text
                }
            }
        }

    private companion object { val JSON = "application/json".toMediaType() }
}
