package com.muyeon.domain.repository

import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.model.support.SupportChatAccessInfo
import com.muyeon.domain.model.support.SupportChatNotice
import com.muyeon.domain.model.support.SupportChatRoomInfo
import com.muyeon.domain.model.support.SupportChatSummary
import com.muyeon.domain.model.support.SupportChatView

/** 상담 채팅(무용연 고객센터). */
interface SupportChatRepository {
    suspend fun access(): ApiResult<SupportChatAccessInfo>
    suspend fun summary(): ApiResult<SupportChatSummary>
    suspend fun notices(): ApiResult<List<SupportChatNotice>>
    suspend fun readAllNotices(): ApiResult<Unit>
    suspend fun rooms(): ApiResult<List<SupportChatRoomInfo>>
    suspend fun createRoom(): ApiResult<Long>
    suspend fun messages(roomId: Long, after: Long): ApiResult<SupportChatView>
    suspend fun send(roomId: Long, content: String, after: Long): ApiResult<SupportChatView>
    /** [nodeId] 또는 [action](BACK·HOME) 하나. */
    suspend fun choose(roomId: Long, nodeId: Long?, action: String?, after: Long): ApiResult<SupportChatView>
    suspend fun handoff(roomId: Long, after: Long): ApiResult<SupportChatView>
    suspend fun close(roomId: Long, after: Long): ApiResult<SupportChatView>
    suspend fun read(roomId: Long): ApiResult<Unit>
}
