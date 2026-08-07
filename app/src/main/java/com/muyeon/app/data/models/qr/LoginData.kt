package com.muyeon.app.data.models.qr

import com.google.gson.annotations.SerializedName

@Suppress("unused")
data class LoginData(
    @SerializedName("accessToken")
    val accessToken: String,

    @SerializedName("refreshToken")
    val refreshToken: String,

    @SerializedName("passwordExpired")
    val passwordExpired: Boolean,

    @SerializedName("mbrSeq")
    val mbrSeq: Int,

    @SerializedName("isNewMember")
    val isNewMember: Boolean
)