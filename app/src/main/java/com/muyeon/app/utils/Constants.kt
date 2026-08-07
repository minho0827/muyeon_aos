package com.muyeon.app.utils

import android.content.Context
import com.muyeon.app.BuildConfig
import com.muyeon.app.data.repository.IpConfigRepository

object Constants {
    fun getBaseUrl(context: Context): String =
        BuildConfig.API_BASE_URL

    const val FILE_PICKER_REQUEST_CODE = 1001
    const val TYPE = "image/*"
    const val AUTHORIZED = "Forever"
    const val LIMITED = "Limited"
    const val REJECTED = "Rejected"

    const val BASE_URL_QR = "http://alb-brycenkorea-dev-1949243487.ap-northeast-2.elb.amazonaws.com:8000"

    const val CHANNEL_ID = "healthcare_notifications"
    const val CHANNEL_NAME = "Healthcare Diet Notifications"
    const val CHANNEL_DESCRIPTION = "Notifications for Healthcare Diet App"
    const val PREFS_NAME = "fcm_prefs"
    const val KEY_FCM_TOKEN = "fcm_token"
    const val LOCATION_TIMEOUT = 10000L
    const val POLLING_INTERVAL_MS = 1000L
}