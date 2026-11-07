package com.muyeon.domain.repository

import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.common.CursorPage
import com.muyeon.domain.model.recruit.JobCard
import com.muyeon.domain.model.recruit.JobFilter
import com.muyeon.domain.model.recruit.SeekFilter
import com.muyeon.domain.model.recruit.SeekerCard
import com.muyeon.domain.model.recruit.SubFilter
import com.muyeon.domain.model.recruit.SubRow
import com.muyeon.domain.model.region.ActiveRegion

/** 채용 탭 목록(구인 · 구직 · 대타). 지역은 정렬에만 쓰인다(서버). */
interface RecruitRepository {
    suspend fun jobsPage(region: ActiveRegion?, filter: JobFilter, cursor: String?): ApiResult<CursorPage<JobCard>>
    suspend fun jobsForMe(): ApiResult<List<JobCard>>
    suspend fun seekersPage(filter: SeekFilter, cursor: String?): ApiResult<CursorPage<SeekerCard>>
    suspend fun subs(region: ActiveRegion?, filter: SubFilter): ApiResult<List<SubRow>>
    /** 대타공고 등록 — 만든 공고 id. 권한(강사·학원·무용수·공연팀 인증)은 서버가 본다(403 이면 서버 문구). */
    suspend fun createSub(draft: com.muyeon.domain.model.recruit.SubDraft): ApiResult<Long>
}
