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

/**
 * 회원탈퇴(웹 Withdraw.js) — 동의 key 는 백엔드 WITHDRAW_AGREEMENTS 와 1:1, 자동 취소가 있으면 [AUTO_CANCEL] 도 받는다.
 *   서버가 WITHDRAW_BLOCKED 로 거절하면(화면을 띄운 뒤 예약·결제가 생김) [WithdrawResult.Blocked].
 */
class WithdrawUseCase(private val repo: AccountRepository) {
    suspend fun info() = repo.withdrawalInfo()

    fun agreementKeys(autoCancel: Boolean) = if (autoCancel) AGREEMENTS + AUTO_CANCEL else AGREEMENTS

    suspend operator fun invoke(
        info: com.muyeon.domain.model.account.WithdrawalInfo, reasonCode: String, reasonText: String, secret: String,
    ): ApiResult<com.muyeon.domain.model.account.WithdrawResult> {
        val r = repo.withdraw(
            reasonCode, if (reasonCode == ETC) reasonText.trim() else null,
            agreementKeys(info.autoCancel.isNotEmpty()), info.requiresPassword, secret,
        )
        return when {
            r is ApiResult.Success -> ApiResult.Success(com.muyeon.domain.model.account.WithdrawResult.Done(r.data ?: info.rejoinableFrom))
            r is ApiResult.Error && r.code == "WITHDRAW_BLOCKED" -> ApiResult.Success(com.muyeon.domain.model.account.WithdrawResult.Blocked)
            else -> r as ApiResult.Error
        }
    }

    companion object {
        const val ETC = "ETC"
        val AGREEMENTS = listOf("dataDeleted", "retention", "rejoinBlocked")
        const val AUTO_CANCEL = "autoCancel"
    }
}

/** 레슨 전공 설정(웹 LessonGenreSettings) — 강사프로필 genres 만, 역할 부여 없음. */
class LessonGenresUseCase(private val repo: AccountRepository) {
    suspend fun load() = repo.lessonGenres()
    suspend fun save(genres: List<String>) = repo.saveLessonGenres(genres)
}
