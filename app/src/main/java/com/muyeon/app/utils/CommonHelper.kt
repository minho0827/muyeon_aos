package com.muyeon.app.utils

object CommonHelper {
    fun filterDoubleInput(newValue: String, oldValue: String): String {
        // Remove all non-digit and non-decimal characters
        var filteredValue = newValue.filter { it.isDigit() || it == '.' }

        // Handle leading zeros
        if (filteredValue.length > 1 && filteredValue.startsWith('0') && !filteredValue.startsWith("0.")) {
            filteredValue = filteredValue.removePrefix("0")
        }

        // Ensure there is at most one decimal point
        if (filteredValue.count { it == '.' } > 1) {
            return oldValue
        }

        return filteredValue
    }
}