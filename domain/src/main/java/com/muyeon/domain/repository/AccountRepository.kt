package com.muyeon.domain.repository

import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.model.account.MyAccount

/** 개인정보 수정 — 내 정보 · 이름/사진 저장 · 사진 올리기 · 휴대폰 본인확인(NICE). */
interface AccountRepository {
    suspend fun me(): ApiResult<MyAccount>
    /** [name] null 이면 이름은 보내지 않는다(본인확인으로 잠김). [image] 빈 글자 = 사진 지우기. */
    suspend fun update(name: String?, image: String): ApiResult<MyAccount>
    suspend fun uploadImage(jpeg: ByteArray): ApiResult<String>
    /** 표준창 주소 — 인증이 끝나면 [returnTo]?niceToken=… 또는 ?niceError=… 로 돌아온다. */
    suspend fun startIdentity(returnTo: String): ApiResult<String>
    suspend fun linkIdentity(token: String): ApiResult<MyAccount>
}
