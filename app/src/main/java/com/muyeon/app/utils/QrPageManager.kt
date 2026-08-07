package com.muyeon.app.utils

object QrPageManager {
    private var qrPageValue: String? = null

    fun save(value: String) {
        qrPageValue = value
    }

    fun getValue(): String? = qrPageValue
    fun hasValue(): Boolean = !qrPageValue.isNullOrEmpty()

    fun clear() {
        qrPageValue = null
    }
}
