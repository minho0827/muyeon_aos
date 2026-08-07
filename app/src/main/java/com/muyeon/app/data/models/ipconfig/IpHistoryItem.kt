package com.muyeon.app.data.models.ipconfig

data class IpHistoryItem(
    val ipAddress: String,
    val timestamp: Long,
    val formattedDate: String
)
