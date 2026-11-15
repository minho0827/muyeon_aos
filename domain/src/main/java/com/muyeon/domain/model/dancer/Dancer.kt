package com.muyeon.domain.model.dancer

/** 무용수 목록 한 줄(웹 DancerList ListRow 재료). */
data class DancerCard(
    val id: Long,
    val name: String?,
    val image: String?,
    /** 전공 — major || genres[0] || fields[0]. */
    val major: String?,
    val gender: String?,
    val height: String?,
    val region: String?,
    val career: String?,
    val hasVideo: Boolean,
    val hasAward: Boolean,
)

/** 무용수 목록 조건(웹 DancerList — major·gender·영상·수상). null/false = 조건 없음. */
data class DancerFilter(val major: String? = null, val gender: String? = null, val hasVideo: Boolean = false, val hasAward: Boolean = false)

/** 무용수 상세(웹 DancerDetail) — 여러 줄 칸은 줄바꿈으로 이어 붙인 글. */
data class DancerProfile(
    val id: Long,
    val name: String,
    val images: List<String>,
    val major: String?,
    val gender: String?,
    val height: String?,
    val weight: String?,
    val region: String?,
    val oneLiner: String?,
    val intro: String?,
    val birth: String?,
    val education: String?,
    val career: String?,
    val companyCareer: String?,
    val awards: String?,
    val performances: String?,
    val videoUrl: String?,
    val availableDays: List<String>,
    val availableTimeSlots: List<String>,
    val hasVideo: Boolean,
    val self: Boolean,
    /**
     * ★ 2026-11-15 소개만 받은 응답(서버 limited:true) — 일반회원·무용수·비회원이 열면 키·몸무게·생년월일·성별·연락처 없이 온다.
     *  이때는 아래 버튼(캐스팅 제안·채팅)을 그리지 않는다.
     */
    val limited: Boolean = false,
    /** 장르 전체(전공은 그 첫째). */
    val genres: List<String> = emptyList(),
    /** 경력 줄들 — careers[{academy, position, period}] → "기관 직책 (기간)". */
    val careers: String? = null,
    /** 확인 배지 원문(JSON 글) — 화면에서 CredentialBadges 로 읽는다. 없으면 null. */
    val badgesJson: String? = null,
)

/** 캐스팅 제안 때 고르는 내 캐스팅 공고. */
data class CastingCallOption(val id: Long, val title: String, val genre: String?, val region: String?)
