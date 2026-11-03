package com.muyeon.app.ui.common.market

import android.os.Bundle
import org.json.JSONObject

/**
 * ★ 2026-11-03 로그인 후 이어서 할 일 — 비로그인 안내에서 [로그인]을 누른 화면과 하려던 동작.
 *
 *  흐름(타이머 없음, 전부 콜백):
 *   1) 화면이 [loginRequired]로 안내 팝업을 띄운다 → [로그인] = 결과 LOGIN_REQUIRED + 이 값(Bundle)을 들고 닫힘.
 *   2) 네이티브 부모는 결과 콜백으로 받아 같은 결과를 들고 닫힌다(ResultActivity).
 *   3) 웹뷰(WebViewActivity)는 받으면 이 값을 기억하고 /login 으로 이동.
 *   4) 웹이 로그인을 마치고 토큰을 저장하는 순간(AndroidToken.saveToken) 웹뷰가 [action]을 브리지 액션으로 다시 연다.
 *
 *  action 은 웹 브리지 액션 이름과 같다(openSpaceDetail·openStudioTransferDetail·openSpaceEditor …).
 *  params 의 "then" 은 다시 연 화면이 이어서 할 동작(reserve·chat·like).
 */
data class LoginResume(val action: String, val params: Map<String, String> = emptyMap()) {

    fun toBundle(): Bundle = Bundle().apply {
        putString(KEY_ACTION, action)
        params.forEach { (k, v) -> putString(k, v) }
    }

    /** 브리지 data 로 넘길 JSON(action 제외). */
    fun paramsJson(): JSONObject = JSONObject().apply { params.forEach { (k, v) -> put(k, v) } }

    /** 안내 팝업으로 보낼 문구 — 화면들의 onMessage(String) 통로를 그대로 쓰려고 표식을 붙인다. */
    fun message(): String = PREFIX + JSONObject().put(KEY_ACTION, action).put("params", paramsJson()).toString()

    companion object {
        private const val KEY_ACTION = "action"
        private const val PREFIX = "\u0001LOGIN:"

        fun isMessage(msg: String) = msg.startsWith(PREFIX)

        fun parse(msg: String): LoginResume? {
            if (!isMessage(msg)) return null
            return runCatching {
                val o = JSONObject(msg.removePrefix(PREFIX))
                val p = o.optJSONObject("params") ?: JSONObject()
                LoginResume(o.getString(KEY_ACTION), p.keys().asSequence().associateWith { p.optString(it) })
            }.getOrNull()
        }

        fun fromBundle(b: Bundle?): LoginResume? {
            val action = b?.getString(KEY_ACTION)?.takeIf { it.isNotEmpty() } ?: return null
            val params = b.keySet().filter { it != KEY_ACTION }.associateWith { b.getString(it).orEmpty() }
            return LoginResume(action, params)
        }
    }
}

/** 비로그인 안내 문구(로그인 후 이어서 할 일 포함). */
fun loginRequired(action: String, vararg params: Pair<String, Any?>): String =
    LoginResume(action, params.filter { it.second != null }.associate { it.first to it.second.toString() }).message()
