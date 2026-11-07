package com.muyeon.domain.usecase.uiflag

import com.muyeon.domain.common.TokenStore
import com.muyeon.domain.repository.UiFlagRepository

/** 안내를 띄워야 하나 — 서버 값을 먼저 합친 뒤 판정(웹 syncTutorialFlagsFromServer → hasSeenTutorial). */
class ShouldShowGuideUseCase(private val repo: UiFlagRepository, private val tokens: TokenStore) {
    suspend operator fun invoke(key: String): Boolean {
        if (tokens.isLoggedIn) repo.syncFromServer()
        return !repo.hasSeen(key)
    }
    fun cached(key: String): Boolean = !repo.hasSeen(key)
}

class MarkGuideSeenUseCase(private val repo: UiFlagRepository) {
    suspend operator fun invoke(key: String) = repo.markSeen(key)
}
