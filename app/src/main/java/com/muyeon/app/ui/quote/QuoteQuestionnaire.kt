package com.muyeon.app.ui.quote

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * ★ 2026-11-10 레슨 요청 설문(서버 동적) — 계약 muyeon-backend/docs/quote-questionnaire-contract.md.
 *
 *  0단계 GET /quote-questionnaire/entry → 신청자 유형 고르기
 *  → GET /quote-questionnaire?requesterType=&categoryId=&direct= → 그 유형의 질문 세트
 *  → POST /quotes { requesterType, questionSetId, answers, crossPost? }
 *
 *  ⚠️ 모르는 칸은 무시한다(앞으로 늘어날 수 있다). 둘 다 못 받으면 하드코딩 설문(QuoteQuestions)으로 돌고,
 *     0단계를 건너뛰며 requesterType·questionSetId 를 보내지 않는다(서버는 예전 앱으로 본다).
 *  iOS QuestionnaireRepository 와 같은 이름·같은 규칙.
 */

/** 0단계 질문 id — 답은 answers 가 아니라 맨 위 requesterType 으로 보낸다. */
const val REQUESTER_TYPE_QID = "requesterType"
const val REQUESTER_TYPE_GENERAL = "GENERAL"

data class QuestionnaireEntry(
    /** 켜진 유형이 하나뿐 — 0단계를 건너뛰고 GENERAL 세트로. */
    val skip: Boolean,
    val question: QuoteQuestion?,
    /** 미리 '선택된 상태'로만 보여 준다(자동 진행 안 함). */
    val preselect: String?,
) {
    companion object {
        fun from(o: JSONObject): QuestionnaireEntry {
            val q = o.optJSONObject("question")?.let { parseQuestion(it) }
                ?.copy(id = REQUESTER_TYPE_QID, type = QuoteAnswerType.SINGLE, required = true)
                ?.takeIf { it.options.isNotEmpty() }
            return QuestionnaireEntry(
                skip = o.optBoolean("skip", false) || q == null,
                question = q,
                preselect = o.str("preselect")?.takeIf { p -> q?.options?.any { it.id == p } == true },
            )
        }
    }
}

data class CrossPostField(
    val id: String,
    val type: String,
    val label: String,
    val required: Boolean,
    val defaultValue: String?,
)

/** 마지막 질문 뒤 '같이 올릴까요?' 화면 스펙(서버 crossPost[]). */
data class CrossPostSpec(
    val kind: String,
    val title: String,
    val body: String,
    val defaultOn: Boolean,
    val fields: List<CrossPostField>,
)

data class QuestionnaireSet(
    val setId: Int?,
    val version: Int?,
    val requesterType: String,
    val badge: String?,
    val questions: List<QuoteQuestion>,
    /** 질문 id → 미리 채울 답(text·region). */
    val prefill: Map<String, QuoteAnswer>,
    val crossPost: List<CrossPostSpec>,
) {
    companion object {
        fun from(o: JSONObject): QuestionnaireSet {
            val questions = o.optJSONArray("questions").objects().mapNotNull(::parseQuestion)
            val prefill = mutableMapOf<String, QuoteAnswer>()
            o.optJSONObject("prefill")?.let { p ->
                p.keys().forEach { qid ->
                    val v = p.optJSONObject(qid) ?: return@forEach
                    val a = QuoteAnswer(
                        questionId = qid,
                        text = v.str("text"),
                        region = v.str("region"),
                        regionCode = v.str("regionCode"),
                    )
                    if (!a.isEmpty) prefill[qid] = a
                }
            }
            val cross = o.optJSONArray("crossPost").objects().mapNotNull { c ->
                val kind = c.str("kind") ?: return@mapNotNull null
                CrossPostSpec(
                    kind = kind,
                    title = c.str("title").orEmpty(),
                    body = c.str("body").orEmpty(),
                    defaultOn = c.optBoolean("defaultOn", true),
                    fields = c.optJSONArray("fields").objects().mapNotNull { f ->
                        val id = f.str("id") ?: return@mapNotNull null
                        CrossPostField(id, f.str("type").orEmpty(), f.str("label").orEmpty(), f.optBoolean("required", false), f.str("defaultValue"))
                    },
                )
            }
            return QuestionnaireSet(
                setId = if (o.has("setId") && !o.isNull("setId")) o.optInt("setId") else null,
                version = if (o.has("version") && !o.isNull("version")) o.optInt("version") else null,
                requesterType = o.str("requesterType") ?: REQUESTER_TYPE_GENERAL,
                badge = o.str("badge"),
                questions = questions,
                prefill = prefill,
                crossPost = cross,
            )
        }
    }
}

