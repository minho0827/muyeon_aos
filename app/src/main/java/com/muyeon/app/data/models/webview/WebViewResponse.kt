package com.muyeon.app.data.models.webview

import com.muyeon.app.utils.WebMessageStatus

data class WebViewResponse<T>(
    val data: T,
    val message: String,
    val status: Int
) {
    companion object {
        fun <T> success(data: T): WebViewResponse<T> {
            return WebViewResponse(
                data = data,
                message = WebMessageStatus.SUCCESS.message,
                WebMessageStatus.SUCCESS.value
            )
        }
        fun <T> failed(data: T): WebViewResponse<T> {
            return WebViewResponse(
                data = data,
                message = WebMessageStatus.FAILED.message,
                WebMessageStatus.FAILED.value
            )
        }
        fun <T> notAuthenticated(data: T): WebViewResponse<T> {
            return WebViewResponse(
                data = data,
                message = WebMessageStatus.NOT_AUTHENTICATED.message,
                WebMessageStatus.NOT_AUTHENTICATED.value
            )
        }
        fun <T> serverError(data: T): WebViewResponse<T> {
            return WebViewResponse(
                data = data,
                message = WebMessageStatus.SERVER_ERROR.message,
                WebMessageStatus.SERVER_ERROR.value
            )
        }
        fun <T> error(message: String, status: Int, data: T): WebViewResponse<T> {
            return WebViewResponse(
                data = data,
                message = message,
                status
            )
        }

        fun <T> notFound(data: T): WebViewResponse<T> {
            return WebViewResponse(
                data = data,
                message = WebMessageStatus.NOT_FOUND.message,
                status = WebMessageStatus.NOT_FOUND.value
            )
        }
    }
}