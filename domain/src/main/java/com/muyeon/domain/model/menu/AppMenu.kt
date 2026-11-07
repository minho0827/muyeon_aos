package com.muyeon.domain.model.menu

/**
 * 서버가 회원유형별로 내려주는 메뉴 한 칸 — `GET /app-menus?surface=TAB|MY`.
 *   출처: muyeon-backend `app-menus` (app-menu.defaults.ts TAB_MENU_SEED / MY_MENU_SEED). 관리자가 켜고 끈다.
 *
 * @param key  "tab.home" 같은 키. 마지막 마디(home)가 탭 식별자([tabKey]).
 * @param title 서버 문구. null 이면 화면이 [tabKey] 로 기본 문구(strings.xml)를 고른다(오프라인 기본 목록).
 * @param bridge MY 메뉴의 네이티브 동작 이름(예: quoteRequest). 없으면 [path] 로 간다.
 */
data class AppMenuItem(
    val key: String,
    val path: String,
    val icon: String?,
    val title: String?,
    val bridge: String? = null,
    val desc: String? = null,
    val groupNo: Int? = null,
    val order: Int? = null,
) {
    val tabKey: String get() = key.substringAfterLast('.')
}

enum class AppMenuSurface(val value: String) { TAB("TAB"), MY("MY") }
