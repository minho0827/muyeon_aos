package com.muyeon.app.utils

enum class WebMessageStatus(val value: Int, val message: String) {
    SUCCESS(200, "Success"),
    FAILED(400, "Failed"),
    NOT_FOUND(404, "Not found"),
    NOT_AUTHENTICATED(401, "Not authenticated"),
    SERVER_ERROR(500, "Internal server error");
    companion object {
        fun fromCode(code: Int): WebMessageStatus? = entries.find { it.value == code }
    }
}
