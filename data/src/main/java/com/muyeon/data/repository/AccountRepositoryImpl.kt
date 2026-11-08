package com.muyeon.data.repository

import com.muyeon.data.common.safeApiCall
import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.model.account.MyAccount
import com.muyeon.domain.repository.AccountRepository
import com.muyeon.network.api.AccountApi
import com.muyeon.network.dto.account.IdentityTokenBody
import com.muyeon.network.dto.account.ReturnToBody
import com.muyeon.network.dto.account.UpdateAccountBody
import com.squareup.moshi.Moshi
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

class AccountRepositoryImpl(private val api: AccountApi, private val moshi: Moshi) : AccountRepository {
    override suspend fun me(): ApiResult<MyAccount> = safeApiCall(moshi) { api.me() }.map { it.toDomain() }

    override suspend fun update(name: String?, image: String): ApiResult<MyAccount> =
        safeApiCall(moshi) { api.update(UpdateAccountBody(name, image)) }.map { it.toDomain() }

    override suspend fun startIdentity(returnTo: String): ApiResult<String> =
        safeApiCall(moshi) { api.startIdentity(ReturnToBody(returnTo)) }.map { it.authUrl.orEmpty() }

    override suspend fun withdrawalInfo(): ApiResult<com.muyeon.domain.model.account.WithdrawalInfo> =
        safeApiCall(moshi) { api.withdrawalInfo() }.map { body ->
            val o = org.json.JSONObject(body.string().ifBlank { "{}" })
            fun notices(k: String) = o.optJSONArray(k).let { a ->
                if (a == null) emptyList() else (0 until a.length()).mapNotNull { a.optJSONObject(it) }.map { n ->
                    com.muyeon.domain.model.account.WithdrawalNotice(
                        n.optString("code"), n.optString("label"), n.optString("detail").ifBlank { null }, n.optString("action").ifBlank { null },
                    )
                }
            }
            val summary = o.optJSONObject("summary")
            val groups = o.optJSONArray("reasonGroups")
            com.muyeon.domain.model.account.WithdrawalInfo(
                nickname = o.optString("nickname").ifBlank { "회원" },
                requiresPassword = o.optBoolean("requiresPassword", true),
                confirmText = o.optString("confirmText"),
                provider = o.optString("provider").ifBlank { null },
                retentionMonths = o.optInt("retentionMonths", 3),
                rejoinableFrom = o.optString("rejoinableFrom").ifBlank { null },
                blockers = notices("blockers"), warnings = notices("warnings"), autoCancel = notices("autoCancel"),
                summary = summary?.keys()?.asSequence()?.associateWith { summary.optInt(it) } ?: emptyMap(),
                reasonGroups = if (groups == null) emptyList() else (0 until groups.length()).mapNotNull { groups.optJSONObject(it) }.map { g ->
                    val items = g.optJSONArray("items")
                    com.muyeon.domain.model.account.WithdrawalReasonGroup(
                        g.optString("key"), g.optString("title"),
                        if (items == null) emptyList() else (0 until items.length()).mapNotNull { items.optJSONObject(it) }.map { it.optString("code") to it.optString("label") },
                    )
                },
            )
        }

    override suspend fun withdraw(reasonCode: String, reasonText: String?, agreements: List<String>, passwordMode: Boolean, secret: String): ApiResult<String?> =
        safeApiCall(moshi) {
            val body = org.json.JSONObject().put("reasonCode", reasonCode)
                .put("agreements", org.json.JSONObject().apply { agreements.forEach { put(it, true) } })
                .put(if (passwordMode) "password" else "confirmText", secret)
            reasonText?.let { body.put("reasonText", it) }
            api.withdraw(body.toString().toRequestBody("application/json".toMediaType()))
        }.map { b -> org.json.JSONObject(b.string().ifBlank { "{}" }).optString("rejoinableFrom").ifBlank { null } }

    override suspend fun changePassword(newPassword: String): ApiResult<Unit> =
        com.muyeon.data.common.safeUnitCall(moshi) { api.changePassword(com.muyeon.network.dto.account.NewPasswordBody(newPassword)) }

    override suspend fun subTrust(): ApiResult<com.muyeon.domain.model.account.SubTrust> = safeApiCall(moshi) { api.subTrust() }.map { it.toDomain() }

    override suspend fun linkIdentity(token: String): ApiResult<MyAccount> =
        safeApiCall(moshi) { api.linkIdentity(IdentityTokenBody(token)) }.map { it.toDomain() }
}
