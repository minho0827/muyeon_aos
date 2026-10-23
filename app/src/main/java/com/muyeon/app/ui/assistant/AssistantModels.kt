package com.muyeon.app.ui.assistant

import org.json.JSONArray
import org.json.JSONObject

/**
 * AI 도우미(플로팅 버튼 창) 서버 응답 + 빠른 이동 검색. iOS AssistantModels.swift 와 같은 규칙.
 *  기능 목록은 서버(GET /api/assistant/catalog)가 활성 유형별로 내려주고, 글자마다 서버를 부르지 않고 기기에서 거른다.
 */
data class AssistantItem(
    val id: String,
    val title: String,
    val subtitle: String?,
    val section: String,
    val kind: String, // NATIVE | WEB | FLOW
    val nativeAction: String?,
    val nativeParams: Map<String, String>,
    val webPath: String?,
    val flow: String?,
    val proOnly: Boolean,
    val keywords: List<String>,
    val chosung: List<String>,
) {
    companion object {
        fun from(o: JSONObject) = AssistantItem(
            id = o.optString("id"),
            title = o.optString("title"),
            subtitle = o.optString("subtitle").takeIf { it.isNotEmpty() && it != "null" },
            section = o.optString("section"),
            kind = o.optString("kind"),
            nativeAction = o.optString("nativeAction").takeIf { it.isNotEmpty() && it != "null" },
            nativeParams = o.optJSONObject("nativeParams")?.let { p -> p.keys().asSequence().associateWith { p.optString(it) } } ?: emptyMap(),
            webPath = o.optString("webPath").takeIf { it.isNotEmpty() && it != "null" },
            flow = o.optString("flow").takeIf { it.isNotEmpty() && it != "null" },
            proOnly = o.optBoolean("proOnly", false),
            keywords = o.optJSONArray("keywords").strings(),
            chosung = o.optJSONArray("chosung").strings(),
        )
    }
}

/** 전체 메뉴 묶음(칩·머리글) — 서버 assistant_sections. key 는 항목의 section 값. */
data class AssistantSection(val key: String, val title: String, val icon: String)

data class AssistantCatalog(val etag: String, val activeType: String, val items: List<AssistantItem>, val sections: List<AssistantSection> = emptyList()) {
    companion object {
        fun from(o: JSONObject): AssistantCatalog {
            val secs = o.optJSONArray("sections")
            return AssistantCatalog(
                o.optString("etag"),
                o.optString("activeType"),
                (0 until (o.optJSONArray("items")?.length() ?: 0)).mapNotNull { i -> o.optJSONArray("items")?.optJSONObject(i)?.let(AssistantItem::from) },
                (0 until (secs?.length() ?: 0)).mapNotNull { i ->
                    secs?.optJSONObject(i)?.let { AssistantSection(it.optString("key"), it.optString("title"), it.optString("icon")) }
                },
            )
        }
    }
}

/** 자주 묻는 질문(GET /api/faqs, 공개). */
data class AssistantFaq(val id: Int, val category: String, val question: String, val answer: String)

data class AssistantFlowSummary(val id: String, val title: String, val locked: Boolean)

data class AssistantConfig(val enabled: Boolean, val flows: List<AssistantFlowSummary>) {
    companion object {
        fun from(o: JSONObject): AssistantConfig {
            val arr = o.optJSONArray("flows")
            return AssistantConfig(
                o.optBoolean("enabled", false),
                (0 until (arr?.length() ?: 0)).mapNotNull { i ->
                    arr?.optJSONObject(i)?.let { AssistantFlowSummary(it.optString("id"), it.optString("title"), it.optBoolean("locked", true)) }
                },
            )
        }
    }
}

private fun JSONArray?.strings(): List<String> = if (this == null) emptyList() else (0 until length()).map { optString(it) }

object AssistantSearch {
    private const val CHO = "ㄱㄲㄴㄷㄸㄹㅁㅂㅃㅅㅆㅇㅈㅉㅊㅋㅌㅍㅎ"

    /** 입력이 전부 자음이면 초성 검색. */
    fun isChosungQuery(q: String) = q.isNotEmpty() && q.all { it in CHO }

    /** 점수: 제목 앞글자 > 별칭 앞글자 > 포함 > 초성. 0 = 안 맞음. */
    fun score(item: AssistantItem, raw: String): Int {
        val q = raw.trim().lowercase().replace(" ", "")
        if (q.isEmpty()) return 0
        val title = item.title.lowercase().replace(" ", "")
        if (isChosungQuery(q)) {
            return when {
                item.chosung.any { it.startsWith(q) } -> 40
                item.chosung.any { it.contains(q) } -> 20
                else -> 0
            }
        }
        val words = item.keywords.map { it.replace(" ", "") }
        return when {
            title.startsWith(q) -> 100
            words.any { it.startsWith(q) } -> 80
            title.contains(q) -> 60
            words.any { it.contains(q) } -> 50
            else -> 0
        }
    }

    private fun chosungOf(s: String) = buildString {
        for (ch in s) { val c = ch.code - 0xAC00; if (c in 0..11171) append(CHO[c / 588]) }
    }

    /** 자주 묻는 질문 검색 — 질문 우선, 답 포함, 초성은 질문만. */
    fun searchFaqs(faqs: List<AssistantFaq>, raw: String): List<AssistantFaq> {
        val q = raw.trim().lowercase().replace(" ", "")
        if (q.isEmpty()) return emptyList()
        val cho = isChosungQuery(q)
        return faqs.mapNotNull { f ->
            val question = f.question.lowercase().replace(" ", "")
            val s = when {
                cho -> if (chosungOf(f.question).contains(q)) 20 else 0
                question.contains(q) -> 100
                f.answer.lowercase().replace(" ", "").contains(q) -> 40
                else -> 0
            }
            if (s > 0) f to s else null
        }.sortedByDescending { it.second }.map { it.first }
    }

    /** 검색 결과(최대 30개) — 점수순, 같으면 서버 순서. */
    fun search(items: List<AssistantItem>, query: String): List<AssistantItem> =
        items.mapIndexed { i, it -> Triple(i, it, score(it, query)) }
            .filter { it.third > 0 }
            .sortedWith(compareByDescending<Triple<Int, AssistantItem, Int>> { it.third }.thenBy { it.first })
            .take(30)
            .map { it.second }
}
