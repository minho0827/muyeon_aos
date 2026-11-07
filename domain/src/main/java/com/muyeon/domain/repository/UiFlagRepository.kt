package com.muyeon.domain.repository

/**
 * 안내(배너·튜토리얼)를 봤는지 — 웹 util/tutorialFlags.js 와 같은 키·같은 서버 저장(계정 기준 /auth/me/ui-flags).
 *   1차 기기 저장(즉시 판정) + 2차 서버(재설치·다른 기기에서도 다시 안 뜨게).
 */
interface UiFlagRepository {
    fun hasSeen(key: String): Boolean
    /** 기기에 저장하고 서버에도 보낸다(실패해도 기기 값으로 동작). */
    suspend fun markSeen(key: String)
    /** 서버 값을 기기로 합친다(세션당 한 번). 로그인 회원만. */
    suspend fun syncFromServer()
}

/** 키 — 웹 TUTORIAL_KEYS 와 같은 문자열(서버 계정 플래그를 공유한다). */
object UiFlagKeys {
    const val LESSON_TAB = "lessonTab.v1"
    const val LESSON_TAB_GUIDE = "lessonTabGuide.v1"
    const val MY_TYPE_TAB = "myTypeTab.v1"
}
