package com.muyeon.app.ui.space.wizard

import org.json.JSONArray
import org.json.JSONObject

/**
 * ★ 2026-11-18 공간 사업자 확인 — 모델과 판단 규칙(화면 없이 시험할 수 있게 여기에 모은다).
 *  서버 규칙: muyeon-backend src/modules/spaces/space-business.service.ts.
 *  - 학원 인증이 끝난 회원: 첫 질문 "지금 운영 중이신 '{학원명}'을 공간으로 등록하시나요?" → 네 = ACADEMY, 아니요 = 번호 입력
 *  - 그 밖의 회원: 바로 번호 입력(OWN)
 *  사업자번호 전체는 화면 밖(로그)에 남기지 않는다.
 */

/** GET /spaces/host-business 의 academy. */
data class SpaceHostAcademy(val verified: Boolean, val name: String?, val numberTail: String?) {
    companion object {
        fun from(o: JSONObject?): SpaceHostAcademy {
            val a = o?.optJSONObject("academy") ?: return SpaceHostAcademy(false, null, null)
            fun str(k: String) = if (a.has(k) && !a.isNull(k)) a.optString(k).trim().ifEmpty { null } else null
            return SpaceHostAcademy(a.optBoolean("verified"), str("name"), str("numberTail"))
        }
    }
}

/** POST /spaces/business-number/check 응답(안내용, 저장 안 함). */
data class SpaceBizCheck(val verified: Boolean, val message: String, val ownerMatched: Boolean?) {
    companion object {
        fun from(o: JSONObject) = SpaceBizCheck(
            verified = o.optBoolean("verified"),
            message = o.optString("message").trim(),
            ownerMatched = if (o.has("ownerMatched") && !o.isNull("ownerMatched")) o.optBoolean("ownerMatched") else null,
        )
    }
}

/** 등록·수정 body 의 business. source = ACADEMY | OWN. */
data class SpaceBusinessInput(
    val source: String,
    val number: String? = null,
    val startDate: String? = null,
    val docs: List<String> = emptyList(),
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("source", source)
        if (source == SOURCE_OWN) {
            number?.let { put("number", it) }
            startDate?.let { put("startDate", it) }
            if (docs.isNotEmpty()) put("docs", JSONArray(docs.map { JSONObject().put("type", "BUSINESS_REGISTRATION").put("url", it) }))
        }
    }

    companion object {
        const val SOURCE_ACADEMY = "ACADEMY"
        const val SOURCE_OWN = "OWN"
        val ACADEMY = SpaceBusinessInput(SOURCE_ACADEMY)

        /** 번호 입력 → OWN. 번호는 숫자 10자리, 개업일자는 8자리일 때만 싣는다. */
        fun own(number: String, startDate: String, docs: List<String>) = SpaceBusinessInput(
            SOURCE_OWN, SpaceBusinessRules.digits(number, 10),
            SpaceBusinessRules.digits(startDate, 8).takeIf { it.length == 8 }, docs.take(SpaceBusinessRules.MAX_DOCS),
        )
    }
}

/** GET /spaces/mine · /spaces/:id(주인) 의 business. */
data class SpaceBusinessStatus(val status: String?, val source: String?, val numberTail: String?, val dueAt: String?, val memo: String?) {
    companion object {
        fun from(o: JSONObject?): SpaceBusinessStatus? {
            o ?: return null
            fun str(k: String) = if (o.has(k) && !o.isNull(k)) o.optString(k).trim().ifEmpty { null } else null
            return SpaceBusinessStatus(str("status"), str("source"), str("numberTail"), str("dueAt"), str("memo"))
        }
    }
}

/** 내 공간 카드 한 줄 — 글 + (있으면) 버튼 이름. warn = 주황 강조. */
data class SpaceBusinessLine(val text: String, val action: String?, val warn: Boolean)

/** 등록(POST)·수정(PATCH) 결과. bizStatus = VERIFIED | PENDING | null(사업자 판정 안 함). */
data class SpaceSaveResult(val id: Int, val bizStatus: String?)

object SpaceBusinessRules {
    const val MAX_DOCS = 5
    const val TITLE_OWN = "등록할 공간의 사업자등록번호를 알려 주세요"
    const val HELP = "대표자 이름·개업일자가 국세청 정보와 맞으면 바로 공개돼요. 맞지 않으면 관리자 확인 후 공개돼요."
    const val OWNER_MATCHED = "국세청 정보와 일치해요. 등록하면 바로 공개돼요."
    const val CHECK_DELAYED = "자동 확인이 지연되고 있어요. 잠시 후 다시 시도해 주세요."
    const val YES_ACADEMY = "네, 이 학원이에요"
    const val NO_OTHER = "아니요, 다른 공간이에요"
    const val PENDING_NOTICE = "관리자 확인 후 공개돼요. 보통 1영업일 안에 확인해요."

    /** 서버가 사업자 단계로 되돌리라는 오류 코드(400) — 문구 그대로 보여 주고 사업자 단계로. */
    val RETRY_CODES = setOf("SPACE_BUSINESS_NOT_VERIFIED", "SPACE_BUSINESS_REQUIRED", "SPACE_BUSINESS_ACADEMY_NOT_VERIFIED")

