package com.muyeon.network.api

import com.muyeon.network.dto.my.AllowedDto
import com.muyeon.network.dto.my.CountDto
import com.muyeon.network.dto.my.EligibleDto
import com.muyeon.network.dto.my.MeDto
import com.muyeon.network.dto.my.MembershipDto
import com.muyeon.network.dto.my.PendingSummaryDto
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET

/** MY 부가 정보(ApiEndpoints.My 출처 주석 참고). */
interface MyApi {
    @GET(ApiEndpoints.My.ME) suspend fun me(): Response<MeDto>
    /** 숫자 하나를 그대로 준다 — 원문으로 받는다. */
    @GET(ApiEndpoints.My.NOTI_UNREAD) suspend fun notiUnread(): Response<ResponseBody>
    @GET(ApiEndpoints.My.QUOTE_UNREAD) suspend fun quoteUnread(): Response<CountDto>
    @GET(ApiEndpoints.My.MEMBERSHIP) suspend fun membership(): Response<MembershipDto>
    @GET(ApiEndpoints.My.PENDING_POSTINGS) suspend fun pendingPostings(): Response<PendingSummaryDto>
    @GET(ApiEndpoints.My.CASTING_MATERIAL_ACCESS) suspend fun castingAccess(): Response<AllowedDto>
    @GET(ApiEndpoints.My.SUPPORT_CHAT_ELIGIBILITY) suspend fun supportChat(): Response<EligibleDto>
}
