package com.muyeon.app.ui.format

import android.net.Uri
import com.muyeon.app.BuildConfig

/**
 * 서버 이미지 주소 — 절대(http…)면 그대로, 상대(/uploads/…)면 서버 주소를 붙인다(CLAUDE.md 이미지 규칙).
 *   목록 썸네일은 웹 util/imageThumbnail.js 와 같은 조건에서만 `variant=thumb` 를 붙인다(미리 만든 작은 사진).
 */
object ImageUrls {
    fun resolve(path: String?): String? {
        if (path.isNullOrBlank()) return null
        return if (path.startsWith("http")) path else BuildConfig.API_BASE_URL.trimEnd('/') + "/" + path.trimStart('/')
    }

    private val V2_KEY = Regex("_v2\\.(jpe?g|png|webp|gif|avif)$", RegexOption.IGNORE_CASE)

    fun thumbnail(path: String?): String? {
        val full = resolve(path) ?: return null
        if (!full.contains("/uploads/view?")) return full
        return runCatching {
            val uri = Uri.parse(full)
            val key = uri.getQueryParameter("key").orEmpty()
            if (uri.getQueryParameter("variant") != null || key.contains("private/") || !V2_KEY.containsMatchIn(key)) full
            else uri.buildUpon().appendQueryParameter("variant", "thumb").build().toString()
        }.getOrDefault(full)
    }
}
