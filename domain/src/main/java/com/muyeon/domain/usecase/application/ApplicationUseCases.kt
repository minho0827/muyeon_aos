package com.muyeon.domain.usecase.application

import com.muyeon.domain.repository.ApplicationRepository

/** 내가 지원한 공고 — 목록·지원 취소·내 목록에서 삭제(웹 MyApplications). */
class MyApplicationsUseCase(private val repo: ApplicationRepository) {
    suspend fun page(tab: com.muyeon.domain.model.application.MyApplicationTab, period: String?, cursor: String?) = repo.minePage(tab, period, cursor)
    suspend fun withdraw(kind: String, id: Long) = repo.withdraw(kind, id)
    suspend fun archive(kind: String, id: Long) = repo.archiveMine(kind, id)
}

/** 받은 지원 — 목록(공고 하나로 거르기·휴지통)·휴지통으로·복원·영구 삭제(웹 ReceivedApplications). */
class ReceivedApplicationsUseCase(private val repo: ApplicationRepository) {
    suspend fun page(kind: String?, postingId: Long?, archived: Boolean, cursor: String?) = repo.receivedPage(kind, postingId, archived, cursor)
    suspend fun archive(kind: String, id: Long) = repo.archiveReceived(kind, id)
    suspend fun restore(kind: String, id: Long) = repo.restoreReceived(kind, id)
    suspend fun purge(kind: String, id: Long) = repo.purgeReceived(kind, id)
}
