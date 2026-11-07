package com.muyeon.data.repository

import com.muyeon.data.common.safeApiCall
import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.common.CursorPage
import com.muyeon.domain.model.recruit.JobCard
import com.muyeon.domain.model.recruit.JobFilter
import com.muyeon.domain.model.recruit.SeekFilter
import com.muyeon.domain.model.recruit.SeekerCard
import com.muyeon.domain.model.recruit.SubFilter
import com.muyeon.domain.model.recruit.SubRow
import com.muyeon.domain.model.region.ActiveRegion
import com.muyeon.domain.repository.RecruitRepository
import com.muyeon.network.api.RecruitApi
import com.squareup.moshi.Moshi
import java.time.OffsetDateTime

class RecruitRepositoryImpl(private val api: RecruitApi, private val moshi: Moshi) : RecruitRepository {
    private fun ActiveRegion?.code() = this?.takeIf { it.kind == ActiveRegion.SIGUNGU }?.code
    private fun ActiveRegion?.sido() = this?.takeIf { it.kind == ActiveRegion.SIDO }?.code
    private fun Set<String>.joined() = takeIf { it.isNotEmpty() }?.sorted()?.joinToString(",")

    override suspend fun jobsPage(region: ActiveRegion?, filter: JobFilter, cursor: String?): ApiResult<CursorPage<JobCard>> =
        safeApiCall(moshi) { api.jobsPage(region.code(), region.sido(), filter.careers.joined(), filter.salaries.joined(), cursor, PAGE) }
            .map { CursorPage(it.items.orEmpty().mapNotNull { d -> d.toDomain() }, it.next) }

    override suspend fun jobsForMe(): ApiResult<List<JobCard>> =
        safeApiCall(moshi) { api.jobsForMe(FOR_ME) }.map { it.mapNotNull { d -> d.toDomain() } }

    override suspend fun seekersPage(filter: SeekFilter, cursor: String?): ApiResult<CursorPage<SeekerCard>> =
        safeApiCall(moshi) {
            api.seekersPage(filter.types.joined(), filter.fields.joined(), filter.targets.joined(), filter.careers.joined(), cursor, PAGE)
        }.map { CursorPage(it.items.orEmpty().mapNotNull { d -> d.toDomain() }, it.next) }

    override suspend fun subs(region: ActiveRegion?, filter: SubFilter): ApiResult<List<SubRow>> {
        val now = System.currentTimeMillis()
        return safeApiCall(moshi) {
            api.subs(region.code(), region.sido(), filter.field, filter.target, filter.salary, filter.scheduleType, filter.minClassCount)
        }.map { list -> list.mapNotNull { d -> d.toDomain(now, ::epochMs) } }
    }

    override suspend fun createSub(draft: com.muyeon.domain.model.recruit.SubDraft): ApiResult<Long> = with(draft) {
        safeApiCall(moshi) {
            api.createSub(com.muyeon.network.dto.recruit.SubCreateBody(title, academy, genre, target, fields, scheduleType, classDate, classTime, classCount,
                region, regionCode, salary, pay, description))
        }.map { it.id ?: 0L }
    }

    private fun epochMs(iso: String?): Long? = iso?.let { runCatching { OffsetDateTime.parse(it).toInstant().toEpochMilli() }.getOrNull() }

    private companion object {
        const val PAGE = 30
        const val FOR_ME = 10
    }
}
