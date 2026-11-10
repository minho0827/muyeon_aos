package com.muyeon.domain.model.castingmaterial

/** ★ 2026-11-08 묶음 2 — 캐스팅·공연 자료 업로드(웹 pages/muyeon/my/CastingMaterials.js). kind = CASTING | PERFORMANCE. */
enum class CastingMaterialKind(val code: String) { CASTING("CASTING"), PERFORMANCE("PERFORMANCE") }

/** 올린 자료 한 묶음 — GET /casting-materials 의 items. status = PENDING | PROCESSING | DONE | FAILED. */
data class CastingMaterialBatch(
    val id: Long,
    val status: String,
    val imageCount: Int,
    val instagramUrl: String?,
    val linkUrl: String?,
    val createdAt: String?,
    val created: List<CastingMaterialCreated>,
    val skipped: List<CastingMaterialSkipped>,
    val errorMessage: String?,
) {
    val processing: Boolean get() = status == "PENDING" || status == "PROCESSING"
}

/** AI 가 등록한 공고·공연 — performanceId 가 있으면 공연, 아니면 캐스팅 공고. */
data class CastingMaterialCreated(val title: String, val castingId: Long?, val performanceId: Long?) {
    /** 앱 주소(웹 COPY.path) — 경로표(NativeRoutes)가 앱 화면으로 연다. */
    val path: String? get() = performanceId?.let { "/performance/$it" } ?: castingId?.let { "/casting/$it" }
}

/** ★ 2026-11-10 지운 묶음 — [kept] 는 그대로 남은 공고·공연. */
data class CastingMaterialDeleted(val id: Long, val kept: List<CastingMaterialCreated>)

/** 건너뛴 사진 — [image] 는 0부터 센 사진 번호. */
data class CastingMaterialSkipped(val title: String?, val image: Int?, val reason: String?)
