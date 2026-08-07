package com.muyeon.app.data.models.token

import com.google.gson.annotations.SerializedName

data class TokenRequest(
    @SerializedName("accessToken") val accessToken: String,

    @SerializedName("refreshToken") val refreshToken: String
)