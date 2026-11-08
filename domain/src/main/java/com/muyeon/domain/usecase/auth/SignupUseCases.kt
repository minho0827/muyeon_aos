package com.muyeon.domain.usecase.auth

import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.model.auth.LoginSession
import com.muyeon.domain.model.auth.NiceConfig
import com.muyeon.domain.model.auth.NiceIdentity
import com.muyeon.domain.model.auth.RoleSubmit
import com.muyeon.domain.repository.AuthRepository

// ★ 2026-11-08 [b1] 웹 pages/account/Register.js · SocialSignup.js · util/consent.js · util/niceIdentity.js ·
//  routes/AppRouter.js(__onRoleComplete · ROLE_LANDING) · util/pendingPosting.js(isTaskResumeRedirect) 를 옮긴 규칙.

/**
 * 약관 동의값 — 웹 consent.js toAgreements 와 같은 모양(가입 요청에 그대로 싣는다).
 *  [checked] 는 약관 화면(SignupTermsScreen)이 넘기는 { terms, privacy, …, marketingPush } 체크 상태.
 */
object SignupAgreements {
    val REQUIRED = listOf("terms", "privacy", "contactSharing", "location", "over14")
    const val VERSION = "2026-10-01"

    fun isAllRequired(checked: Map<String, Boolean>?): Boolean = checked != null && REQUIRED.all { checked[it] == true }

    fun toBody(checked: Map<String, Boolean>): Map<String, Any> {
        fun on(k: String) = checked[k] == true
        return mapOf(
            "terms" to on("terms"), "privacy" to on("privacy"), "contactSharing" to on("contactSharing"),
            "location" to on("location"), "over14" to on("over14"),
            "marketing" to (on("marketingEmail") || on("marketingSms") || on("marketingPush")),
            "marketingEmail" to on("marketingEmail"), "marketingSms" to on("marketingSms"), "marketingPush" to on("marketingPush"),
            "version" to VERSION,
        )
    }
}

/** 가입 뒤 갈 곳 — 웹 safeRedirect · isTaskResumeRedirect(광고 공고 자동 등록 · 레슨 요청 작성 중 가입). */
object SignupRedirect {
    const val HOME = "/home"
    private val PENDING_RESUME = setOf("/subs/create?resume=pending", "/jobs/create?resume=pending")

    fun safe(value: String?): String? = value?.takeIf { it.startsWith("/") && !it.startsWith("//") }

    /** 회원유형 선택 없이 하던 일로 바로 돌아가는 주소인가. */
    fun isTaskResume(value: String?): Boolean {
        val v = safe(value) ?: return false
        return v in PENDING_RESUME || v.startsWith("/quoteRequest")
    }
}

/** 휴대폰 본인확인(가입 전, 로그인 안 한 상태) — 설정 · 표준창 주소 · 결과(표시용). */
class SignupIdentityUseCase(private val repo: AuthRepository) {
    /** 조회 실패 = 미사용(웹: catch 무시 — 가입 자체를 막지 않는다). */
    suspend fun config(): NiceConfig = (repo.niceConfig() as? ApiResult.Success)?.data ?: NiceConfig(available = false, required = false)

    suspend fun start(returnTo: String): ApiResult<String> = repo.startNice(returnTo).let { r ->
        if (r is ApiResult.Success && r.data.isBlank()) ApiResult.Error(IllegalStateException("authUrl"), NO_AUTH_URL) else r
    }

    suspend fun result(token: String): ApiResult<NiceIdentity> = repo.niceResult(token)

    companion object {
        /** 표준창이 끝나고 돌아올 주소(웹과 같은 경로) — 앱은 이 주소로 가는 순간을 가로채 토큰을 읽는다. */
        const val RETURN_REGISTER = "/register"
        const val RETURN_SOCIAL = "/social-signup"
        /** 웹 niceIdentity.js 의 같은 문구(서버 계약 쪽 문장). */
        const val NO_AUTH_URL = "본인인증 창 주소를 받지 못했어요."
    }
}

/**
 * 이메일 가입 — 웹 Register.js.
 *  확인 순서(웹 submit 그대로): 빈 칸(이메일·비밀번호·이름) → 이메일 인증 → (필수면) 본인인증 → 8자 이상 → 확인 일치 → 약관 동의값.
 */
class RegisterUseCase(private val repo: AuthRepository) {
    enum class Invalid { MISSING_FIELDS, EMAIL_UNVERIFIED, IDENTITY_MISSING, PASSWORD_SHORT, PASSWORD_MISMATCH, TERMS_MISSING }
    enum class CodeInvalid { EMAIL, CODE }

    fun checkEmail(email: String): CodeInvalid? = if (EMAIL_RE.matches(email)) null else CodeInvalid.EMAIL
    fun checkCode(code: String): CodeInvalid? = if (code.length < CODE_LENGTH) CodeInvalid.CODE else null

    fun check(
        email: String, password: String, confirm: String, name: String,
        emailVerified: Boolean, identityRequired: Boolean, identity: NiceIdentity?, agreements: Map<String, Boolean>?,
    ): Invalid? = when {
        email.isEmpty() || password.isEmpty() || name.isEmpty() -> Invalid.MISSING_FIELDS
        !emailVerified -> Invalid.EMAIL_UNVERIFIED
        identityRequired && identity == null -> Invalid.IDENTITY_MISSING
        password.length < MIN_PASSWORD -> Invalid.PASSWORD_SHORT
        password != confirm -> Invalid.PASSWORD_MISMATCH
        !SignupAgreements.isAllRequired(agreements) -> Invalid.TERMS_MISSING
        else -> null
    }

