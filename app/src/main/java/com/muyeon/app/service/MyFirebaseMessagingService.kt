package com.muyeon.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.muyeon.app.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import androidx.core.content.edit
import com.muyeon.app.webview.WebViewActivity

class MyFirebaseMessagingService : FirebaseMessagingService() {
    private val tag = "FCMService"
    private lateinit var prefs: SharedPreferences
    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences(com.muyeon.app.utils.Constants.PREFS_NAME, Context.MODE_PRIVATE)
        createNotificationChannel()
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.POST_NOTIFICATIONS
            )
            if (permission != PackageManager.PERMISSION_GRANTED) {
                Log.w(tag, "Notification permission not granted, skipping notification")
                return
            }
        }

        var title: String? = null
        var body: String? = null
        var notificationType: String? = null
        var notificationUrl: String? = null
        val customData = mutableMapOf<String, String>()

        if (remoteMessage.data.isNotEmpty()) {
            val data = remoteMessage.data

            title = data["title"]
            body = data["body"]
            notificationType = data["notification_type"]
            notificationUrl = data["notification_url"]

            val standardFields = setOf("title", "body", "notification_type", "notification_url", "notification_id")
            data.forEach { (key, value) ->
                if (key !in standardFields) {
                    customData[key] = value
                }
            }
        }

        remoteMessage.notification?.let { n ->
            if (title.isNullOrEmpty()) title = n.title
            if (body.isNullOrEmpty()) body = n.body
        }

        val finalTitle = title.orDefault { getString(R.string.app_name) }
        val finalBody = body.orDefault { getString(R.string.notification) }
        val finalType = notificationType ?: "unknown"
        val finalUrl = notificationUrl
        
        when (finalType) {
            "booking_success" -> {
                Log.d(tag, "✅ Handling booking_success notification")
                showNotification(finalTitle, finalBody, finalUrl, customData)
            }
            else -> {
                showNotification(finalTitle, finalBody, finalUrl, customData)
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(tag, "🔑 FCM 새 토큰 발급: $token")
        prefs.edit { putString(com.muyeon.app.utils.Constants.KEY_FCM_TOKEN, token) }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                com.muyeon.app.utils.Constants.CHANNEL_ID,
                com.muyeon.app.utils.Constants.CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = com.muyeon.app.utils.Constants.CHANNEL_DESCRIPTION
                enableVibration(true)
                setShowBadge(true)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showNotification(
        title: String,
        body: String,
        url: String?,
        customData: Map<String, String> = emptyMap()
    ) {
        val intent = Intent(this, WebViewActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (!url.isNullOrEmpty()) putExtra("notification_url", url)
            customData.forEach { (k, v) -> putExtra(k, v) }
        }

        val notificationId = System.currentTimeMillis().toInt()
        val pendingIntent = PendingIntent.getActivity(
            this,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, com.muyeon.app.utils.Constants.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notification)
    }

    private fun String?.orDefault(default: () -> String) = if (this.isNullOrBlank()) default() else this
}