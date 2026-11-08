package com.muyeon.domain.model.policy

/**
 * ★ 2026-11-08 (b4) 약관·정책 문서 — `GET /policies/:docKey`(현재 + 시행 예정 + 이전 목록) · `/versions/:version`(그 버전).
 *   policy-documents.controller.ts. 웹 pages/setting/Policy.js 가 그리는 값.
 */
data class PolicyDoc(
    val title: String?,
    val version: String?,
    val effectiveDate: String?,
    /** 버전 열람일 때만 — SCHEDULED(시행 예정)·ACTIVE·RETIRED. */
    val status: String?,
    val body: String,
    val upcoming: PolicyVersion?,
    val history: List<PolicyVersion>,
)

data class PolicyVersion(val version: String, val effectiveDate: String?, val changeSummary: String?)

/** 화면에 그릴 값 — [current] 서버 현재 문서(없으면 기본 문구로 폴백한 경우), [shown] ?version= 으로 연 버전. */
data class PolicyView(val body: String, val current: PolicyDoc?, val shown: PolicyDoc?) {
    /** 지금 보는 것이 현재 버전이 아닌 다른 버전(지난·시행 예정)인가 — 웹 isOther. */
    val isOther: Boolean get() = shown != null && shown.version != current?.version
}

/**
 * 마크다운 비슷한 본문 → 블록 — 웹 Policy.js renderMd 와 같은 규칙(## 제목 / ### 소제목 / - 목록 / 표는 '첫 칸 · 나머지 칸' 목록 / 그 밖 문단).
 *   ⚠️ 관리자 미리보기(muyeon-admin policies.js renderMd)·iOS PolicyMarkdown 과 규칙을 맞춘다.
 */
sealed interface PolicyBlock {
    data class Heading(val text: String) : PolicyBlock
    data class SubHeading(val text: String) : PolicyBlock
    data class Paragraph(val text: String) : PolicyBlock
    data class Bullets(val items: List<String>) : PolicyBlock

    companion object {
        private val DIVIDER = Regex("^:?-{2,}:?$")

        fun parse(md: String): List<PolicyBlock> {
            val out = mutableListOf<PolicyBlock>()
            val list = mutableListOf<String>()
            fun flush() { if (list.isNotEmpty()) { out += Bullets(list.toList()); list.clear() } }
            var tableHeader = true
            md.split("\n").forEach { line ->
                val t = line.trim()
                if (t.isEmpty()) { flush(); tableHeader = true; return@forEach }
                if (t.length >= 2 && t.startsWith("|") && t.endsWith("|")) {
                    val cells = t.substring(1, t.length - 1).split("|").map { it.trim() }
                    if (cells.all { DIVIDER.matches(it) }) return@forEach
                    if (tableHeader) { tableHeader = false; return@forEach }
                    list += cells.filter { it.isNotEmpty() }.joinToString(" · ")
                    return@forEach
                }
                tableHeader = true
                when {
                    t.startsWith("### ") -> { flush(); out += SubHeading(t.substring(4)) }
                    t.startsWith("## ") -> { flush(); out += Heading(t.substring(3)) }
                    t.startsWith("- ") -> list += t.substring(2)
                    else -> { flush(); out += Paragraph(t) }
                }
            }
            flush()
            return out
        }
    }
}
