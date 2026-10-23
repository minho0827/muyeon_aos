package com.muyeon.app.ui.assistant

import android.content.Context
import org.json.JSONArray

/** 최근 검색어(기기 저장, 최대 10개, 최근 것이 앞). 서버로 보내지 않는다 — 검색 기록 통계는 search-logs 가 따로. */
object AssistantRecent {
    private const val PREFS = "muyeon.assistant"
    private const val KEY = "recentQueries"
    private const val MAX = 10

    fun load(context: Context): List<String> = runCatching {
        val arr = JSONArray(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "[]"))
        (0 until arr.length()).map { arr.getString(it) }
    }.getOrDefault(emptyList())

    private fun save(context: Context, list: List<String>): List<String> {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, JSONArray(list).toString()).apply()
        return list
    }

    fun add(context: Context, q: String): List<String> {
        val t = q.trim().take(40)
        if (t.isEmpty()) return load(context)
        return save(context, (listOf(t) + load(context).filter { it != t }).take(MAX))
    }

    fun remove(context: Context, q: String) = save(context, load(context).filter { it != q })
    fun clear(context: Context) = save(context, emptyList())

    // ── 최근 연 메뉴(전체 메뉴의 [최근] 묶음, 6개) ──
    private const val KEY_MENUS = "recentMenus"

    fun loadMenus(context: Context): List<String> = runCatching {
        val arr = JSONArray(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_MENUS, "[]"))
        (0 until arr.length()).map { arr.getString(it) }
    }.getOrDefault(emptyList())

    fun addMenu(context: Context, id: String) {
        val next = (listOf(id) + loadMenus(context).filter { it != id }).take(6)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_MENUS, JSONArray(next).toString()).apply()
    }
}
