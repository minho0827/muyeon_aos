package com.muyeon.network.dto.menu

import com.muyeon.domain.model.menu.AppMenuItem
import com.squareup.moshi.JsonClass

/**
 * `GET /app-menus` 응답 `{ activeType, items: MenuItemDto[] }` — app-menus.service.ts listFor.
 *   ⚠️ 서버가 null 을 보내는 칸은 전부 `?` (Moshi 는 non-null 칸에 null 이 오면 던진다).
 */
@JsonClass(generateAdapter = true)
data class AppMenuListDto(
    val activeType: String? = null,
    val items: List<AppMenuItemDto>? = null,
) {
    fun toDomain(): List<AppMenuItem> = items.orEmpty().mapNotNull { it.toDomain() }
}

@JsonClass(generateAdapter = true)
data class AppMenuItemDto(
    val key: String? = null,
    val title: String? = null,
    val path: String? = null,
    val bridge: String? = null,
    val icon: String? = null,
    val desc: String? = null,
    val groupNo: Int? = null,
    val order: Int? = null,
) {
    /** 경로·키가 없는 칸은 버린다(누를 곳이 없는 메뉴). */
    fun toDomain(): AppMenuItem? {
        val k = key?.takeIf { it.isNotBlank() } ?: return null
        val p = path?.takeIf { it.isNotBlank() } ?: return null
        return AppMenuItem(k, p, icon, title?.takeIf { it.isNotBlank() }, bridge?.takeIf { it.isNotBlank() }, desc, groupNo, order)
    }
}
