package com.muyeon.data.local

import android.content.Context
import com.muyeon.domain.common.KeyValueStore

/** 화면 기억값 저장(웹 usePersistedState). */
class PrefsKeyValueStore(context: Context) : KeyValueStore {
    private val prefs = context.applicationContext.getSharedPreferences("muyeon.uiState", Context.MODE_PRIVATE)
    override fun getString(key: String): String? = prefs.getString(key, null)
    override fun putString(key: String, value: String?) {
        prefs.edit().apply { if (value == null) remove(key) else putString(key, value) }.apply()
    }
}
