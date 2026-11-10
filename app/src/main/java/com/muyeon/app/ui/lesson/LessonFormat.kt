package com.muyeon.app.ui.lesson

/**
 * 레슨 상품 진행 방식(lesson_products.formats) 규칙 — iOS `LessonFormat` 과 같다.
 *
 *  ★ 2026-11-10 '온라인 레슨'은 isOnline 칸(서버가 받지 않고 버린다) 대신 formats 값 "온라인" 으로 저장한다.
 *   - 저장: 온라인이면 formats 에 "온라인" 을 넣고, 아니면 뺀다. 장소명이 비면 '온라인 레슨'(옛 앱·웹이 이 이름으로 판정).
 *   - 불러오기: formats 에 "온라인" 이 있거나 장소명이 '온라인 레슨' 이면 온라인.
 *   - 라벨: 모르는 값은 값 그대로 보여 준다("온라인" 은 옛 앱에서도 그대로 읽힌다).
 */
object LessonFormat {
    const val ONLINE = "온라인"
    const val ONLINE_PLACE = "온라인 레슨"

    /** 위저드에서 손으로 고르는 진행 방식(견적 문진 format id 체계). 온라인은 위치 단계 토글이 맡는다. */
    val manualOptions: List<Pair<String, String>> = QuotePrefsOptions.formats

    /** 둘러보기 필터·상세 라벨 — 온라인 포함. */
    val options: List<Pair<String, String>> = manualOptions + (ONLINE to "온라인")

    fun label(id: String): String = options.firstOrNull { it.first == id }?.second ?: id

    fun labels(formats: List<String>?): String? =
        formats?.takeIf { it.isNotEmpty() }?.joinToString(", ") { label(it) }

    fun isOnline(formats: List<String>?, place: String?): Boolean =
        formats.orEmpty().contains(ONLINE) || place?.trim() == ONLINE_PLACE

    /** 토글 상태를 formats 에 반영 — 다른 값과 순서는 그대로 두고 "온라인" 만 넣거나 뺀다. */
    fun applyOnline(formats: List<String>, online: Boolean): List<String> =
        formats.filter { it != ONLINE } + if (online) listOf(ONLINE) else emptyList()

    /** 서버로 보낼 장소명 — 온라인인데 비어 있으면 '온라인 레슨'. */
    fun payloadPlace(place: String, online: Boolean): String =
        if (online && place.isBlank()) ONLINE_PLACE else place
}
