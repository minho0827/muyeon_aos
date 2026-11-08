package com.muyeon.domain.model.auth

/** 휴대폰 본인확인(NICE) 설정 — 서버가 정한다(GET /identity/nice/config). 조회 실패 = 둘 다 false(가입을 막지 않는다). */
data class NiceConfig(val available: Boolean, val required: Boolean)

/** 본인확인 결과(표시용) — [phone] 은 가운데를 가린 값이라 저장·전송하지 않는다. 실제 값은 서버가 [token] 에서 꺼낸다. */
data class NiceIdentity(val token: String, val name: String, val phone: String)

/** 가입 직후 회원유형 서류 제출 결과 — 웹 AppRouter submitDocs 의 false / true / "approved". */
enum class RoleSubmit { NONE, SUBMITTED, APPROVED }
