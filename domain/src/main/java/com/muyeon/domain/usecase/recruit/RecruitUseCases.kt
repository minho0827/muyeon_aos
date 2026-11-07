package com.muyeon.domain.usecase.recruit

import com.muyeon.domain.model.recruit.JobFilter
import com.muyeon.domain.model.recruit.SeekFilter
import com.muyeon.domain.model.recruit.SubFilter
import com.muyeon.domain.model.region.ActiveRegion
import com.muyeon.domain.repository.HomeRepository
import com.muyeon.domain.repository.RecruitRepository
import com.muyeon.domain.usecase.home.DemoContent

/** 구인 — 커서 목록 · 나에게 맞는 공고 · 지원함/열람함. 예시 공고는 뺀다. */
class JobListUseCase(private val repo: RecruitRepository, private val home: HomeRepository) {
    suspend fun page(region: ActiveRegion?, filter: JobFilter, cursor: String?) =
        repo.jobsPage(region, filter, cursor).map { p -> p.copy(items = p.items.filterNot { DemoContent.isDemoTitle(it.title) }) }
    suspend fun forMe() = repo.jobsForMe().getOrNull().orEmpty().filterNot { DemoContent.isDemoTitle(it.title) }
    suspend fun appliedIds() = home.myAppliedJobIds()
    suspend fun viewedIds() = home.myViewedJobIds()
}

/** 구직 — 커서 목록. */
class SeekerListUseCase(private val repo: RecruitRepository) {
    suspend fun page(filter: SeekFilter, cursor: String?) = repo.seekersPage(filter, cursor)
}

/** 대타 — 한 번에 받는 목록(서버에 커서 없음). 장르는 받은 뒤 거른다(웹 SubList 와 같다). 예시 공고는 뺀다. */
class SubListUseCase(private val repo: RecruitRepository) {
    suspend operator fun invoke(region: ActiveRegion?, filter: SubFilter) = repo.subs(region, filter).map { list ->
        list.filter { filter.genre == null || it.genre == filter.genre }.filterNot { DemoContent.isDemoTitle(it.title) }
    }
}

/** 대타공고 설문 등록. */
class CreateSubUseCase(private val repo: RecruitRepository) {
    suspend operator fun invoke(draft: com.muyeon.domain.model.recruit.SubDraft) = repo.createSub(draft)
}
