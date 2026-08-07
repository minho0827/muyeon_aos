package com.muyeon.app.data.models.qr

import com.google.gson.annotations.SerializedName

data class ReservationData(
    @SerializedName("rsv_seq")
    val rsvSeq: Int?,

    @SerializedName("mbr_seq")
    val mbrSeq: Int?,

    @SerializedName("cf_fd_seq")
    val cfFdSeq: Int?,

    @SerializedName("rsv_status_cd")
    val rsvStatusCd: String?,

    @SerializedName("rsv_date")
    val rsvDate: String?,

    @SerializedName("rsv_cnt")
    val rsvCnt: Int?
)