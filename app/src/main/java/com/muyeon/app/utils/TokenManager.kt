package com.muyeon.app.utils

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import com.muyeon.app.data.api_endpoint.TokenAPI
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import androidx.core.content.edit

object TokenManager {
    private const val PREF_NAME = "secure_prefs_v2"
    private const val KEY_STORE_ALIAS = "token_key_alias"

    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_REFRESH_TOKEN = "refresh_token"
    private const val KEY_ACCESS_TOKEN_IV = "access_token_iv"
    private const val KEY_REFRESH_TOKEN_IV = "refresh_token_iv"

    // Define a constant for the cipher algorithm
    private const val AES_GCM_NO_PADDING_ALGORITHM = "AES/GCM/NoPadding"

    // Use regular SharedPreferences since we're handling the encryption
    private fun getRegularPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    private fun getSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)

        if (!keyStore.containsAlias(KEY_STORE_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                "AndroidKeyStore"
            )
            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                KEY_STORE_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()

            keyGenerator.init(keyGenParameterSpec)
            return keyGenerator.generateKey()
        }
        return keyStore.getKey(KEY_STORE_ALIAS, null) as SecretKey
    }

    fun saveTokens(context: Context, accessToken: String, refreshToken: String) {
        val key = getSecretKey()
        val cipher = Cipher.getInstance(AES_GCM_NO_PADDING_ALGORITHM)

        // Encrypt Access Token
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val encryptedAccessToken = cipher.doFinal(accessToken.toByteArray(Charsets.UTF_8))
        val accessTokenIV = cipher.iv

        // Encrypt Refresh Token
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val encryptedRefreshToken = cipher.doFinal(refreshToken.toByteArray(Charsets.UTF_8))
        val refreshTokenIV = cipher.iv

        // Save encrypted data and IVs to SharedPreferences
        getRegularPrefs(context).edit {
            putString(KEY_ACCESS_TOKEN, Base64.encodeToString(encryptedAccessToken, Base64.DEFAULT))
            putString(KEY_ACCESS_TOKEN_IV, Base64.encodeToString(accessTokenIV, Base64.DEFAULT))
            putString(
                KEY_REFRESH_TOKEN,
                Base64.encodeToString(encryptedRefreshToken, Base64.DEFAULT)
            )
            putString(KEY_REFRESH_TOKEN_IV, Base64.encodeToString(refreshTokenIV, Base64.DEFAULT))
        }
    }

    fun getAccessToken(context: Context): String? {
        val encryptedToken = getRegularPrefs(context).getString(KEY_ACCESS_TOKEN, null)
        val iv = getRegularPrefs(context).getString(KEY_ACCESS_TOKEN_IV, null)

        return if (encryptedToken != null && iv != null) {
            try {
                val key = getSecretKey()
                val cipher = Cipher.getInstance(AES_GCM_NO_PADDING_ALGORITHM)
                val ivSpec = GCMParameterSpec(128, Base64.decode(iv, Base64.DEFAULT))
                cipher.init(Cipher.DECRYPT_MODE, key, ivSpec)
                val decryptedBytes = cipher.doFinal(Base64.decode(encryptedToken, Base64.DEFAULT))
                String(decryptedBytes, Charsets.UTF_8)
            } catch (e: Exception) {
                Log.e("TokenManager", "Decryption failed for access token", e)
                null
            }
        } else {
            null
        }
    }

    fun getRefreshToken(context: Context): String? {
        val encryptedToken = getRegularPrefs(context).getString(KEY_REFRESH_TOKEN, null)
        val iv = getRegularPrefs(context).getString(KEY_REFRESH_TOKEN_IV, null)

        return if (encryptedToken != null && iv != null) {
            try {
                val key = getSecretKey()
                val cipher = Cipher.getInstance(AES_GCM_NO_PADDING_ALGORITHM)
                val ivSpec = GCMParameterSpec(128, Base64.decode(iv, Base64.DEFAULT))
                cipher.init(Cipher.DECRYPT_MODE, key, ivSpec)
                val decryptedBytes = cipher.doFinal(Base64.decode(encryptedToken, Base64.DEFAULT))
                String(decryptedBytes, Charsets.UTF_8)
            } catch (e: Exception) {
                Log.e("TokenManager", "Decryption failed for refresh token", e)
                null
            }
        } else {
            null
        }
    }

    fun clearTokens(context: Context) {
        getRegularPrefs(context).edit {
            remove(KEY_ACCESS_TOKEN)
                .remove(KEY_REFRESH_TOKEN)
                .remove(KEY_ACCESS_TOKEN_IV)
                .remove(KEY_REFRESH_TOKEN_IV)
                .remove(com.muyeon.app.utils.Constants.KEY_FCM_TOKEN)
        }
    }

    suspend fun checkToken(context: Context): WebMessageStatus? {
        val accessToken =
            getAccessToken(context)
        val refreshToken =
            getRefreshToken(context)

        if (accessToken.isNullOrEmpty() || refreshToken.isNullOrEmpty()) {
            return WebMessageStatus.NOT_AUTHENTICATED
        }
        return TokenAPI.refreshToken(context, accessToken, refreshToken)
    }
}