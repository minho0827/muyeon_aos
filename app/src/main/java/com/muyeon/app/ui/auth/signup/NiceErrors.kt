package com.muyeon.app.ui.auth.signup

import com.muyeon.app.R

/** 표준창이 돌려준 niceError 코드 → 문구(웹 niceIdentity.js NICE_ERROR_MESSAGE, 모르는 코드는 FAILED). */
object NiceErrors {
    fun res(code: String): Int = when (code) {
        "CANCELLED" -> R.string.auth_nice_cancelled
        "EXPIRED" -> R.string.auth_nice_expired
        "ALREADY_USED" -> R.string.auth_nice_already_used
        "NOT_FOUND" -> R.string.auth_nice_not_found
        else -> R.string.auth_nice_failed
    }
}
