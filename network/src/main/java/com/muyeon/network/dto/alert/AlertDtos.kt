package com.muyeon.network.dto.alert

import com.muyeon.domain.model.alert.AlertPrefs
import com.squareup.moshi.JsonClass

/** `GET /me/alert-prefs` — 행이 없으면 `{}`(alerts.service getPrefs). */
@JsonClass(generateAdapter = true)
data class AlertPrefsDto(
    val regions: List<String>? = null,
    val fields: List<String>? = null,
    val genres: List<String>? = null,
    val targets: List<String>? = null,
    val types: List<String>? = null,
    val push: Boolean? = null,
) {
    fun toDomain() = AlertPrefs(regions.orEmpty(), fields.orEmpty(), genres.orEmpty(), targets.orEmpty(), types.orEmpty(), push != false)
}

/**
 * `PATCH /me/alert-prefs` — 빈 배열 = 조건 해제(보내야 지워진다). [types] 가 null 이면 키를 빼서(서버가 예전 값 유지 — 웹 undefined).
 */
@JsonClass(generateAdapter = true)
data class AlertPrefsBody(
    val regions: List<String>,
    val fields: List<String>,
    val genres: List<String>,
    val targets: List<String>,
    val types: List<String>?,
    val push: Boolean,
) {
    companion object {
        fun of(p: AlertPrefs) = AlertPrefsBody(p.regions, p.fields, p.genres, p.targets, p.types.ifEmpty { null }, p.push)
    }
}
