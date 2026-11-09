package com.muyeon.app.util

import org.json.JSONObject

/**
 * ★ 2026-11-09 org.json 의 optString 은 값이 JSON null 이면 글자 "null" 을 돌려준다 → 화면에 'null · null' 이 보였다.
 *   빈 값·null·"null" 은 모두 null 로 읽는다. 앱 전체가 이것 하나를 쓴다.
 */
fun JSONObject.optStringOrNull(key: String): String? =
    if (isNull(key)) null else optString(key).takeIf { it.isNotEmpty() && it != "null" }

/** 공백만 있어도 null. */
fun JSONObject.optTextOrNull(key: String): String? = optStringOrNull(key)?.takeIf { it.isNotBlank() }
