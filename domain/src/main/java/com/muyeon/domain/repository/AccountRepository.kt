package com.muyeon.domain.repository

import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.model.account.MyAccount

/** 개인정보 수정 — 내 정보 · 이름 저장(사진은 지금 값 그대로) · 휴대폰 본인확인(NICE) · 비밀번호 · 대타 신뢰도 · 탈퇴. */
interface AccountRepository {
    suspend fun me(): ApiResult<MyAccount>
    /** [name] null 이면 이름은 보내지 않는다(본인확인으로 잠김). [image] 빈 글자 = 사진 지우기. */
    suspend fun update(name: String?, image: String): ApiResult<MyAccount>
    /** 표준창 주소 — 인증이 끝나면 [returnTo]?niceToken=… 또는 ?niceError=… 로 돌아온다. */
    suspend fun startIdentity(returnTo: String): ApiResult<String>
    suspend fun linkIdentity(token: String): ApiResult<MyAccount>
    suspend fun withdrawalInfo(): ApiResult<com.muyeon.domain.model.account.WithdrawalInfo>
    /** [secret] 은 [passwordMode] 면 비밀번호, 아니면 확인 문구. 동의 key 들은 모두 true 로 보낸다. */
    suspend fun withdraw(reasonCode: String, reasonText: String?, agreements: List<String>, passwordMode: Boolean, secret: String): ApiResult<String?>
    suspend fun changePassword(newPassword: String): ApiResult<Unit>
    suspend fun subTrust(): ApiResult<com.muyeon.domain.model.account.SubTrust>
}
