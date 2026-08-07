package com.muyeon.app.utils

import android.os.Build
import androidx.annotation.RequiresApi
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.muyeon.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.security.KeyFactory
import java.security.interfaces.RSAPrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64
import java.util.Date

class ServiceAccountHelper {
    private val client = OkHttpClient()

    data class ServiceAccount(
        val clientEmail: String,
        val privateKey: String,
        val projectId: String,
        val tokenUri: String
    )

    /**
     * Load service account strictly from BuildConfig.
     * Throws IllegalStateException if fields are missing.
     */
    private fun loadServiceAccountFromBuildConfig(): ServiceAccount {
        val clientEmail = BuildConfig.SA_CLIENT_EMAIL
        val rawPrivate = BuildConfig.SA_PRIVATE_KEY
        val projectId = BuildConfig.SA_PROJECT_ID
        val tokenUriRaw = BuildConfig.SA_TOKEN_URI

        if (clientEmail.isBlank() || rawPrivate.isBlank() || projectId.isBlank()) {
            throw IllegalStateException("Service account BuildConfig fields are not set. Ensure SA_CLIENT_EMAIL, SA_PRIVATE_KEY and SA_PROJECT_ID are provided.")
        }

        // Convert escaped newlines into actual newlines
        val privateKey = rawPrivate.replace("\\n", "\n")
        val tokenUri = tokenUriRaw.ifBlank { "https://oauth2.googleapis.com/token" }

        return ServiceAccount(clientEmail, privateKey, projectId, tokenUri)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun parsePrivateKey(pem: String): RSAPrivateKey {
        val cleaned = pem
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("\\s+".toRegex(), "")
        val decoded = Base64.getDecoder().decode(cleaned)
        val spec = PKCS8EncodedKeySpec(decoded)
        val kf = KeyFactory.getInstance("RSA")
        return kf.generatePrivate(spec) as RSAPrivateKey
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun createSignedJwt(scope: String = "https://www.googleapis.com/auth/firebase.messaging"): String =
        withContext(Dispatchers.IO) {
            val sa = loadServiceAccountFromBuildConfig()
            val nowSec = System.currentTimeMillis() / 1000
            val expSec = nowSec + 3600 // 1 hour

            val privateKey = parsePrivateKey(sa.privateKey)
            val algorithm = Algorithm.RSA256(null, privateKey)

            val token = JWT.create()
                .withIssuer(sa.clientEmail)
                .withSubject(sa.clientEmail)
                .withAudience(sa.tokenUri)
                .withIssuedAt(Date(nowSec * 1000))
                .withExpiresAt(Date(expSec * 1000))
                .withClaim("scope", scope)
                .sign(algorithm)

            token
        }

    private suspend fun fetchAccessToken(signedJwt: String): String? = withContext(Dispatchers.IO) {
        val sa = loadServiceAccountFromBuildConfig()
        val form = JSONObject().apply {
            put("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer")
            put("assertion", signedJwt)
        }
        val body = form.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val req = Request.Builder()
            .url(sa.tokenUri)
            .post(body)
            .addHeader("Content-Type", "application/json")
            .build()

        client.newCall(req).execute().use { resp ->
            val code = resp.code
            val respBody = resp.body?.string()
            if (code in 200..299 && respBody != null) {
                val o = JSONObject(respBody)
                return@withContext o.optString("access_token", null.toString())
            } else {
                return@withContext null
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getAccessToken(): String? {
        val jwt = createSignedJwt()
        return fetchAccessToken(jwt)
    }

    fun getProjectId(): String = loadServiceAccountFromBuildConfig().projectId
}
