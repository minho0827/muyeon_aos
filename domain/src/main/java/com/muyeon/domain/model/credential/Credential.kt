package com.muyeon.domain.model.credential

/** 경력·자격 확인 한 항목 — [status] NONE(미등록)·PENDING(확인 중)·APPROVED(확인 완료)·REJECTED(반려, [memo] 사유)·WITHDRAWN(회원 철회, [withdrawnAt]). */
data class CredentialItem(
    val kind: String,
    val label: String,
    val desc: String?,
    val docs: String?,
    val status: String,
    val title: String?,
    val years: String?,
    val memo: String?,
    val id: Long? = null,
    val withdrawnAt: String? = null,
) {
    /** 서류를 (다시) 낼 수 있다 — 철회한 항목도 다시 등록. */
    val canSubmit: Boolean get() = status == "NONE" || status == "REJECTED" || status == "WITHDRAWN"

    /** 확인 중·확인 완료만 철회할 수 있다(나머지는 서버도 409). */
    val canWithdraw: Boolean get() = status == "PENDING" || status == "APPROVED"
}

data class MyCredentials(val identityVerified: Boolean, val unsortedPending: Boolean, val items: List<CredentialItem>)

/** 올릴 서류 한 장 — 이미지는 줄인 JPEG, PDF 는 원본. */
data class CredentialDocument(val bytes: ByteArray, val fileName: String, val mimeType: String)