/** POST /quotes 응답의 crossPosts 한 줄 — 완료 화면이 message 와 버튼을 그린다. */
data class CrossPostResult(
    val kind: String,
    val id: Int?,
    /** OPEN | PENDING_VERIFY | DRAFT | FAILED */
    val status: String,
    val message: String,
    val path: String?,
) {
    companion object {
        fun from(o: JSONObject) = CrossPostResult(
            kind = o.str("kind").orEmpty(),
            id = if (o.has("id") && !o.isNull("id")) o.optInt("id") else null,
            status = o.str("status").orEmpty(),
            message = o.str("message").orEmpty(),
            path = o.str("path"),
        )
    }
}

/** POST /quotes 결과 — 기존 { id } + crossPosts. */
data class CreatedQuote(val id: Int, val crossPosts: List<CrossPostResult>) {
    companion object {
        fun from(o: JSONObject) = CreatedQuote(
            id = o.optInt("id"),
            crossPosts = o.optJSONArray("crossPosts").objects().map(CrossPostResult::from),
        )
    }
}

/**
 * 설문 저장소 — 받아 오고, 마지막으로 성공한 응답을 기기에 남긴다(같은 조건이 다음에 실패하면 그걸로).
 *  둘 다 없으면 null → 호출부가 하드코딩 설문으로 돈다.
 */
class QuestionnaireRepository(context: Context, private val token: String?) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    suspend fun entry(): QuestionnaireEntry? {
        val key = "entry"
        val raw = QuoteApi(token).questionnaireEntry().getOrNull()?.also { prefs.edit().putString(key, it).apply() }
            ?: prefs.getString(key, null)
        return raw?.let { runCatching { QuestionnaireEntry.from(JSONObject(it)) }.getOrNull() }
    }

    suspend fun set(requesterType: String, categoryId: String, direct: Boolean): QuestionnaireSet? {
        val key = "set:$requesterType:$categoryId:${if (direct) 1 else 0}"
        val raw = QuoteApi(token).questionnaireSet(requesterType, categoryId, direct).getOrNull()
            ?.also { prefs.edit().putString(key, it).apply() }
            ?: prefs.getString(key, null)
        return raw?.let { runCatching { QuestionnaireSet.from(JSONObject(it)) }.getOrNull() }
            ?.takeIf { it.questions.isNotEmpty() }
    }

    /** 서버가 '설문이 바뀌었어요'(QUESTION_SET_INVALID·REQUESTER_TYPE_UNAVAILABLE)라 했을 때 — 남긴 응답을 버린다. */
    fun clear() = prefs.edit().clear().apply()

    private companion object {
        const val PREFS = "muyeon.quoteQuestionnaire"
    }
}

// MARK: - 파싱

private fun JSONObject.str(key: String): String? =
    if (!has(key) || isNull(key)) null else optString(key).trim().ifEmpty { null }

private fun JSONArray?.objects(): List<JSONObject> =
    if (this == null) emptyList() else (0 until length()).mapNotNull { optJSONObject(it) }

private fun typeOf(raw: String?): QuoteAnswerType? = when (raw) {
    "single" -> QuoteAnswerType.SINGLE
    "multi" -> QuoteAnswerType.MULTI
    // 사진 첨부 입력은 위저드에 아직 없어 글 입력으로 받는다(사진 없이 text 만 보낸다).
    "text", "textPhoto" -> QuoteAnswerType.TEXT
    "region" -> QuoteAnswerType.REGION
    "date" -> QuoteAnswerType.DATE
    else -> null
}

/** 서버 질문 JSON → 위저드 질문. 모르는 type 은 그릴 수 없어 뺀다. */
private fun parseQuestion(o: JSONObject): QuoteQuestion? {
    val id = o.str("id") ?: return null
    val type = typeOf(o.str("type") ?: "single") ?: return null
    val options = o.optJSONArray("options").objects().mapNotNull { op ->
        val oid = op.str("id") ?: return@mapNotNull null
        QuoteOption(
            id = oid,
            label = op.str("label") ?: oid,
            short = op.str("short"),
            exclusive = op.optBoolean("exclusive", false),
            description = op.str("description"),
        )
    }
    if ((type == QuoteAnswerType.SINGLE || type == QuoteAnswerType.MULTI) && options.isEmpty()) return null
    val visible = o.optJSONObject("visibleWhen")?.let { v ->
        val qid = v.str("questionId") ?: return@let null
        val any = v.optJSONArray("anyOf")?.let { a -> (0 until a.length()).mapNotNull { a.optString(it).ifEmpty { null } } }.orEmpty()
        QuoteVisibleWhen(qid, any)
    }
    return QuoteQuestion(
        id = id,
        type = type,
        title = o.str("title") ?: id,
        options = options,
        required = o.optBoolean("required", true),
        subtitle = o.str("subtitle"),
        placeholder = o.str("placeholder"),
        short = o.str("short"),
        visibleWhen = visible,
    )
}
