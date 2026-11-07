package com.muyeon.data.local

import android.content.Context
import com.muyeon.domain.model.menu.AppMenuItem
import com.muyeon.network.dto.menu.AppMenuItemDto
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

/**
 * 서버 메뉴의 유형별 마지막 성공 응답 — 웹 localStorage `tabMenus:<type>` 과 같은 역할.
 *   오프라인·서버 오류 때 탭바·MY 메뉴가 비지 않게 한다. 키: `<surface>:<type>`.
 */
class MenuCache(context: Context, moshi: Moshi) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val adapter = moshi.adapter<List<AppMenuItemDto>>(
        Types.newParameterizedType(List::class.java, AppMenuItemDto::class.java),
    )

    fun read(surface: String, type: String): List<AppMenuItem>? =
        prefs.getString(key(surface, type), null)
            ?.let { runCatching { adapter.fromJson(it) }.getOrNull() }
            ?.mapNotNull { it.toDomain() }

    fun write(surface: String, type: String, items: List<AppMenuItem>) {
        val dto = items.map { AppMenuItemDto(it.key, it.title, it.path, it.bridge, it.icon, it.desc, it.groupNo, it.order) }
        prefs.edit().putString(key(surface, type), adapter.toJson(dto)).apply()
    }

    private fun key(surface: String, type: String) = "$surface:$type"

    private companion object { const val PREFS = "muyeon.menuCache" }
}
