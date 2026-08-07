package com.muyeon.app.domain.models.device_info

data class DeviceInfo(
    val deviceId: String,
    val deviceName: String,
    val deviceModel: String,
    val systemName: String,
    val systemVersion: String,
    val appVersion: String,
    val deviceToken: String,
    val cameraStatus: String,
    val galleryStatus: String,
    val locationStatus: String,
    val pushNotificationStatus: String
)
