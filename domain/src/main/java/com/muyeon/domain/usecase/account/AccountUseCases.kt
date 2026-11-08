package com.muyeon.domain.usecase.account

import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.model.account.MyAccount
import com.muyeon.domain.repository.AccountRepository

/**
 * 개인정보 수정(웹 ProfileEdit) — 저장 규칙:
 *   이름이 비면 막는다 · 본인확인으로 잠긴 이름은 보내지 않는다 · 휴대폰은 보내지 않는다(본인확인으로만 바뀐다).
 */
class EditAccountUseCase(private val repo: AccountRepository) {
    suspend fun load() = repo.me()

    suspend fun save(current: MyAccount, name: String, image: String?): ApiResult<MyAccount> {
        val n = name.trim()
        if (!current.nameLocked && n.isEmpty()) return ApiResult.Error(IllegalArgumentException("name"), "이름을 입력해 주세요.")
        return repo.update(if (current.nameLocked) null else n, image.orEmpty())
    }

    suspend fun upload(jpeg: ByteArray): ApiResult<String> = repo.uploadImage(jpeg).let { r ->
        if (r is ApiResult.Success && r.data.isBlank()) ApiResult.Error(IllegalStateException("url"), "사진을 올리지 못했어요.") else r
    }

    suspend fun startIdentity(): ApiResult<String> = repo.startIdentity(RETURN_TO).let { r ->
        if (r is ApiResult.Success && r.data.isBlank()) ApiResult.Error(IllegalStateException("authUrl"), "본인인증 창 주소를 받지 못했어요.") else r
    }

    suspend fun linkIdentity(token: String) = repo.linkIdentity(token)

    companion object {
        /** 표준창이 끝나고 돌아올 주소(웹과 같은 경로) — 앱은 이 주소로 가는 순간을 가로채 토큰을 읽는다. */
        const val RETURN_TO = "/profileEdit"
        const val TOKEN_PARAM = "niceToken"
        const val ERROR_PARAM = "niceError"
    }
}

/** 비밀번호 변경(웹 ChangePassword) — 영문·숫자·특수문자 8~20자, 확인 칸과 같아야 한다. */
class ChangePasswordUseCase(private val repo: AccountRepository) {
    enum class Invalid { POLICY, MISMATCH }

    fun check(password: String, confirm: String): Invalid? = when {
        !POLICY.matches(password) -> Invalid.POLICY
        confirm.isEmpty() || password != confirm -> Invalid.MISMATCH
        else -> null
    }

    suspend operator fun invoke(password: String) = repo.changePassword(password)

    companion object {
        /** 웹 passwordRegex 와 같다. */
        val POLICY = Regex("^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-={}\\[\\]|\\\\:;\"'<>,.?/]).{8,20}$")
    }
}

/** 대타 신뢰도(웹 SubTrust). */
class GetSubTrustUseCase(private val repo: AccountRepository) {
    suspend operator fun invoke() = repo.subTrust()
}
