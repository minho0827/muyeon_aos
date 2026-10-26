package com.muyeon.app.ui.castingstage.create

import org.json.JSONObject
import java.time.LocalDate

/**
 * 공연 공고 등록 입력 값 + 선택지 — 웹 /casting/create(CastingCreate.js)·iOS CastingCreateForm.swift 와 같은 항목·값.
 *  선택지 값은 웹 constants/castingOptions.js · 서버 casting-fee.ts 와 같아야 한다(크로스레포 계약).
 */
object CastingCreateOptions {
    /** 공연 장르(웹 CASTING_GENRES). 전공 조건도 같은 목록. */
    val genres = listOf("한국무용", "현대무용", "발레", "행사")
    /** 모집 역할(복수) — "군무, 솔리스트" 로 이어 저장한다. */
    val roles = listOf("주역", "솔리스트", "듀엣", "군무", "앙상블", "조역", "언더스터디")
    val genders = listOf("여성", "남성", "무관")
    val feeRanges = listOf(
        "F0_10" to "10만원 미만",
        "F10_30" to "10만원~30만원 미만",
        "F30_50" to "30만원~50만원 미만",
        "F50_100" to "50만원~100만원 미만",
        "F100" to "100만원 이상",
        "NEGOTIABLE" to "추후 협의",
        "UNPAID" to "무보수",
    )
    fun feeLabel(v: String?): String = feeRanges.firstOrNull { it.first == v }?.second.orEmpty()

    const val TITLE_MAX = 50
    const val PLACE_MAX = 50
    const val SHORT_MAX = 20
    const val FEE_MAX = 50
    const val DESC_MAX = 1000
    const val LINE_MAX = 80
}

data class CastingCreateForm(
    // 1. 공연 정보
    val title: String = "",
    val genre: String? = null,
    val place: String = "",
    val region: String = "",
    val regionCode: String? = null,
    val imageUrl: String? = null,
    val schedule: LocalDate? = null,
    val rehearsalStart: LocalDate? = null,
    val rehearsalEnd: LocalDate? = null,
    val deadline: LocalDate? = null,
    val deadlineUndecided: Boolean = false,
    // 2. 모집 조건
    val roles: List<String> = emptyList(),
    val headcount: String = "",
    val gender: String? = null,
    val age: String = "",
    val height: String = "",
    val major: String? = null,
    // 3. 비용·제출물·안내 — 웹은 예/아니오 셀렉트지만 '예'일 때만 true 를 보내므로 스위치와 같다.
    val feeRange: String? = null,
    val fee: String = "",
    val rehearsalFee: Boolean = false,
    val transportFee: Boolean = false,
    val videoRequired: Boolean = false,
    val photoRequired: Boolean = false,
    val awardRequired: Boolean = false,
    val careerRequired: Boolean = false,
    val introRequired: Boolean = false,
    val desc: String = "",
    val applyMethod: String = "",
    val contact: String = "",
) {
    val trimmedTitle get() = title.trim()
    /** 1단계 필수: 공연명·장르(웹 submit 검증과 같은 기준). */
    val basicValid get() = trimmedTitle.isNotEmpty() && genre != null
    /** 2단계 필수: 모집 역할. */
    val recruitValid get() = roles.isNotEmpty()
    val isDirty get() = this != CastingCreateForm()

    fun toggleRole(r: String) = copy(roles = if (r in roles) roles - r else roles + r)

    val rehearsalText: String?
        get() = if (rehearsalStart == null && rehearsalEnd == null) null
        else "${rehearsalStart ?: ""} ~ ${rehearsalEnd ?: ""}".trim()

    /** 마감 미정은 "-"(웹 DateSelect 규약). */
    val deadlineText: String? get() = if (deadlineUndecided) "-" else deadline?.toString()

    /** 웹 callCreateCasting 과 같은 모양. */
    fun toJson(activityName: String): JSONObject = JSONObject().apply {
        fun opt(k: String, v: String?) { v?.trim()?.takeIf { it.isNotEmpty() }?.let { put(k, it) } }
        put("title", trimmedTitle)
        put("genre", genre.orEmpty())
        put("role", roles.joinToString(", "))
        put("headcount", (headcount.toIntOrNull() ?: 1).coerceAtLeast(1))
        put("rehearsalFee", rehearsalFee)
        put("transportFee", transportFee)
        put("requirements", JSONObject().apply {
            put("video", videoRequired); put("photo", photoRequired); put("award", awardRequired)
            put("career", careerRequired); put("intro", introRequired)
        })
        // 하위호환(기존 상세 화면 칸)
        put("awardPreferred", awardRequired)
        put("videoRequired", videoRequired)
        opt("team", activityName); opt("region", region); opt("regionCode", regionCode)
        opt("image", imageUrl); opt("place", place); opt("schedule", schedule?.toString())
        opt("rehearsal", rehearsalText); opt("deadline", deadlineText); opt("gender", gender)
        opt("age", age); opt("height", height); opt("major", major); opt("fee", fee)
        opt("feeRange", feeRange); opt("description", desc); opt("applyMethod", applyMethod); opt("contact", contact)
    }
}

/** 'yyyy.MM.dd' 표시. */
fun LocalDate?.dotted(): String? = this?.toString()?.replace("-", ".")
