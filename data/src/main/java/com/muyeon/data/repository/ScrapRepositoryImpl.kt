package com.muyeon.data.repository

import com.muyeon.data.common.safeApiCall
import com.muyeon.data.common.safeUnitCall
import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.common.KeyValueStore
import com.muyeon.domain.model.scrap.ScrapTarget
import com.muyeon.domain.repository.ScrapChange
import com.muyeon.domain.repository.ScrapRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import com.muyeon.network.api.ScrapApi
import com.squareup.moshi.Moshi

class ScrapRepositoryImpl(
    private val api: ScrapApi,
    private val moshi: Moshi,
    private val store: KeyValueStore,
) : ScrapRepository {
    private val _changes = MutableSharedFlow<ScrapChange>(extraBufferCapacity = 16)
    override val changes: SharedFlow<ScrapChange> = _changes.asSharedFlow()

    override suspend fun likedIds(target: ScrapTarget): ApiResult<Set<Long>> =
        safeApiCall(moshi) { api.mine(target.serverType) }
            .map { rows -> rows.filter { it.targetType == null || it.targetType == target.serverType }.mapNotNull { it.targetId }.toSet() }

    override suspend fun likedItems(target: ScrapTarget?): ApiResult<List<com.muyeon.domain.model.scrap.LikedItem>> =
        safeApiCall(moshi) { api.entries(target?.serverType) }.map { rows -> rows.mapNotNull { it.toDomain() } }

    override suspend fun setLiked(target: ScrapTarget, id: Long, liked: Boolean): ApiResult<Unit> =
        safeUnitCall(moshi) { if (liked) api.add(target.pathSegment, id) else api.remove(target.pathSegment, id) }
            .onSuccess { _changes.tryEmit(ScrapChange(target, id, liked)) }

    override fun savePending(target: ScrapTarget, id: Long) = store.putString(PENDING, "${target.name}:$id")

    override fun takePending(target: ScrapTarget): Long? {
        val raw = store.getString(PENDING) ?: return null
        val (type, id) = raw.split(':').takeIf { it.size == 2 } ?: return null.also { store.putString(PENDING, null) }
        if (type != target.name) return null
        store.putString(PENDING, null)
        return id.toLongOrNull()
    }

    private companion object { const val PENDING = "scrap.pending" }
}
