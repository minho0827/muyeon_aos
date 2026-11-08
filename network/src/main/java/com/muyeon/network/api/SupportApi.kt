package com.muyeon.network.api

import com.muyeon.network.dto.support.CreateSupportInquiryBody
import com.muyeon.network.dto.support.FaqDto
import com.muyeon.network.dto.support.NoticeDto
import com.muyeon.network.dto.support.SupportInquiryDto
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** 출처: content.controller.ts · support-inquiries.controller.ts. */
interface SupportApi {
    @GET(ApiEndpoints.Support.NOTICES) suspend fun notices(): Response<List<NoticeDto>>
    @GET(ApiEndpoints.Support.NOTICE) suspend fun notice(@Path("id") id: Long): Response<NoticeDto>
    @GET(ApiEndpoints.Support.FAQS) suspend fun faqs(@Query("category") category: String?): Response<List<FaqDto>>
    @GET(ApiEndpoints.Support.FAQ_CATEGORIES) suspend fun faqCategories(): Response<List<String>>
    @GET(ApiEndpoints.Support.MY_INQUIRIES) suspend fun myInquiries(): Response<List<SupportInquiryDto>>
    @GET(ApiEndpoints.Support.INQUIRY) suspend fun inquiry(@Path("id") id: Long): Response<SupportInquiryDto>
    @DELETE(ApiEndpoints.Support.INQUIRY) suspend fun deleteInquiry(@Path("id") id: Long): Response<ResponseBody>
    @POST(ApiEndpoints.Support.INQUIRIES) suspend fun createInquiry(@Body body: CreateSupportInquiryBody): Response<ResponseBody>
}
