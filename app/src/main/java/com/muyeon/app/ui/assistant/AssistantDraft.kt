package com.muyeon.app.ui.assistant

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * AI 초안 받기·채택 기록(3단계) — iOS AssistantDraft 와 1:1. 등록 화면이 aiDraftId 로 칸을 채우고, 저장하면 채택을 알린다.
 *  ★ AI 가 직접 저장하지 않는다 — 회원이 보고 고친 뒤 기존 [저장] 으로.
 */
object AssistantDraft {
    const val EXTRA = "aiDraftId"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    suspend fun payload(context: Context, id: String): JSONObject? {
        val r = AssistantApi.call(context, "GET", "/assistant/drafts/$id")
        return if (r.ok) r.json?.optJSONObject("payload") else null
    }

    fun saved(context: Context, id: String?, targetType: String, targetId: Int?, finalText: String) {
        if (id.isNullOrEmpty()) return
        val app = context.applicationContext
        val body = JSONObject().put("outcome", "SAVED").put("targetType", targetType).put("finalText", finalText.take(5000))
        if (targetId != null && targetId > 0) body.put("targetId", targetId)
        scope.launch { AssistantApi.call(app, "POST", "/assistant/drafts/$id/outcome", body) }
    }
}

/** JSON 문자열 값(없거나 "null" 이면 null). */
internal fun JSONObject.str(key: String): String? = optString(key).takeIf { it.isNotEmpty() && it != "null" }
