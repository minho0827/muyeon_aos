package com.muyeon.app.data.models.token

import com.google.gson.annotations.SerializedName

data class RefreshTokenResponse(
    @SerializedName("data")
    val `data`: Data,
    @SerializedName("status")
    val status: Int
) {
    data class Data(
        @SerializedName("isRefreshToken")
        val isRefreshToken: Boolean
    )
}