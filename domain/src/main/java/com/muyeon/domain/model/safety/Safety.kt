package com.muyeon.domain.model.safety

/** 내가 차단한 회원. */
data class BlockedUser(val userId: Long, val name: String?, val image: String?, val blockedAt: String?)