    fun digits(s: String, max: Int): String = s.filter(Char::isDigit).take(max)

    fun question(academyName: String?): String =
        academyName?.trim()?.takeIf { it.isNotEmpty() }?.let { "지금 운영 중이신 '$it'을 공간으로 등록하시나요?" }
            ?: "지금 운영 중이신 학원을 공간으로 등록하시나요?"

    /** 번호가 10자리면 자동 확인할 (번호, 개업일자?) — 개업일자는 8자리일 때만. 확인할 게 없으면 null. */
    fun checkRequest(number: String, startDate: String): Pair<String, String?>? {
        val n = digits(number, 10)
        if (n.length != 10) return null
        return n to digits(startDate, 8).takeIf { it.length == 8 }
    }

    /**
     * 확인 결과 아래 문구 — (글, 빨강?). 없으면 null.
     *  verified=false → 서버 문구 빨강 · 일치 → 바로 공개 안내 · 불일치·개업일자 없음 → 안내 없음(사용자 결정, 불일치 경고 금지).
     */
    fun checkMessage(check: SpaceBizCheck?, failed: Boolean): Pair<String, Boolean>? = when {
        failed -> CHECK_DELAYED to true
        check == null -> null
        !check.verified -> check.message.ifEmpty { "사업자등록번호를 확인해 주세요." } to true
        check.ownerMatched == true -> OWNER_MATCHED to false
        else -> null
    }

    /** 번호 입력 화면 [다음] — 번호 10자리 + 확인 끝(verified) + 개업일자는 비었거나 8자리 + 확인·업로드 중 아님. 사진은 선택. */
    fun canSubmitOwn(number: String, startDate: String, check: SpaceBizCheck?, checking: Boolean, uploading: Boolean): Boolean {
        val d = digits(startDate, 8)
        return digits(number, 10).length == 10 && (d.isEmpty() || d.length == 8) && !checking && !uploading && check?.verified == true
    }

    /** 내 공간 카드 줄. VERIFIED·기한 없는 null 은 표시 없음. */
    fun mineLine(b: SpaceBusinessStatus?): SpaceBusinessLine? {
        b ?: return null
        return when (b.status) {
            "VERIFIED" -> null
            "PENDING" -> SpaceBusinessLine("사업자 확인 중이에요", null, false)
            "REJECTED" -> SpaceBusinessLine(
                listOfNotNull("사업자 확인이 반려됐어요", b.memo).joinToString(" · "), "다시 확인하기", true,
            )
            null -> b.dueAt?.let { due ->
                SpaceBusinessLine(listOfNotNull("사업자 확인이 필요해요", dueLabel(due)?.let { "${it}까지" }).joinToString(" · "), "확인하기", true)
            }
            else -> null
        }
    }

    /** 서버 시각(ISO) → 한국 날짜 "11월 30일". 못 읽으면 null. */
    fun dueLabel(iso: String?): String? {
        if (iso.isNullOrBlank()) return null
        return runCatching {
            val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Seoul"))
            val m = Regex("^(\\d{4})-(\\d{2})-(\\d{2})(?:[T ](\\d{2}):(\\d{2})(?::(\\d{2}))?)?").find(iso.trim()) ?: return@runCatching null
            val (y, mo, d) = m.destructured
            val hasTime = m.groupValues[4].isNotEmpty()
            if (!hasTime) return@runCatching "${mo.toInt()}월 ${d.toInt()}일"
            // 시각이 있으면 UTC(끝 Z·오프셋 없음) 로 보고 한국 시간으로 옮긴다.
            val utc = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
                clear(); set(y.toInt(), mo.toInt() - 1, d.toInt(), m.groupValues[4].toInt(), m.groupValues[5].toInt(), m.groupValues[6].ifEmpty { "0" }.toInt())
            }
            val offset = Regex("([+-])(\\d{2}):?(\\d{2})$").find(iso.trim())
            var ms = utc.timeInMillis
            if (offset != null && !iso.trim().endsWith("Z")) {
                val sign = if (offset.groupValues[1] == "+") 1 else -1
                ms -= sign * (offset.groupValues[2].toLong() * 3600_000 + offset.groupValues[3].toLong() * 60_000)
            }
            cal.timeInMillis = ms
            "${cal.get(java.util.Calendar.MONTH) + 1}월 ${cal.get(java.util.Calendar.DAY_OF_MONTH)}일"
        }.getOrNull()
    }

    /** 등록 끝 안내 — PENDING 이면 관리자 확인 안내를 붙인다. */
    fun savedMessage(edit: Boolean, bizStatus: String?): String = when {
        edit -> "수정했어요."
        bizStatus == "PENDING" -> "공간을 등록했어요. $PENDING_NOTICE"
        else -> "공간을 등록했어요."
    }

    /** 내 공간 [확인하기] 제출 뒤 안내. */
    fun recheckMessage(bizStatus: String?): String =
        if (bizStatus == "VERIFIED") "사업자 확인이 끝났어요." else "사업자 정보를 보냈어요. $PENDING_NOTICE"
}
