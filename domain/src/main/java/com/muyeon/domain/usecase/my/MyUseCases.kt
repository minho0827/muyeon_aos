package com.muyeon.domain.usecase.my

import com.muyeon.domain.model.member.ActiveType
import com.muyeon.domain.model.my.MyPageInfo
import com.muyeon.domain.repository.MyPageRepository
import com.muyeon.domain.repository.SessionController
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/** MY 부가 정보 한 번에(동시에) — 비공개 공고 안내는 일반 유형만(웹 isGeneral). */
class GetMyPageInfoUseCase(private val repo: MyPageRepository) {
    suspend operator fun invoke(activeType: String): MyPageInfo = coroutineScope {
        val name = async { repo.myName() }
        val noti = async { repo.notiUnread() }
        val quote = async { repo.quoteUnread() }
        val membership = async { repo.membership() }
        val pending = async { if (activeType == ActiveType.GENERAL) repo.pendingPostings() else null }
        val casting = async { repo.castingMaterialAllowed() }
        val support = async { repo.supportChatEligible() }
        MyPageInfo(name.await(), noti.await(), quote.await(), membership.await(), pending.await()?.takeIf { it.total > 0 }, casting.await(), support.await())
    }
}

class GetBadgeCountsUseCase(private val repo: MyPageRepository) {
    suspend fun noti(): Int = repo.notiUnread()
    suspend fun quotes(): Int = repo.quoteUnread()
}

class LogoutUseCase(private val session: SessionController) {
    operator fun invoke() = session.logout()
}
