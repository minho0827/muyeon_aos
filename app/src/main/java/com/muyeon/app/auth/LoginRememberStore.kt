package com.muyeon.app.auth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * 로그인 화면 '아이디·비밀번호 저장' — 웹 Login.js REMEMBER_KEY(localStorage, base64 로 살짝 가림)와 같은 기능.
 *   앱은 Android Keystore(AES-GCM)로 암호화해 둔다(TokenManager 와 같은 방식, 다른 키).
 *   켠 채 로그인에 성공하면 저장, 끈 채 성공하면 지운다(웹과 같다).
 */
class LoginRememberStore(context: Context) {
    data class Saved(val email: String, val password: String)

    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(): Saved? = runCatching {
        val data = prefs.getString(KEY_DATA, null) ?: return null
        val iv = prefs.getString(KEY_IV, null) ?: return null
        val cipher = Cipher.getInstance(ALGO)
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP)))
        val o = JSONObject(String(cipher.doFinal(Base64.decode(data, Base64.NO_WRAP)), Charsets.UTF_8))
        Saved(o.optString("email"), o.optString("password"))
    }.getOrNull()

    fun save(email: String, password: String) {
        runCatching {
            val cipher = Cipher.getInstance(ALGO)
            cipher.init(Cipher.ENCRYPT_MODE, key())
            val enc = cipher.doFinal(JSONObject().put("email", email).put("password", password).toString().toByteArray(Charsets.UTF_8))
            prefs.edit()
                .putString(KEY_DATA, Base64.encodeToString(enc, Base64.NO_WRAP))
                .putString(KEY_IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
                .apply()
        }
    }

    fun clear() = prefs.edit().remove(KEY_DATA).remove(KEY_IV).apply()

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return gen.generateKey()
    }

    private companion object {
        const val PREFS = "muyeon.loginRemember"
        const val KEY_DATA = "data"
        const val KEY_IV = "iv"
        const val ALIAS = "muyeon_login_remember"
        const val ALGO = "AES/GCM/NoPadding"
    }
}
