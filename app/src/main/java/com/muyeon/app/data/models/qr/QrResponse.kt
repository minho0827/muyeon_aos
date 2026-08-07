package com.muyeon.app.data.models.qr

import com.google.gson.annotations.SerializedName

data class QrResponse(
    @SerializedName("rsv_seq")
    val rsvSeq: Int?,

    @SerializedName("mbr_seq")
    val mbrSeq: Int?,

    @SerializedName("cf_fd_seq")
    val cfFdSeq: Int?,

    @SerializedName("rsv_result_cd")
    val rsvResultCd: String?,

    @SerializedName("result_msg")
    val resultMsg: String?,

    @SerializedName("result_cd")
    val resultCd: String?
)