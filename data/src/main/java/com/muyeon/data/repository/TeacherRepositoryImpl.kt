package com.muyeon.data.repository

import com.muyeon.data.common.safeApiCall
import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.common.CursorPage
import com.muyeon.domain.model.region.ActiveRegion
import com.muyeon.domain.model.teacher.TeacherCard
import com.muyeon.domain.repository.TeacherRepository
import com.muyeon.network.api.TeacherApi
import com.squareup.moshi.Moshi

class TeacherRepositoryImpl(private val api: TeacherApi, private val moshi: Moshi) : TeacherRepository {
    override suspend fun teachersPage(genre: String?, region: ActiveRegion?, cursor: String?, limit: Int): ApiResult<CursorPage<TeacherCard>> =
        safeApiCall(moshi) {
            api.page(null, genre, null, region?.name, region?.kind, null, cursor, limit)
        }.map { CursorPage(it.items.orEmpty().mapNotNull { d -> d.toDomain() }, it.next) }

    override suspend fun lessonTeachersPage(
        genre: String?, field: String?, region: ActiveRegion?, cursor: String?, limit: Int, sort: String?,
    ): ApiResult<CursorPage<TeacherCard>> =
        safeApiCall(moshi) {
            api.page(LESSON, genre, field, region?.name, region?.kind, sort, cursor, limit)
        }.map { CursorPage(it.items.orEmpty().mapNotNull { d -> d.toDomain() }, it.next) }

    private companion object { const val LESSON = "true" }
}