    suspend fun sendCode(email: String) = repo.sendSignupCode(email)
    suspend fun verifyCode(email: String, code: String) = repo.verifySignupCode(email, code)

    /** 본인확인을 했으면 휴대폰은 보내지 않는다(인증된 번호가 정본) — 웹 phone: identity ? undefined : (phone || undefined). */
    suspend operator fun invoke(
        email: String, password: String, name: String, phone: String, identity: NiceIdentity?, agreements: Map<String, Boolean>,
    ): ApiResult<LoginSession> = repo.register(
        email, password, name,
        phone = if (identity != null) null else phone.ifEmpty { null },
        identityToken = identity?.token,
        agreements = SignupAgreements.toBody(agreements),
    )

    companion object {
        const val CODE_LENGTH = 6
        const val MIN_PASSWORD = 8
        private val EMAIL_RE = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
    }
}

/** 소셜 신규회원 추가정보 — 웹 SocialSignup.js(이름 필수 · 필수면 본인인증 · 닉네임 안 보냄). */
class SocialSignupUseCase(private val repo: AuthRepository) {
    enum class Invalid { NAME, IDENTITY_MISSING }

    /** 소셜 자동가입 기본 이름이면 입력칸을 비운다(웹 DEFAULT_NAMES). */
    fun initialName(current: String?): String = current?.takeIf { it !in DEFAULT_NAMES }.orEmpty()

    fun check(name: String, identityRequired: Boolean, identity: NiceIdentity?): Invalid? = when {
        name.isEmpty() -> Invalid.NAME
        identityRequired && identity == null -> Invalid.IDENTITY_MISSING
        else -> null
    }

    suspend operator fun invoke(name: String, identity: NiceIdentity?): ApiResult<String> = repo.completeSocial(name, identity?.token)

    private companion object { val DEFAULT_NAMES = setOf("애플회원", "카카오회원") }
}

/**
 * 가입 직후 회원유형 선택 완료 — 웹 AppRouter __onRoleComplete 를 앱이 직접(웹 수정 금지라 같은 API 를 앱이 부른다).
 *  ① 역할 추가(실패 무시) ② 서류가 있으면(학원은 사업자번호만으로도) 제출 ③ 갈 곳(ROLE_LANDING · 심사 안내).
 *  공간(SPACE)은 기능이 꺼져 있어 바로 홈(웹 SPACE_FEATURE_ENABLED=false).
 */
class CompleteRoleOnboardingUseCase(private val repo: AuthRepository) {
    suspend operator fun invoke(role: String, urls: List<String>, academyName: String?, businessNumber: String?, startDate: String?): String {
        if (role == SPACE) return SignupRedirect.HOME
        repo.addRole(role)
        val submitted = submit(role, urls, academyName, businessNumber, startDate)
        return landing(role, submitted)
    }

    /** 서류만 제출(역할은 이미 있음) — 웹 __onRoleVerify 의 submitDocs. 차단 화면 '학원 인증하기'가 쓴다. */
    suspend fun submit(role: String, urls: List<String>, academyName: String?, businessNumber: String?, startDate: String?): RoleSubmit {
        if (urls.isEmpty() && !(role == ACADEMY && !businessNumber.isNullOrBlank())) return RoleSubmit.NONE
        return when (val r = repo.submitRoleVerification(role, urls, academyName?.ifBlank { null }, businessNumber?.ifBlank { null }, startDate?.ifBlank { null })) {
            is ApiResult.Success -> if (r.data) RoleSubmit.APPROVED else RoleSubmit.SUBMITTED
            is ApiResult.Error -> RoleSubmit.NONE
        }
    }

    /** 웹 __onRoleComplete 의 to 결정(광고 퍼널 복귀 takeFunnelRedirect 는 웹 창 sessionStorage 라 앱엔 없다). */
    fun landing(role: String, submitted: RoleSubmit): String = when {
        submitted == RoleSubmit.APPROVED -> MYPAGE
        role in IDENTITY_ROLES -> ROLE_LANDING[role] ?: MAIN
        submitted == RoleSubmit.SUBMITTED -> "$MYPAGE?verifySubmitted=1"
        role in VERIFICATION_REQUIRED_ROLES -> MYPAGE
        else -> ROLE_LANDING[role] ?: MAIN
    }

    companion object {
        const val SPACE = "SPACE"
        const val ACADEMY = "ACADEMY"
        const val MYPAGE = "/mypage"
        const val MAIN = "/main"
        val IDENTITY_ROLES = setOf("TEACHER", "DANCER")
        val VERIFICATION_REQUIRED_ROLES = setOf("ACADEMY", "TEAM")
        val ROLE_LANDING = mapOf("HOBBY" to "/lessons", "SPACE" to "/home", "ACADEMY" to "/jobs", "TEAM" to "/jobs", "TEACHER" to "/jobs", "DANCER" to "/jobs")
    }
}
