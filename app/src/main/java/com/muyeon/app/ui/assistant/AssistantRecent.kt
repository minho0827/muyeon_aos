package com.muyeon.app.ui.assistant

import android.content.Context
import com.muyeon.app.webview.ActiveRole
import org.json.JSONArray

/**
 * 최근 검색어(10개)·최근 연 메뉴(6개) — 기기 저장, **활성 유형별로 따로**(10-23 사용자 요청).
 *  강사로 찾던 메뉴가 일반 유형 화면에 섞이면 헷갈린다 → 키에 유형을 붙인다(recentQueries.TEACHER …).
 *  계정이 바뀌면(AssistantBadges.onScreenResumed 가 토큰 변화를 감지) clearAll 로 전부 지운다.
 *  예전(유형 구분 없던) 키는 어느 유형 기록인지 몰라 버린다. 서버로 보내지 않는다(통계는 search-logs 가 유형과 함께 따로).
 */
object AssistantRecent {
    private const val PREFS = "muyeon.assistant"
    private const val Q = "recentQueries"
    private const val M = "recentMenus"

    private fun key(context: Context, base: String) = "$base.${ActiveRole.current(context)}"

    private fun read(context: Context, k: String): List<String> = runCatching {
        val arr = JSONArray(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(k, "[]"))
        (0 until arr.length()).map { arr.getString(it) }
    }.getOrDefault(emptyList())

    private fun write(context: Context, k: String, list: List<String>): List<String> {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(k, JSONArray(list).toString()).apply()
        return list
    }

    fun load(context: Context) = read(context, key(context, Q))

    fun add(context: Context, q: String): List<String> {
        val t = q.trim().take(40)
        if (t.isEmpty()) return load(context)
        return write(context, key(context, Q), (listOf(t) + load(context).filter { it != t }).take(10))
    }

    fun remove(context: Context, q: String) = write(context, key(context, Q), load(context).filter { it != q })
    fun clear(context: Context) = write(context, key(context, Q), emptyList())

    fun loadMenus(context: Context) = read(context, key(context, M))

    fun addMenu(context: Context, id: String) {
        write(context, key(context, M), (listOf(id) + loadMenus(context).filter { it != id }).take(6))
    }

    /** 계정이 바뀌면 모든 유형의 기록을 지운다(+예전 구분 없던 키). */
    fun clearAll(context: Context) {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val e = p.edit()
        p.all.keys.filter { it == Q || it == M || it.startsWith("$Q.") || it.startsWith("$M.") }.forEach { e.remove(it) }
        e.apply()
    }
}
