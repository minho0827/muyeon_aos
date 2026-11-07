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
}
