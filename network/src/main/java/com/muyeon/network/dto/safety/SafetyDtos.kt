package com.muyeon.network.dto.safety

import com.squareup.moshi.JsonClass

/** POST /reports — reports CreateReportDto. */
@JsonClass(generateAdapter = true)
data class ReportBody(val targetType: String, val targetId: Long, val reason: String, val detail: String? = null)
