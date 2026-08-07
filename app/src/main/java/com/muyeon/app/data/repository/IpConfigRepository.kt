package com.muyeon.app.data.repository

import android.content.Context
import com.muyeon.app.data.models.ipconfig.IpHistoryItem
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.content.edit

class IpConfigRepository(context: Context) {
    private val prefs =
        context.getSharedPreferences("ip_config_prefs", Context.MODE_PRIVATE)
    private val dateFormat =
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    fun saveIpAddress(ipAddress: String) {
        val timestamp = System.currentTimeMillis()
        val formattedDate = dateFormat.format(Date(timestamp))
        val entry = IpHistoryItem(ipAddress, timestamp, formattedDate)

        val current = getIpHistory().toMutableList()
        current.removeAll { it.ipAddress == ipAddress }
        current.add(0, entry)
        val limited = current.take(20)

        val set = limited.map { "${it.ipAddress}|${it.timestamp}|${it.formattedDate}" }
            .toSet()
        prefs.edit {
            putStringSet("ip_history", set)
                .putString("current_ip", ipAddress)
        }
    }

    fun getIpHistory(): List<IpHistoryItem> {
        val set = prefs.getStringSet("ip_history", emptySet()) ?: emptySet()
        return set.mapNotNull { raw ->
            raw.split("|").takeIf { it.size == 3 }?.let {
                val (ip, ts, date) = it
                IpHistoryItem(ip, ts.toLongOrNull() ?: 0L, date)
            }
        }.sortedByDescending { it.timestamp }
    }

    fun getCurrentIp(): String =
        prefs.getString("current_ip", "") ?: ""

}
