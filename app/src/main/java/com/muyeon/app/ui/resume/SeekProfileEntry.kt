package com.muyeon.app.ui.resume

import android.app.Activity
import com.muyeon.app.utils.TokenManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * ★ 2026-11-08 구직 프로필 진입(사용자 요청) — 이미 등록한 강사프로필(무용수는 무용수 이력서)이 있으면 이력서 관리로,
 *   없으면 등록 화면으로. [+ 글쓰기]·웹 /profileManage?seek=1 이 같이 쓴다.
 *   목록을 못 읽으면(네트워크) 등록으로 보내지 않는다 — 이미 있는데 새로 또 만들게 되는 걸 막으려고 관리 화면으로 연다(거기서 다시 읽는다).
 */
object SeekProfileEntry {
    fun open(activity: Activity, scope: CoroutineScope, dancer: Boolean) {
        val mode = if (dancer) ResumeMode.DANCER else ResumeMode.TEACHER
        scope.launch {
            val r = ResumeApi(TokenManager.getAccessToken(activity)).list(mode)
            val existing = r.getOrNull()
            if (existing != null && existing.isEmpty()) ResumeActivity.startEdit(activity, null, mode.raw, seekProfile = true)
            else ResumeActivity.startList(activity, mode.raw)
        }
    }
}
