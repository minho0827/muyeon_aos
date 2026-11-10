package com.muyeon.app.navigation

/**
 * ★ 2026-11-08 묶음 3(공고·커뮤니티 작성/수정) — 웹 창으로 가던 작성·수정 주소를 앱 화면으로.
 *   [NativeRoutes.resolve] 가 묶음 0 표 다음에 이 표를 본다. iOS Shell/Navigation/NativeRoutes+B3.swift 와 같은 표·같은 순서.
 *   모두 로그인해야 쓰는 화면(웹 meta auth) — 비회원이면 셸이 앱 로그인부터(돌아오면 이 주소로).
 *   /community/write?id=N 은 예전 iOS 가 글 수정에 쓰던 주소 — 수정으로 받는다.
 *   /casting/:id/edit 는 11-10 부터 앱 화면(NativeRoutesB0 — 등록 흐름 수정 모드).
 */
internal object NativeRoutesB3 {
    private val SUB_EDIT = Regex("^/subs/(\\d+)/edit$")
    private val POST_EDIT = Regex("^/community/post/(\\d+)/edit$")

    /** [p] = 끝 / 를 뗀 경로, [query] = 쿼리. */
    fun resolve(p: String, query: Map<String, String>): NativeRoutes.Target? {
        if (p == "/community/write") {
            return NativeRoutes.Target.Route(Screen.CommunityWrite.build(query["id"]?.toLongOrNull()?.takeIf { it > 0 }), auth = true)
        }
        POST_EDIT.find(p)?.let { return NativeRoutes.Target.Route(Screen.CommunityWrite.build(it.groupValues[1].toLong()), auth = true) }
        SUB_EDIT.find(p)?.let { return NativeRoutes.Target.Route(Screen.SubEdit.build(it.groupValues[1].toLong()), auth = true) }
        return null
    }
}
