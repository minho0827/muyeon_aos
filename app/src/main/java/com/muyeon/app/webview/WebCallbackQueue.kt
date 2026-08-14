package com.muyeon.app.webview

import android.content.Context
import org.json.JSONArray

/**
 * 네이티브 → 웹 콜백 대기열 (디스크 보존).
 *
 * 왜 필요한가 — AOS 는 웹뷰가 **다른 액티비티**에 있어 네이티브 화면에서 곧바로
 * `evaluateJavaScript` 를 할 수 없다. 그래서 콜백을 모았다가 화면을 나갈 때 보내는데,
 * 그 대기열이 액티비티 메모리에만 있으면 **프로세스가 정리되는 순간 통째로 사라진다**.
 *
 * 그 경우 화면에는 반영돼 보이는데 서버는 모르는 상태가 되고,
 * 활성유형은 이미 저장돼 있어(ActiveRole) 이후 요청이 SWITCH_REQUIRED 로 튕긴다.
 * → SharedPreferences 에 적어두고, 웹뷰가 다시 앞으로 나올 때 흘려보낸다.
 *
 * ★ 여기 쌓는 것은 **부수효과가 있는 통지**다. 중복 실행돼도 안전한 형태여야 한다
 *   (`window.__onRoleManageAdd('TEACHER')` 처럼 멱등한 것만). 결제·전송류는 넣지 말 것.
 */
object WebCallbackQueue {
    private const val PREFS = "muyeon.webcb"
    private const val KEY = "pending"
    private const val MAX = 50 // 폭주 방지 — 넘치면 오래된 것부터 버린다

    @Synchronized
    fun enqueue(context: Context, js: String) {
        if (js.isBlank()) return
        val cur = read(context)
        cur.add(js)
        while (cur.size > MAX) cur.removeAt(0)
        write(context, cur)
    }

    /**
     * 대기열 내용만 본다(비우지 않는다). 비었으면 null.
     *  ★ 실행에 성공한 뒤에 clear 해야 한다. 먼저 비우면 웹이 아직 콜백을 등록하기 전일 때
     *    그대로 유실된다(콜드 스타트에서 실제로 발생 가능).
     */
    @Synchronized
    fun peek(context: Context): String? =
        read(context).takeIf { it.isNotEmpty() }?.joinToString("\n")

    @Synchronized
    fun clear(context: Context) = write(context, mutableListOf())

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun read(context: Context): MutableList<String> {
        val raw = prefs(context).getString(KEY, null) ?: return mutableListOf()
        return runCatching {
            val arr = JSONArray(raw)
            MutableList(arr.length()) { arr.optString(it) }.filter { it.isNotBlank() }.toMutableList()
        }.getOrDefault(mutableListOf())
    }

    private fun write(context: Context, list: List<String>) {
        prefs(context).edit().putString(KEY, JSONArray(list).toString()).apply()
    }
}
